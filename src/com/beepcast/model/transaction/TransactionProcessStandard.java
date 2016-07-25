package com.beepcast.model.transaction;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.dbmanager.common.ClientCountriesCommon;
import com.beepcast.dbmanager.common.ProviderCommon;
import com.beepcast.dbmanager.table.TClientToCountry;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.dbmanager.table.TProvider;
import com.beepcast.dbmanager.table.TProviderToEvent;
import com.beepcast.loadmng.LoadManagement;
import com.beepcast.loadmng.LoadManagementApi;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.client.ClientState;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.specialMessage.SpecialMessageName;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionProcessStandard extends TransactionProcessSteps {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "TransactionProcessStandard" );

  static LoadManagement loadMng = LoadManagement.getInstance();

  public static final String PROVIDER_INTERNAL = "INTERNAL";

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

  public TransactionProcessStandard( boolean debug ) {
    super( debug );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public String info() {
    return "Transaction Process Standard";
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

    // trap load
    loadHitMo( 1 );

    // clean input message
    support.cleanInputMessage( imsg );

    // set message param provider bean
    TProvider providerBean = ProviderCommon.getIncomingProvider( imsg
        .getOriginalProvider() );
    if ( providerBean != null ) {
      imsg.addMessageParam( TransactionMessageParam.HDR_PROVIDER_BEAN ,
          providerBean );
      log.debug( "Input msg params of provider bean : id = "
          + providerBean.getProviderId() + " , name = "
          + providerBean.getProviderName() );
    } else {
      log.warning( "Found invalid incoming provider id = "
          + imsg.getOriginalAddress() );
    }

    // persist incoming message destination address based
    // on provider's short code if found any
    if ( ( providerBean != null )
        && ( !StringUtils.isBlank( providerBean.getShortCode() ) )
        && ( StringUtils.isBlank( imsg.getDestinationAddress() ) ) ) {
      log.debug( "Updated input msg destination address : "
          + imsg.getDestinationAddress() + " -> " + providerBean.getShortCode() );
      imsg.setDestinationAddress( providerBean.getShortCode() );
    }

    // resolve countryBean based on prefix phone number
    log.debug( "Trying to persist country profile" );
    if ( !support.persistCountryBean( imsg ) ) {
      log.warning( "Failed to persist country profile" );
    }
    TCountry countryBean = (TCountry) imsg
        .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );

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
          app.getSessionLock( imsg.getOriginalAddress() ) , true );
    } else {
      transQueue = support.resolveTransactionQueueBean(
          conf.getSessionParams() , imsg ,
          app.getSessionLock( imsg.getOriginalAddress() ) , true );
    }
    if ( transQueue == null ) {
      log.warning( "Failed to resolve trans queue bean" );

      // log input message into gateway log
      boolean insertGatewayLog = support
          .insertIncomingMessageToGatewayLog( imsg );
      if ( insertGatewayLog ) {
        log.debug( "Inserted an input message into gateway log" );
      } else {
        log.warning( "Failed to insert an input message into gateway log" );
      }

      // process invalid message as bogus
      log.warning( "Converted an input message as bogus message" );
      support.processBogusMesssage( imsg , "unresolved trans queue object" );

      // generate invalid reply message
      omsgs.add( support.createInvalidReplyMessage( imsg ,
          SpecialMessageName.UNRECOGNIZED_CODE ) );
      processCode = ProcessCode.PROCESS_SUCCEED;
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
    MobileUserBean mobileUserBean = (MobileUserBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );

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
    support.persistContactList( imsg );

    // update global valid number only for incoming message with valid modem as
    // active provider's type
    if ( ( providerBean != null )
        && ( "MODEM".equalsIgnoreCase( providerBean.getType() ) ) ) {
      if ( subscriberApp.doValidated( (int) clientBean.getClientID() ,
          mobileUserBean.getPhone() , true ) ) {
        log.debug( "Found incoming provider " + providerBean.getProviderName()
            + " as " + providerBean.getType()
            + " , updated valid number for phone " + mobileUserBean.getPhone()
            + " with clientId = " + clientBean.getClientID()
            + " from global invalid number table" );
      }
    }

    // perform mo log debit and log in the gateway
    boolean doDebitMoLeg = false;
    if ( imsg.isAsBroadcastMessage() ) {
      log.debug( "Bypass debit at MO Leg , coz found message as broadcast" );
    } else if ( StringUtils.equalsIgnoreCase( imsg.getOriginalProvider() ,
        PROVIDER_INTERNAL ) ) {
      log.debug( "Bypass debit at MO Leg , coz found internal provider " );
    } else if ( StringUtils.indexOf(
        oprops.getString( "Transaction.BypassDebitMoLegForProviderIds" , "" ) ,
        imsg.getOriginalProvider() ) > -1 ) {
      log.debug( "Bypass debit at MO Leg , coz found "
          + "bypass debit provider id = " + imsg.getOriginalProvider() );
    } else if ( eventBean.isSuspend()
        && ( imsg
            .getMessageParam( TransactionMessageParam.HDR_BYPASS_SUSPENDED_EVENT ) == null ) ) {
      imsg.setDebitAmount( 0 );
      log.debug( "Bypass debit at MO Leg , coz found suspended event bean" );
      String messageStatus = "FAILED-SUSPENDED";
      log.debug( "Writing transaction into gateway log table "
          + ", with message status = " + messageStatus );
      support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
      return processCode;
    } else if ( StringUtils
        .equals( clientBean.getState() , ClientState.SUSPEND ) ) {
      imsg.setDebitAmount( 0 );
      log.debug( "Bypass debit at MO Leg , because found client state as "
          + clientBean.getState() );
      String messageStatus = "FAILED-SUSPENDED";
      log.debug( "Writing transaction into gateway log table "
          + ", with message status = " + messageStatus );
      support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
      return processCode;
    } else if ( StringUtils.equals( clientBean.getState() ,
        ClientState.SUSPEND_TRAFFIC ) ) {
      imsg.setDebitAmount( 0 );
      log.debug( "Bypass debit at MO Leg , because found client state as "
          + clientBean.getState() );
      String messageStatus = "FAILED-SUSPENDED";
      log.debug( "Writing transaction into gateway log table "
          + ", with message status = " + messageStatus );
      support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
      return processCode;
    } else {

      // is the country profile registered ?
      {
        if ( oprops.getBoolean( "Transaction.RejectInvalidCountryMessage" ,
            false ) ) {
          if ( ( countryBean == null ) || ( !countryBean.isActive() ) ) {
            log.warning( "Reject message , found invalid country bean" );
            String messageStatus = "FAILED";
            log.debug( "Writing transaction into gateway log table "
                + ", with message status = " + messageStatus );
            support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
            return processCode;
          }
        }
        if ( oprops.getBoolean( "Transaction.RejectUnregisterCountryMessage" ,
            false ) ) {
          TClientToCountry clientToCountryBean = null;
          if ( ( countryBean != null ) && ( countryBean.isActive() ) ) {
            clientToCountryBean = ClientCountriesCommon.getClientToCountry(
                imsg.getClientId() , countryBean.getId() );
          }
          if ( ( clientToCountryBean == null )
              || ( !clientToCountryBean.isActive() ) ) {
            // because this is a reject message , need to set null to omsg
            log.warning( "Reject message , found can't find match client "
                + "to country profile" );
            String messageStatus = "FAILED";
            log.debug( "Writing transaction into gateway log table "
                + ", with message status = " + messageStatus );
            support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
            return processCode;
          }
        }
      }

      // make sure the message has valid provider
      if ( providerBean == null ) {
        log.warning( "Failed to perform debit at MO Leg "
            + ", found invalid provider : " + imsg.getOriginalProvider()
            + " , stop the process here" );
        String messageStatus = "FAILED";
        log.debug( "Writing transaction into gateway log table "
            + ", with message status = " + messageStatus );
        support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
        return processCode;
      }

      // perform mo debit at mo leg
      doDebitMoLeg = true;
      double debitAmount = imsg.getMessageCount();
      debitAmount = debitAmount * providerBean.getInCreditCost();
      log.debug( "Trying to perform debit at MO Leg "
          + ", defined total debit amount = " + imsg.getDebitAmount() + " -> "
          + debitAmount + " unit(s) , based on : provider.id = "
          + providerBean.getProviderId() + " , provider.creditCost = "
          + providerBean.getInCreditCost() + " unit(s) , and total = "
          + imsg.getMessageCount() + " msg(s)" );
      imsg.setDebitAmount( debitAmount );
      if ( !support.eventSupport()
          .doDebitPayment( imsg , imsg.getDebitAmount() ) ) {
        String messageStatus = "FAILED-NO BALANCE";
        log.warning( "Failed to perform debit at MO Leg "
            + ", writing transaction into gateway log table "
            + ", with message status = " + messageStatus );
        support.insertIncomingMessageToGatewayLog( imsg , messageStatus );
        return processCode;
      }
      log.debug( "Successfully perform debit at MO Leg" );

    }

    // log input message only for deducted transaction
    if ( doDebitMoLeg ) {
      if ( !support.insertIncomingMessageToGatewayLog( imsg , null ) ) {
        log.debug( "Failed to insert input message into gateway log" );
      } else {
        log.debug( "Successfully insert input message into gateway log" );
        // insert into client msg api
        support.insertIncomingMessageToClientApiMsg( imsg );
        // update mobile user last request code ( only from IOD Service )
        String newLastCode = transQueue.getCode();
        support.updateMobileUserLastCode( mobileUserBean , newLastCode );
      }
    }

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

      // iterate index outbound message
      idxOmsgs = idxOmsgs + 1;

      // add small sleep for the remain message(s)
      if ( idxOmsgs > 2 ) {
        try {
          Thread.sleep( 10 );
        } catch ( Exception e ) {
        }
      }

      // prepare for header log
      headerLog = "[OutMsg-" + idxOmsgs + "] ";

      // set default process code
      omsg.setProcessCode( ProcessCode.PROCESS_SUCCEED );

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

        // temp variable
        boolean resolved = false;

        // is the number's country registered ? if not will rejected
        {
          TCountry countryBean = (TCountry) omsg
              .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );
          if ( oprops.getBoolean( "Transaction.RejectInvalidCountryMessage" ,
              false ) ) {
            if ( ( countryBean == null ) || ( !countryBean.isActive() ) ) {
              log.warning( "Reject outbound message , found invalid country profile" );
              if ( !support.insertOutgoingMessageToGatewayLog( omsg , "FAILED" ) ) {
                log.warning( headerLog
                    + "Found failed to insert output message "
                    + "into gateway log with status FAILED" );
              }
              omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
              continue;
            }
          }
          if ( oprops.getBoolean( "Transaction.RejectUnregisterCountryMessage" ,
              false ) ) {
            TClientToCountry clientToCountryBean = null;
            if ( ( countryBean != null ) && ( countryBean.isActive() ) ) {
              clientToCountryBean = ClientCountriesCommon.getClientToCountry(
                  omsg.getClientId() , countryBean.getId() );
            }
            if ( ( clientToCountryBean == null )
                || ( !clientToCountryBean.isActive() ) ) {
              log.warning( "Reject outbound message "
                  + ", found can't find match client to country profile" );
              if ( !support.insertOutgoingMessageToGatewayLog( omsg , "FAILED" ) ) {
                log.warning( headerLog
                    + "Found failed to insert output message "
                    + "into gateway log with status FAILED" );
              }
              omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
              continue;
            }
          }
        }

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
          if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
            log.warning( headerLog + "Found failed to insert output message "
                + "into gateway log with status ERROR" );
          }
          omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
          continue;
        }

        // resolve destination provider
        log.debug( headerLog + "Trying to resolve destination provider " );
        resolved = support.resolveDestinationProviderAndMask( imsg , omsg );
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

        // perform mt debit at mt leg
        if ( omsg.getMessageParam( TransactionMessageParam.HDR_BYPASS_MT_DEBIT ) != null ) {
          log.debug( headerLog + "Found bypass mt debit message param "
              + ", set zero debit mt amount , and no mt debiting performed." );
          omsg.setDebitAmount( 0 );
        } else {

          // get provider bean of omsg
          TProvider providerBean = (TProvider) omsg
              .getMessageParam( TransactionMessageParam.HDR_PROVIDER_BEAN );
          // get country bean of omsg
          TCountry countryBean = (TCountry) omsg
              .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );
          if ( ( providerBean == null ) || ( countryBean == null ) ) {
            log.warning( headerLog + "Failed to deduct mt leg "
                + ", found invalid provider and/or country profile" );
            if ( !support.insertOutgoingMessageToGatewayLog( omsg , "ERROR" ) ) {
              log.warning( headerLog + "Found failed to insert output message "
                  + "into gateway log with status ERROR" );
            }
            omsg.setProcessCode( ProcessCode.PROCESS_FAILED );
            continue;
          }

          double debitAmount = omsg.getMessageCount();
          debitAmount = debitAmount * providerBean.getOuCreditCost();
          debitAmount = debitAmount * countryBean.getCreditCost();
          log.debug( "Trying to perform debit at MT Leg "
              + ", defined total debit amount = " + omsg.getDebitAmount()
              + " -> " + debitAmount + " unit(s) , based on : provider.id = "
              + providerBean.getProviderId() + " , provider.creditCost = "
              + providerBean.getOuCreditCost() + " , country.name = "
              + countryBean.getName() + " , country.creditCost = "
              + countryBean.getCreditCost() + " , and total = "
              + omsg.getMessageCount() + " msg(s)" );
          omsg.setDebitAmount( debitAmount );
          if ( !support.eventSupport().doDebitPayment( omsg ,
              omsg.getDebitAmount() ) ) {
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

  public static int loadHitMo() {
    return loadMng.getLoad( LoadManagementApi.HDRPROF_TRANSACTION ,
        LoadManagementApi.CONTYPE_SMSMO , "STANDARD" , false , false );
  }

  public static int loadHitMt() {
    return loadMng.getLoad( LoadManagementApi.HDRPROF_TRANSACTION ,
        LoadManagementApi.CONTYPE_SMSMT , "STANDARD" , false , false );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private void loadHitMo( int count ) {
    loadMng.hit( LoadManagementApi.HDRPROF_TRANSACTION ,
        LoadManagementApi.CONTYPE_SMSMO , "STANDARD" , count , true , false );
  }

  private void loadHitMt( int count ) {
    loadMng.hit( LoadManagementApi.HDRPROF_TRANSACTION ,
        LoadManagementApi.CONTYPE_SMSMT , "STANDARD" , count , true , false );
  }

}
