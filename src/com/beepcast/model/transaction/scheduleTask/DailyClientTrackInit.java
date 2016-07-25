package com.beepcast.model.transaction.scheduleTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.beepcast.billing.BillingApp;
import com.beepcast.billing.BillingResult;
import com.beepcast.billing.BillingStatus;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.database.DatabaseLibrary.QueryItem;
import com.beepcast.database.DatabaseLibrary.QueryResult;
import com.beepcast.model.transaction.billing.AccountProfile;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class DailyClientTrackInit implements Job {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "DailyClientTrackInit" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private DatabaseLibrary dbLib;
  private BillingApp billingApp;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public DailyClientTrackInit() {
    dbLib = DatabaseLibrary.getInstance();
    billingApp = BillingApp.getInstance();
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void execute( JobExecutionContext context )
      throws JobExecutionException {
    // get all active client id
    int totalRecords = processClients( 0 );
    DLog.debug( lctx , "Successfully processed the scheduler task , total = "
        + totalRecords + " record(s)" );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean execute( int clientid ) {
    boolean result = false;
    // get specific client id
    if ( clientid < 1 ) {
      DLog.warning( lctx , "Failed to execute , found zero client id" );
      return result;
    }
    if ( processClients( clientid ) > 0 ) {
      result = true;
    }
    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private int processClients( int clientid ) {
    int totalRecords = 0;
    Map mapClientIds = generateCandidateClientIds( clientid );
    if ( ( mapClientIds == null ) || ( mapClientIds.size() < 1 ) ) {
      return totalRecords;
    }
    List listClientIds = new ArrayList( mapClientIds.keySet() );
    Iterator iterClientIds = listClientIds.iterator();
    while ( iterClientIds.hasNext() ) {
      Integer clientId = (Integer) iterClientIds.next();
      if ( ( clientId == null ) || ( clientId.intValue() < 1 ) ) {
        continue;
      }
      Double initUnit = (Double) mapClientIds.get( clientId );
      if ( processClient( clientId , initUnit ) ) {
        totalRecords = totalRecords + 1;
      }
    }
    return totalRecords;
  }

  private Map generateCandidateClientIds( int clientid ) {
    Map map = new HashMap();

    // compose sql
    String sqlSelect = "SELECT c.client_id , cl.daily_traffic_quota ";
    String sqlFrom = "FROM `client` c ";
    String sqlJoin = "INNER JOIN client_level cl ON cl.id = c.client_level_id ";
    String sqlWhere = "WHERE ( cl.daily_traffic_quota > 0 ) ";
    if ( clientid > 0 ) {
      sqlWhere += "AND ( c.client_id = " + clientid + " ) ";
    }
    String sqlOrder = "ORDER BY c.client_id ASC ";
    String sql = sqlSelect + sqlFrom + sqlJoin + sqlWhere + sqlOrder;

    // execute sql
    QueryResult qr = dbLib.simpleQuery( "profiledb" , sql );
    if ( ( qr == null ) || ( qr.size() < 1 ) ) {
      return map;
    }

    String stemp;
    int itemp;
    double dtemp;
    Integer clientId;
    Double initUnit;

    // retrieve all record(s)
    Iterator iter = qr.iterator();
    while ( iter.hasNext() ) {
      QueryItem qi = (QueryItem) iter.next();

      clientId = null;
      initUnit = null;

      stemp = (String) qi.get( 0 ); // client_id
      if ( ( stemp != null ) && ( !stemp.equals( "" ) ) ) {
        try {
          itemp = Integer.parseInt( stemp );
          if ( itemp > 0 ) {
            clientId = new Integer( itemp );
          }
        } catch ( NumberFormatException e ) {

        }
      }

      stemp = (String) qi.get( 1 ); // daily_traffic_quota
      if ( ( stemp != null ) && ( !stemp.equals( "" ) ) ) {
        try {
          dtemp = Double.parseDouble( stemp );
          if ( dtemp > 0 ) {
            initUnit = new Double( dtemp );
          }
        } catch ( NumberFormatException e ) {

        }
      }

      if ( ( clientId != null ) && ( initUnit != null ) ) {
        map.put( clientId , initUnit );
      }

    } // iterator all record(s)

    return map;
  }

  private boolean processClient( Integer clientId , Double initUnit ) {
    boolean result = false;

    if ( clientId == null ) {
      return result;
    }

    if ( initUnit == null ) {
      return result;
    }

    BillingResult billingResult = null;

    String profileId = AccountProfile.DAILY_CLIENT_TRACK;

    String headerLog = "[Client-" + clientId + "] ";

    billingResult = billingApp.getBalance( profileId , clientId );
    if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_SUCCEED ) {
      billingResult = billingApp.doDebit( profileId , clientId ,
          billingResult.getBalanceAfter() );
      if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_SUCCEED ) {
        billingResult = billingApp.doCredit( profileId , clientId , initUnit );
      }
    } else if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_FAILED_NOACCOUNT ) {
      billingResult = billingApp.doCredit( profileId , clientId , initUnit );
    }
    if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_SUCCEED ) {
      billingResult = billingApp.getBalance( profileId , clientId );
    }
    if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_SUCCEED ) {
      DLog.debug( lctx , headerLog + "Updated daily client track balance = "
          + billingResult.getBalanceAfter() + " unit(s)" );
      result = true;
    }

    return result;
  }
}
