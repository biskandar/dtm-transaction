package com.beepcast.model.transaction;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.database.ConnectionWrapper;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.encrypt.EncryptApp;
import com.beepcast.util.Util;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionLogDAO {

  static final DLogContext lctx = new SimpleContext( "TransactionLogDAO" );

  private DatabaseLibrary dbLib;
  private final String keyPhoneNumber;

  public TransactionLogDAO() {
    dbLib = DatabaseLibrary.getInstance();
    EncryptApp encryptApp = EncryptApp.getInstance();
    keyPhoneNumber = encryptApp.getKeyValue( EncryptApp.KEYNAME_PHONENUMBER );
  }

  public boolean insert( TransactionLogBean tlBean ) {
    boolean result = false;

    Date dateTm = tlBean.getDateTm();
    String phone = tlBean.getPhone();
    String providerId = tlBean.getProviderId();
    String code = tlBean.getCode();
    String params = tlBean.getParams();

    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    phone = ( phone == null ) ? "" : phone.trim();
    providerId = ( providerId == null ) ? "" : providerId.trim();
    code = ( code == null ) ? "" : code.trim();
    params = ( params == null ) ? "" : params.trim();
    String strDateTm = Util.strFormat( dateTm , "yyyy-mm-dd hh:nn:ss" );

    String sqlInsert = "INSERT INTO trans_log (client_id,event_id,next_step"
        + ",catagory_id,date_tm,phone,encrypt_phone,provider_id,message_count"
        + ",code,params,jump_count,location_id,closed_reason_id) ";
    String sqlValues = "VALUES ( " + tlBean.getClientID() + ","
        + tlBean.getEventID() + "," + tlBean.getNextStep() + ","
        + tlBean.getCatagoryID() + ",'" + strDateTm + "','',"
        + sqlEncryptPhoneNumber( phone ) + ",'"
        + StringEscapeUtils.escapeSql( providerId ) + "',"
        + tlBean.getMessageCount() + ",'" + StringEscapeUtils.escapeSql( code )
        + "','" + StringEscapeUtils.escapeSql( params ) + "',"
        + tlBean.getJumpCount() + "," + tlBean.getLocationID() + ","
        + tlBean.getClosedReasonId() + " ) ";
    String sql = sqlInsert + sqlValues;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public Vector select( Date fromDate , Date toDate , String criteria ) {
    return select( fromDate , toDate , criteria , false );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc ) {
    return select( fromDate , toDate , criteria , desc , 1000 );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc , int limit ) {

    // prepare holder
    Vector transObjects = new Vector( 1000 , 1000 );

    // build query

    String sqlSelect = sqlSelect();
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = null;
    if ( fromDate != null ) {
      if ( sqlWhere == null ) {
        sqlWhere = "WHERE ";
      } else {
        sqlWhere += "AND ";
      }
      sqlWhere += "( date_tm >= '"
          + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    }
    if ( toDate != null ) {
      if ( sqlWhere == null ) {
        sqlWhere = "WHERE ";
      } else {
        sqlWhere += "AND ";
      }
      sqlWhere += "( date_tm < '"
          + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    }
    if ( ( criteria != null ) && ( !criteria.equals( "" ) ) ) {
      if ( sqlWhere == null ) {
        sqlWhere = "WHERE ";
      } else {
        sqlWhere += "AND ";
      }
      sqlWhere += criteria + " ";
    }
    String sqlOrder = "ORDER BY date_tm ";
    if ( desc ) {
      sqlOrder += "DESC ";
    }
    String sqlLimit = "";
    if ( limit > 0 ) {
      sqlLimit += "LIMIT " + limit + " ";
    }
    if ( sqlWhere == null ) {
      return transObjects;
    }

    // compose query
    String sql = sqlSelect + sqlFrom + sqlWhere + sqlOrder + sqlLimit;

    // execute query
    ConnectionWrapper conn = dbLib.getReaderConnection( "transactiondb" );
    try {
      Statement stmt = conn.createStatement();
      // DLog.debug( lctx , "Perform " + sql );
      ResultSet rs = stmt.executeQuery( sql );
      while ( rs.next() ) {
        transObjects.addElement( populateBean( rs ) );
      }
      rs.close();
      stmt.close();
    } catch ( SQLException sqle ) {
      DLog.warning( lctx , "Failed to execute query , " + sqle );
    } finally {
      conn.disconnect( true );
    }

    return transObjects;
  }

  public boolean update( TransactionLogBean tlBean ) {
    boolean result = false;

    int logId = tlBean.getLogId();
    Date dateTm = tlBean.getDateTm();
    String phone = tlBean.getPhone();
    String code = tlBean.getCode();
    String params = tlBean.getParams();

    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    phone = ( phone == null ) ? "" : phone.trim();
    code = ( code == null ) ? "" : code.trim();
    params = ( params == null ) ? "" : params.trim();
    String strDateTm = Util.strFormat( dateTm , "yyyy-mm-dd hh:nn:ss" );

    String sqlUpdate = "UPDATE trans_log ";
    String sqlSet = "SET event_id = " + tlBean.getEventID()
        + " , catagory_id = " + tlBean.getCatagoryID() + " , client_id = "
        + tlBean.getClientID() + " , message_count = "
        + tlBean.getMessageCount() + " , code = '"
        + StringEscapeUtils.escapeSql( code ) + "' , params = '"
        + StringEscapeUtils.escapeSql( params ) + "' , jump_count = "
        + tlBean.getJumpCount() + " , location_id = " + tlBean.getLocationID()
        + " ";
    String sqlWhere = "";
    if ( logId > 0 ) {
      sqlWhere = "WHERE ( log_id = " + logId + " ) ";
    } else {
      sqlWhere = "WHERE ( encrypt_phone = " + sqlEncryptPhoneNumber( phone )
          + " ) AND ( date_tm = '" + strDateTm + "' ) ";
    }

    String sql = sqlUpdate + sqlSet + sqlWhere;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public boolean delete( long transLogId ) {
    boolean result = false;
    if ( transLogId < 1 ) {
      return result;
    }

    String sqlDelete = "DELETE FROM trans_log ";
    String sqlWhere = "WHERE ( log_id = " + transLogId + " ) ";

    String sql = sqlDelete + sqlWhere;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter ) {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT SUM(message_count) ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( date_tm >= '"
        + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    sqlWhere += "AND ( date_tm < '"
        + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    if ( clientID > 0 ) {
      sqlWhere += "AND ( client_id = " + clientID + " ) ";
    }
    if ( eventID > 0 ) {
      sqlWhere += "AND ( event_id = " + eventID + " ) ";
    }
    if ( iter > 0 ) {
      sqlWhere += "AND ( message_count = " + iter + " ) ";
    }
    String sql = sqlSelect + sqlFrom + sqlWhere;

    // execute sql

    result = TransactionLogQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter , boolean jumpTo ) {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT SUM(message_count) ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( date_tm >= '"
        + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    sqlWhere += "AND ( date_tm < '"
        + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    if ( clientID > 0 ) {
      sqlWhere += "AND ( client_id = " + clientID + " ) ";
    }
    if ( eventID > 0 ) {
      sqlWhere += "AND ( event_id = " + eventID + " ) ";
    }
    if ( iter > 0 ) {
      sqlWhere += "AND ( message_count = " + iter + " ) ";
    }
    if ( jumpTo ) {
      sqlWhere += "AND ( jump_count > 0 ) ";
    } else {
      sqlWhere += "AND ( jump_count = 0 ) ";
    }
    String sql = sqlSelect + sqlFrom + sqlWhere;

    // execute sql

    result = TransactionLogQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code ) {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT COUNT(log_id) ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( date_tm >= '"
        + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    sqlWhere += "AND ( date_tm < '"
        + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    if ( clientID > 0 ) {
      sqlWhere += "AND ( client_id = " + clientID + " ) ";
    }
    if ( eventID > 0 ) {
      sqlWhere += "AND ( event_id = " + eventID + " ) ";
    }
    if ( code != null ) {
      sqlWhere += "AND ( code = '" + StringEscapeUtils.escapeSql( code )
          + "' ) ";
    }
    String sql = sqlSelect + sqlFrom + sqlWhere;

    // execute sql

    result = TransactionLogQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code , boolean jumpTo ) {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT COUNT(log_id) ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( date_tm >= '"
        + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    sqlWhere += "AND ( date_tm < '"
        + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    if ( clientID > 0 ) {
      sqlWhere += "AND ( client_id = " + clientID + " ) ";
    }
    if ( eventID > 0 ) {
      sqlWhere += "AND ( event_id = " + eventID + " ) ";
    }
    if ( code != null ) {
      sqlWhere += "AND ( code = '" + StringEscapeUtils.escapeSql( code )
          + "' ) ";
    }
    if ( jumpTo ) {
      sqlWhere += "AND ( jump_count > 0 ) ";
    } else {
      sqlWhere += "AND ( jump_count = 0 ) ";
    }
    String sql = sqlSelect + sqlFrom + sqlWhere;

    // execute sql

    result = TransactionLogQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public int getMobileUserCount( long clientID , long eventID , Date fromDate ,
      Date toDate ) {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT COUNT(encrypt_phone) ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( date_tm >= '"
        + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    sqlWhere += "AND ( date_tm < '"
        + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    if ( clientID > 0 ) {
      sqlWhere += "AND ( client_id = " + clientID + " ) ";
    }
    if ( eventID > 0 ) {
      sqlWhere += "AND ( event_id = " + eventID + " ) ";
    }
    String sqlGroup = "GROUP BY encrypt_phone ";
    String sql = sqlSelect + sqlFrom + sqlWhere + sqlGroup;

    // execute sql

    result = TransactionLogQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public Vector getMobileUsers( long clientID , long eventID , Date fromDate ,
      Date toDate ) {
    return getMobileUsers( clientID , eventID , fromDate , toDate , null );
  }

  public Vector getMobileUsers( long clientID , long eventID , Date fromDate ,
      Date toDate , String code ) {
    Vector result = null;

    // compose sql

    String sqlSelect = "SELECT " + sqlDecryptPhoneNumber() + " ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( date_tm >= '"
        + Util.strFormat( fromDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    sqlWhere += "AND ( date_tm < '"
        + Util.strFormat( toDate , "yyyy-mm-dd hh:nn:ss" ) + "' ) ";
    if ( clientID > 0 ) {
      sqlWhere += "AND ( client_id = " + clientID + " ) ";
    }
    if ( eventID > 0 ) {
      sqlWhere += "AND ( event_id = " + eventID + " ) ";
    }
    if ( code != null ) {
      sqlWhere += "AND ( code = '" + StringEscapeUtils.escapeSql( code )
          + "' ) ";
    }
    String sqlGroup = "GROUP BY encrypt_phone ";
    String sql = sqlSelect + sqlFrom + sqlWhere + sqlGroup;

    // execute sql

    result = TransactionLogQuery.getStrFirstValueRecords( sql );

    return result;
  }

  public String[] getEventCodes( long eventID ) {
    String[] result = null;

    // compose sql

    String sqlSelect = "SELECT code ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( event_id = " + eventID + " ) ";
    String sqlGroup = "GROUP BY code ";
    String sql = sqlSelect + sqlFrom + sqlWhere + sqlGroup;

    // execute sql

    Vector vector = TransactionLogQuery.getStrFirstValueRecords( sql );
    if ( vector == null ) {
      return result;
    }

    // vector to array string

    result = new String[vector.size()];
    for ( int i = 0 ; i < result.length ; i++ ) {
      result[i] = (String) vector.elementAt( i );
    }

    return result;
  }

  public int getCodeLocation( long eventID , String code ) throws IOException {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT location_id ";
    String sqlFrom = "FROM trans_log ";
    String sqlWhere = "WHERE ( event_id = " + eventID + " ) ";
    sqlWhere += "( code = '" + StringEscapeUtils.escapeSql( code ) + "' ) ";
    String sqlOrder = "ORDER BY log_id ASC ";
    String sqlLimit = "LIMIT 1 ";
    String sql = sqlSelect + sqlFrom + sqlWhere + sqlOrder + sqlLimit;

    // execute sql

    result = TransactionLogQuery.getIntFirstValueRecord( sql );

    return result;
  }

  private String sqlSelect() {
    String sqlSelect = "SELECT log_id , client_id , event_id , next_step "
        + ", catagory_id , date_tm , " + sqlDecryptPhoneNumber()
        + " , provider_id , message_count "
        + ", code , params , jump_count , location_id , closed_reason_id ";
    return sqlSelect;
  }

  private TransactionLogBean populateBean( ResultSet rs ) {
    TransactionLogBean tlBean = new TransactionLogBean();
    try {
      tlBean.setLogId( (int) rs.getInt( "log_id" ) );
      tlBean.setClientID( (long) rs.getDouble( "client_id" ) );
      tlBean.setEventID( (long) rs.getDouble( "event_id" ) );
      tlBean.setNextStep( (int) rs.getInt( "next_step" ) );
      tlBean.setCatagoryID( (long) rs.getDouble( "catagory_id" ) );
      tlBean.setDateTm( Util.getUtilDate( rs.getDate( "date_tm" ) ,
          rs.getTime( "date_tm" ) ) );
      tlBean.setPhone( rs.getString( "phone" ) );
      tlBean.setProviderId( StringUtils.trimToEmpty( rs
          .getString( "provider_id" ) ) );
      tlBean.setMessageCount( (int) rs.getDouble( "message_count" ) );
      tlBean.setCode( rs.getString( "code" ) );
      tlBean.setParams( rs.getString( "params" ) );
      tlBean.setJumpCount( (int) rs.getDouble( "jump_count" ) );
      tlBean.setLocationID( (long) rs.getDouble( "location_id" ) );
      tlBean.setClosedReasonId( (int) rs.getInt( "closed_reason_id" ) );
    } catch ( Exception e ) {
      tlBean = null;
      DLog.warning( lctx , "Failed to populated the trans log record , " + e );
    }
    return tlBean;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Util Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  private String sqlEncryptPhoneNumber( String phoneNumber ) {
    StringBuffer sb = new StringBuffer();
    sb.append( "AES_ENCRYPT('" );
    sb.append( phoneNumber );
    sb.append( "','" );
    sb.append( keyPhoneNumber );
    sb.append( "')" );
    return sb.toString();
  }

  private String sqlDecryptPhoneNumber() {
    StringBuffer sb = new StringBuffer();
    sb.append( "AES_DECRYPT(encrypt_phone,'" );
    sb.append( keyPhoneNumber );
    sb.append( "') AS phone" );
    return sb.toString();
  }

}
