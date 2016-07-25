package com.beepcast.model.reservedCode.mobileUser;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.dbmanager.util.DateTimeFormat;
import com.beepcast.model.beepid.BeepIDBean;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.event.CatagoryBean;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.event.EventSupport;
import com.beepcast.model.event.ProcessBean;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.MobileUserService;
import com.beepcast.model.transaction.TransactionLogBean;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class MobileUserMenu {

  static final DLogContext lctx = new SimpleContext( "MobileUserMenu" );

  private int clientId;
  private String phone;

  private Vector transactions;
  private Vector processes = new Vector( 100 , 100 );
  private int processStep;
  private int nextExpectStep = 2;
  private int currentMenuItem;

  private class Step {

    // level 1 = catagory menu
    // level 2 = client menu
    // level 3 = event menu
    // level 4 = event
    // level 5 = go back

    int level;
    int zlevel;
    long catagoryID;
    long clientID;
    Vector catagories;
    Vector clients;
    Vector events;
    String response;
    String nextStep;
    boolean processed;
  }

  private Hashtable steps = new Hashtable( 100 );

  private EventSupport eventSupport = new EventSupport( null );

  public MobileUserMenu( int clientId , String phone ) {
    this.clientId = clientId;
    this.phone = phone;
  }

  public String getMenu() throws IOException {

    // set three months time frame
    Date toDate = new Date();
    Calendar c = new GregorianCalendar();
    c.setTime( toDate );
    c.add( Calendar.MONTH , -3 );
    Date fromDate = c.getTime();
    DLog.debug(
        lctx ,
        "Prepare get menu , dig out into "
            + "transaction log table with time frame : "
            + DateTimeFormat.convertToString( fromDate ) + " till "
            + DateTimeFormat.convertToString( toDate ) );

    /*-------------------------
      get transaction log records
    -------------------------*/
    String criteria = " phone = '" + StringEscapeUtils.escapeSql( phone )
        + "' ";
    try {
      transactions = new TransactionLogService().select( fromDate , toDate ,
          criteria );
      transactions = filterEvents( transactions );
    } catch ( Exception e ) {
      throw new IOException( "MobileUserMenu.getMenu(): " + e.getMessage() );
    }

    /*-------------------------
      if no transactions, tell them
    -------------------------*/
    if ( transactions.size() == 0 ) {
      String noTransMsg = "Sorry, your personal menu has no valid entries.";
      return noTransMsg;
    }

    /*-------------------------
      add initial menu
    -------------------------*/
    Vector catagories = getDistinctCatagories();
    if ( catagories.size() > 1 ) {
      addCatagoryMenu( catagories );
    } else {
      CatagoryBean catagory = (CatagoryBean) catagories.elementAt( 0 );
      Vector clients = getDistinctClients( catagory );
      if ( clients.size() > 1 ) {
        addClientMenu( catagory , clients );
      } else {
        ClientBean client = (ClientBean) clients.elementAt( 0 );
        Vector events = getDistinctEvents( catagory , client );
        if ( events.size() > 1 ) {
          addEventMenu( catagory , client , events );
        } else {
          // return now if only one event
          EventBean _event = (EventBean) events.elementAt( 0 );
          ProcessBean processSteps[] = eventSupport.extractProcessClob( _event ,
              false );
          return "<<<" + _event.getCodes() + ">>>\n"
              + processSteps[0].getResponse();
        }
      }
    }

    /*-------------------------
      iterate thru levels
    -------------------------*/
    nextIteration: for ( int i = 1 ; i <= steps.size() ; i++ ) {
      Step step = (Step) steps.get( "" + i );
      if ( !step.processed ) {
        if ( step.level == 1 )
          addClientMenus( step );
        else if ( step.level == 2 )
          addEventMenus( step );
        else if ( step.level == 3 )
          addEvents( step );
        step.processed = true;
        continue nextIteration;
      }
    }

    /*-------------------------
      resolve zlevels
    -------------------------*/
    resolveZlevels();

    /*-------------------------
      create temporary event
    -------------------------*/
    EventBean _event = createTemporaryEvent();

    /*-------------------------
      create transaction object
    -------------------------*/
    createTransactionObject( _event );

    /*-------------------------
      return process step 1 response
    -------------------------*/
    return ( (ProcessBean) processes.elementAt( 0 ) ).getResponse();

  } // getMenu()

  private void addClientMenus( Step prevStep ) {
    Vector catagories = prevStep.catagories;
    currentMenuItem = 0;

    /*----------------------------
      add client menus
    ----------------------------*/
    try {
      for ( int i = 0 ; i < catagories.size() ; i++ ) {
        CatagoryBean catagory = (CatagoryBean) catagories.elementAt( i );
        Vector clients = getDistinctClients( catagory );
        if ( clients.size() > 1 )
          addClientMenu( catagory , clients );
        else {
          ClientBean client = (ClientBean) clients.elementAt( 0 );
          Vector events = getDistinctEvents( catagory , client );
          if ( events.size() > 1 )
            addEventMenu( catagory , client , events );
          else {
            EventBean _event = (EventBean) events.elementAt( 0 );
            addEvent( _event , 1 );
          }
        }
      }
    } catch ( Exception e ) {
    }

    /*----------------------------
      add separater
    ----------------------------*/
    addVarStep();
  } // addClientMenus()

  private void addEventMenus( Step prevStep ) {
    Vector clients = prevStep.clients;
    currentMenuItem = 0;

    /*----------------------------
      add event menus
    ----------------------------*/
    try {
      CatagoryBean catagory = new CatagoryBean().select( prevStep.catagoryID );
      for ( int i = 0 ; i < clients.size() ; i++ ) {
        ClientBean client = (ClientBean) clients.elementAt( i );
        Vector events = getDistinctEvents( catagory , client );
        if ( events.size() > 1 )
          addEventMenu( catagory , client , events );
        else {
          EventBean _event = (EventBean) events.elementAt( 0 );
          addEvent( _event , 2 );
        }
      }
    } catch ( Exception e ) {
    }

    /*----------------------------
      add "go back" step
    ----------------------------*/
    Step goBackStep = findStep( 1 ); // catagory menu step
    if ( goBackStep != null )
      addGoBack( goBackStep );

    /*----------------------------
      add separater
    ----------------------------*/
    addVarStep();
  } // addEventMenus()

  private void addEvents( Step prevStep ) {

    Vector events = prevStep.events;

    currentMenuItem = 0;

    /*----------------------------
      add events
    ----------------------------*/
    for ( int i = 0 ; i < events.size() ; i++ ) {
      EventBean _event = (EventBean) events.elementAt( i );
      addEvent( _event , 3 );
    }

    /*----------------------------
      add "go back" step
    ----------------------------*/
    Step goBackStep = findStep( 2 , prevStep.catagoryID ); // client menu step
    if ( goBackStep == null )
      goBackStep = findStep( 1 ); // catagory menu step
    if ( goBackStep != null )
      addGoBack( goBackStep );

    /*----------------------------
      add separater
    ----------------------------*/
    addVarStep();

  } // addEvents()

  private void addCatagoryMenu( Vector catagories ) {

    String type = "CODE";
    String response = "Reply a-"
        + new Character( (char) ( 'a' + ( catagories.size() - 1 ) ) )
            .toString() + "\n";
    String rfa = "";

    try {
      for ( int i = 0 ; i < catagories.size() ; i++ ) {
        CatagoryBean catagory = (CatagoryBean) catagories.elementAt( i );
        response += new Character( (char) ( 'a' + i ) ).toString() + ") "
            + catagory.getCatagoryName() + "\n";
      }
    } catch ( Exception e ) {
    }

    addProcess( type , "" , response , rfa , "" + nextExpectStep );

    Step step = new Step();
    step.level = 1; // catagory menu
    step.catagories = catagories;
    step.response = response;
    step.nextStep = "" + nextExpectStep;
    steps.put( "" + processStep , step );

    nextExpectStep += ( catagories.size() + 1 );

  } // addCatagoryMenu()

  private void addClientMenu( CatagoryBean catagory , Vector clients ) {

    String type = "CODE";
    String response = "Reply a-"
        + new Character( (char) ( 'a' + ( clients.size() - 1 ) ) ).toString()
        + "\n";
    String rfa = "";
    String goBack = "";
    int addedSteps = 1;
    if ( processStep > 0 ) {
      type = "EXPECT";
      response = "";
      goBack = "z) go back\n";
      addedSteps = 2;
    }

    try {
      for ( int i = 0 ; i < clients.size() ; i++ ) {
        ClientBean client = (ClientBean) clients.elementAt( i );
        response += new Character( (char) ( 'a' + i ) ).toString() + ") "
            + client.getCompanyName() + "\n";
      }
    } catch ( Exception e ) {
    }
    response += goBack;

    addProcess( type , "" , response , rfa , "" + nextExpectStep );

    Step step = new Step();
    step.level = 2; // client menu
    step.catagoryID = catagory.getCatagoryID();
    step.clients = clients;
    step.response = response;
    step.nextStep = "" + nextExpectStep;
    steps.put( "" + processStep , step );

    nextExpectStep += ( clients.size() + addedSteps );

  } // addClientMenu()

  private void addEventMenu( CatagoryBean catagory , ClientBean client ,
      Vector events ) {

    String type = "CODE";
    String response = "Reply a-"
        + new Character( (char) ( 'a' + ( events.size() - 1 ) ) ).toString()
        + "\n";
    String rfa = "";
    String goBack = "";
    int addedSteps = 1;
    if ( processStep > 0 ) {
      type = "EXPECT";
      response = "";
      goBack = "z) go back\n";
      addedSteps = 2;
    }

    try {
      for ( int i = 0 ; i < events.size() ; i++ ) {
        EventBean _event = (EventBean) events.elementAt( i );
        response += new Character( (char) ( 'a' + i ) ).toString() + ") "
            + _event.getEventName() + "\n";
      }
    } catch ( Exception e ) {
    }
    response += goBack;

    addProcess( type , "" , response , rfa , "" + nextExpectStep );

    Step step = new Step();
    step.level = 3; // event menu
    step.catagoryID = catagory.getCatagoryID();
    step.clientID = client.getClientID();
    step.events = events;
    step.response = response;
    step.nextStep = "" + nextExpectStep;
    steps.put( "" + processStep , step );

    nextExpectStep += ( events.size() + addedSteps );

  } // addEventMenu()

  private void addEvent( EventBean _event , int zlevel ) {

    ProcessBean processSteps[] = eventSupport.extractProcessClob( _event ,
        false );

    String type = "EXPECT";
    String response = "<<<" + _event.getCodes() + ">>>\n"
        + processSteps[0].getResponse();
    String rfa = "(z to go back)";

    addProcess( type , "" , response , rfa , "END" );

    Step step = new Step();
    step.level = 4; // event
    step.zlevel = zlevel;
    step.catagoryID = _event.getCatagoryID();
    step.clientID = _event.getClientID();
    step.nextStep = "END";
    step.processed = true;
    steps.put( "" + processStep , step );

  } // addEvent()

  private void addGoBack( Step toStep ) {

    addProcess( "EXPECT" , "Z" , toStep.response , "" , toStep.nextStep );

    Step step = new Step();
    step.level = 5; // go back
    step.zlevel = toStep.level;
    step.catagoryID = toStep.catagoryID;
    step.clientID = toStep.clientID;
    step.processed = true;
    steps.put( "" + processStep , step );

  } // addGoBack()

  private void addVarStep() {

    addProcess( "VAR" , "" , "" , "" , "" );
    Step step = new Step();
    step.processed = true;
    steps.put( "" + processStep , step );

  } // addVarStep()

  private void addProcess( String type , String value , String response ,
      String rfa , String nextStep ) {

    processStep++;

    /*------------------------------
      setup process attributes
    ------------------------------*/
    String paramLabel = "VALUE=";
    if ( type.equals( "EXPECT" ) ) {
      if ( value == "" )
        value = new Character( (char) ( 'A' + currentMenuItem ) ).toString();
      currentMenuItem++;
    } else if ( type.equals( "CODE" ) ) {
      paramLabel = "(N/A)";
      value = "(N/A)";
    } else if ( type.equals( "VAR" ) ) {
      paramLabel = "NAMES=";
      value = "v" + processStep; // must be unique
      nextStep = "END";
    }

    /*------------------------------
      add process bean to vector
    ------------------------------*/
    ProcessBean _process = new ProcessBean();
    _process.setStep( "" + processStep );
    _process.setType( type );
    _process.setParamLabel( paramLabel );
    String names[] = new String[1];
    names[0] = value.toUpperCase();
    _process.setNames( names );
    _process.setResponse( response );
    _process.setNextStep( nextStep );
    _process.setRfa( rfa );
    processes.addElement( _process );

  } // addProcess()

  private void resolveZlevels() {

    /*--------------------------
      replace "END" with correct "go back" step
    --------------------------*/
    nextLoop: while ( true ) {
      for ( int i = 1 ; i <= steps.size() ; i++ ) {
        Step step = (Step) steps.get( "" + i );
        if ( step.level == 4 && step.nextStep.equals( "END" ) ) {
          boolean found = false;
          for ( int j = 1 ; j <= steps.size() ; j++ ) {
            Step zstep = (Step) steps.get( "" + j );
            if ( zstep.level == 5 && zstep.zlevel == step.zlevel ) {
              if ( step.zlevel == 1 )
                found = true;
              else if ( step.zlevel == 2 && step.catagoryID == zstep.catagoryID )
                found = true;
              else if ( step.zlevel == 3 && step.catagoryID == zstep.catagoryID
                  && step.clientID == zstep.clientID )
                found = true;
              if ( found ) {
                ( (ProcessBean) processes.elementAt( i - 1 ) ).setNextStep( ""
                    + j );
                break; // for j
              }
            }
          }
          if ( !found ) {
            Step goBackStep = null;
            if ( step.zlevel == 1 )
              goBackStep = findStep( 1 ); // catagory menu step
            else if ( step.zlevel == 2 )
              goBackStep = findStep( 2 , step.catagoryID ); // client menu step
            else if ( step.zlevel == 3 )
              goBackStep = findStep( 3 , step.catagoryID , step.clientID ); // event
            // menu
            // step
            if ( goBackStep != null ) {
              addGoBack( goBackStep );
              addVarStep();
              continue nextLoop;
            }
          }
        }
      }
      break; // while
    }

  } // resolveZlevels()

  private Step findStep( int level ) {

    for ( int i = 1 ; i <= steps.size() ; i++ ) {
      Step step = (Step) steps.get( "" + i );
      if ( step.level == level )
        return step;
    }

    // step not found
    return null;

  } // findStep(level)

  private Step findStep( int level , long catagoryID ) {

    for ( int i = 1 ; i <= steps.size() ; i++ ) {
      Step step = (Step) steps.get( "" + i );
      if ( step.level == level )
        if ( step.catagoryID == catagoryID )
          return step;
    }

    return null;

  } // findStep(level,catagoryID)

  private Step findStep( int level , long catagoryID , long clientID ) {

    for ( int i = 1 ; i <= steps.size() ; i++ ) {
      Step step = (Step) steps.get( "" + i );
      if ( step.level == level )
        if ( step.catagoryID == catagoryID )
          if ( step.clientID == clientID )
            return step;
    }

    return null;

  } // findStep(level,catagoryID,clientID)

  private Vector getDistinctCatagories() throws IOException {
    Hashtable catagories = new Hashtable( 100 );
    try {
      for ( int i = 0 ; i < transactions.size() ; i++ ) {
        TransactionLogBean trans = (TransactionLogBean) transactions
            .elementAt( i );
        long catagoryID = trans.getCatagoryID();
        if ( !catagories.containsKey( "" + catagoryID ) ) {
          CatagoryBean catagory = new CatagoryBean().select( catagoryID );
          catagories.put( "" + catagoryID , catagory );
        }
      }
    } catch ( Exception e ) {
      throw new IOException( "MobileUserMenu.getDistinctCatagories(): "
          + e.getMessage() );
    }
    return new Vector( catagories.values() );
  } // getDistinctCatagories()

  private Vector getDistinctClients( CatagoryBean catagory ) throws IOException {
    EventService eventService = new EventService();
    Hashtable clients = new Hashtable( 100 );
    try {
      for ( int i = 0 ; i < transactions.size() ; i++ ) {
        TransactionLogBean trans = (TransactionLogBean) transactions
            .elementAt( i );
        long catagoryID = trans.getCatagoryID();
        if ( catagoryID == catagory.getCatagoryID() ) {
          long clientID = trans.getClientID();
          String mobileMenuBrandName = ( eventService.select( trans
              .getEventID() ) ).getMobileMenuBrandName();
          String clientKey = "" + clientID + ":" + mobileMenuBrandName;
          if ( !clients.containsKey( clientKey ) ) {
            ClientService clientService = new ClientService();
            ClientBean client = clientService.select( clientID );
            if ( !mobileMenuBrandName.equals( "" ) ) {
              client.setCompanyName( mobileMenuBrandName );
            }
            clients.put( clientKey , client );
          }
        }
      }
    } catch ( Exception e ) {
      throw new IOException( "MobileUserMenu.getDistinctClients(): "
          + e.getMessage() );
    }
    return new Vector( clients.values() );
  } // getDistinctClients()

  private Vector getDistinctEvents( CatagoryBean catagory , ClientBean client )
      throws IOException {

    EventService eventService = new EventService();
    Hashtable events = new Hashtable( 100 );

    try {
      for ( int i = 0 ; i < transactions.size() ; i++ ) {
        TransactionLogBean trans = (TransactionLogBean) transactions
            .elementAt( i );
        long catagoryID = trans.getCatagoryID();
        String catagoryName = new CatagoryBean().select( catagoryID )
            .getCatagoryName();
        if ( catagoryID == catagory.getCatagoryID() ) {
          long clientID = trans.getClientID();
          if ( clientID == client.getClientID() ) {
            long eventID = trans.getEventID();
            String mobileMenuBrandName = ( eventService.select( eventID ) )
                .getMobileMenuBrandName();
            if ( mobileMenuBrandName.equals( "" )
                || mobileMenuBrandName.equals( client.getCompanyName() ) ) {
              String eventKey = "" + eventID;
              String code = trans.getCode();
              EventBean _event = eventService.select( eventID );
              String mobileMenuName = _event.getMobileMenuName();
              if ( catagoryName.equals( "BEEPID" ) ) {

                BeepIDBean beepIDBean = new BeepIDBean().select( code , true );
                int clientId = (int) beepIDBean.getClientID();
                String phone = beepIDBean.getPhone();
                MobileUserService mobileUserService = new MobileUserService();
                MobileUserBean mobileUserBean = mobileUserService.select(
                    clientId , phone );
                _event.setEventName( mobileUserBean.getName() + " "
                    + mobileUserBean.getLastName() );
                eventKey += "_" + code;

              } else if ( mobileMenuName != null && !mobileMenuName.equals( "" ) )
                _event.setEventName( mobileMenuName );
              if ( !events.containsKey( eventKey ) ) {
                _event.setCodes( code );
                events.put( eventKey , _event );
              }
            }
          }
        }
      }
    } catch ( Exception e ) {
      throw new IOException( "MobileUserMenu.getDistinctEvents(): "
          + e.getMessage() );
    }

    return new Vector( events.values() );

  } // getDistinctEvents()

  private EventBean createTemporaryEvent() throws IOException {

    EventService eventService = new EventService();

    /*----------------------------
      build process clob
    ----------------------------*/
    ProcessBean processSteps[] = new ProcessBean[processes.size()];
    for ( int i = 0 ; i < processSteps.length ; i++ )
      processSteps[i] = (ProcessBean) processes.elementAt( i );
    String processClob = eventSupport.buildProcessClob( processSteps );

    EventBean _event = null;
    try {

      /*----------------------------
        delete old temporary events
      ----------------------------*/
      while ( ( _event = eventSupport.select( "mobileUserMenu" , phone ) ) != null ) {
        eventService.delete( _event );
      }

      /*----------------------------
        create new temporary event
      ----------------------------*/
      _event = new EventBean();
      _event.setEventName( phone + ( Math.random() * 1000 ) );
      _event.setStartDate( new Date() );
      _event.setEndDate( new Date() );
      _event.setRemindDate( new Date() );
      _event.setRemindFreq( "NEVER" );
      _event.setCatagoryID( 0 );
      _event.setClientID( 0 );
      _event.setNumCodes( 1 );
      _event.setCodeLength( 5 );
      _event.setCodes( "?MENU" );
      _event.setProcess( processClob );
      _event.setComment( "mobileUserMenu" );

      // get eventID from insert
      _event.setEventID( eventService.insert( _event ) );

    } catch ( Exception e ) {
      throw new IOException( "MobileUserMenu.createTemporaryEvent(): "
          + e.getMessage() );
    }

    return _event;
  } // createTemporaryEvent()

  private void createTransactionObject( EventBean _event ) throws IOException {

    try {
      /*-----------------------
        delete old transaction
      -----------------------*/
      TransactionQueueBean transObj = new TransactionQueueService()
          .select( phone );
      if ( transObj != null ) {
        new TransactionQueueService().delete( transObj );
      }
      transObj = new TransactionQueueBean();

      /*-----------------------
        create new transaction
      -----------------------*/
      transObj.setEventID( _event.getEventID() );
      transObj.setClientID( _event.getClientID() );
      transObj.setDateTm( new Date() );
      transObj.setPhone( phone );
      transObj.setNextStep( 2 );
      transObj.setMessageCount( 1 );
      transObj.setCode( "?MENU" );
      transObj.setParams( "" );
      new TransactionQueueService().insert( transObj );

    } catch ( Exception e ) {
      throw new IOException( "MobileUserMenu.createTransactionObject(): "
          + e.getMessage() );
    }

  } // createTransactionObject()

  private Vector filterEvents( Vector transactions ) throws IOException {

    Vector v = new Vector( 100 , 100 );

    EventService eventService = new EventService();

    for ( int i = 0 ; i < transactions.size() ; i++ ) {

      TransactionLogBean transLogBean = (TransactionLogBean) transactions
          .elementAt( i );
      if ( transLogBean == null ) {
        continue;
      }

      // read and verify trans log bean code
      String transLogBeanCode = transLogBean.getCode();
      if ( StringUtils.isBlank( transLogBeanCode ) ) {
        continue;
      }

      // verify active event
      int eventId = (int) transLogBean.getEventID();
      if ( eventId < 1 ) {
        continue;
      }
      EventBean eventBean = eventService.select( eventId );
      if ( eventBean == null ) {
        continue;
      }

      // filter out events that are disabled from this menu
      if ( !eventBean.getMobileMenuEnabled() ) {
        continue;
      }

      // filter out expired events
      String strCodes = eventBean.getCodes();
      if ( StringUtils.isBlank( strCodes ) ) {
        continue;
      }
      String[] arrCodes = StringUtils.split( strCodes , "," );
      if ( ( arrCodes == null ) || ( arrCodes.length < 1 ) ) {
        continue;
      }

      // trans log code must be contains from event codes
      boolean validCode = false;
      for ( int j = 0 ; j < arrCodes.length ; j++ ) {
        if ( arrCodes[j].equals( transLogBeanCode ) ) {
          validCode = true;
          break;
        }
      }
      if ( !validCode ) {
        continue;
      }

      // store as list of clean trans log bean
      v.addElement( transLogBean );
    }

    DLog.debug( lctx , "Filtered trans log found total " + v.size()
        + " record(s) are ready to process" );

    // success
    return v;
  }

}
