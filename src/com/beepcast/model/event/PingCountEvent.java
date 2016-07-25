package com.beepcast.model.event;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.model.transaction.BogusRequestService;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionLogConstanta;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.beepcast.router.RouterApp;
import com.beepcast.util.StrTok;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class PingCountEvent {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "PingCountEvent" );

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

  public PingCountEvent() {
    init();
  }

  public PingCountEvent( TransactionProcessBasic trans ) {
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

  public String pingCountEvent( EventBean eventBean ,
      ProcessBean processSteps[] , TransactionQueueBean transObj ,
      TransactionInputMessage imsg , boolean simulation ) throws IOException {
    String response = null;

    // get current ping count
    int pingCount = (int) eventBean.getPingCount();
    log.debug( "Define current ping index = " + pingCount );

    // find corresponding message
    for ( int i = 0 ; i < processSteps.length ; i++ ) {
      ProcessBean processBean = processSteps[i];
      if ( processBean == null ) {
        continue;
      }
      String rfa = processBean.getRfa();
      if ( ( rfa == null ) || ( rfa.equals( "" ) ) ) {
        continue;
      }

      int intBeginRange = 0 , intEndRange = 999999;
      StrTok st = new StrTok( rfa , ":" );
      String strBeginRange = st.nextTok();
      String strEndRange = st.nextTok();
      try {
        intBeginRange = Integer.parseInt( strBeginRange );
      } catch ( NumberFormatException nfe ) {
      }
      try {
        intEndRange = Integer.parseInt( strEndRange );
      } catch ( NumberFormatException nfe ) {
      }
      log.debug( "Define process step with range scope : " + intBeginRange
          + " - " + intEndRange );

      if ( pingCount < intBeginRange ) {
        continue;
      }
      if ( pingCount > intEndRange ) {
        continue;
      }

      // found current ping count is entering the scope
      log.debug( "Found current process step is in the scope range" );

      if ( processBean.getType().equals( "JUMP TO" ) ) {
        log.debug( "Perform jump to event" );
        String[] processBeanNames = processBean.getNames();
        if ( processBeanNames != null ) {
          String eventName = processBeanNames[0];
          if ( eventName != null ) {
            EventBean eb = eventService.select( eventName ,
                transObj.getClientID() );
            if ( eb != null ) {
              String codes[] = StringUtils.split( eb.getCodes() , "," );
              if ( codes != null ) {
                response = trans.support().jumpToEvent( codes[0] , transObj ,
                    imsg , "" , simulation );
                log.debug( "Done jump to event , with result response = "
                    + StringEscapeUtils.escapeJava( response ) );
              } else {
                log.warning( "Failed to process jump to "
                    + ", found empty event codes" );
              }
            } else {
              log.warning( "Failed to process jump to "
                  + ", can find event bean with name = " + eventName
                  + " , and client id = " + transObj.getClientID() );
            }
          } else {
            log.warning( "Failed to process jump to "
                + ", found null event name" );
          }
        } else {
          log.warning( "Failed to process jump to "
              + ", found null proces bean names" );
        }
      } else {
        try {
          response = processBean.getResponse();
          response = EventTransQueueReservedVariables.replaceReservedVariables(
              log , response , transObj );
          response = EventOutboundReservedVariables.replaceReservedVariables(
              log , response , imsg );
          log.debug( "Process event ping count , with result response = "
              + StringEscapeUtils.escapeJava( response ) );
        } catch ( Exception e ) {
          log.warning( "Failed to process ping count event , " + e );
        }
      }

      break;

    }
    if ( ( response == null ) || response.equals( "" ) ) {
      log.debug( "Found empty corresponding message response, stop here." );
      return response;
    }

    // delete transaction from queue
    transQueueService.delete( transObj );
    log.debug( "Deleted trans queue record , with : eventId = "
        + transObj.getEventID() );

    // insert permanent log record
    transLogService.logTransaction( transObj , simulation ,
        TransactionLogConstanta.CLOSED_REASON_NORMAL );
    log.debug( "Stored log transaction , with : eventId = "
        + transObj.getEventID() );

    return response;
  } // pingCountEvent()

} // eof
