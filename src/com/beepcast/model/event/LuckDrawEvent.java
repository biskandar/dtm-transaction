package com.beepcast.model.event;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.dbmanager.util.DateTimeFormat;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.model.transaction.BogusRequestService;
import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionLogBean;
import com.beepcast.model.transaction.TransactionLogConstanta;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.beepcast.router.RouterApp;
import com.beepcast.util.NamingService;
import com.beepcast.util.StrTok;
import com.beepcast.util.Util;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

/*******************************************************************************
 * Luck draw event class.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class LuckDrawEvent {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "LuckDrawEvent" );

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

  public LuckDrawEvent() {
    init();
  }

  public LuckDrawEvent( TransactionProcessBasic trans ) {
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

  /*****************************************************************************
   * Process lucky draw.
   * <p>
   * 
   * @param _event
   *          The event record.
   * @param processSteps
   *          Array of process steps.
   * @param transObj
   *          This transaction queue object.
   * @param simulation
   * @param receiveRecord
   *          Contains sms message.
   * @return An array of menu numbers.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public TransactionOutputMessage luckyDraw( EventBean _event ,
      ProcessBean processSteps[] , TransactionQueueBean transObj ,
      boolean simulation , TransactionInputMessage imsg ) throws IOException {
    TransactionOutputMessage omsg = null;

    EventSupport eventSupport = new EventSupport( trans );
    String _provider = imsg.getOriginalProvider();
    String winningPhone = "";
    NamingService nameSvc = NamingService.getInstance();
    String baseUploadPath = (String) nameSvc.getAttribute( "baseUploadPath" );

    // get lucky draw type
    long flags = _event.getBitFlags();
    long luckyDrawType = 0L;
    if ( ( flags & EventBean.LUCKY_DRAW_RANDOM_WINNER ) == EventBean.LUCKY_DRAW_RANDOM_WINNER ) {
      luckyDrawType = EventBean.LUCKY_DRAW_RANDOM_WINNER;
      log.debug( "Define lucky draw type = RANDOM_WINNER" );
    } else if ( ( flags & EventBean.LUCKY_DRAW_RANDOM_WINNER_MULT ) == EventBean.LUCKY_DRAW_RANDOM_WINNER_MULT ) {
      luckyDrawType = EventBean.LUCKY_DRAW_RANDOM_WINNER_MULT;
      log.debug( "Define lucky draw type = RANDOM_WINNER_MULTI" );
    } else if ( ( flags & EventBean.LUCKY_DRAW_MOST_PINGS_WINNER ) == EventBean.LUCKY_DRAW_MOST_PINGS_WINNER ) {
      luckyDrawType = EventBean.LUCKY_DRAW_MOST_PINGS_WINNER;
      log.debug( "Define lucky draw type = MOST_PINGS_WINNER" );
    } else {
      log.warning( "Failed to perform lucky draw "
          + ", found anonymous lucky draw type" );

      String messageContent = "INTERNAL SYSTEM ERROR: Anonymous lucky draw type.";
      omsg = trans.support().createReplyMessage( imsg , messageContent );
      transQueueService.delete( transObj );
      transLogService.logTransaction( transObj , simulation ,
          TransactionLogConstanta.CLOSED_REASON_NORMAL );

      return omsg;
    }

    // get participating events
    String eventIDs = _event.getRemindFreq();
    if ( StringUtils.isBlank( eventIDs ) ) {
      log.warning( "Failed to perform lucky draw "
          + ", there no participating events in the provisioning list" );

      String messageContent = "FAILED: There are no participating events. "
          + "This must be setup in the BEEPadmin web app.";
      omsg = trans.support().createReplyMessage( imsg , messageContent );
      transQueueService.delete( transObj );
      transLogService.logTransaction( transObj , simulation ,
          TransactionLogConstanta.CLOSED_REASON_NORMAL );

      return omsg;
    }

    // get lucky draw event date
    Date fromDate = _event.getDateInserted(); // _event.getStartDate();
    long oneYearDuration = 365 * 24 * 60 * 60 * 1000L;
    Date toDate = new Date( fromDate.getTime() + oneYearDuration ); // _event.getEndDate();

    log.debug( "Defined lucky draw period event date ["
        + DateTimeFormat.convertToString( fromDate ) + "] until ["
        + DateTimeFormat.convertToString( toDate ) + "]" );

    /*----------------------------
      load data
    ----------------------------*/
    String criteria = "event_id in (" + eventIDs + ")";
    Vector vTransQueue = transQueueService
        .select( fromDate , toDate , criteria );
    Vector vTransLog = transLogService.select( fromDate , toDate , criteria );
    Vector vMerged = transLogService.mergeData( vTransQueue , vTransLog );
    if ( vMerged.size() == 0 ) {
      log.warning( "Failed to perform lucky draw "
          + ", there no participating events during the event period" );

      String messageContent = "FAILED: There are no participants in the event period time.";
      omsg = trans.support().createReplyMessage( imsg , messageContent );
      transQueueService.delete( transObj );
      transLogService.logTransaction( transObj , simulation ,
          TransactionLogConstanta.CLOSED_REASON_NORMAL );

      return omsg;
    }

    /*----------------------------
      check for keywords
    ----------------------------*/
    String keywords = _event.getMobileMenuBrandName();
    if ( !keywords.equals( "" ) ) {
      Vector vKeywords = null;

      // keywords in file
      if ( keywords.startsWith( "FILE=" ) ) {
        log.debug( "Found keyword load from file" );

        String keywordFile = baseUploadPath + "clients/" + _event.getClientID()
            + "/" + keywords.substring( 5 );
        vKeywords = Util.fileToWords( keywordFile );
        if ( vKeywords == null ) {
          log.warning( "Failed to perform lucky draw , found no file found" );

          String messageContent = "INTERNAL SYSTEM ERROR: No file found";
          omsg = trans.support().createReplyMessage( imsg , messageContent );
          transQueueService.delete( transObj );
          transLogService.logTransaction( transObj , simulation ,
              TransactionLogConstanta.CLOSED_REASON_NORMAL );

          return omsg;
        }
      }

      // keywords in table
      else if ( keywords.startsWith( "TABLE=" ) ) {
        log.debug( "Found keyword load from table" );

        vKeywords = new Vector( 1000 , 1000 );
        String keywordTable = keywords.substring( 6 );
        if ( vKeywords.size() == 0 ) {
          log.warning( "Failed to perform lucky draw , found no table found" );

          String messageContent = "INTERNAL SYSTEM ERROR: No table found";
          omsg = trans.support().createReplyMessage( imsg , messageContent );
          transQueueService.delete( transObj );
          transLogService.logTransaction( transObj , simulation ,
              TransactionLogConstanta.CLOSED_REASON_NORMAL );

          return omsg;
        }
      }

      // keywords in event
      else {
        log.debug( "Found keyword load from mobile menu brand name : "
            + StringEscapeUtils.escapeJava( keywords ) );

        vKeywords = new Vector( 10 );
        String keyword = "";
        StrTok st = new StrTok( keywords , " ," );
        while ( !( keyword = st.nextTok().trim() ).equals( "" ) ) {
          vKeywords.addElement( keyword.toUpperCase() );
        }

      }

      // count matching keywords
      int requiredMatches = 0;
      try {
        requiredMatches = Integer.parseInt( _event.getUnsubscribeResponse() );
      } catch ( NumberFormatException e ) {
        log.warning( "Failed to parse required total matches , " + e );
      }
      log.debug( "Define total required matches = " + requiredMatches );

      for ( int i = 0 ; i < vMerged.size() ; i++ ) {
        int matches = 0;
        TransactionLogBean transLog = (TransactionLogBean) vMerged
            .elementAt( i );
        String params = transLog.getParams();
        String param = "";
        StrTok st = new StrTok( params , "," );
        while ( !( param = st.nextTok() ).equals( "" ) ) {
          StrTok st2 = new StrTok( param , "=" );
          st2.nextTok();
          String value = st2.nextTok();
          if ( vKeywords.contains( value.toUpperCase() ) ) {
            matches++;
          }
        }
        if ( matches < requiredMatches ) {
          vMerged.remove( i );
          i--;
        }
      }

      // check for participants with matching keywords
      if ( vMerged.size() == 0 ) {
        log.warning( "There is no participants with matching keywords" );

        String messageContent = "FAILED: There are no participants with matching keywords.";
        omsg = trans.support().createReplyMessage( imsg , messageContent );
        transQueueService.delete( transObj );
        transLogService.logTransaction( transObj , simulation ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );

        return omsg;
      }

    } // check for keywords

    // build list of phone numbers
    Vector vPhones = new Vector( 1000 , 1000 );
    for ( int i = 0 ; i < vMerged.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vMerged.elementAt( i );
      String phone = transLog.getPhone();
      if ( !vPhones.contains( phone )
          || luckyDrawType != EventBean.LUCKY_DRAW_RANDOM_WINNER ) {
        vPhones.addElement( phone );
      }
    }
    log.debug( "Define total candidate winners = " + vPhones.size()
        + " phone(s)" );

    // get current winners list
    ProcessBean _process = processSteps[2];
    String winnersList = _process.getResponse();
    log.debug( "Define list of previous winner = " + winnersList );
    if ( !winnersList.equals( "" )
        && ( StringUtils.split( winnersList , "," ).length >= vPhones.size() ) ) {
      log.debug( "Failed to perform lucky draw "
          + ", found no more phone winners" );

      String messageContent = "No more phone numbers. "
          + "Everyone participating has already won a lucky draw.";
      omsg = trans.support().createReplyMessage( imsg , messageContent );
      transQueueService.delete( transObj );
      transLogService.logTransaction( transObj , simulation ,
          TransactionLogConstanta.CLOSED_REASON_NORMAL );

      return omsg;
    }

    // LUCKY_DRAW_RANDOM_WINNER
    if ( luckyDrawType == EventBean.LUCKY_DRAW_RANDOM_WINNER
        || luckyDrawType == EventBean.LUCKY_DRAW_RANDOM_WINNER_MULT ) {
      log.debug( "Finding phone winner based on random result" );
      boolean gotWinner = false;
      for ( int i = 0 ; i < vPhones.size() * 100 ; i++ ) {
        int randomIndex = (int) ( Math.random() * vPhones.size() );
        winningPhone = (String) vPhones.elementAt( randomIndex );
        if ( winnersList.equals( "" )
            || winnersList.indexOf( winningPhone ) == -1 ) {
          gotWinner = true;
          break;
        }
      }
      if ( !gotWinner ) {
        log.debug( "Failed to perform lucky draw "
            + ", found no more phone winners" );

        String messageContent = "No more phone numbers. "
            + "Everyone participating has already won a lucky draw.";
        omsg = trans.support().createReplyMessage( imsg , messageContent );
        transQueueService.delete( transObj );
        transLogService.logTransaction( transObj , simulation ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );

        return omsg;
      }
      log.debug( "Found phone winner from random mechanism = " + winningPhone );
    }

    // LUCKY_DRAW_MOST_PINGS_WINNER
    else {
      log.debug( "Finding phone winner based on most pings" );
      boolean gotWinner = false;
      Util.sortVector( vPhones );
      winningPhone = (String) vPhones.elementAt( 0 );
      String lastPhone = winningPhone;
      String thisPhone = winningPhone;
      int winningCount = 0;
      int thisCount = 1;
      for ( int i = 1 ; i < vPhones.size() ; i++ ) {
        thisPhone = (String) vPhones.elementAt( i );
        if ( thisPhone.equals( lastPhone ) )
          thisCount++;
        else {
          if ( thisCount > winningCount ) {
            if ( winnersList.equals( "" )
                || winnersList.indexOf( lastPhone ) == -1 ) {
              gotWinner = true;
              winningPhone = lastPhone;
              winningCount = thisCount;
            }
          }
          lastPhone = thisPhone;
          thisCount = 1;
        }
      }
      if ( thisCount > winningCount ) {
        if ( winnersList.equals( "" ) || winnersList.indexOf( thisPhone ) == -1 ) {
          gotWinner = true;
          winningPhone = thisPhone;
        }
      }
      if ( !gotWinner ) {
        log.debug( "Failed to perform lucky draw "
            + ", found no more phone winners" );

        String messageContent = "No more phone numbers. "
            + "Everyone participating has already won a lucky draw.";
        omsg = trans.support().createReplyMessage( imsg , messageContent );
        transQueueService.delete( transObj );
        transLogService.logTransaction( transObj , simulation ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );

        return omsg;
      }
      log.debug( "Found phone winner from most pings = " + winningPhone );
    }

    // update winners list
    if ( winnersList.equals( "" ) ) {
      winnersList = winningPhone;
    } else {
      winnersList += ", " + winningPhone;
    }
    _process.setResponse( winnersList );
    String processClob = eventSupport.buildProcessClob( processSteps );
    _event.setProcess( processClob );
    eventService.update( _event );
    log.debug( "Reupdate all the winners in the event "
        + ", with list string winner = " + winnersList );

    // setup lucky draw master message
    _process = processSteps[0];
    String message = _process.getResponse();
    if ( message == null || message.equals( "" ) ) {
      log.warning( "Failed to setup lucky draw master message "
          + ", found null response" );
      return omsg;
    }
    String managerPhone = transObj.getPhone();
    if ( message.indexOf( "<#USERPHONE#>" ) != -1 ) {
      transObj.setPhone( winningPhone );
      message = EventTransQueueReservedVariables.replaceReservedVariables( log ,
          message , transObj );
      message = EventOutboundReservedVariables.replaceReservedVariables( log ,
          message , imsg );
      transObj.setPhone( managerPhone );
    } else {
      message += " " + winningPhone;
    }

    log.debug( "Sending message to manager phone mobile (" + managerPhone
        + ") : " + StringEscapeUtils.escapeJava( message ) );

    omsg = trans.support().createReplyMessage( imsg , message );
    transQueueService.delete( transObj );
    transLogService.logTransaction( transObj , simulation ,
        TransactionLogConstanta.CLOSED_REASON_NORMAL );

    // setup winning mobile user message
    _process = processSteps[1];
    message = _process.getResponse();
    if ( StringUtils.isBlank( message ) ) {
      log.warning( "Failed to setup winning mobile user message "
          + ", found null response" );
      return omsg;
    }

    String senderId = null;
    EventBean eventBean = (EventBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( eventBean != null ) {
      senderId = eventBean.getSenderID();
      if ( StringUtils.isBlank( senderId ) ) {
        senderId = eventBean.getOutgoingNumber();
      }
    }

    // send it
    if ( routerApp.sendMtMessage(
        TransactionMessageFactory.generateMessageId( "INT" ) ,
        MessageType.TEXT_TYPE , 1 , message , 1.0 , winningPhone , "EE1" ,
        omsg.getEventId() , 0 , senderId , omsg.getPriority() , 0 , new Date() ,
        null ) ) {
      log.debug( "Successfully insert winning "
          + "mobile user message into send buffer" );
    } else {
      log.warning( "Failed to insert winning "
          + "mobile user message into send buffer" );
    }

    return omsg;
  } // luckyDraw()

}
