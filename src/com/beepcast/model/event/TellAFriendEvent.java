package com.beepcast.model.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.model.friend.FriendBean;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.model.transaction.BogusRequestService;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionLogConstanta;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.beepcast.router.RouterApp;
import com.beepcast.util.StrTok;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TellAFriendEvent {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TellAFriendEvent" );

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

  private TransactionProcessBasic trans;
  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TellAFriendEvent() {
    init();
  }

  public TellAFriendEvent( TransactionProcessBasic trans ) {
    init();
    if ( trans != null ) {
      this.trans = trans;
      this.log = trans.log();
    } else {
      this.log = new TransactionLog();
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

  }

  public String tellAFriend( ProcessBean processSteps[] ,
      TransactionQueueBean transObj , TransactionInputMessage imsg , List omsgs )
      throws IOException {
    String messageResponse = null;

    if ( processSteps == null ) {
      log.warning( "Failed to process tell a friend "
          + ", found null process steps" );
      return messageResponse;
    }
    if ( transObj == null ) {
      log.warning( "Failed to process tell a friend "
          + ", found null trans queue" );
      return messageResponse;
    }
    if ( imsg == null ) {
      log.warning( "Failed to process tell a friend "
          + ", found null input message" );
      return messageResponse;
    }
    if ( omsgs == null ) {
      log.warning( "Failed to process tell a friend "
          + ", found null output messages" );
      return messageResponse;
    }

    // log it first
    log.debug( "Processing tell a friend : transQueue.eventId = "
        + transObj.getEventID() + " , transQueue.code = " + transObj.getCode() );

    // init parameters
    String tqParams = transObj.getParams();
    String senderPhone = transObj.getPhone();
    String senderName = "";
    ArrayList friendPhones = new ArrayList();

    // get parameters
    StrTok st = new StrTok( tqParams , "," );
    while ( true ) {
      try {
        String param = st.nextTok();
        if ( ( param == null ) || ( param.equals( "" ) ) ) {
          break;
        }
        StrTok st2 = new StrTok( param , "=" );
        String paramName = st2.nextTok();

        // get sender name
        if ( paramName.equals( "NAME" ) ) {
          senderName = st2.nextTok();
          if ( senderName.equals( "" ) || StringUtils.isNumeric( senderName ) ) {
            transQueueService.delete( transObj );
            transLogService.logTransaction( transObj , false ,
                TransactionLogConstanta.CLOSED_REASON_NORMAL );
            log.warning( "Failed to get parameters , found blank senderName " );
            return messageResponse;
          }
          if ( senderName.length() > 10 ) {
            log.debug( "Chop senderName : " + senderName + " -> "
                + senderName.substring( 0 , 10 ) );
            senderName = senderName.substring( 0 , 10 );
          }
        }

        // get friend phone numbers
        if ( paramName.startsWith( "PHONE" ) ) {
          String friendPhone = st2.nextTok();
          if ( ( friendPhone != null ) && ( friendPhone.length() > 6 ) ) {
            if ( friendPhone.startsWith( "+" ) ) {
              friendPhone = friendPhone.substring( 1 );
            } else {
              friendPhone = senderPhone.substring( 1 , 3 ).concat( friendPhone );
            }
            if ( StringUtils.isNumeric( friendPhone ) ) {
              friendPhone = "+".concat( friendPhone );
              friendPhones.add( friendPhone );
            }
          }
        }

      } catch ( Exception e ) {
        log.warning( "Failed to process tell a friend , " + e );
        return messageResponse;
      }
    } // while ( true )
    log.debug( "Parsed incoming message , and read : tqParams = " + tqParams
        + " , senderPhone = " + senderPhone + " , senderName = " + senderName
        + " , friendPhones = " + friendPhones );

    if ( friendPhones.size() < 1 ) {
      log.warning( "Failed to process tell a friend "
          + ", found empty friend phones" );
      return messageResponse;
    }

    if ( processSteps.length > 0 ) {
      // send message to sender
      ProcessBean processBean = processSteps[0];
      messageResponse = EventOutboundReservedVariables
          .replaceReservedVariableWithValue( processBean.getResponse() ,
              "SENDER" , senderName );
    }

    // send message to friends
    if ( processSteps.length > 1 ) {
      ProcessBean processBean = processSteps[1];
      String messageToFriend = EventOutboundReservedVariables
          .replaceReservedVariableWithValue( processBean.getResponse() ,
              "SENDER" , senderName );
      log.debug( "Composed message response for friend : "
          + StringEscapeUtils.escapeJava( messageToFriend ) );
      if ( !StringUtils.isBlank( messageToFriend ) ) {
        Iterator iterFriendPhones = friendPhones.iterator();
        while ( iterFriendPhones.hasNext() ) {
          try {
            String friendPhone = (String) iterFriendPhones.next();
            if ( StringUtils.isBlank( friendPhone ) ) {
              continue;
            }

            // compose message for friend

            TransactionOutputMessage omsg = trans.support().createReplyMessage(
                imsg , messageToFriend );
            omsg.setMessageId( TransactionMessageFactory
                .generateMessageId( "INT" ) );
            String headerLog = "[MsgToFriend-" + omsg.getMessageId() + "] ";
            omsg.setDestinationAddress( friendPhone );
            if ( !trans.support().resolveOriginalAddressAndMask( omsg ) ) {
              log.warning( headerLog + "Failed to process message to friend "
                  + ", found failed to resolve original address and mask" );
              continue;
            }
            trans.support().resolveMessageContent( omsg );
            if ( omsg.getMessageCount() < 1 ) {
              log.warning( headerLog + "Failed to process message to friend "
                  + ", found failed to resolve message content" );
              continue;
            }
            omsgs.add( omsg );
            log.debug( headerLog + "Created output message : messageId = "
                + omsg.getMessageId() + " , originalAddress = "
                + omsg.getOriginalAddress() + " , destinationAddress = "
                + omsg.getDestinationAddress() + " , messageCount = "
                + omsg.getMessageCount() + " , messageContent = "
                + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );

            // tracked friend as history

            FriendBean friendBean = new FriendBean();
            friendBean.setSenderPhone( senderPhone );
            friendBean.setFriendPhone( friendPhone );
            friendBean.setEventID( transObj.getEventID() );
            friendBean.insert();
            log.debug( headerLog + "Inserted new friend : senderPhone = "
                + friendBean.getSenderPhone() + " , friendPhone = "
                + friendBean.getFriendPhone() + " , eventId = "
                + friendBean.getEventID() );

          } catch ( Exception e ) {
            log.warning( "Failed to process message to friend , " + e );
          }
        } // while ( iterFriendPhones.hasNext() )
      } // if ( !StringUtils.isBlank( messageToFriend ) )
    } // if ( processSteps.length > 1 )

    // close transaction
    log.debug( "Closing transaction log and queue for tell a friend event" );
    transLogService.logTransaction( transObj , false ,
        TransactionLogConstanta.CLOSED_REASON_NORMAL );
    long callingEventID = transObj.getCallingEventID();
    if ( callingEventID != 0 ) {
      transObj.setEventID( callingEventID );
      transObj.setCode( ( StringUtils.split(
          ( eventService.select( callingEventID ) ).getCodes() , "," ) )[0] );
      transObj.setCallingEventID( 0 );
      transObj.setMessageCount( 0 );
      transObj.setNextStep( 2 );
      transQueueService.update( transObj );
    } else {
      transQueueService.delete( transObj );
    }

    return messageResponse;
  } // tellAFriend()
} // eof
