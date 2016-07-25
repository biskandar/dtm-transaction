package com.beepcast.model.transaction;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.channel.ChannelLogSimulationBean;
import com.beepcast.channel.ChannelLogSimulationService;
import com.beepcast.dbmanager.common.ClientCountriesCommon;
import com.beepcast.dbmanager.common.ProviderCommon;
import com.beepcast.dbmanager.table.TClientToCountry;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.dbmanager.table.TProvider;
import com.beepcast.dbmanager.table.TProviderToEvent;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.specialMessage.SpecialMessageName;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionProcessSimulation extends TransactionProcessSteps {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "TransactionProcessSimulation" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionProcessSimulation( boolean debug ) {
    super( debug );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public String info() {
    return "Transaction Process Simulation";
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

    // validate incoming message - optional
    if ( !verifyInputMessageContentWithLog( imsg ) ) {
      log.warning( "Failed to process the transaction "
          + ", found invalid input message content" );
      return processCode;
    }

    // clean input message
    support.cleanInputMessage( imsg );

    // set message param provider bean
    TProvider providerBean = ProviderCommon.getIncomingProvider( imsg
        .getOriginalProvider() );
    if ( providerBean != null ) {
      imsg.addMessageParam( TransactionMessageParam.HDR_PROVIDER_BEAN ,
          providerBean );
      log.debug( "Input msg params of provider bean : " + providerBean );
    } else {
      log.warning( "Found invalid incoming provider id = "
          + imsg.getOriginalAddress() );
    }

    // persist incoming message destination address
    if ( ( providerBean != null )
        && ( StringUtils.isBlank( imsg.getDestinationAddress() ) ) ) {
      log.debug( "Updated input msg destination address : "
          + imsg.getDestinationAddress() + " -> " + providerBean.getShortCode() );
      imsg.setDestinationAddress( providerBean.getShortCode() );
    }

    // resolve countryBean based on prefix phone number
    TCountry countryBean = null;
    log.debug( "Trying to persist country profile" );
    if ( !support.persistCountryBean( imsg ) ) {
      log.warning( "Failed to persist country profile" );
    } else {
      countryBean = (TCountry) imsg
          .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );
    }

    // is the country prefix phone number in the country list ?
    if ( oprops.getBoolean( "Transaction.RejectInvalidCountryMessage" , false ) ) {
      if ( ( countryBean == null ) || ( !countryBean.isActive() ) ) {
        // because this is a reject message , need to set null to omsg
        log.warning( "Reject message , found invalid country bean" );
        return processCode;
      }
    }

    // verify map of provider country to specific event
    TProviderToEvent providerToEvent = null;
    if ( ( providerBean != null ) && ( countryBean != null ) ) {
      providerToEvent = ProviderCommon.getProviderToEvent(
          providerBean.getProviderId() , countryBean.getId() , false );
      if ( providerToEvent != null ) {
        log.debug( "Resolved an event profile from map provider country table"
            + " : event id = " + providerToEvent.getEventId()
            + " , event code = " + providerToEvent.getEventCode()
            + " , store session = " + providerToEvent.isStoreSession() );
      }
    }

    // generate transQueue
    log.debug( "Trying to resolve trans queue bean" );
    TransactionQueueBean transQueue = null;
    if ( providerToEvent != null ) {
      transQueue = support.resolveTransactionQueueBean( providerToEvent ,
          conf.getSessionParams() , imsg ,
          app.getSessionLock( imsg.getOriginalAddress() ) , false );
    } else {
      transQueue = support.resolveTransactionQueueBean(
          conf.getSessionParams() , imsg ,
          app.getSessionLock( imsg.getOriginalAddress() ) , false );
    }
    if ( transQueue == null ) {
      log.warning( "Failed to resolve trans queue bean" );
      return processCode;
    }
    log.debug( "Successfully resolved trans queue bean , clientId : "
        + transQueue.getClientID() + " , eventId : " + transQueue.getEventID() );

    // resolve main identifier params : clientId and eventId
    log.debug( "Trying to resolve message identifier params "
        + ", for clientId and/or eventId" );
    boolean resolveIdentifier = support.resolveMessageIdentifier( imsg ,
        transQueue );
    if ( !resolveIdentifier ) {
      // create invalid reply message
      log.warning( "Failed to resolve message identifier "
          + ", creating invalid reply message" );
      omsgs.add( support.createInvalidReplyMessage( imsg ,
          SpecialMessageName.UNRECOGNIZED_CLIENT_OR_EVENT ) );
      processCode = ProcessCode.PROCESS_SUCCEED;
      return processCode;
    }

    // resolve mobileUserBean
    log.debug( "Trying to persist mobile user profile" );
    if ( !support.persistMobileUserBean( imsg ) ) {
      log.warning( "Failed to persist mobile user profile" );
      return processCode;
    }

    // validate and process only for internal message
    {
      TransactionOutputMessage omsg = support.processInternalMessage( imsg ,
          transQueue );
      if ( omsg != null ) {
        log.debug( "Processed as an internal message" );
        omsgs.add( omsg );
        processCode = ProcessCode.PROCESS_SUCCEED;
        return processCode;
      }
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
      log.warning( "Failed to retrieve event profile "
          + ", creating invalid reply message" );
      omsgs.add( support.createInvalidReplyMessage( imsg ,
          SpecialMessageName.UNRECOGNIZED_CLIENT_OR_EVENT ) );
      processCode = ProcessCode.PROCESS_SUCCEED;
      return processCode;
    }
    log.debug( "Resolved event profile : id = " + eventBean.getEventID()
        + " , eventName = " + eventBean.getEventName() );
    if ( clientBean == null ) {
      log.warning( "Failed to retrieve client profile "
          + ", creating invalid reply message" );
      omsgs.add( support.createInvalidReplyMessage( imsg ,
          SpecialMessageName.UNRECOGNIZED_CLIENT_OR_EVENT ) );
      processCode = ProcessCode.PROCESS_SUCCEED;
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
        log.warning( "Reject message , found can't find match client "
            + "to country profile" );
        return processCode;
      }
      log.debug( "Found matched client to country profile : id = "
          + clientToCountry.getId() );
    }

    // bypass any mo leg debit and/or log into gateway log
    log.debug( "Bypass any mo leg debit and/or log into gateway log" );

    // verify if need to process response
    if ( imsg.isNoProcessResponse() ) {
      log.debug( "Found no process response property is enabled "
          + ", stop process here." );
      return processCode;
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

    log.debug( "Sending total " + omsgs.size() + " outbound message(s)" );

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

      // add small sleep for the remain message(s)
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
        log.debug( headerLog + "Processing as invalid message" );
        // nothing to do yet
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

        // get the MT message content
        String mtMessageContent = omsg.getMessageContent();

        // temp variable
        boolean resolved = false;

        // when the MT message content reply is not exist
        // than do the stop process
        if ( StringUtils.isBlank( mtMessageContent ) ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found empty mt message content" );
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
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }

        // resolve destination provider
        log.debug( headerLog + "Trying to resolve destination provider " );
        resolved = support.resolveDestinationProviderAndMask( imsg , omsg );
        if ( !resolved ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found failed to resolve destination provider" );
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }
        log.debug( headerLog + "Resolved final : destination provider = "
            + omsg.getDestinationProvider() + " , original masking address = "
            + omsg.getOriginalMaskingAddress() );

        // resolve message type
        log.debug( headerLog + "Trying to resolve message type" );
        support.resolveMessageType( omsg );

        // resolve shortener link if any
        log.debug( headerLog + "Trying to resolve shortener link" );
        support.resolveShortenerLink( omsg );

        // resolve message content
        log.debug( headerLog + "Trying to resolve message content" );
        support.resolveMessageContent( omsg );

        // verify message count
        if ( omsg.getMessageCount() < 1 ) {
          log.warning( headerLog + "Failed to finalized "
              + ", found zero total message send" );
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }

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

        // bypass mt leg debit and/or log into gateway log
        log.debug( headerLog + "Bypass mt leg debit and/or log "
            + "into gateway log" );

        // send message back into channel log simulation
        // when found the incoming message from broadcast
        ChannelLogSimulationBean clsBean = (ChannelLogSimulationBean) imsg
            .getMessageParam( TransactionMessageParam.HDR_CHANNEL_LOG_BEAN );
        if ( clsBean != null ) {
          ChannelLogSimulationService clsService = new ChannelLogSimulationService();
          clsBean.setSenderId( omsg.getOriginalMaskingAddress() );
          clsBean.setMessageCount( omsg.getMessageCount() );
          clsBean.setMessageText( omsg.getMessageContent() );
          boolean updated = clsService.updateMessageResult( clsBean );
          log.debug( headerLog + "Setup output destination into channel log "
              + "simulation record : result = " + updated + " , id = "
              + clsBean.getId() + " , senderId = " + clsBean.getSenderId()
              + " , messageCount = " + clsBean.getMessageCount()
              + " , messageText = "
              + StringEscapeUtils.escapeJava( clsBean.getMessageText() ) );
        } else {
          log.debug( headerLog + "No output destination node, stop here. "
              + "Last outbound message was : count = " + omsg.getMessageCount()
              + " msg(s) , content = "
              + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );
        }

        log.debug( headerLog + "Successfully processed as normal message" );
        continue;
      } // if ( msgProfile == MessageProfile.PROFILE_NORMAL_MSG )

    } // while ( iterOmsgs.hasNext() )

    return processCode;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

}
