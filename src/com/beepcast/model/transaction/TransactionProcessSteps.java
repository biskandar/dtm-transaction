package com.beepcast.model.transaction;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.client.ClientBean;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventOutboundReservedVariables;
import com.beepcast.model.event.EventOutboundXipmeReservedVariables;
import com.beepcast.model.event.EventProcessCreateQrImage;
import com.beepcast.model.event.EventResponse;
import com.beepcast.model.event.EventService;
import com.beepcast.model.event.EventTransQueueReservedVariables;
import com.beepcast.model.event.ProcessBean;
import com.beepcast.model.specialMessage.SpecialMessageName;
import com.beepcast.util.StrTok;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public abstract class TransactionProcessSteps extends TransactionProcessBasic {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionProcessSteps" );

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

  public TransactionProcessSteps( boolean debug ) {
    super( debug );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  protected int processSteps( TransactionInputMessage imsg , LinkedList omsgs ,
      TransactionQueueBean transQueue , ClientBean clientBean ,
      EventBean eventBean ) {
    int processCode = ProcessCode.PROCESS_SUCCEED;
    try {

      // check for ONE_PING_ONLY
      log.debug( "Trying to check one ping only" );
      if ( support.eventSupport().checkOnePingOnly( imsg , omsgs ) ) {
        log.debug( "Identified message as one ping only , close session" );
        session.deleteSession( transQueue );
        return processCode;
      }

      // process exchange
      if ( transQueue.getCode().equals( "X" ) ) {
        log.debug( "Trigger as exchange message" );
        String response = support.processExchange( transQueue ,
            imsg.getMessageContent() , imsg.getOriginalProvider() );
        log.debug( "Compose exchange message response " + response );
        if ( ( response == null ) || !response.equals( "default" ) ) {
          omsgs.add( support.createReplyMessage( imsg , response ) );
        } else {
          log.warning( "Failed to create exchange message "
              + ", is an error message" );
        }
        return processCode;
      }

      // get process steps from event
      log.debug( "Extracting event process steps" );
      ProcessBean processSteps[] = support.eventSupport().extractProcessClob(
          eventBean , true , imsg.getMessageContent() );
      if ( processSteps == null ) {
        log.warning( "Failed to extract event process steps "
            + ", is an error message" );
        return processCode;
      }
      log.debug( "Successfully extracted event process steps : "
          + StringUtils.join( processSteps , "," ) );

      // process lucky draw event
      if ( eventBean.getProcessType() == EventBean.LUCKY_DRAW_TYPE ) {
        log.debug( "Process message as Lucky Draw Type" );
        omsgs.add( support.luckDrawEventService().luckyDraw( eventBean ,
            processSteps , transQueue , false , imsg ) );
        return processCode;
      }

      // process ping count event
      if ( eventBean.getProcessType() == EventBean.PING_COUNT_TYPE ) {
        log.debug( "Process message as Ping Count Type" );
        String response = support.pingCountEventService().pingCountEvent(
            eventBean , processSteps , transQueue , imsg , false );
        if ( !StringUtils.isBlank( response ) ) {
          omsgs.add( support.createReplyMessage( imsg , response ) );
        }
        return processCode;
      }

      // process tell a friend event
      if ( eventBean.getProcessType() == EventBean.TELL_A_FRIEND_TYPE ) {
        log.debug( "Process message as Tell a Friend Type" );
        ProcessBean processBean = processSteps[0];
        transQueue = support.appendParameters( transQueue , imsg , processBean ,
            processSteps );
        String response = support.tellAFriendEventService().tellAFriend(
            processSteps , transQueue , imsg , omsgs );
        if ( !StringUtils.isBlank( response ) ) {
          omsgs.add( support.createReplyMessage( imsg , response ) );
        }
        return processCode;
      }

      // resolve the next process bean
      ProcessBean processBean = null;
      try {
        log.debug( "Trying to resolve next process bean" );
        processBean = support.eventSupport().resolveNextProcessBean( this ,
            transQueue , processSteps , imsg );
      } catch ( Exception e ) {
        log.warning( "Failed to get next process step , " + e );
      }
      if ( processBean == null ) {
        log.warning( "Found empty next process step" );
        if ( imsg.getMessageContent().equals( "recursed" ) ) {
          log.warning( "Stopped the process , found recursed message "
              + ", doesn't trigger the Bogus" );
          return processCode;
        }
        if ( eventBean.getChannel() ) {
          log.warning( "Stopped the process , found event as channel "
              + ", doesn't trigger the Bogus" );
          return processCode;
        }
        // process invalid message as bogus
        log.warning( "Can not resolve menu choice "
            + ", process as bogus message" );
        support.processBogusMesssage( imsg , "unresolved menu choice" );
        // create invalid reply message
        log.debug( "Creating invalid reply message" );
        omsgs.add( support.createInvalidReplyMessage( imsg ,
            SpecialMessageName.INVALID_MENU_CHOICE ) );
        return processCode;
      }
      log.debug( "Found next process : type = " + processBean.getType()
          + " , response = "
          + StringEscapeUtils.escapeJava( processBean.getResponse() ) );

      { // append parameters (name-value pairs) of transaction queue record
        log.debug( "Trying to append trans queue parameters" );
        transQueue = support.appendParameters( transQueue , imsg , processBean ,
            processSteps );
        log.debug( "Appended trans queue parameters : "
            + transQueue.getParams() );
      }

      { // update appended parameters
        log.debug( "Trying to update appended trans queue parameters" );
        boolean updatedAppendedParameters = support.updateAppendedParameters(
            transQueue , processBean );
        log.debug( "Updated trans queue parameters : "
            + updatedAppendedParameters );
      }

      { // resolve pending event from mobile user profile
        log.debug( "Trying to get pending event from mobile user profile" );
        EventBean pendingEvent = support.mobileUserService().updateProfile(
            imsg , transQueue , processBean );
        if ( pendingEvent != null ) {
          log.debug( "Found pending event = ( " + pendingEvent.getEventID()
              + " ) " + pendingEvent.getEventName() + " , will use this one" );
          eventBean = pendingEvent;
          processSteps = support.eventSupport().extractProcessClob( eventBean );
          processBean = processSteps[transQueue.getNextStep() - 1]; // zero
          // based
        } else {
          log.debug( "There are no pending event" );
        }
      }

      { // building main outbound transaction message
        TransactionOutputMessage omsg = null;
        if ( StringUtils.equalsIgnoreCase( processBean.getType() ,
            "CREATE_QR_IMAGE" ) ) {
          log.debug( "Build outbound message as qr image" );
          omsg = EventProcessCreateQrImage.process( "" , support , log ,
              transQueue , processBean , imsg , false );
        } else {
          log.debug( "Build outbound message as text" );
          // replace response & rfa variables with parameter values <%...%>
          log.debug( "Trying to update response message "
              + ", by replacing variables <%...%>" );
          processBean = EventTransQueueReservedVariables
              .replaceReservedVariables( log , processBean , transQueue );
          // replace response & rfa reserved variables <#...#>
          log.debug( "Trying to update response message "
              + ", by replacing reserved variables <#...#>" );
          processBean = EventOutboundReservedVariables
              .replaceReservedVariables( log , processBean , transQueue , imsg );
          processBean = EventOutboundXipmeReservedVariables
              .replaceXipmeReservedVariables( log , processBean , transQueue ,
                  imsg );
          // create outbound message based on process's response
          omsg = support.createReplyMessage( imsg ,
              EventResponse.buildResponse( log , processBean ) );
          // update set send date str param from inbound message
          String msgParamVal = (String) imsg
              .getMessageParam( TransactionMessageParam.HDR_SET_SENDDATESTR );
          if ( msgParamVal != null ) {
            omsg.addMessageParam( TransactionMessageParam.HDR_SET_SENDDATESTR ,
                msgParamVal );
            log.debug( "Updated output msg param : "
                + TransactionMessageParam.HDR_SET_SENDDATESTR + " = "
                + msgParamVal );
          }
        }
        if ( omsg != null ) {
          omsgs.addFirst( omsg );
          log.debug( "Created main outputbound message : messageId = "
              + omsg.getMessageId() + " , count = " + omsg.getMessageCount()
              + " , response = "
              + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );
        }
      }

      // verify and execute each of the steps
      processBean = executeProcessNextSteps( transQueue , processBean ,
          processSteps , imsg , omsgs );
      if ( processBean == null ) {
        log.warning( "Failed to execute and resolve the process next steps" );
        // create invalid reply message
        log.debug( "Creating invalid reply message" );
        omsgs.add( support.createInvalidReplyMessage( imsg ,
            SpecialMessageName.INVALID_MENU_CHOICE ) );
        return processCode;
      }

      // when found process next step end , will close the session here
      String nextStep = StringUtils.trimToEmpty( processBean.getNextStep() );
      if ( StringUtils.equals( nextStep , "END" ) ) {
        log.debug( "Process transaction queue is finished here "
            + ", found end step" );
        if ( support.transLogService().logTransaction( transQueue , false ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL ) ) {
          log.debug( "Created new record in the transaction log table" );
        } else {
          log.warning( "Failed to create new record in the "
              + "transaction log table" );
        }
        long callingEventID = transQueue.getCallingEventID();
        if ( callingEventID != 0 ) {
          EventService eventService = new EventService();
          transQueue.setEventID( callingEventID );
          transQueue
              .setCode( ( StringUtils.split(
                  ( eventService.select( callingEventID ) ).getCodes() , "," ) )[0] );
          transQueue.setCallingEventID( 0 );
          transQueue.setMessageCount( 0 );
          transQueue.setNextStep( 2 );
          log.debug( "Recompose transaction queue = " + transQueue );
          if ( support.transQueueService().update( transQueue ) ) {
            log.debug( "Updated transaction queue : queueId = "
                + transQueue.getQueueId() + " , phone = "
                + transQueue.getPhone() );
          } else {
            log.warning( "Failed to update transaction queue bean" );
          }
        } else {
          if ( support.transQueueService().delete( transQueue ) ) {
            log.debug( "Deleted transaction queue : queueId = "
                + transQueue.getQueueId() + " , phone = "
                + transQueue.getPhone() );
          } else {
            log.warning( "Failed to delete transaction queue bean" );
          }
        }
      } else {
        // else just update transaction queue
        try {
          transQueue.setNextStep( Integer.parseInt( nextStep ) );
          if ( support.transQueueService().update( transQueue ) ) {
            log.debug( "Updated next step value in transaction queue "
                + ", with value = " + transQueue.getNextStep() );
          } else {
            log.warning( "Failed to update transaction queue bean" );
          }
        } catch ( Exception e ) {
          log.warning( "Failed to update next step in trans queue , " + e );
        }
      }

      // validate "jump to" or "call" event
      if ( ( !StringUtils.isBlank( processBean.getNextType() ) )
          && ( ",JUMP TO,CALL,JUMP TO WITH NO REPLY".indexOf( ","
              .concat( processBean.getNextType() ) ) > -1 ) ) {
        log.debug( "Found next type as jumpTo or call event : "
            + processBean.getNextType() );
        // send this response
        Calendar c = new GregorianCalendar();
        c.setTime( new Date() );
        c.add( Calendar.SECOND , 5 );
        // ensure this goes out first (send_buffer is LIFO)
        log.debug( "Compose calendar " + c.getTime() );
        // jump to next event
        String localNextType = processBean.getNextType();
        String localNextStep = processBean.getNextStep();
        if ( ( localNextStep != null )
            && ( !localNextStep.equalsIgnoreCase( "END" ) ) ) {
          EventService eventService = new EventService();
          processBean = processSteps[Integer.parseInt( localNextStep ) - 1];
          String temp[] = processBean.getNames();
          String jumpToEventName = temp[0];
          EventBean eb = eventService.select( jumpToEventName ,
              transQueue.getClientID() );
          String codes[] = StringUtils.split( eb.getCodes() , "," );
          String jumpToEventCode = codes[0];
          log.debug( "Perform step " + localNextType + " event name = "
              + jumpToEventName + " , code = " + jumpToEventCode );
          String response = support.jumpToEvent( jumpToEventCode , transQueue ,
              imsg , processBean.getType() , false );
          if ( StringUtils.equalsIgnoreCase( localNextType ,
              "JUMP TO WITH NO REPLY" ) ) {
            response = "";
            log.debug( "Deleted response message "
                + ", found contain no reply keyword step" );
          }

          // verify if there is keyword jumpto inside response message
          if ( ( response != null ) && ( response.length() > 6 )
              && response.substring( 0 , 3 ).equals( "<<<" ) ) {
            log.debug( "Found <<<keyword>>> in response "
                + ", perform jump to another event" );
            StrTok st = new StrTok( response.substring( 3 ) , ">>>" );
            String code = st.nextTok();
            response = support.jumpToEvent( code , transQueue , imsg , "" ,
                false );
          }

          if ( !StringUtils.isBlank( response ) ) {
            log.debug( "Composed message response = "
                + StringEscapeUtils.escapeJava( response ) );
            omsgs.add( support.createReplyMessage( imsg , response ) );
          }

        }
      }

    } catch ( Exception e ) {
      // failed during the process transaction
      log.warning( "Failed to process transaction , " + e );
    }

    return processCode;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

}
