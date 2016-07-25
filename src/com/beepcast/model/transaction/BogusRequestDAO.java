package com.beepcast.model.transaction;

import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;

import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.util.DateTimeFormat;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class BogusRequestDAO {

  static final DLogContext lctx = new SimpleContext( "BogusRequestDAO" );

  private DatabaseLibrary dbLib = DatabaseLibrary.getInstance();

  public boolean insert( BogusRequestBean bogusRequest ) {
    boolean result = false;

    // read all record(s)

    long clientId = bogusRequest.getClientID();
    long eventId = bogusRequest.getEventID();
    String phone = bogusRequest.getPhone();
    String shortCode = bogusRequest.getShortCode();
    Date dateTm = bogusRequest.getDateTm();
    String message = bogusRequest.getMessage();
    String description = bogusRequest.getDescription();

    // clean record(s)
    phone = ( phone == null ) ? "" : phone;
    shortCode = ( shortCode == null ) ? "" : shortCode;
    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    message = ( message == null ) ? "" : message;
    description = ( description == null ) ? "" : description;

    // compose sql
    String sqlInsert = "INSERT INTO bogus_request ";
    sqlInsert += "(client_id,event_id,phone,short_code,date_tm,message,description) ";
    String sqlValues = "VALUES ( " + clientId + " , " + eventId + " , '"
        + StringEscapeUtils.escapeSql( phone ) + "' , '"
        + StringEscapeUtils.escapeSql( shortCode ) + "' , '"
        + DateTimeFormat.convertToString( dateTm ) + "' , '"
        + StringEscapeUtils.escapeSql( message ) + "' , '"
        + StringEscapeUtils.escapeSql( description ) + "' ) ";
    String sql = sqlInsert + sqlValues;

    // debug
    DLog.debug( lctx , "Perform " + sql );

    // execute sql
    Integer irslt = dbLib.executeQuery( "transactiondb" , sql );
    if ( ( irslt != null ) && ( irslt.intValue() > 0 ) ) {
      result = true;
    }

    return result;
  }

}
