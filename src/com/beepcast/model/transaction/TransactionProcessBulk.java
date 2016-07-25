package com.beepcast.model.transaction;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.dbmanager.common.ClientCountriesCommon;
import com.beepcast.dbmanager.table.TClientToCountry;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.loadmng.LoadManagement;
import com.beepcast.loadmng.LoadManagementApi;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.client.ClientState;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventOutboundReservedVariables;
import com.beepcast.model.event.EventProcessCommon;
import com.beepcast.model.event.EventService;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionProcessBulk extends TransactionProcessBasic {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionProcessBulk" );

  static LoadManagement loadMng = LoadManagement.getInstance();

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  // ...

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionProcessBulk( boolean debug ) {
    super( debug );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public String info() {
    return "Transaction Process Bulk";
  }

  public int begin( TransactionInputMessage imsg , LinkedList omsgs ) {
    int processCode = ProcessCode.PROCESS_SUCCEED;

    // validate incoming message

    if ( imsg == null ) {
      DLog.warning( lctx , "Failed to process the transaction "
          + ", found null input message" );
      processCode = ProcessCode.PROCESS_FATAL;
      return processCode;
    }

    // validate phone number

    String phoneNumber = imsg.getDestinationAddress();
    if ( StringUtils.isBlank( phoneNumber ) ) {
      DLog.warning( lctx , "Failed to process the transaction "
          + ", found blank destination address" );
      processCode = ProcessCode.PROCESS_FATAL;
      return processCode;
    }

    // clean phone number

    phoneNumber = StringUtils.trimToEmpty( phoneNumber );
    if ( !phoneNumber.startsWith( "+" ) ) {
      phoneNumber = "+" + phoneNumber;
    }
    if ( !StringUtils.isNumeric( phoneNumber.substring( 1 ) ) ) {
      DLog.warning( lctx , "Failed to process the transaction "
          + ", found invalid format phone number" );
      processCode = ProcessCode.PROCESS_FATAL;
      return processCode;
    }
    imsg.setDestinationAddress( phoneNumber );

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

    // transform input message into output message

    TransactionOutputMessage omsg = transformMessage( imsg );
    if ( omsg == null ) {
      log.warning( "Failed to transform input into output message" );
      return processCode;
    }

    // put omsg as valid result

    omsgs.add( omsg );
    processCode = ProcessCode.PROCESS_SUCCEED;

    // verify client and event id

    log.debug( "Trying to resolve message identifier params "
        + ", for clientId and/or eventId" );
    if ( omsg.getEventId() < 1 ) {
      log.warning( "Found invalid or zero event id "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Unrecognized event" );
      return processCode;
    }
    if ( omsg.getClientId() < 1 ) {
      log.warning( "Found invalid or zero client id "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Unrecognized client" );
      return processCode;
    }

    // resolve and store client and event profile

    EventBean eventBean = null;
    ClientBean clientBean = null;
    log.debug( "Trying to retrive client and event profile" );
    try {
      EventService eventService = new EventService();
      eventBean = eventService.select( omsg.getEventId() );
      ClientService clientService = new ClientService();
      clientBean = clientService.select( omsg.getClientId() );
    } catch ( Exception e ) {
      log.warning( "Failed to retrieve client and event profile , " + e );
    }
    if ( eventBean == null ) {
      log.warning( "Failed to retrieve event profile "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Unrecognized client" );
      return processCode;
    }
    log.debug( "Resolved event profile : id = " + eventBean.getEventID()
        + ", eventName = " + eventBean.getEventName() );
    if ( clientBean == null ) {
      log.warning( "Failed to retrieve client profile "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Unrecognized event" );
      return processCode;
    }
    log.debug( "Resolved client profile : id = " + clientBean.getClientID()
        + " , companyName = " + clientBean.getCompanyName() );
    if ( eventBean.getClientID() != clientBean.getClientID() ) {
      log.warning( "Found not match between client and event profile "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Unmatched event and client" );
      return processCode;
    }

    // add message params : client and event profile

    omsg.addMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN , clientBean );
    omsg.addMessageParam( TransactionMessageParam.HDR_EVENT_BEAN , eventBean );
    log.debug( "Added input msg params of client and event bean profile" );

    // resolve mobileUserBean

    log.debug( "Trying to persist mobile user profile" );
    if ( !support.persistMobileUserBean( omsg ) ) {
      log.warning( "Failed to persist mobile user profile" );
      asInvalidMessage( omsg , "Invalid destination address" );
      return processCode;
    }

    // resolve countryBean based on prefix destination phone number

    TCountry countryBean = null;
    log.debug( "Trying to persist country profile" );
    if ( !support.persistCountryBean( omsg ) ) {
      log.warning( "Failed to persist country profile" );
    } else {
      countryBean = (TCountry) omsg
          .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );
    }

    // is the country prefix phone number in the country list ?

    if ( oprops.getBoolean( "Transaction.RejectInvalidCountryMessage" , false ) ) {
      if ( ( countryBean == null ) || ( !countryBean.isActive() ) ) {
        log.warning( "Found invalid destination country bean "
            + ", creating invalid reply message" );
        asInvalidMessage( omsg , "Invalid destination country" );
        return processCode;
      }
    }

    // is the prefix country number is registered ?

    if ( oprops.getBoolean( "Transaction.RejectUnregisterCountryMessage" ,
        false ) ) {
      TClientToCountry clientToCountryBean = null;
      if ( countryBean != null ) {
        clientToCountryBean = ClientCountriesCommon.getClientToCountry(
            imsg.getClientId() , countryBean.getId() );
      }
      if ( ( clientToCountryBean == null )
          || ( !clientToCountryBean.isActive() ) ) {
        log.warning( "Found invalid destination country bean "
            + ", creating invalid reply message" );
        asInvalidMessage( omsg , "Invalid destination country" );
        return processCode;
      }
    }

    // verify suspend event and / or client profile

    if ( eventBean.isSuspend() ) {
      log.warning( "Found invalid suspended event profile "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Suspend event profile" );
      return processCode;
    }
    if ( clientBean.getState() == ClientState.SUSPEND ) {
      log.warning( "Found invalid suspended client profile "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Suspend client profile" );
      return processCode;
    }
    if ( clientBean.getState() == ClientState.SUSPEND_TRAFFIC ) {
      log.warning( "Found invalid suspended client profile "
          + ", creating invalid reply message" );
      asInvalidMessage( omsg , "Suspend client profile" );
      return processCode;
    }

    // resolved original address from event bean

    String oldOriginalAddress = omsg.getOriginalAddress();
    log.debug( "Defined old original address = " + oldOriginalAddress );
    if ( ( oldOriginalAddress == null ) || ( oldOriginalAddress.equals( "" ) ) ) {
      String newOriginalAddress = eventBean.getOutgoingNumber();
      log.debug( "Resolved new original address = " + newOriginalAddress );
      omsg.setOriginalAddress( newOriginalAddress );
    }

    // resolve message content from event's first response
    // and resolve reserved variables ( ~ only from event )

    String oldMessageContent = omsg.getMessageContent();
    log.debug( "Defined old message content : "
        + StringEscapeUtils.escapeJava( oldMessageContent ) );
    if ( ( oldMessageContent == null ) || ( oldMessageContent.equals( "" ) ) ) {
      String newMessageContent = getFirstEventMessageResponse( eventBean );
      newMessageContent = EventOutboundReservedVariables
          .replaceReservedVariables( log , newMessageContent , imsg );
      log.debug( "Resolved new message content : "
          + StringEscapeUtils.escapeJava( newMessageContent ) );
      omsg.setMessageContent( newMessageContent );
    }

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
        log.debug( "Stop transaction here , defined as "
            + "an invalid message : messageId = " + omsg.getMessageId()
            + " , statusCode = " + omsg.getMessageStatusCode()
            + " , statusDesc = " + omsg.getMessageStatusDescription()
            + " , originalAddress = " + omsg.getOriginalAddress()
            + " , destinationAddress = " + omsg.getDestinationAddress()
            + " , messageContent = "
            + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );
        continue;
      }

      // process internal message
      if ( msgProfile == MessageProfile.PROFILE_INTERNAL_MSG ) {
        log.debug( headerLog + "Processing as internal message" );
        // nothing to do yet
        continue;
      }

      // process normal message
      if ( msgProfile == MessageProfile.PROFILE_NORMAL_MSG ) {
        log.debug( headerLog + "Processing as normal message" );

        // temp variable
        boolean resolved = false;

        // when the MT message content reply is not exist
        // than do the stop process
        if ( StringUtils.isBlank( omsg.getMessageContent() ) ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found empty mt message content" );
          if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
            log.warning( headerLog + "Found failed to insert output message "
                + "into gateway log with status ERROR" );
          }
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }

        // resolve original ( with/out masking ) address
        log.debug( headerLog + "Trying to resolve original "
            + "( with/out masking ) address" );
        resolved = support.resolveOriginalAddressAndMask( omsg );
        if ( !resolved ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found failed to resolve original address" );
          if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
            log.warning( headerLog + "Found failed to insert output message "
                + "into gateway log with status ERROR" );
          }
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }

        // resolve message type
        log.debug( headerLog + "Trying to resolve message type" );
        support.resolveMessageType( omsg );

        // resolve message content
        log.debug( headerLog + "Trying to resolve message content" );
        support.resolveMessageContent( omsg );

        // verify message count
        if ( omsg.getMessageCount() < 1 ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found zero total message send" );
          if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
            log.warning( headerLog + "Found failed to insert output message "
                + "into gateway log with status ERROR" );
          }
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }

        // resolve destination provider
        log.debug( headerLog + "Trying to resolve destination provider " );
        resolved = support.resolveDestinationProviderAndMask( null , omsg );
        if ( !resolved ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found failed to resolve destination provider" );
          if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
            log.warning( headerLog + "Found failed to insert output message "
                + "into gateway log with status ERROR" );
          }
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }
        log.debug( headerLog + "Resolved final : destination provider = "
            + omsg.getDestinationProvider() + " , original masking address = "
            + omsg.getOriginalMaskingAddress() );

        // get country bean of imsg
        TCountry countryBean = (TCountry) omsg
            .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );

        // prepare debit amount for mt leg
        double debitAmount = 0;
        if ( countryBean != null ) {
          int messageCount = omsg.getMessageCount();
          debitAmount = countryBean.getCreditCost();
          debitAmount = debitAmount * messageCount;
          log.debug( headerLog + "Prepare total MT Leg debit amount = "
              + debitAmount
              + " unit(s) , calculated based on defined country code = "
              + countryBean.getCode() + " and total = " + messageCount
              + " msg(s)" );
        }

        // do the payment for MT Leg
        if ( omsg.getMessageParam( TransactionMessageParam.HDR_BYPASS_MT_DEBIT ) != null ) {
          log.debug( headerLog + "Found bypass mt debit message param "
              + ", set zero debit mt amount , and no mt debiting performed." );
          omsg.setDebitAmount( 0 );
        } else {
          log.debug( headerLog + "Trying to perform debit at MT Leg" );
          omsg.setDebitAmount( debitAmount );
          if ( !support.eventSupport().doDebitPayment( omsg , debitAmount ) ) {
            log.warning( headerLog + "Failed to finalized "
                + ", found failed to perform debit at MT Leg " );
            if ( !support.insertOutgoingMessageToGatewayLog( omsg ,
                "FAILED-NO BALANCE" ) ) {
              log.warning( headerLog + "Found failed to insert output message "
                  + "into gateway log with status FAILED-NO BALANCE" );
            }
            omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
            continue;
          }
          log.debug( headerLog + "Successfully perform debit at MT Leg" );
        }

        // insert output message into the send buffer table thru router app
        if ( omsg
            .getMessageParam( TransactionMessageParam.HDR_BYPASS_SEND_PROVIDER ) != null ) {
          log.debug( headerLog + "Found bypass send provider message param "
              + ", no message sending to provider agent performed." );
          if ( omsg
              .getMessageParam( TransactionMessageParam.HDR_BYPASS_GATEWAY_LOG ) != null ) {
            log.debug( headerLog + "Found bypass gateway log message param "
                + ", no message inserting to gateway log performed" );
          } else {
            if ( !support.insertOutgoingMessageToGatewayLog( omsg , "LOGGED" ) ) {
              log.warning( headerLog
                  + "Failed to finalized , found failed to insert "
                  + "output message into gateway log with status LOGGED" );
              omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
              continue;
            }
          }
        } else {
          if ( !support.insertOutgoingMessageToSendBuffer( omsg ) ) {
            log.warning( headerLog + "Failed to finalized "
                + ", found failed to insert outgoing message into send buffer" );
            if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
              log.warning( headerLog + "Found failed to insert output message "
                  + "into gateway log with status ERROR" );
            }
            omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
            continue;
          }
        }

        // trap load
        loadHitMt( 1 );

        log.debug( headerLog + "Successfully processed as normal message" );
        continue;
      } // if ( msgProfile == MessageProfile.PROFILE_NORMAL_MSG )

    } // while ( iterOmsgs.hasNext() )

    return processCode;
  }

  public static int loadHitMt() {
    return loadMng.getLoad( LoadManagementApi.HDRPROF_TRANSACTION ,
        LoadManagementApi.CONTYPE_SMSMT , "BULK" , false , false );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private String getFirstEventMessageResponse( EventBean eventBean ) {
    String messageContent = null;

    if ( eventBean == null ) {
      return messageContent;
    }

    String[] arrTextMessageResponses = EventProcessCommon
        .getFirstAndNextExecutedProcessBeanResponses( (int) eventBean
            .getEventID() );
    if ( ( arrTextMessageResponses == null )
        || ( arrTextMessageResponses.length < 1 ) ) {
      return messageContent;
    }

    String textMessageResponse = null;
    for ( int idx = 0 ; idx < arrTextMessageResponses.length ; idx++ ) {
      textMessageResponse = arrTextMessageResponses[idx];
      if ( ( textMessageResponse != null )
          && ( !textMessageResponse.equals( "" ) ) ) {
        messageContent = textMessageResponse;
        break;
      }
    }

    return messageContent;
  }

  private TransactionOutputMessage transformMessage(
      TransactionInputMessage imsg ) {
    TransactionOutputMessage omsg = null;
    if ( imsg == null ) {
      return omsg;
    }

    // prepare default params

    String messageStatusCode = "";
    String messageStatusDescription = "";

    // create transaction output message based on input

    omsg = TransactionMessageFactory.createOutputMessage( imsg.getMessageId() ,
        MessageProfile.PROFILE_NORMAL_MSG , messageStatusCode ,
        messageStatusDescription , imsg.getMessageId() , imsg.getMessageType() ,
        imsg.getMessageContent() , Node.DTM , imsg.getOriginalAddress() , "" ,
        "" , Node.DTM , imsg.getDestinationAddress() , "" , imsg.getClientId() ,
        imsg.getEventId() , imsg.getChannelSessionId() , imsg.getPriority() );

    if ( omsg == null ) {
      log.warning( "Failed to transform message "
          + ", can not create output message from factory" );
      return omsg;
    }

    // copy message params

    String copiedMsgParams = TransactionMessageFactory.copyMessageParams( imsg ,
        omsg );

    // debug

    log.debug( "Created output message , based on input message "
        + ": messageId = " + omsg.getMessageId() + " , clientId = "
        + omsg.getClientId() + " , eventId = " + omsg.getEventId()
        + " , channelSessionId = " + omsg.getChannelSessionId()
        + " , priority = " + omsg.getPriority() + " , messageParams = ["
        + copiedMsgParams + "]" );

    return omsg;
  }

  private boolean asInvalidMessage( TransactionOutputMessage omsg , String desc ) {
    boolean result = false;
    if ( omsg == null ) {
      return result;
    }

    desc = StringUtils.trimToEmpty( desc );

    omsg.setMessageProfile( MessageProfile.PROFILE_INVALID_MSG );
    omsg.setMessageStatusCode( MessageStatusCode.SC_OK );
    omsg.setMessageStatusDescription( desc );

    result = true;
    return result;
  }

  private void loadHitMt( int count ) {
    loadMng.hit( LoadManagementApi.HDRPROF_TRANSACTION ,
        LoadManagementApi.CONTYPE_SMSMT , "BULK" , count , true , false );
  }

}
