package com.beepcast.model.transaction;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.dbmanager.common.ClientCommon;
import com.beepcast.dbmanager.common.EventCommon;
import com.beepcast.dbmanager.common.ModemNumberCommon;
import com.beepcast.dbmanager.common.ProviderCommon;
import com.beepcast.dbmanager.table.TClient;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.dbmanager.table.TEvent;
import com.beepcast.dbmanager.table.TModemNumberToClient;
import com.beepcast.dbmanager.table.TModemNumberToClients;
import com.beepcast.dbmanager.table.TProvider;
import com.beepcast.dbmanager.table.TProviderToEvent;
import com.beepcast.dbmanager.util.DateTimeFormat;
import com.beepcast.encrypt.EncryptApp;
import com.beepcast.keyword.ClientKeywordBean;
import com.beepcast.keyword.KeywordApp;
import com.beepcast.model.beepcode.BeepcodeBean;
import com.beepcast.model.beepcode.BeepcodeService;
import com.beepcast.model.beepcode.BeepcodeSupport;
import com.beepcast.model.beepid.BeepIDBean;
import com.beepcast.model.beepid.BeepIDSupport;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ResolvedTransactionQueueService {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "ResolvedTransactionQueueService" );

  public static final String CODE_TYPE_KEYWORD = "keyword";
  public static final String CODE_TYPE_BEEP_CODE = "beepcode";
  public static final String CODE_TYPE_BEEP_ID = "beepid";

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private boolean initialized;

  private DBManagerApp dbMan;
  private OnlinePropertiesApp oprops;
  private KeywordApp keywordApp;

  private final String keyPhoneNumber;

  private TransactionProcessBasic trans;
  private TransactionConf conf;
  private TransactionLog log;
  private TransactionSession session;

  private EventService eventService;
  private TransactionQueueService transQueueService;
  private TransactionLogService transLogService;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public ResolvedTransactionQueueService( TransactionProcessBasic trans ) {
    initialized = false;

    dbMan = DBManagerApp.getInstance();
    oprops = OnlinePropertiesApp.getInstance();
    keywordApp = KeywordApp.getInstance();

    EncryptApp encryptApp = EncryptApp.getInstance();
    keyPhoneNumber = encryptApp.getKeyValue( EncryptApp.KEYNAME_PHONENUMBER );

    if ( trans == null ) {
      DLog.warning( lctx , "Failed to initialized , found null trans" );
      return;
    }

    this.trans = trans;
    this.conf = trans.conf();
    this.log = trans.log();
    this.session = trans.session();

    eventService = new EventService();
    transQueueService = new TransactionQueueService();
    transLogService = new TransactionLogService();

    initialized = true;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionQueueBean execute( Map sessionParams ,
      TransactionInputMessage imsg , Object sessionLock ,
      boolean sessionPersist , String guestCode ) {
    TransactionQueueBean transQueueBeanResult = null;

    if ( !initialized ) {
      DLog.warning( lctx , "Failed to generate trans queue bean "
          + ", found not yet initialized" );
      return transQueueBeanResult;
    }

    // validate message object
    if ( imsg == null ) {
      log.warning( "Failed to generate trans queue bean "
          + ", found null input message" );
      return transQueueBeanResult;
    }

    // validate blank message content
    if ( StringUtils.isBlank( imsg.getMessageContent() ) ) {
      log.warning( "Failed to generate trans queue bean "
          + ", found a blank message" );
      return transQueueBeanResult;
    }

    // log it
    log.debug( "Resolving trans queue bean , with : sessionPersist = "
        + sessionPersist + " , guestCode = "
        + StringEscapeUtils.escapeJava( guestCode ) );

    // need to perform thread safe in the session mechanism
    sessionLock = ( sessionLock == null ) ? new Object() : sessionLock;
    synchronized ( sessionLock ) {

      // trap delta time
      long deltaTime = System.currentTimeMillis();

      // extract a guest code from message content
      if ( StringUtils.isBlank( guestCode ) ) {
        log.debug( "Found empty guest code "
            + ", trying get the one from message content" );
        guestCode = extractMessageCode( imsg.getMessageContent() ,
            imsg.getMessageType() );
      }
      if ( StringUtils.isBlank( guestCode ) ) {
        log.warning( "Failed to generate trans queue bean "
            + "found an empty guest code , assume as a blank message" );
        return transQueueBeanResult;
      }
      log.debug( "Defined guestCode = "
          + StringEscapeUtils.escapeJava( guestCode )
          + " from message : type = " + imsg.getMessageType() + " , content = "
          + StringEscapeUtils.escapeJava( imsg.getMessageContent() ) );

      // special case set message content for
      // guest code of "?MENU" or "X?"
      if ( guestCode.equals( "MENU" ) || guestCode.equals( "BEEPBACK" ) ) {
        guestCode = "?MENU";
        imsg.setMessageContent( guestCode );
        log.debug( "Found special case , force to change message content = "
            + imsg.getMessageContent() );
      }
      if ( guestCode.equals( "X?" ) ) {
        guestCode = "X";
        imsg.setMessageContent( guestCode );
        log.debug( "Found special case , force to change message content = "
            + imsg.getMessageContent() );
      }

      // resolve a guest code as : keyword , beepcode , or beepid type
      String codeType = null;
      {
        if ( codeType == null ) {
          if ( resolveKeyword( guestCode , imsg ) ) {
            codeType = CODE_TYPE_KEYWORD;
          }
        }
        if ( codeType == null ) {
          if ( resolveBeepCode( guestCode , imsg ) ) {
            codeType = CODE_TYPE_BEEP_CODE;
          }
        }
        if ( codeType == null ) {
          if ( resolveBeepId( guestCode , imsg ) ) {
            codeType = CODE_TYPE_BEEP_ID;
          }
        }
        if ( codeType == null ) {
          if ( resolveKeyword( imsg ) ) {
            codeType = CODE_TYPE_KEYWORD;
          }
        }
      }
      if ( codeType == null ) {
        log.debug( "Can not find any valid keyword , beep code or beep id "
            + "based on the message content" );
      } else {
        imsg.addMessageParam( "codeType" , codeType );
        log.debug( "Added input message param for codeType = " + codeType );
      }
      log.debug( "Defined code " + StringEscapeUtils.escapeJava( guestCode )
          + " as " + codeType );

      // get the latest transaction in the trans queue table
      TransactionQueueBean transQueueBeanCur = currentTransQueueBean( imsg
          .getOriginalAddress() );
      if ( transQueueBeanCur == null ) {
        log.debug( "Found empty last session profile" );
      } else {
        log.debug( "Found last session profile : queueId = "
            + transQueueBeanCur.getQueueId() + " , code = "
            + transQueueBeanCur.getCode() + " , phone = "
            + transQueueBeanCur.getPhone() + " , eventId = "
            + transQueueBeanCur.getEventID() + " , clientId = "
            + transQueueBeanCur.getClientID() + " , nextStep = "
            + transQueueBeanCur.getNextStep() + " , params = "
            + StringEscapeUtils.escapeJava( transQueueBeanCur.getParams() ) );
      }

      // when found unknown code type and empty last session
      // will check from mapping provider to event with
      // active post validation
      if ( ( codeType == null ) && ( transQueueBeanCur == null ) ) {
        if ( resolveFromProviderToEventMapping( imsg ) ) {
          log.debug( "Resolved code from provider to event mapping" );

          // verify if there is no need to store session function
          transQueueBeanResult = (TransactionQueueBean) imsg
              .getMessageParam( "pte.transQueueBean" );
          if ( transQueueBeanResult != null ) {
            log.debug( "Defined code with no session stored" );
            return transQueueBeanResult;
          }

          // restore guest code and code type from provider to event mapping
          String pteGuestCode = (String) imsg.getMessageParam( "pte.guestCode" );
          String pteCodeType = (String) imsg.getMessageParam( "pte.codeType" );
          if ( !StringUtils.isBlank( pteGuestCode )
              && !StringUtils.isBlank( pteCodeType ) ) {
            log.debug( "Replaced the guest code from provider "
                + "to event mapping : " + guestCode + " -> " + pteGuestCode );
            guestCode = pteGuestCode;
            codeType = pteCodeType;
            imsg.addMessageParam( "codeType" , codeType );
            log.debug( "Added input message param for codeType = " + codeType );
          }

        }
      }

      // check exchange event as the latest transaction
      if ( isExchageEvent( transQueueBeanCur ) ) {
        log.debug( "Found the last session profile has an exchange event" );
        transQueueBeanResult = transQueueBeanCur;
        return transQueueBeanResult;
      }

      // get expect client profile
      TClient expectClientBean = null;
      if ( imsg.getClientId() > 0 ) {
        expectClientBean = ClientCommon.getClient( imsg.getClientId() );
      }
      if ( expectClientBean != null ) {
        log.debug( "Defined expect client bean : id = "
            + expectClientBean.getClientId() + " , name = "
            + expectClientBean.getCompanyName() );
      }

      // get expect event profile
      TEvent expectEventBean = null;
      if ( imsg.getEventId() > 0 ) {
        expectEventBean = EventCommon.getEvent( imsg.getEventId() );
      }
      if ( expectEventBean != null ) {
        log.debug( "Defined expect event bean : id = "
            + expectEventBean.getEventId() + " , name = "
            + expectEventBean.getEventName() );
      }

      // try to resolve expect client profile from expect event profile
      if ( ( expectClientBean == null ) && ( expectEventBean != null ) ) {
        expectClientBean = ClientCommon.getClient( expectEventBean
            .getClientId() );
        if ( expectClientBean != null ) {
          log.debug( "Resolved expected client profile based on "
              + "expected event profile , defined expect client bean : id = "
              + expectClientBean.getClientId() + " , name = "
              + expectClientBean.getCompanyName() );
        }
      }

      // resolve dedicated modem to client bean based on
      // phone number and get expect client id
      if ( !StringUtils.isBlank( imsg.getDestinationAddress() ) ) {
        TClient expectClientBeanTmp = resolveDedicatedModemToClient(
            imsg.getDestinationAddress() , imsg );
        if ( expectClientBeanTmp != null ) {
          expectClientBean = expectClientBeanTmp;
          log.debug( "Resolved dedicated modem to client "
              + ", defined expect client bean : id = "
              + expectClientBean.getClientId() + " , name = "
              + expectClientBean.getCompanyName() );
        }
      }

      // if both expect client and event profile defined
      // than both must be matched as well
      if ( ( expectClientBean != null ) && ( expectEventBean != null )
          && ( expectClientBean.getClientId() != expectEventBean.getClientId() ) ) {
        log.warning( "Found conflict between expect client ( id = "
            + expectClientBean.getClientId() + " ) and event ( id = "
            + expectEventBean.getEventId() + " ) profile "
            + ", will follow the client one, disposed both expect "
            + "client and event profile from input message params" );
        expectClientBean = null;
        expectEventBean = null;
      }

      // store expect client and event profile into message params
      if ( expectClientBean != null ) {
        imsg.addMessageParam( TransactionMessageParam.HDR_EXPECT_CLIENT_BEAN ,
            expectClientBean );
        log.debug( "Added input message param for "
            + TransactionMessageParam.HDR_EXPECT_CLIENT_BEAN + " : id = "
            + expectClientBean.getClientId() + " , name = "
            + expectClientBean.getCompanyName() );
      }
      if ( expectEventBean != null ) {
        imsg.addMessageParam( TransactionMessageParam.HDR_EXPECT_EVENT_BEAN ,
            expectEventBean );
        log.debug( "Added input message param for "
            + TransactionMessageParam.HDR_EXPECT_EVENT_BEAN + " : id = "
            + expectEventBean.getEventId() + " , name = "
            + expectEventBean.getEventName() );
      }

      // differentiate the process between request code
      // and interaction
      if ( codeType != null ) {
        transQueueBeanResult = resolveMessageCode( guestCode , codeType ,
            transQueueBeanCur , expectClientBean , expectEventBean , imsg ,
            sessionParams , sessionPersist );
      } else {
        transQueueBeanResult = resolveMessageInteraction( guestCode , codeType ,
            transQueueBeanCur , expectClientBean , expectEventBean , imsg ,
            sessionParams , sessionPersist );
      }

      // trap delta time
      deltaTime = System.currentTimeMillis() - deltaTime;

      // log it
      log.debug( "Resolved session as message "
          + ( codeType != null ? "code" : "interaction" ) + " , took "
          + deltaTime + " ms" );

    } // end synchronized

    return transQueueBeanResult;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private String extractMessageCode( String messageContent , int messageType ) {
    String result = null;
    try {

      if ( messageContent == null ) {
        return result;
      }

      // when found text type message will only read alphanumeric
      // characters at the first word ( plus underscore and hypen )
      if ( messageType == MessageType.TEXT_TYPE ) {
        StringBuffer sbCode = null;
        int len = messageContent.length();
        for ( int idx = 0 ; idx < len ; idx++ ) {
          char ch = messageContent.charAt( idx );
          if ( ( ( ch >= 'A' ) && ( ch <= 'Z' ) )
              || ( ( ch >= 'a' ) && ( ch <= 'z' ) )
              || ( ( ch >= '0' ) && ( ch <= '9' ) ) || ( ch == '_' )
              || ( ch == '-' ) ) {
            if ( sbCode == null ) {
              sbCode = new StringBuffer();
            }
            sbCode.append( ch );
          } else {
            if ( sbCode != null ) {
              break;
            }
          }
        } // for ( int idx = 0 ; idx < len ; idx++ )
        if ( sbCode != null ) {
          result = sbCode.toString();
        }
        return result;
      }

      // when found as unicode type message will clean space characters only
      if ( messageType == MessageType.UNICODE_TYPE ) {
        StringBuffer sbCode = null;
        int len = messageContent.length();
        for ( int idx = 0 ; idx < len ; idx++ ) {
          char ch = messageContent.charAt( idx );
          if ( ( ch != ' ' ) && ( ch != '\t' ) && ( ch != '\r' )
              && ( ch != '\n' ) ) {
            if ( sbCode == null ) {
              sbCode = new StringBuffer();
            }
            sbCode.append( ch );
          } else {
            if ( sbCode != null ) {
              break;
            }
          }
        } // for ( int idx = 0 ; idx < len ; idx++ )
        if ( sbCode != null ) {
          result = sbCode.toString();
        }
        return result;
      }

    } catch ( Exception e ) {
      log.warning( "Failed to extract message code , " + e );
    }
    return result;
  }

  private boolean resolveKeyword( TransactionInputMessage imsg ) {
    return resolveKeyword( null , imsg );
  }

  private boolean resolveKeyword( String guestCode ,
      TransactionInputMessage imsg ) {
    boolean result = false;

    boolean feature = oprops.getBoolean(
        "Transaction.VerifyMessageCodeFromKeywordList" , false );
    if ( !feature ) {
      log.debug( "Failed to resolve keyword , found feature verify "
          + "message code from keyword is disabled" );
      return result;
    }

    String providerId = imsg.getOriginalProvider();
    if ( StringUtils.isBlank( providerId ) ) {
      log.debug( "Failed to resolve keyword , found blank provider id" );
      return result;
    }

    log.debug( "Trying to find matched client keyword map "
        + ", based on : providerId = " + providerId + " , guestCode = "
        + StringEscapeUtils.escapeJava( guestCode ) );

    ClientKeywordBean keywordBean = keywordApp.searchKeyword( providerId ,
        guestCode );
    if ( keywordBean == null ) {
      log.debug( "Failed to resolve keyword , can not find match keyword "
          + "any based on provider id = " + providerId + " and code = "
          + StringEscapeUtils.escapeJava( guestCode ) );
      return result;
    }

    if ( !keywordBean.isActive() ) {
      log.debug( "Failed to resolve keyword , found inactive keyword" );
      return result;
    }
    if ( keywordBean.isSuspend() ) {
      log.debug( "Failed to resolve keyword , found suspended keyword" );
      return result;
    }

    Date dateNow = new Date();
    Date dateStarted = keywordBean.getDateStarted();
    if ( dateStarted.getTime() > dateNow.getTime() ) {
      log.debug( "Failed to resolve keyword , found pending keyword" );
      return result;
    }

    if ( keywordBean.getClientId() < 1 ) {
      log.debug( "Failed to resolve keyword "
          + ", found unidentified client profile keyword" );
      return result;
    }
    if ( keywordBean.getEventId() < 1 ) {
      log.debug( "Failed to resolve keyword "
          + ", found unidentified event profile keyword" );
      return result;
    }

    result = true;
    log.debug( "Define a guest code as a keyword : providerId = "
        + keywordBean.getProviderId() + " , incomingNumber = "
        + keywordBean.getIncomingNumber() + " , code = "
        + StringEscapeUtils.escapeJava( keywordBean.getCode() )
        + " , clientId = " + keywordBean.getClientId() + " , eventId = "
        + keywordBean.getEventId() );

    imsg.addMessageParam( "keywordBean" , keywordBean );
    log.debug( "Added input message param for keywordBean : id = "
        + keywordBean.getId() + " , code = "
        + StringEscapeUtils.escapeJava( keywordBean.getCode() ) );

    return result;
  }

  private boolean resolveBeepCode( String guestCode ,
      TransactionInputMessage imsg ) {
    boolean result = false;
    if ( StringUtils.isBlank( guestCode ) ) {
      return result;
    }
    boolean feature = oprops.getBoolean(
        "Transaction.VerifyMessageCodeFromBeepCodeList" , false );
    if ( !feature ) {
      return result;
    }

    log.debug( "Trying to find guest code in the beepcode list" );

    BeepcodeBean beepcodeBean = new BeepcodeService().select( guestCode );
    if ( beepcodeBean == null ) {
      log.debug( "Failed to find guest code in the beepcode list" );
      return result;
    }
    if ( !beepcodeBean.getActive() ) {
      log.debug( "Found a guest code in the beepcode list "
          + ", but it's not active" );
      return result;
    }

    result = true;
    log.debug( "Define a guest code as a beepcode : code = "
        + StringEscapeUtils.escapeJava( beepcodeBean.getCode() )
        + " , clientId = " + beepcodeBean.getClientID() + " , eventId = "
        + beepcodeBean.getEventID() );

    imsg.addMessageParam( "beepcodeBean" , beepcodeBean );
    log.debug( "Added input message param for beepcodeBean : code = "
        + StringEscapeUtils.escapeJava( beepcodeBean.getCode() )
        + " , clientId = " + beepcodeBean.getClientID() + " , eventId = "
        + beepcodeBean.getEventID() );

    try {
      new BeepcodeSupport().updateLastHitDate( guestCode );
      log.debug( "Updated beepcode last hit date" );
    } catch ( IOException e ) {
      log.warning( "Failed to update beepcode last hit date , " + e );
    }

    return result;
  }

  private boolean resolveBeepId( String guestCode , TransactionInputMessage imsg ) {
    boolean result = false;
    if ( StringUtils.isBlank( guestCode ) ) {
      return result;
    }
    boolean feature = oprops.getBoolean(
        "Transaction.VerifyMessageCodeFromBeepIdList" , false );
    if ( !feature ) {
      return result;
    }

    log.debug( "Trying to find guest code in the beepid list" );

    BeepIDBean beepidBean = null;
    try {
      beepidBean = new BeepIDBean().select( guestCode , true );
    } catch ( IOException e ) {
    }
    if ( beepidBean == null ) {
      log.debug( "Failed to find guest code in the beepid list" );
      return result;
    }

    result = true;
    log.debug( "Define a guest code as a beepid : code = "
        + StringEscapeUtils.escapeJava( beepidBean.getBeepID() )
        + " , clientId = " + beepidBean.getClientID() + " , eventId = "
        + beepidBean.getEventID() );

    imsg.addMessageParam( "beepidBean" , beepidBean );
    log.debug( "Added input message param for beepidBean" );

    try {
      new BeepIDSupport().updateLastHitDate( guestCode );
      log.debug( "Updated beepid last hit date" );
    } catch ( IOException e ) {
      log.warning( "Failed to update beepid last hit date , " + e );
    }

    return result;
  }

  private TransactionQueueBean currentTransQueueBean( String phoneNumber ) {
    TransactionQueueBean transQueueBean = null;
    log.debug( "Trying to find current trans queue bean "
        + "from the trans queue table , based on : phoneNumber = "
        + phoneNumber );
    transQueueBean = session.getLastSession( phoneNumber );
    if ( transQueueBean == null ) {
      log.debug( "Can not find any trans queue bean in the last session" );
      return transQueueBean;
    }
    log.debug( "Found a current trans queue bean with : lastAccessDate = "
        + DateTimeFormat.convertToString( transQueueBean.getDateTm() )
        + " , lastCode = "
        + StringEscapeUtils.escapeJava( transQueueBean.getCode() )
        + " , eventId = " + transQueueBean.getEventID() );
    return transQueueBean;
  }

  private boolean resolveFromProviderToEventMapping(
      TransactionInputMessage imsg ) {
    boolean result = false;

    // preparing parameter(s) for provider to event map bean
    TProviderToEvent providerToEvent = null;
    TProvider providerBean = (TProvider) imsg.getMessageParam( "providerBean" );
    TCountry countryBean = (TCountry) imsg.getMessageParam( "countryBean" );
    if ( ( providerBean != null ) && ( countryBean != null ) ) {
      log.debug( "Trying to find valid code from mapping provider "
          + "to event table ( with post validation actived )" );
      providerToEvent = ProviderCommon.getProviderToEvent(
          providerBean.getProviderId() , countryBean.getId() , true );
    }

    // verify the provider to event map bean
    if ( providerToEvent == null ) {
      log.debug( "Can not resolve trans queue bean "
          + "from provider to event mapping , found no mapping" );
      return result;
    }
    log.debug( "Found provider to event : id = " + providerToEvent.getId() );

    // verify the event code of provider to event map
    String providerToEventCode = providerToEvent.getEventCode();
    if ( StringUtils.isBlank( providerToEventCode ) ) {
      log.debug( "Can not resolve trans queue bean "
          + "from provider to event mapping , found blank event code" );
      return result;
    }

    // resolve the code type
    String codeType = null;
    {
      log.debug( "Found provider to event : code = " + providerToEventCode );
      if ( resolveBeepCode( providerToEventCode , imsg ) ) {
        codeType = CODE_TYPE_BEEP_CODE;
      } else if ( resolveBeepId( providerToEventCode , imsg ) ) {
        codeType = CODE_TYPE_BEEP_ID;
      }
    }
    if ( codeType == null ) {
      log.debug( "Can not find any valid beep code and/or beep id "
          + "based on the mapping provider to event" );
      return result;
    }

    // found code type than set default result as valid
    result = true;

    // verify is the mapping want to store the session or not ?
    if ( !providerToEvent.isStoreSession() ) {
      log.debug( "Found store session property is disabled "
          + ", will not process any response ( no mt leg processed )" );
      int eventId = providerToEvent.getEventId();
      if ( eventId < 1 ) {
        log.warning( "Failed to create trans queue bean "
            + ", found zero event id " );
      } else {
        TEvent eventBean = EventCommon.getEvent( eventId );
        if ( eventBean == null ) {
          log.warning( "Failed to create trans queue bean "
              + ", found invalid event id = " + eventId );
        } else {

          // force to bypass process response
          imsg.setNoProcessResponse( true );

          // build new trans queue from provider to event properties
          TransactionQueueBean transQueueBean = new TransactionQueueBean();
          transQueueBean.setClientID( eventBean.getClientId() );
          transQueueBean.setEventID( eventBean.getEventId() );
          transQueueBean.setCode( providerToEventCode );
          transQueueBean.setDateTm( new Date() );
          log.debug( "Created new trans queue : client id = "
              + transQueueBean.getClientID() + " , event id = "
              + transQueueBean.getEventID() + " , code = "
              + transQueueBean.getCode() );

          // store code type parameter into imsg
          imsg.addMessageParam( "pte.transQueueBean" , transQueueBean );
          log.debug( "Added input message param for pte.transQueueBean = "
              + imsg.getMessageParam( "pte.transQueueBean" ) );

          return result;
        }
      }
    }

    // store code type parameter into imsg
    imsg.addMessageParam( "pte.codeType" , codeType );
    log.debug( "Added input message param for pte.codeType = "
        + imsg.getMessageParam( "pte.codeType" ) );

    // store guest code parameter into imsg
    imsg.addMessageParam( "pte.guestCode" , providerToEventCode );
    log.debug( "Added input message param for pte.guestCode = "
        + imsg.getMessageParam( "pte.guestCode" ) );

    return result;
  }

  private boolean isExchageEvent( TransactionQueueBean transQueueBean ) {
    boolean result = false;
    if ( transQueueBean == null ) {
      log.debug( "This message does not contain of exchange event "
          + ", found null trans queue bean" );
      return result;
    }
    String code = transQueueBean.getCode();
    if ( !StringUtils.equals( code , "X" ) ) {
      log.debug( "This message does not contain of exchange event "
          + ", found the event code is not equal to X" );
      return result;
    }
    String params = transQueueBean.getParams();
    if ( !StringUtils.contains( params , "MODE=E" ) ) {
      log.debug( "This message does not contain of exchange event "
          + ", found the event param does not contain MODE=E" );
      return result;
    }
    result = true;
    log.debug( "This message does contain of exchange event" );
    return result;
  }

  private TClient resolveDedicatedModemToClient( String modemNumber ,
      TransactionInputMessage imsg ) {
    TClient expectClientBean = null;

    if ( StringUtils.isBlank( modemNumber ) ) {
      log.debug( "Unresolved dedicated modem to client "
          + ", found null modem number" );
      return expectClientBean;
    }

    log.debug( "Found modem number = " + modemNumber
        + " , trying to resolve expect client id" );

    TModemNumberToClient modemNumberToClientBean = resolveModemNumberToClient( modemNumber );
    if ( modemNumberToClientBean == null ) {
      log.debug( "Unresolved dedicated modem to client "
          + ", found empty matched map bean" );
      return expectClientBean;
    }
    if ( !modemNumberToClientBean.isActive() ) {
      log.debug( "Unresolved dedicated modem to client "
          + ", found inactive matched map bean" );
      return expectClientBean;
    }

    expectClientBean = ClientCommon.getClient( modemNumberToClientBean
        .getClientId() );
    if ( expectClientBean == null ) {
      log.debug( "Unresolved dedicated modem to client "
          + ", found invalid client profile" );
      return expectClientBean;
    }

    log.debug( "Resolved dedicated modem to client "
        + ", found expect client : id = " + expectClientBean.getClientId()
        + " , name = " + expectClientBean.getCompanyName() );

    imsg.addMessageParam(
        TransactionMessageParam.HDR_MODEM_NUMBER_TO_CLIENT_BEAN ,
        modemNumberToClientBean );
    log.debug( "Added input message param for "
        + TransactionMessageParam.HDR_MODEM_NUMBER_TO_CLIENT_BEAN );

    return expectClientBean;
  }

  private TModemNumberToClient resolveModemNumberToClient( String modemNumber ) {
    TModemNumberToClient modemNumberToClient = null;
    if ( StringUtils.isBlank( modemNumber ) ) {
      log.warning( "Failed to resolve modem number to client "
          + ", found blank modem number" );
      return modemNumberToClient;
    }
    modemNumberToClient = ModemNumberCommon
        .getModemNumberToClientByModemNumber( modemNumber );
    return modemNumberToClient;
  }

  private TransactionQueueBean resolveMessageCode( String guestCode ,
      String codeType , TransactionQueueBean transQueueBeanCur ,
      TClient expectClientBean , TEvent expectEventBean ,
      TransactionInputMessage imsg , Map sessionParams , boolean sessionPersist ) {
    TransactionQueueBean transQueueBeanResult = null;

    // get keyword
    ClientKeywordBean keywordBean = null;
    if ( StringUtils.equals( codeType , CODE_TYPE_KEYWORD ) ) {
      keywordBean = (ClientKeywordBean) imsg.getMessageParam( "keywordBean" );
      if ( keywordBean == null ) {
        log.warning( "Failed to generate trans queue bean "
            + ", found null keyword bean" );
        return transQueueBeanResult;
      }
    }

    // get beepcode bean
    BeepcodeBean beepcodeBean = null;
    if ( StringUtils.equals( codeType , CODE_TYPE_BEEP_CODE ) ) {
      beepcodeBean = (BeepcodeBean) imsg.getMessageParam( "beepcodeBean" );
      if ( beepcodeBean == null ) {
        log.warning( "Failed to generate trans queue bean "
            + ", found null beepcode bean" );
        return transQueueBeanResult;
      }
    }

    // get beepid bean
    BeepIDBean beepidBean = null;
    if ( StringUtils.equals( codeType , CODE_TYPE_BEEP_ID ) ) {
      beepidBean = (BeepIDBean) imsg.getMessageParam( "beepidBean" );
      if ( beepidBean == null ) {
        log.warning( "Failed to generate trans queue bean "
            + ", found null beepid bean" );
        return transQueueBeanResult;
      }
    }

    // get and validate beep : client and event id
    int beepClientId = 0;
    int beepEventId = 0;
    String beepCode = null;
    if ( StringUtils.equals( codeType , CODE_TYPE_KEYWORD ) ) {
      beepClientId = keywordBean.getClientId();
      beepEventId = keywordBean.getEventId();
      beepCode = keywordBean.getCode();
    }
    if ( StringUtils.equals( codeType , CODE_TYPE_BEEP_CODE ) ) {
      beepClientId = (int) beepcodeBean.getClientID();
      beepEventId = (int) beepcodeBean.getEventID();
      beepCode = beepcodeBean.getCode();
    }
    if ( StringUtils.equals( codeType , CODE_TYPE_BEEP_ID ) ) {
      beepClientId = (int) beepidBean.getClientID();
      beepEventId = (int) beepidBean.getEventID();
      beepCode = beepidBean.getBeepID();
    }
    if ( beepClientId < 1 ) {
      log.warning( "Failed to generate trans queue bean "
          + ", found zero beep client id" );
      return transQueueBeanResult;
    }
    if ( beepEventId < 1 ) {
      log.warning( "Failed to generate trans queue bean "
          + ", found zero beep event id" );
      return transQueueBeanResult;
    }
    log.debug( "Defined beep client id = " + beepClientId
        + " and beep event id = " + beepEventId );

    // when expect client bean exist , will match with beepClientId
    if ( expectClientBean != null ) {
      if ( beepClientId != expectClientBean.getClientId() ) {
        log.warning( "Failed to generate trans queue bean "
            + ", found unmatched code's client id with the expected one" );
        return transQueueBeanResult;
      }
      log.debug( "Successfully matched between code's client id "
          + "and expect client id" );
    }

    // when expect event bean exist , will match with beepEventId
    if ( expectEventBean != null ) {
      if ( beepEventId != expectEventBean.getEventId() ) {
        log.warning( "Failed to generate trans queue bean "
            + ", found unmatched code's event id with the expected one" );
        return transQueueBeanResult;
      }
      log.debug( "Successfully matched between code's event id "
          + "and expect event id" );
    }

    if ( ( sessionPersist ) && transQueueBeanCur != null ) {

      int closedReasonId = TransactionLogConstanta.CLOSED_REASON_DIFF_EVENT;
      if ( transQueueBeanCur.getClientID() == beepClientId ) {
        log.debug( "Found code's client id has the same value with current session" );
        if ( transQueueBeanCur.getEventID() == beepEventId ) {
          log.debug( "Found code's event id has the same value with current session" );
          closedReasonId = TransactionLogConstanta.CLOSED_REASON_RENEW_EVENT;
        }
      }

      // found new session has different params with the current one
      int ctr = 0 , max = 5 , delay1s = 1000;
      while ( ctr < max ) {
        log.debug( "[Ctr-" + ctr + "] Trying to close current session , with "
            + TransactionLogConstanta.closedReasonToString( closedReasonId ) );
        if ( session.closeSession( transQueueBeanCur , closedReasonId ) ) {
          break;
        }
        try {
          Thread.sleep( delay1s );
        } catch ( InterruptedException e ) {
        }
        ctr = ctr + 1;
      } // while ( ctr < max ) {
      if ( ctr >= max ) {
        log.warning( "[Ctr-" + ctr + "] Failed to close the current session "
            + ", trigger as invalid message" );
        return transQueueBeanResult;
      }
      log.debug( "[Ctr-" + ctr + "] The current session is successfully "
          + "closed : queueId = " + transQueueBeanCur.getQueueId() );
    }

    // set default location id
    int defaultLocationId = 0;

    {
      int ctr = 0 , max = 5 , delay1s = 1000;
      while ( ctr < max ) {
        // create new session based on beep params
        log.debug( "[Ctr-" + ctr + "] Trying to create new session "
            + ", with : phone = " + imsg.getOriginalAddress()
            + " , beepCode = " + beepCode + " , beepEventId = " + beepEventId
            + " , codeType = " + codeType + " , defaultLocationId = "
            + defaultLocationId );
        transQueueBeanResult = session.createSession( sessionPersist ,
            beepEventId , beepClientId , imsg.getOriginalAddress() ,
            imsg.getOriginalProvider() , beepCode , defaultLocationId );
        if ( transQueueBeanResult != null ) {
          break;
        }
        try {
          Thread.sleep( delay1s );
        } catch ( InterruptedException e ) {
        }
        ctr = ctr + 1;
      } // while ( ctr < max ) {
      if ( ctr >= max ) {
        log.warning( "[Ctr-" + ctr + "] Failed to create new session "
            + ", trigger as invalid message" );
        return transQueueBeanResult;
      }
      log.debug( "[Ctr-" + ctr + "] The new session is successfully "
          + "created" );
    }

    return transQueueBeanResult;
  }

  private TransactionQueueBean resolveMessageInteraction( String guestCode ,
      String codeType , TransactionQueueBean transQueueBeanCur ,
      TClient expectClientBean , TEvent expectEventBean ,
      TransactionInputMessage imsg , Map sessionParams , boolean sessionPersist ) {
    TransactionQueueBean transQueueBeanResult = null;

    // verify to filter message content
    if ( !TransactionUtil.filterMessageContent( imsg.getMessageContent() ) ) {
      log.warning( "Failed to generate trans queue bean "
          + ", found a filter word inside the message" );
      return transQueueBeanResult;
    }

    // especially process for "Z" request
    if ( ( transQueueBeanCur != null )
        && ( StringUtils.equals( guestCode , "Z" ) ) ) {
      log.debug( "Found Z code , perform update next step = 1" );
      try {
        EventBean pendingEventBean = null;
        long pendingEventId = transQueueBeanCur.getPendingEventID();
        if ( pendingEventId > 0 ) {
          pendingEventBean = eventService.select( pendingEventId );
        }
        if ( pendingEventBean != null ) {
          transQueueBeanCur.setEventID( pendingEventBean.getEventID() );
          transQueueBeanCur.setCode( transQueueBeanCur.getPendingCode() );
          transQueueBeanCur.setUpdateProfile( false );
          log.debug( "Change trans queue params based on "
              + "the pending event , change eventId = "
              + transQueueBeanCur.getEventID() + ", change code = "
              + transQueueBeanCur.getCode() + ", set update profile = "
              + transQueueBeanCur.isUpdateProfile() );
          transQueueBeanCur.setNextStep( 1 );
          log.debug( "Force to set next step = 1 , bypass" );
        } else {
          log.warning( "Failed to update pending event" );
        }
      } catch ( IOException e ) {
        log.warning( "Failed to process Z code , " + e );
      }
    }

    // when found current trans queue is exist
    if ( transQueueBeanCur != null ) {

      // is using dedicated modem ?
      TModemNumberToClient modemNumberToClient = ModemNumberCommon
          .getModemNumberToClientByModemName( transQueueBeanCur.getProviderId() );
      if ( ( modemNumberToClient != null )
          && ( transQueueBeanCur.getClientID() != modemNumberToClient
              .getClientId() ) ) {
        log.warning( "Failed to match with dedicated modem number "
            + "to client ( id = " + modemNumberToClient.getId()
            + " ) , reset current session "
            + ", and stop resolve session process here" );
        return transQueueBeanResult;
      }

      // prepare is valid session set true by default
      boolean validSession = true;

      // is the session value exist ?
      if ( ( transQueueBeanCur.getClientID() < 1 )
          || ( transQueueBeanCur.getEventID() < 1 ) ) {
        validSession = false;
        log.warning( "Found invalid current session "
            + ", found invalid session client and/or event id" );
      }

      // is match with expect client profile ?
      if ( ( expectClientBean != null )
          && ( transQueueBeanCur.getClientID() != expectClientBean
              .getClientId() ) ) {
        validSession = false;
        log.warning( "Found invalid current session "
            + ", found session client id doesn't match with expected one" );
      }

      // is match with expect event profile ?
      if ( ( expectEventBean != null )
          && ( transQueueBeanCur.getEventID() != expectEventBean.getEventId() ) ) {
        validSession = false;
        log.warning( "Found invalid current session "
            + ", found session event id doesn't match with expected one" );
      }

      // the predefined transQueueBeanCur as result
      // and will stop process here if found any
      if ( validSession ) {
        log.debug( "Use the current session as trans queue bean "
            + ", and stop resolve session process here . " );
        transQueueBeanResult = transQueueBeanCur;
        return transQueueBeanResult;
      }

    }

    // is enable session to persist ?
    if ( !sessionPersist ) {
      log.debug( "Not required to persist the session "
          + ", so no need to dig into the log session history "
          + "in order to find the matched session." );
      return transQueueBeanResult;
    }

    // get session from trans log based on original address
    // and expected client and/or event profile if found any
    log.debug( "Trying to dig into the log session history based on "
        + "original address with expected client and/or event profile" );
    TransactionLogBean transLogBeanCur = matchTransLogId( sessionParams ,
        expectClientBean , expectEventBean , imsg.getOriginalAddress() );
    if ( transLogBeanCur == null ) {
      log.warning( "Failed to find matched session from log history "
          + " , stop resolve session process here ." );
      return transQueueBeanResult;
    }

    // log it
    log.debug( "Resolved session from log history : log id = "
        + transLogBeanCur.getLogId() + " , client id = "
        + transLogBeanCur.getClientID() + " , event id = "
        + transLogBeanCur.getEventID() );

    // switch between trans queue and log to generate new trans queue
    transQueueBeanResult = switchedTransQueueAndLog( transQueueBeanCur ,
        transLogBeanCur );
    if ( transQueueBeanResult != null ) {
      log.debug( "Successfully generated new session from log history" );
    }

    return transQueueBeanResult;
  }

  private TransactionLogBean matchTransLogId( Map sessionParams ,
      TClient expectClientBean , TEvent expectEventBean , String phoneNumber ) {
    TransactionLogBean transLogBean = null;

    // get limitDays
    int limitDays = 0;
    if ( sessionParams != null ) {
      Integer integer = (Integer) sessionParams
          .get( TransactionConf.SESSION_PARAM_ID_LIMIT_DAYS );
      if ( integer != null ) {
        limitDays = integer.intValue();
      }
    }
    if ( limitDays < 1 ) {
      limitDays = 12 * 30; // one year default
    }

    // get limitRecords ( no need right now )
    int limitRecords = 0;
    if ( sessionParams != null ) {
      Integer integer = (Integer) sessionParams
          .get( TransactionConf.SESSION_PARAM_ID_LIMIT_RECORDS );
      if ( integer != null ) {
        limitRecords = integer.intValue();
      }
    }
    if ( limitRecords < 1 ) {
      limitRecords = 1000; // one thousand max records default
    }

    // 12 months before
    Date maxDate = new Date();
    Date minDate = new Date();
    long ltime = minDate.getTime() - ( limitDays * 24L * 3600L * 1000L );
    minDate.setTime( ltime );

    // compose criteria based on phone number
    String criteria = "( encrypt_phone = "
        + sqlEncryptPhoneNumber( phoneNumber ) + " ) ";
    log.debug( "Build match trans log records based on phone = " + phoneNumber );

    // compose criteria based on close reason id
    criteria += "AND ( closed_reason_id = "
        + TransactionLogConstanta.CLOSED_REASON_DIFF_EVENT + " ) ";
    log.debug( "Build match trans log records based on closed reason id = "
        + TransactionLogConstanta.CLOSED_REASON_DIFF_EVENT );

    // compose criteria based on expected client id
    if ( ( expectClientBean != null ) && ( expectClientBean.getClientId() > 0 ) ) {
      criteria += "AND ( client_id = " + expectClientBean.getClientId() + " ) ";
      log.debug( "Build match trans log records based on expect client id = "
          + expectClientBean.getClientId() );
    } else {
      // when expect client id == 0 , means need to filter
      // to find the session exclude dedicated modems .
      String strDedicatedClientIds = strDedicatedClientIds();
      if ( !StringUtils.isBlank( strDedicatedClientIds ) ) {
        criteria += "AND ( client_id NOT IN (  " + strDedicatedClientIds
            + " ) ) ";
        log.debug( "Build match trans log records based on not dedicated client ids = "
            + strDedicatedClientIds );
      }
    }

    // compose criteria based on expected event id
    if ( ( expectEventBean != null ) && ( expectEventBean.getEventId() > 0 ) ) {
      criteria += "AND ( event_id = " + expectEventBean.getEventId() + " ) ";
      log.debug( "Build match trans log records based on expect event id = "
          + expectEventBean.getEventId() );
    }

    // log it
    log.debug( "Composed sql where criteria to find a match session "
        + "in trans log table : " + criteria );

    // order by
    boolean orderByDesc = true;

    // record limit
    int recLimit = 1;

    // fetch query and iterate
    Vector vecRec = transLogService.select( minDate , maxDate , criteria ,
        orderByDesc , recLimit );
    if ( vecRec == null ) {
      log.debug( "Found empty record(s) matched from trans log table" );
    }

    // validate per record
    Iterator iterRec = vecRec.iterator();
    if ( iterRec.hasNext() ) {
      transLogBean = (TransactionLogBean) iterRec.next();
    }

    return transLogBean;
  }

  private String strDedicatedClientIds() {
    String strClientIds = null;

    TModemNumberToClients listModemNumbers = ModemNumberCommon
        .getModemNumberToClients();
    if ( listModemNumbers == null ) {
      return strClientIds;
    }

    List listIds = listModemNumbers.getModemNumberToClientIds();
    if ( ( listIds == null ) || ( listIds.size() < 1 ) ) {
      return strClientIds;
    }

    StringBuffer sbClientIds = null;

    Iterator iterIds = listIds.iterator();
    while ( iterIds.hasNext() ) {
      String key = (String) iterIds.next();
      if ( StringUtils.isBlank( key ) ) {
        continue;
      }
      TModemNumberToClient modemNumberToClient = (TModemNumberToClient) listModemNumbers
          .getModemNumberToClient( key );
      if ( modemNumberToClient == null ) {
        continue;
      }
      if ( sbClientIds == null ) {
        sbClientIds = new StringBuffer();
      } else {
        sbClientIds.append( "," );
      }
      sbClientIds.append( modemNumberToClient.getClientId() );
    }

    if ( sbClientIds != null ) {
      strClientIds = sbClientIds.toString();
    }

    return strClientIds;
  }

  private TransactionQueueBean switchedTransQueueAndLog(
      TransactionQueueBean transQueueOld , TransactionLogBean transLogOld ) {
    TransactionQueueBean transQueueNew = null;

    // validate history session
    if ( transLogOld == null ) {
      log.warning( "Failed to switched trans queue and log "
          + ", found empty trans log" );
      return transQueueNew;
    }
    int transLogId = transLogOld.getLogId();
    if ( transLogId < 1 ) {
      log.warning( "Failed to switched trans queue and log "
          + ", found empty trans log id" );
      return transQueueNew;
    }

    // compose header log
    String headerLog = "[TransLog-" + transLogId + "] ";

    // when current session not exist , try to build it based on
    // trans log phone number
    if ( transQueueOld == null ) {
      transQueueOld = session.getLastSession( transLogOld.getPhone() );
    }

    // when current session exist , will close it
    if ( transQueueOld != null ) {
      boolean closedSession = session.closeSession( transQueueOld ,
          TransactionLogConstanta.CLOSED_REASON_DIFF_EVENT );
      if ( !closedSession ) {
        log.warning( headerLog + "Failed to switched trans queue and log "
            + ", found can not close the current session" );
        return transQueueNew;
      }
    }

    // delete specific history session
    boolean deleted = transLogService.delete( transLogOld.getLogId() );
    if ( !deleted ) {
      log.warning( headerLog + "Failed to switched trans queue and log "
          + ", found can not to clean the specific history session" );
      return transQueueNew;
    }

    // create new session based on the old trans log
    transQueueNew = session.createSession( transLogOld );
    if ( transQueueNew == null ) {
      log.warning( headerLog + "Failed to switched trans queue and log "
          + ", found can not create a new session" );
    }

    return transQueueNew;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Util Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  private String sqlEncryptPhoneNumber( String phoneNumber ) {
    StringBuffer sb = new StringBuffer();
    sb.append( "AES_ENCRYPT('" );
    sb.append( phoneNumber );
    sb.append( "','" );
    sb.append( keyPhoneNumber );
    sb.append( "')" );
    return sb.toString();
  }

}
