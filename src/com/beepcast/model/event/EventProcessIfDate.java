package com.beepcast.model.event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.model.util.DateTimeFormat;

public class EventProcessIfDate {

  public static int process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    int result = TransactionProcessBasic.NEXT_STEP_END;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null process bean" );
      return result;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null input message" );
      return result;
    }
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null output messages" );
      return result;
    }

    // read process type
    String processType = pBean.getType();
    if ( StringUtils.isBlank( processType ) ) {
      log.warning( headerLog + "Failed to process "
          + ", found blank process type " );
      return result;
    }
    log.debug( headerLog + "Read process type = " + processType );

    // read now date
    Date nowDate = new Date();

    // prepare date time format parser
    String format = "yyyy-MM-dd HH:mm";
    SimpleDateFormat sdf = new SimpleDateFormat( format );
    log.debug( headerLog + "Prepared threshold date format parser = " + format );

    // read threshold date
    Date thresholdDate = null;
    {
      String paramLabel = pBean.getParamLabel();
      if ( ( paramLabel == null ) || ( paramLabel.equals( "" ) ) ) {
        log.warning( headerLog + "Failed to process , failed to "
            + "read threshold date , found empty in the param label" );
        return result;
      }
      if ( !paramLabel.equals( "DATE=" ) ) {
        log.warning( headerLog + "Failed to process , failed to "
            + "read threshold date , found wrong paramLabel = " + paramLabel );
        return result;
      }
      String[] names = pBean.getNames();
      if ( ( names == null ) || ( names.length < 1 ) ) {
        log.warning( headerLog + "Failed to process , failed to "
            + "read threshold date , found empty names" );
        return result;
      }
      try {
        thresholdDate = sdf.parse( StringUtils.join( names , " " ) );
      } catch ( ParseException e ) {
        log.warning( headerLog + "Failed to process , failed to read "
            + "threshold date , failed to parse next process names , " + e );
      }
    }
    if ( thresholdDate == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found empty threshold date" );
      return result;
    }
    log.debug( headerLog + "Read threshold date = "
        + DateTimeFormat.convertToString( thresholdDate ) + " , now date = "
        + DateTimeFormat.convertToString( nowDate ) );

    if ( processType.equals( "IF DATE BEFORE" ) ) {
      if ( thresholdDate.getTime() > nowDate.getTime() ) {
        result = TransactionProcessBasic.NEXT_STEP_TRU;
      } else {
        result = TransactionProcessBasic.NEXT_STEP_FAL;
      }
    }
    if ( processType.equals( "IF DATE AFTER" ) ) {
      if ( thresholdDate.getTime() < nowDate.getTime() ) {
        result = TransactionProcessBasic.NEXT_STEP_TRU;
      } else {
        result = TransactionProcessBasic.NEXT_STEP_FAL;
      }
    }

    return result;
  }

}
