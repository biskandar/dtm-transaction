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

public class TransactionQueueDAO {

  static final DLogContext lctx = new SimpleContext( "TransactionQueueDAO" );

  private DatabaseLibrary dbLib;
  private final String keyPhoneNumber;

  public TransactionQueueDAO() {
    dbLib = DatabaseLibrary.getInstance();
    EncryptApp encryptApp = EncryptApp.getInstance();
    keyPhoneNumber = encryptApp.getKeyValue( EncryptApp.KEYNAME_PHONENUMBER );
  }

  public boolean insert( TransactionQueueBean tqBean ) {
    boolean result = false;

    Date dateTm = tqBean.getDateTm();
    String phone = tqBean.getPhone();
    String providerId = tqBean.getProviderId();
    String code = tqBean.getCode();
    String pendingCode = tqBean.getPendingCode();
    String params = tqBean.getParams();

    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    phone = ( phone == null ) ? "" : phone.trim();
    providerId = ( providerId == null ) ? "" : providerId.trim();
    code = ( code == null ) ? "" : code.trim();
    pendingCode = ( pendingCode == null ) ? "" : pendingCode.trim();
    params = ( params == null ) ? "" : params.trim();
    String strDateTm = Util.strFormat( dateTm , "yyyy-mm-dd hh:nn:ss" );

    if ( select( phone , 0 ) != null ) {
      DLog.warning( lctx , "Failed to insert record into trans queue table "
          + ", found phoneNumber = " + phone + " is already exist" );
      return result;
    }

    String sqlInsert = "INSERT INTO trans_queue (event_id,client_id"
        + ",pending_event_id,date_tm,phone,encrypt_phone,provider_id"
        + ",next_step,message_count,code,pending_code,params"
        + ",update_profile,new_user,jump_count,location_id"
        + ",calling_event_id,date_inserted,date_updated) ";
    String sqlValues = "VALUES (" + tqBean.getEventID() + ","
        + tqBean.getClientID() + "," + tqBean.getPendingEventID() + ",'"
        + strDateTm + "',''," + sqlEncryptPhoneNumber( phone ) + ",'"
        + StringEscapeUtils.escapeSql( providerId ) + "',"
        + tqBean.getNextStep() + "," + tqBean.getMessageCount() + ",'"
        + StringEscapeUtils.escapeSql( code ) + "','"
        + StringEscapeUtils.escapeSql( pendingCode ) + "','"
        + StringEscapeUtils.escapeSql( params ) + "',"
        + ( ( tqBean.isUpdateProfile() == true ) ? 1 : 0 ) + ","
        + ( ( tqBean.isNewUser() == true ) ? 1 : 0 ) + ","
        + tqBean.getJumpCount() + "," + tqBean.getLocationID() + ","
        + tqBean.getCallingEventID() + ",NOW(),NOW())";
    String sql = sqlInsert + sqlValues;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public TransactionQueueBean select( String phone , int eventId ) {
    TransactionQueueBean tqBean = null;

    // clean params

    phone = ( phone == null ) ? "" : phone.trim();

    // compose sql

    String sqlSelect = sqlSelect();
    String sqlFrom = "FROM trans_queue ";
    String sqlWhere = "WHERE ( encrypt_phone = "
        + sqlEncryptPhoneNumber( phone ) + " ) ";
    if ( eventId > 0 ) {
      sqlWhere += "AND ( event_id = " + eventId + " ) ";
    }
    String sqlOrder = "ORDER BY queue_id ASC ";
    String sqlLimit = "LIMIT 1 ";
    String sql = sqlSelect + sqlFrom + sqlWhere + sqlOrder + sqlLimit;

    // execute sql

    ConnectionWrapper conn = dbLib.getReaderConnection( "transactiondb" );
    try {
      Statement stmt = conn.createStatement();
      // DLog.debug( lctx , "Perform " + sql );
      ResultSet rs = stmt.executeQuery( sql );
      if ( rs.next() ) {
        tqBean = populateBean( rs );
      }
      rs.close();
      stmt.close();
    } catch ( SQLException sqle ) {
      DLog.warning( lctx , "Failed to retrieve trans_queue based on phone , "
          + sqle );
    } finally {
      conn.disconnect( true );
    }

    return tqBean;
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc , int limit ) {

    // prepare holder

    Vector transObjects = new Vector( 1000 , 1000 );

    // build query

    String sqlSelect = sqlSelect();
    String sqlFrom = "FROM trans_queue ";
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

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter , Boolean jumpTo ) {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT SUM(message_count) AS total ";
    String sqlFrom = "FROM trans_queue ";
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
    if ( jumpTo != null ) {
      if ( jumpTo.booleanValue() ) {
        sqlWhere += "AND ( jump_count > 0 ) ";
      } else {
        sqlWhere += "AND ( jump_count = 0 ) ";
      }
    }
    String sql = sqlSelect + sqlFrom + sqlWhere;

    // execute sql

    result = TransactionQueueQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code , Boolean jumpTo ) throws IOException {
    int result = 0;

    // build query

    String sqlSelect = "SELECT COUNT(queue_id) AS total ";
    String sqlFrom = "FROM trans_queue ";
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
    if ( jumpTo != null ) {
      if ( jumpTo.booleanValue() ) {
        sqlWhere += "AND ( jump_count > 0 ) ";
      } else {
        sqlWhere += "AND ( jump_count = 0 ) ";
      }
    }
    String sql = sqlSelect + sqlFrom + sqlWhere;

    // execute sql

    result = TransactionQueueQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public int getMobileUserCount( long clientID , long eventID , Date fromDate ,
      Date toDate ) throws IOException {
    int result = 0;

    // compose sql

    String sqlSelect = "SELECT COUNT(encrypt_phone) ";
    String sqlFrom = "FROM trans_queue ";
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

    result = TransactionQueueQuery.getIntFirstValueRecord( sql );

    return result;
  }

  public Vector getMobileUsers( long clientID , long eventID , Date fromDate ,
      Date toDate , String code ) throws IOException {
    Vector result = null;

    // compose sql

    String sqlSelect = "SELECT " + sqlDecryptPhoneNumber() + " ";
    String sqlFrom = "FROM trans_queue ";
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

    result = TransactionQueueQuery.getStrFirstValueRecords( sql );

    return result;
  }

  public boolean update( TransactionQueueBean tqBean ) {
    boolean result = false;

    if ( tqBean == null ) {
      return result;
    }

    Date dateTm = tqBean.getDateTm();
    String phone = tqBean.getPhone();
    String providerId = tqBean.getProviderId();
    String code = tqBean.getCode();
    String pendingCode = tqBean.getPendingCode();
    String params = tqBean.getParams();

    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    phone = ( phone == null ) ? "" : phone.trim();
    providerId = ( providerId == null ) ? "" : providerId.trim();
    code = ( code == null ) ? "" : code.trim();
    pendingCode = ( pendingCode == null ) ? "" : pendingCode.trim();
    params = ( params == null ) ? "" : params.trim();
    String strDateTm = Util.strFormat( dateTm , "yyyy-mm-dd hh:nn:ss" );

    String sqlUpdate = "UPDATE trans_queue ";
    String sqlSet = "SET event_id = " + tqBean.getEventID() + " , client_id = "
        + tqBean.getClientID() + " , pending_event_id = "
        + tqBean.getPendingEventID() + " , date_tm = '" + strDateTm + "' , "
        + "phone = '' , encrypt_phone = " + sqlEncryptPhoneNumber( phone )
        + " , provider_id = '" + StringEscapeUtils.escapeSql( providerId )
        + "' , next_step = " + tqBean.getNextStep() + " , message_count = "
        + tqBean.getMessageCount() + " , code = '"
        + StringEscapeUtils.escapeSql( code ) + "' , pending_code = '"
        + StringEscapeUtils.escapeSql( pendingCode ) + "' , params = '"
        + StringEscapeUtils.escapeSql( params ) + "' , update_profile = "
        + ( tqBean.isUpdateProfile() ? 1 : 0 ) + " , new_user = "
        + ( tqBean.isNewUser() ? 1 : 0 ) + " , jump_count = "
        + tqBean.getJumpCount() + " , location_id = " + tqBean.getLocationID()
        + " , calling_event_id = " + tqBean.getCallingEventID()
        + " , date_updated = NOW() ";
    String sqlWhere = "";
    if ( tqBean.getQueueId() > 0 ) {
      sqlWhere = "WHERE ( queue_id = " + tqBean.getQueueId() + " ) ";
    } else {
      sqlWhere = "WHERE ( encrypt_phone = " + sqlEncryptPhoneNumber( phone )
          + " ) ";
    }

    String sql = sqlUpdate + sqlSet + sqlWhere;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public boolean updatePhone( TransactionQueueBean tqBean , String newPhone ) {
    boolean result = false;

    if ( tqBean == null ) {
      return result;
    }
    if ( newPhone == null ) {
      return result;
    }

    Date dateTm = tqBean.getDateTm();
    String phone = tqBean.getPhone();
    String providerId = tqBean.getProviderId();
    String code = tqBean.getCode();
    String pendingCode = tqBean.getPendingCode();
    String params = tqBean.getParams();

    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    phone = ( phone == null ) ? "" : phone.trim();
    providerId = ( providerId == null ) ? "" : providerId.trim();
    code = ( code == null ) ? "" : code.trim();
    pendingCode = ( pendingCode == null ) ? "" : pendingCode.trim();
    params = ( params == null ) ? "" : params.trim();
    String strDateTm = Util.strFormat( dateTm , "yyyy-mm-dd hh:nn:ss" );

    String sqlUpdate = "UPDATE trans_queue ";
    String sqlSet = "SET event_id = " + tqBean.getEventID() + " , client_id = "
        + tqBean.getClientID() + " , pending_event_id = "
        + tqBean.getPendingEventID() + " , date_tm = '" + strDateTm + "' , "
        + "phone = '' , encrypt_phone = " + sqlEncryptPhoneNumber( newPhone )
        + " , provider_id = '" + StringEscapeUtils.escapeSql( providerId )
        + "' , next_step = " + tqBean.getNextStep() + " , message_count = "
        + tqBean.getMessageCount() + " , code = '"
        + StringEscapeUtils.escapeSql( code ) + "' , pending_code = '"
        + StringEscapeUtils.escapeSql( pendingCode ) + "' , params = '"
        + StringEscapeUtils.escapeSql( params ) + "' , update_profile = "
        + ( tqBean.isUpdateProfile() ? 1 : 0 ) + " , new_user = "
        + ( tqBean.isNewUser() ? 1 : 0 ) + " , jump_count = "
        + tqBean.getJumpCount() + " , location_id = " + tqBean.getLocationID()
        + " , calling_event_id = " + tqBean.getCallingEventID()
        + " , date_updated = NOW() ";
    String sqlWhere = "";
    if ( tqBean.getQueueId() > 0 ) {
      sqlWhere = "WHERE ( queue_id = " + tqBean.getQueueId() + " ) ";
    } else {
      sqlWhere = "WHERE ( encrypt_phone = " + sqlEncryptPhoneNumber( phone )
          + " ) ";
    }

    String sql = sqlUpdate + sqlSet + sqlWhere;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public boolean delete( TransactionQueueBean tqBean ) {
    boolean result = false;

    if ( tqBean == null ) {
      return result;
    }

    String phone = tqBean.getPhone();

    phone = ( phone == null ) ? "" : phone.trim();

    String sqlDelete = "DELETE FROM trans_queue ";
    String sqlWhere = "";
    if ( tqBean.getQueueId() > 0 ) {
      sqlWhere = "WHERE ( queue_id = " + tqBean.getQueueId() + " ) ";
    } else {
      sqlWhere = "WHERE ( encrypt_phone = " + sqlEncryptPhoneNumber( phone )
          + " ) ";
    }

    String sql = sqlDelete + sqlWhere;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

  public int delete( String criteria , int limit ) {
    int totalRecords = 0;
    if ( ( criteria == null ) || ( criteria.equals( "" ) ) ) {
      return totalRecords;
    }
    if ( limit < 1 ) {
      return totalRecords;
    }

    String sqlDelete = "DELETE FROM trans_queue ";
    String sqlWhere = "WHERE " + criteria + " ";
    String sqlLimit = "LIMIT " + limit + " ";
    String sql = sqlDelete + sqlWhere + sqlLimit;

    // DLog.debug( lctx , "Perform " + sql );
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( irslt == null ) {
      return totalRecords;
    }
    totalRecords = irslt.intValue();
    return totalRecords;
  }

  private String sqlSelect() {
    String sqlSelect = "SELECT queue_id , event_id , client_id "
        + ", pending_event_id , date_tm , " + sqlDecryptPhoneNumber()
        + " , provider_id , next_step , message_count , code "
        + ", pending_code , params , update_profile , new_user "
        + ", jump_count , location_id , calling_event_id ";
    return sqlSelect;
  }

  private TransactionQueueBean populateBean( ResultSet rs ) {
    TransactionQueueBean tqBean = new TransactionQueueBean();
    try {
      tqBean.setQueueId( (int) rs.getInt( "queue_id" ) );
      tqBean.setEventID( (long) rs.getDouble( "event_id" ) );
      tqBean.setClientID( (long) rs.getDouble( "client_id" ) );
      tqBean.setPendingEventID( (long) rs.getDouble( "pending_event_id" ) );
      tqBean.setDateTm( Util.getUtilDate( rs.getDate( "date_tm" ) ,
          rs.getTime( "date_tm" ) ) );
      tqBean.setPhone( rs.getString( "phone" ) );
      tqBean.setProviderId( StringUtils.trimToEmpty( rs
          .getString( "provider_id" ) ) );
      tqBean.setNextStep( (int) rs.getDouble( "next_step" ) );
      tqBean.setMessageCount( (int) rs.getDouble( "message_count" ) );
      tqBean.setCode( rs.getString( "code" ) );
      tqBean.setPendingCode( rs.getString( "pending_code" ) );
      tqBean.setParams( rs.getString( "params" ) );
      tqBean.setUpdateProfile( ( rs.getDouble( "update_profile" ) == 1 ) ? true
          : false );
      tqBean.setNewUser( ( rs.getDouble( "new_user" ) == 1 ) ? true : false );
      tqBean.setJumpCount( (int) rs.getDouble( "jump_count" ) );
      tqBean.setLocationID( (long) rs.getDouble( "location_id" ) );
      tqBean.setCallingEventID( (long) rs.getDouble( "calling_event_id" ) );
    } catch ( Exception e ) {
      tqBean = null;
      DLog.warning( lctx , "Failed to populated the trans queue record , " + e );
    }
    return tqBean;
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
