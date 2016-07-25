package com.beepcast.model.mobileUser;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.model.beepcode.BeepcodeBean;
import com.beepcast.model.beepcode.BeepcodeService;
import com.beepcast.model.event.CatagoryBean;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.event.EventSupport;
import com.beepcast.model.event.ProcessBean;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.model.transaction.BogusRequestService;
import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.beepcast.model.user.UserBean;
import com.beepcast.router.RouterApp;
import com.beepcast.util.Util;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class MobileUserSupport {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "MobileUserSupport" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private DatabaseLibrary dbLib = null;
  private DBManagerApp dbMan = null;
  private BillingApp billingApp = null;
  private ClientApp clientApp = null;
  private ProviderApp providerApp = null;
  private RouterApp routerApp = null;

  private EventService eventService = null;
  private TransactionQueueService transQueueService = null;
  private TransactionLogService transLogService = null;
  private BogusRequestService bogusReqService = null;

  private GatewayLogService gatewayLogService = null;
  private MobileUserService mobileUserService = null;

  private TransactionProcessBasic trans;
  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public MobileUserSupport() {
    init();
  }

  public MobileUserSupport( TransactionProcessBasic trans ) {
    init();
    if ( trans != null ) {
      this.trans = trans;
      this.log = trans.log();
    }
  }

  private void init() {

    dbLib = DatabaseLibrary.getInstance();
    dbMan = DBManagerApp.getInstance();
    billingApp = BillingApp.getInstance();
    clientApp = ClientApp.getInstance();
    providerApp = ProviderApp.getInstance();
    routerApp = RouterApp.getInstance();

    eventService = new EventService();
    transQueueService = new TransactionQueueService();
    transLogService = new TransactionLogService();
    bogusReqService = new BogusRequestService();

    gatewayLogService = new GatewayLogService();
    mobileUserService = new MobileUserService();

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public MobileUserBean persistMobileUser( int clientId , String phoneNumber ) {
    MobileUserBean mobileUserBean = null;
    // generate mobileUser based on phoneNumber
    mobileUserBean = selectMobileUser( clientId , phoneNumber );
    if ( mobileUserBean == null ) {
      // create mobile user if found empty
      mobileUserBean = createMobileUser( clientId , phoneNumber , null , null );
      if ( mobileUserBean != null ) {
        log.debug( "Created new mobile user profile , phoneNumber = "
            + phoneNumber );
      } else {
        log.warning( "Failed to create new mobile user profile "
            + ", phoneNumber = " + phoneNumber );
      }
    } else {
      log.debug( "Defined mobile user profile , phoneNumber = " + phoneNumber
          + " , last access code = "
          + StringEscapeUtils.escapeJava( mobileUserBean.getLastCode() ) );
    }
    return mobileUserBean;
  }

  public MobileUserBean selectMobileUser( int clientId , String phoneNumber ) {
    MobileUserBean mobileUserBean = mobileUserService.select( clientId ,
        phoneNumber );
    if ( mobileUserBean == null ) {
      log.warning( "Found empty mobile user , with phoneNumber = "
          + phoneNumber );
    } else {
      log.debug( "Successfully find mobile user , with phoneNumber = "
          + mobileUserBean.getPhone() );
    }
    return mobileUserBean;
  }

  public EventBean updateProfile( TransactionInputMessage imsg ,
      TransactionQueueBean transQueue , ProcessBean processBean ) {
    EventBean pendingEvent = null;

    // identify mobile user
    MobileUserBean mobileUserBean = (MobileUserBean) imsg
        .getMessageParam( "mobileUserBean" );
    if ( mobileUserBean == null ) {
      log.warning( "Failed to fetch mobile user from input message" );
      return pendingEvent;
    }

    // update profile
    if ( transQueue.isUpdateProfile() ) {
      String params = transQueue.getParams();
      StringTokenizer st = new StringTokenizer( params , "," );
      int numParams = st.countTokens();
      try {
        for ( int i = 0 ; i < numParams ; i++ ) {
          String param = st.nextToken().trim();
          StringTokenizer st2 = new StringTokenizer( param , "=" );
          String paramName = st2.nextToken().trim();
          // update email address
          if ( paramName.equals( "EMAIL" ) ) {
            log.debug( "Found param name as EMAIL" );
            String paramValue = st2.nextToken().trim();
            // valid email address
            if ( Util.validEmailAddress( paramValue ) ) {

              // update mobile user profile
              log.debug( "Updating mobile user's email" );
              mobileUserBean.setEmail( paramValue );
              if ( mobileUserService.update( mobileUserBean ) ) {
                log.debug( "Successfully updated mobile user's email = "
                    + mobileUserBean.getEmail() );
              } else {
                log.warning( "Failed to update mobile user's email" );
              }

              // reload pending event, and email step
              transQueue.setUpdateProfile( false );
              pendingEvent = eventService.select( transQueue
                  .getPendingEventID() );
              transQueue.setEventID( pendingEvent.getEventID() );
              transQueue.setCode( transQueue.getPendingCode() );
              ProcessBean processSteps[] = new EventSupport( null )
                  .extractProcessClob( pendingEvent );
              for ( int j = 0 ; j < processSteps.length ; j++ ) {
                ProcessBean p = processSteps[j];
                if ( p.getType().equals( "EMAIL" ) ) {
                  for ( int k = 0 ; k < processSteps.length ; k++ ) {
                    ProcessBean p2 = processSteps[k];
                    if ( p2.getNextStep().equals( p.getStep() ) ) {
                      transQueue.setNextStep( k + 1 );
                      break;
                    }
                  }
                  break;
                }
              }
            }
            // invalid email address
            else {
              processBean.setNextStep( "2" ); // 2 = get email address
              processBean.setNextType( "VAR" );
              processBean.setResponse( "You entered an invalid email address. "
                  + "Format should be:\nxxx@yyy.zzz\n"
                  + "Please reply with your valid email address, "
                  + "or reply z to exit." );
              transQueue.setParams( "" );
            }
          }
        }
      } catch ( Exception e ) {
        log.warning( "Failed to update mobile user profile , " + e );
      }
    }

    return pendingEvent;
  } // updateProfile()

  public UserBean createMobileUserUser( int clientId , String phone )
      throws IOException {

    String name = "";
    String email = "";

    /*--------------------------
      get mobile user info
    --------------------------*/
    MobileUserBean mobileUserBean = selectMobileUser( clientId , phone );
    if ( mobileUserBean != null ) {
      name = mobileUserBean.getName();
      email = mobileUserBean.getEmail();
    }

    /*--------------------------
      setup roles
    --------------------------*/
    String roles[] = { "MOBILE_USER" };

    /*--------------------------
      create user bean
    --------------------------*/
    UserBean user = new UserBean();
    user.setUserID( "mobileUser" );
    user.setPassword( "" );
    user.setRoles( roles );
    user.setName( name );
    user.setPhone( phone );
    user.setEmail( email );
    user.setClientID( 0 );

    return user;

  } // createMobileUserUser()

  public String sendPassword( int clientId , String phone ) throws IOException {

    /*------------------------
      validate phone
    ------------------------*/
    if ( phone == null || phone.length() < 8 || !StringUtils.isNumeric( phone ) )
      return "ERROR: Invalid phone number.";
    if ( phone.length() == 8 )
      phone = "+65" + phone;
    else if ( !phone.startsWith( "+" ) )
      phone = "+" + phone;

    try {
      /*------------------------
        authenticate mobile user
      ------------------------*/
      MobileUserBean mobileUser = selectMobileUser( clientId , phone );

      // if new mobile user
      if ( mobileUser == null ) {
        mobileUser = createMobileUser( clientId , phone , null , null );
      }

      /*-------------------------
        send password via SMS
      -------------------------*/
      String sms = "Hello " + mobileUser.getName() + ". "
          + "Your password is [" + mobileUser.getPassword() + "]. "
          + "Thank you for using BEEPCAST.";
      routerApp.sendMtMessage(
          TransactionMessageFactory.generateMessageId( "INT" ) ,
          MessageType.TEXT_TYPE , 1 , sms , 1.0 , phone , null , 0 , 0 , null ,
          0 , 0 , new Date() , null );

      // success
      return "OK";

      /*-------------------------
        handle exception
      -------------------------*/
    } catch ( Exception e ) {
      String msg = "MobileUserSupport.sendPassword(): " + e.getMessage();
      System.out.println( msg );
      throw new IOException( msg );
    }

  } // sendPassword()

  public void sendFirstTimeMessage( TransactionQueueBean transObj ,
      boolean simulation ) throws IOException {

    EventSupport es = new EventSupport( null );

    /*-------------------------
      setup first time message
    -------------------------*/
    BeepcodeBean beepcode = new BeepcodeService().select( "FIRSTTIM" );
    EventBean _event = eventService.select( beepcode.getEventID() );
    ProcessBean processSteps[] = es.extractProcessClob( _event , false );
    ProcessBean _process = processSteps[0];
    String message = _process.getResponse();

    /*-------------------------
      setup message time
    -------------------------*/
    Calendar c = new GregorianCalendar();
    c.setTime( new Date() );
    c.add( Calendar.MINUTE , 60 ); // 1 hour
    Date dateTm = c.getTime();

  } // sendFirstTimeMessage()

  public MobileUserBean createMobileUser( int clientId , String phoneNumber ,
      String password , String lastCode ) {

    MobileUserBean mobileUserBean = MobileUserFactory.createMobileUserBean(
        clientId , phoneNumber );

    if ( mobileUserBean == null ) {
      log.warning( "Failed to create a new mobile user "
          + ", found empty clientId and/or phoneNumber" );
      return mobileUserBean;
    }

    // generate new password if empty
    if ( ( password == null ) || ( password.equals( "" ) ) ) {
      password = RandomStringUtils.randomNumeric( 6 );
      log.debug( "Generated mobile user password = " + password );
    }

    mobileUserBean.setPassword( password );
    mobileUserBean.setLastCode( lastCode );

    log.debug( "Creating new mobile user with clientId = " + clientId
        + " , phoneNumber = " + phoneNumber + " , password = " + password
        + " , lastCode = " + lastCode );

    if ( mobileUserService.insert( mobileUserBean ) ) {
      log.info( "Successfully insert a new mobile user "
          + ", with phoneNumber = " + phoneNumber );
    } else {
      mobileUserBean = null;
      log.warning( "Failed to create a new mobile user "
          + ", failed to insert into the table" );
    }

    return mobileUserBean;
  } // createMobileUser()

  public boolean updateMobileUserEmail( int clientId , String phone ,
      String emailAddress , TransactionQueueBean transObj ) {
    boolean result = false;
    log.debug( "Updating mobile user email" );
    // validate mobile user
    MobileUserBean mobileUserBean = selectMobileUser( clientId , phone );
    if ( mobileUserBean == null ) {
      mobileUserBean = createMobileUser( clientId , phone , null , null );
      if ( transObj != null ) {
        transObj.setNewUser( true );
      }
    }
    if ( mobileUserBean == null ) {
      log.warning( "Failed to update mobile user email "
          + ", can not persist the mobile user bean" );
      return result;
    }
    // update email address
    if ( Util.validEmailAddress( emailAddress ) ) {
      mobileUserBean.setEmail( emailAddress );
      result = mobileUserService.update( mobileUserBean );
    }
    if ( result ) {
      log.debug( "Successfully updated mobile user email" );
    } else {
      log.warning( "Failed to update mobile user email" );
    }
    return result;
  }

  public boolean updateMobileUserName( int clientId , String phone ,
      String firstName , String lastName , TransactionQueueBean transObj ) {
    boolean result = false;
    log.debug( "Updating mobile user name" );
    // validate mobile user
    MobileUserBean mobileUserBean = selectMobileUser( clientId , phone );
    if ( mobileUserBean == null ) {
      mobileUserBean = createMobileUser( clientId , phone , null , null );
      if ( transObj != null ) {
        transObj.setNewUser( true );
      }
    }
    if ( mobileUserBean == null ) {
      log.warning( "Failed to update mobile user name "
          + ", can not persist the mobile user bean" );
      return result;
    }
    // update email address
    if ( !StringUtils.isBlank( firstName ) ) {
      mobileUserBean.setName( firstName );
      mobileUserBean.setLastName( lastName );
      result = mobileUserService.update( mobileUserBean );
    }
    if ( result ) {
      log.debug( "Successfully updated mobile user name" );
    } else {
      log.warning( "Failed to update mobile user name" );
    }
    return result;
  }

  public boolean updateMobileUserIc( int clientId , String phone , String ic ,
      TransactionQueueBean transObj ) {
    boolean result = false;
    log.debug( "Updating mobile user ic" );
    // validate mobile user
    MobileUserBean mobileUserBean = selectMobileUser( clientId , phone );
    if ( mobileUserBean == null ) {
      mobileUserBean = createMobileUser( clientId , phone , null , null );
      if ( transObj != null ) {
        transObj.setNewUser( true );
      }
    }
    if ( mobileUserBean == null ) {
      log.warning( "Failed to update mobile user ic "
          + ", can not persist the mobile user bean" );
      return result;
    }
    // update email address
    if ( !StringUtils.isBlank( ic ) ) {
      mobileUserBean.setIc( ic );
      result = mobileUserService.update( mobileUserBean );
    }
    if ( result ) {
      log.debug( "Successfully updated mobile user ic" );
    } else {
      log.warning( "Failed to update mobile user ic" );
    }
    return result;
  }

  public boolean updateMobileUserLastCode( MobileUserBean mobileUserBean ,
      String lastCode ) {
    boolean result = false;
    if ( mobileUserBean == null ) {
      return result;
    }
    if ( StringUtils.equals( mobileUserBean.getLastCode() , lastCode ) ) {
      return result;
    }
    log.debug( "Updating mobile user new lastCode : "
        + mobileUserBean.getLastCode() + " -> " + lastCode );
    // update email address
    if ( !StringUtils.isBlank( lastCode ) ) {
      mobileUserBean.setLastCode( lastCode );
      result = mobileUserService.updateLastCode( mobileUserBean );
    }
    if ( result ) {
      log.debug( "Successfully updated mobile user new lastCode" );
    } else {
      log.warning( "Failed to update mobile user new lastCode" );
    }
    return result;
  }

  public String suggestedNickname( String attemptedNickname )
      throws IOException {

    int startOfNum = 0;
    int suffix = 0;
    final int MAX_ATTEMPS = 1000;

    /*-------------------------
      check for numeric suffix
    -------------------------*/
    for ( int i = attemptedNickname.length() - 1 ; i >= 0 ; i-- ) {
      String letter = attemptedNickname.substring( i , i + 1 );
      if ( !StringUtils.isNumeric( letter ) ) {
        startOfNum = i + 1;
        break;
      }
    }
    if ( startOfNum != attemptedNickname.length() )
      suffix = Integer.parseInt( attemptedNickname.substring( startOfNum ) );

    /*-------------------------
      get puffix
    -------------------------*/
    String prefix = attemptedNickname.substring( 0 , startOfNum );

    /*-------------------------
      return suggested nickname
    -------------------------*/
    for ( int i = 1 ; i <= MAX_ATTEMPS ; i++ ) {
      String newNickname = prefix + ( suffix + i );
      NicknameBean nicknameBean = new NicknameBean().select( newNickname );
      if ( nicknameBean == null )
        return newNickname;
    }

    // all attempts failed
    return null;

  } // suggestedNickname()

  public void createDialogEvent( long clientID ) throws IOException {

    /*-----------------------
      dialog event exists?
    -----------------------*/
    EventBean _event = eventService.select( "Web-SMS Dialog" , clientID );
    if ( _event != null )
      return;

    /*-----------------------
      setup event elements
    -----------------------*/
    Date startDate = new Date();
    Calendar c = new GregorianCalendar();
    c.setTime( startDate );
    c.add( Calendar.YEAR , 10 );
    Date endDate = c.getTime();

    String code = "D_" + clientID;
    long catagoryID = new CatagoryBean().select( "OTHER" ).getCatagoryID();

    /*-----------------------
      create event
    -----------------------*/
    _event = new EventBean();
    _event.setEventName( "Web-SMS Dialog" );
    _event.setClientID( clientID );
    _event.setCatagoryID( catagoryID );
    _event.setCodes( code );
    _event.setNumCodes( 1 );
    _event.setCodeLength( code.length() );
    _event.setStartDate( startDate );
    _event.setEndDate( endDate );
    _event.setRemindDate( startDate );
    _event.setRemindFreq( "NEVER" );
    _event
        .setComment( "DO NOT DELETE THIS EVENT.  This event is used to charge Web-SMS Dialog to the client." );
    _event.setProcessType( EventBean.ADVANCED_TYPE );
    _event.setProcess( "2:1^CODE^(N/A)^(N/A)^^2^~2^VAR^NAMES=^INPUT^^2^" );
    long eventID = eventService.insert( _event );

    /*-----------------------
      create beepcode
    -----------------------*/
    BeepcodeBean beepcode = new BeepcodeBean();
    beepcode.setCode( code );
    beepcode.setCodeLength( code.length() );
    beepcode.setEventID( eventID );
    beepcode.setClientID( clientID );
    beepcode.setCatagoryID( catagoryID );
    beepcode.setActive( true );
    beepcode.setReserved( true );
    beepcode.setLastHitDate( new Date() );
    new BeepcodeService().insert( beepcode );

  } // createDialogEvent()

} // eof
