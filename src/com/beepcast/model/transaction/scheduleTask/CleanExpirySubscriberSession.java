package com.beepcast.model.transaction.scheduleTask;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.beepcast.dbmanager.util.DateTimeFormat;
import com.beepcast.model.transaction.TransactionApp;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionLogConstanta;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class CleanExpirySubscriberSession implements Job {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "CleanExpirySubscriberSession" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private boolean initialized;

  private TransactionApp transApp;
  private TransactionConf transConf;

  private int limitRecords;
  private int expiryDays;

  private TransactionQueueService transQueueServ;
  private TransactionLogService transLogServ;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public CleanExpirySubscriberSession() {
    initialized = false;

    transApp = TransactionApp.getInstance();
    if ( transApp == null ) {
      DLog.warning( lctx , "Failed to initialized , found null trans app" );
      return;
    }

    transConf = transApp.getTransactionConf();
    if ( transConf == null ) {
      DLog.warning( lctx , "Failed to initialized , found null trans conf" );
      return;
    }

    Map sessionParams = transConf.getSessionParams();
    if ( sessionParams == null ) {
      DLog.warning( lctx , "Failed to initialized , found null session params" );
      return;
    }

    Integer intLimitRecords = (Integer) sessionParams
        .get( TransactionConf.SESSION_PARAM_ID_LIMIT_RECORDS );
    if ( intLimitRecords == null ) {
      DLog.warning( lctx , "Failed to initialized "
          + ", found empty session param limit records" );
      return;
    }
    limitRecords = intLimitRecords.intValue();

    Integer intExpiryDays = (Integer) sessionParams
        .get( TransactionConf.SESSION_PARAM_ID_EXPIRY_DAYS );
    if ( intExpiryDays == null ) {
      DLog.warning( lctx , "Failed to initialized "
          + ", found empty session param expiry days" );
      return;
    }
    expiryDays = intExpiryDays.intValue();

    transQueueServ = new TransactionQueueService();
    transLogServ = new TransactionLogService();

    initialized = true;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void execute( JobExecutionContext context )
      throws JobExecutionException {

    // verify initialized
    if ( !initialized ) {
      return;
    }

    // log it first
    DLog.debug( lctx , "Cleaning subscriber session(s) : expiry = "
        + expiryDays + " day(s) , limit = " + limitRecords + " record(s)" );

    // execute clean expiry sessions
    long deltaTime = System.currentTimeMillis();
    int totalSessions = cleanExpirySessions( expiryDays , limitRecords );
    deltaTime = System.currentTimeMillis() - deltaTime;

    // log it
    DLog.debug( lctx , "Successfully clean total " + totalSessions
        + " subscriber session(s) , took " + deltaTime + " ms" );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public int cleanExpirySessions( int expiryDays , int limitRecords ) {
    int totalSessions = 0;
    try {
      int ctrLoop = 0 , maxLoop = 1000;
      while ( ctrLoop < maxLoop ) {

        Thread.sleep( 2000 );

        // load the expired record(s)
        String criteria = "( DATEDIFF( CURDATE() , date_updated ) > "
            + expiryDays + " )";
        Vector vectorTransQueues = transQueueServ.select( null , null ,
            criteria , false , limitRecords );

        // bypass for the empty
        if ( ( vectorTransQueues == null ) || ( vectorTransQueues.size() < 1 ) ) {
          break;
        }

        // close sessions
        totalSessions += closeSessions( vectorTransQueues );

        // iterate the next loop
        ctrLoop = ctrLoop + 1;

      } // while ( ctrLoop < maxLoop )
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to close trans queue record(s) , " + e );
    }
    return totalSessions;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private int closeSessions( Vector vectorTransQueues ) {
    int totalSessions = 0;

    if ( vectorTransQueues == null ) {
      return totalSessions;
    }

    DLog.debug( lctx , "Found total " + vectorTransQueues.size()
        + " expiry session(s) are ready to close" );

    TransactionQueueBean transQueueBean = null;
    Iterator iterTransQueues = vectorTransQueues.iterator();
    while ( iterTransQueues.hasNext() ) {
      transQueueBean = (TransactionQueueBean) iterTransQueues.next();
      if ( transQueueBean == null ) {
        continue;
      }
      if ( !closeSession( transQueueBean ) ) {
        continue;
      }
      totalSessions = totalSessions + 1;
    }

    return totalSessions;
  }

  private boolean closeSession( TransactionQueueBean transQueue ) {
    boolean result = false;

    if ( transQueue == null ) {
      return result;
    }

    String phone = transQueue.getPhone();
    if ( StringUtils.isBlank( phone ) ) {
      return result;
    }

    String headerLog = "[TRANS-SESSION-" + phone + "] ";

    try {

      if ( !transQueueServ.delete( transQueue ) ) {
        DLog.warning( lctx , headerLog + "Failed to close session "
            + ", found failed to delete inside trans queue " );
      }

      if ( !transLogServ.logTransaction( transQueue ,
          TransactionLogConstanta.CLOSED_REASON_EXPIRY ) ) {
        DLog.warning( lctx , headerLog + "Failed to close session "
            + ", found failed to insert record into trans log" );
      }

      DLog.debug(
          lctx ,
          headerLog + "Closed expiry session : eventId = "
              + transQueue.getEventID() + " , clientId = "
              + transQueue.getClientID() + " , messageCount = "
              + transQueue.getMessageCount() + " , code = "
              + transQueue.getCode() + " , dateTime = "
              + DateTimeFormat.convertToString( transQueue.getDateTm() )
              + " , params = " + transQueue.getParams() );

      Thread.sleep( 10 );

      result = true;

    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to close expiry session , " + e );

    }
    return result;
  }

}
