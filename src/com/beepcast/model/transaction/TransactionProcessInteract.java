package com.beepcast.model.transaction;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;

import com.beepcast.dbmanager.common.ClientCountriesCommon;
import com.beepcast.dbmanager.table.TClientToCountry;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.webTransaction.WebTransactionBean;
import com.beepcast.model.webTransaction.WebTransactionFactory;
import com.beepcast.model.webTransaction.WebTransactionService;
import com.beepcast.model.webTransaction.WebTransactionStatus;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionProcessInteract extends TransactionProcessSteps {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "TransactionProcessInteract" );

  public static final String PROVIDER_INTERNAL = "INTERNAL";

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private WebTransactionService wtService;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionProcessInteract( boolean debug ) {
    super( debug );

    wtService = new WebTransactionService();
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public String info() {
    return "Transaction Process Interact";
  }

  public int begin( TransactionInputMessage imsg , LinkedList omsgs ) {
    int processCode = ProcessCode.PROCESS_SUCCEED;

    // validate incoming message - must be
    if ( !validTransactionInputMessage( imsg ) ) {
      DLog.warning( lctx , "Failed to process the transaction "
          + ", found invalid input message" );
      processCode = ProcessCode.PROCESS_FATAL;
      return processCode;
    }

    // generate messageId
    TransactionMessageFactory.generateMessageId( imsg );

    // generate headerLog
    log.generateHeaderLog( imsg.getMessageId() );

    // log
    log.info( "Processing inbound message : messageType = "
        + imsg.getMessageType() + " , messageCount = " + imsg.getMessageCount()
        + " , messageContent = "
        + StringEscapeUtils.escapeJava( imsg.getMessageContent() )
        + " , oriNode = " + imsg.getOriginalNode() + " , oriAddress = "
        + imsg.getOriginalAddress() + " , oriMaskingAddress = "
        + imsg.getOriginalMaskingAddress() + " , oriProvider = "
        + imsg.getOriginalProvider() + " , dstNode = "
        + imsg.getDestinationNode() + " , dstAddress = "
        + imsg.getDestinationAddress() + " , dstMaskingAddress = "
        + imsg.getDestinationMaskingAddress() + " , dstProvider = "
        + imsg.getDestinationProvider() + " , eventId = " + imsg.getEventId()
        + " , clientId = " + imsg.getClientId() + " , channelSessionId = "
        + imsg.getChannelSessionId() + " , noProcessResponse = "
        + imsg.isNoProcessResponse() + " , replyMessageContent = "
        + StringEscapeUtils.escapeJava( imsg.getReplyMessageContent() )
        + " , messageParams = " + imsg.getMessageParams() );

    return processCode;
  }

  public int run( TransactionInputMessage imsg , LinkedList omsgs ) {
    int processCode = ProcessCode.PROCESS_FAILED;

    // resolve countryBean based on prefix phone number
    log.debug( "Trying to persist country profile" );
    if ( !support.persistCountryBean( imsg ) ) {
      log.warning( "Failed to persist country profile" );
    }
    TCountry countryBean = (TCountry) imsg
        .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );

    // is the country prefix phone number in the country list ?
    if ( oprops.getBoolean( "Transaction.RejectInvalidCountryMessage" , false ) ) {
      if ( ( countryBean == null ) || ( !countryBean.isActive() ) ) {
        // because this is a reject message , need to set null to omsg
        log.warning( "Reject message , found invalid country bean" );
        return processCode;
      }
    }

    // generate trans queue
    log.debug( "Trying to resolve trans queue bean" );
    TransactionQueueBean transQueue = support.resolveTransactionQueueBean(
        conf.getSessionParams() , imsg ,
        app.getSessionLock( imsg.getOriginalAddress() ) , true );
    if ( transQueue == null ) {
      log.warning( "Failed to resolve trans queue bean" );
      return processCode;
    }
    log.debug( "Successfully resolved trans queue bean , clientId : "
        + transQueue.getClientID() + " , eventId : " + transQueue.getEventID() );

    // resolve main identifier params : clientId and eventId
    log.debug( "Trying to resolve message identifier params "
        + ", for clientId and/or eventId" );
    if ( !support.resolveMessageIdentifier( imsg , transQueue ) ) {
      // create invalid reply message
      log.warning( "Failed to resolve message identifier "
          + ", creating invalid reply message" );
      return processCode;
    }

    // resolve mobileUserBean
    log.debug( "Trying to persist mobile user profile" );
    if ( !support.persistMobileUserBean( imsg ) ) {
      log.warning( "Failed to persist mobile user profile" );
      return processCode;
    }

    // get and validate clientBean and eventBean
    EventBean eventBean = null;
    ClientBean clientBean = null;
    log.debug( "Trying to retrive client and event profile" );
    try {
      EventService eventService = new EventService();
      eventBean = eventService.select( imsg.getEventId() );
      ClientService clientService = new ClientService();
      clientBean = clientService.select( imsg.getClientId() );
    } catch ( Exception e ) {
      log.warning( "Failed to retrieve client and event profile , " + e );
    }
    if ( eventBean == null ) {
      log.warning( "Failed to retrieve event profile " );
      return processCode;
    }
    log.debug( "Resolved event profile : id =" + eventBean.getEventID()
        + ", eventName = " + eventBean.getEventName() );
    if ( clientBean == null ) {
      log.warning( "Failed to retrieve client profile " );
      return processCode;
    }
    log.debug( "Resolved client profile : id = " + clientBean.getClientID()
        + " , companyName = " + clientBean.getCompanyName() );

    // add message params : client and event profile
    imsg.addMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN , clientBean );
    imsg.addMessageParam( TransactionMessageParam.HDR_EVENT_BEAN , eventBean );
    log.debug( "Added input msg params of client and event bean profile" );

    // resolve contact list
    log.debug( "Trying to persist contact list" );
    if ( !support.persistContactList( imsg ) ) {
      log.warning( "Failed to persist contact list" );
    }

    // is the prefix country number is registered ?
    if ( oprops.getBoolean( "Transaction.RejectUnregisterCountryMessage" ,
        false ) ) {
      TClientToCountry clientToCountry = null;
      if ( countryBean != null ) {
        clientToCountry = ClientCountriesCommon.getClientToCountry(
            imsg.getClientId() , countryBean.getId() );
      }
      if ( clientToCountry == null ) {
        // because this is a reject message , need to set null to omsg
        log.warning( "Reject message , found unregister country bean" );
        return processCode;
      }
      log.debug( "Found matched client to country profile : id = "
          + clientToCountry.getId() );
    }

    // verify if need to process response
    if ( imsg.isNoProcessResponse() ) {
      log.debug( "Found no process response property is enabled "
          + ", stop process here" );
      return processCode;
    }

    // process menu event
    if ( ( eventBean.getProcessType() == EventBean.MENU_TYPE )
        || ( eventBean.getProcessType() == EventBean.SURVEY_TYPE ) ) {
      if ( ( eventBean.getProcessType() == EventBean.MENU_TYPE ) ) {
        log.debug( "Process message as Menu Type" );
      }
      if ( ( eventBean.getProcessType() == EventBean.SURVEY_TYPE ) ) {
        log.debug( "Process message as Survey Type" );
      }
      imsg.addMessageParam( TransactionMessageParam.HDR_HAS_EVENT_MENU_TYPE ,
          "true" );
      log.debug( "Add input msg param : "
          + TransactionMessageParam.HDR_HAS_EVENT_MENU_TYPE + " = true" );
    }

    // execute process steps
    processCode = processSteps( imsg , omsgs , transQueue , clientBean ,
        eventBean );

    return processCode;
  }

  public int end( TransactionInputMessage imsg , LinkedList omsgs ) {
    int processCode = ProcessCode.PROCESS_SUCCEED;

    if ( omsgs == null ) {
      processCode = ProcessCode.PROCESS_FAILED;
      return processCode;
    }

    int idxOmsgs = 0;
    String headerLog = "";
    TransactionOutputMessage omsg = null;
    Iterator iterOmsgs = omsgs.iterator();
    while ( iterOmsgs.hasNext() ) {
      omsg = (TransactionOutputMessage) iterOmsgs.next();
      if ( omsg == null ) {
        continue;
      }

      // set default process code
      omsg.setProcessCode( ProcessCode.PROCESS_SUCCEED );

      // prepare for header log
      idxOmsgs = idxOmsgs + 1;
      headerLog = "[" + idxOmsgs + "] ";

      // add small sleep for the second and rest message(s)
      if ( idxOmsgs > 1 ) {
        try {
          Thread.sleep( 10 );
        } catch ( Exception e ) {
        }
      }

      // guard output message before the next process
      if ( !validTransactionOutputMessage( omsg ) ) {
        log.warning( headerLog + "Failed to finalized "
            + ", found empty output message" );
        omsg.setProcessCode( ProcessCode.PROCESS_ERROR );
        continue;
      }

      // get messageProfile
      String msgProfile = omsg.getMessageProfile();

      // process message based on message profile
      if ( msgProfile == null ) {
        log.warning( headerLog + "Failed to finalized "
            + ", found null message profile" );
        omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
        continue;
      }

      // process bogus message
      if ( msgProfile == MessageProfile.PROFILE_INVALID_MSG ) {
        log.debug( headerLog + "Processing invalid message" );
        storeTransactionSucceed( imsg , omsg );
        continue;
      }

      // process internal message
      if ( msgProfile == MessageProfile.PROFILE_INTERNAL_MSG ) {
        log.debug( headerLog + "Processing internal message" );
        storeTransactionSucceed( imsg , omsg );
        continue;
      }

      // process normal message
      if ( msgProfile == MessageProfile.PROFILE_NORMAL_MSG ) {
        log.debug( headerLog + "Processing normal message" );
        storeTransactionSucceed( imsg , omsg );
        continue;
      }

    } // while ( iterOmsgs.hasNext() )

    return processCode;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean storeTransactionSucceed( TransactionInputMessage imsg ,
      TransactionOutputMessage omsg ) {
    return storeTransaction( imsg , omsg , WebTransactionStatus.CODE_OK , "OK" );
  }

  public boolean storeTransactionFailed( TransactionInputMessage imsg ,
      String statusDescription ) {
    return storeTransaction( imsg , null , WebTransactionStatus.CODE_FAILED ,
        statusDescription );
  }

  public boolean storeTransactionError( TransactionInputMessage imsg ,
      String statusDescription ) {
    return storeTransaction( imsg , null , WebTransactionStatus.CODE_ERROR ,
        statusDescription );
  }

  public boolean storeTransaction( TransactionInputMessage imsg ,
      TransactionOutputMessage omsg , String statusCode ,
      String statusDescription ) {
    boolean result = false;

    if ( imsg == null ) {
      log.warning( "Failed to store transaction "
          + ", found null input message" );
      return result;
    }

    String messageId = imsg.getMessageId();
    String mobileNumber = imsg.getOriginalAddress();
    String browserAddress = (String) imsg.getMessageParam( "browserAddress" );
    String browserAgent = (String) imsg.getMessageParam( "browserAgent" );
    String messageRequest = imsg.getMessageContent();

    browserAddress = ( browserAddress == null ) ? "" : browserAddress;
    browserAgent = ( browserAgent == null ) ? "" : browserAgent;

    statusCode = ( statusCode == null ) ? "" : statusCode;
    statusDescription = ( statusDescription == null ) ? "" : statusDescription;

    int eventId = 0;
    double debitAmount = 0;
    String messageResponse = "";

    if ( omsg != null ) {
      eventId = omsg.getEventId();
      debitAmount = 0;
      messageResponse = omsg.getMessageContent();
    }

    WebTransactionBean wtBean = WebTransactionFactory.createWebTransactionBean(
        messageId , eventId , mobileNumber , debitAmount , browserAddress ,
        browserAgent , messageRequest , messageResponse , statusCode ,
        statusDescription );
    if ( wtBean == null ) {
      log.warning( "Failed to store transaction "
          + ", found failed to create bean" );
      return result;
    }

    if ( !wtService.insert( wtBean ) ) {
      log.warning( "Failed to store transaction "
          + ", found failed to insert into table" );
      return result;
    }

    result = true;
    return result;
  }

}
