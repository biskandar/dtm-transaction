package com.beepcast.model.event;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.model.transaction.script.ScriptRandomCodeData;
import com.beepcast.model.transaction.script.ScriptRandomCodeDataFactory;
import com.beepcast.model.transaction.script.ScriptRandomCodeExec;

public class EventProcessRandomCode {

  private static final String DEFAULT_FORMAT = ScriptRandomCodeData.FORMAT_ALPHANUMERIC;
  private static final int DEFAULT_LENGTH = 5;

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg ) {
    boolean result = false;

    // validate must be params
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process random code "
          + ", found null input message" );
      return result;
    }

    // read variable name
    String[] arrVarNames = pBean.getNames();
    if ( ( arrVarNames == null ) || ( arrVarNames.length < 1 ) ) {
      log.warning( headerLog + "Failed to process random code "
          + ", found null variable name" );
      return result;
    }
    String varName = arrVarNames[0];
    if ( ( varName == null ) || ( varName.equals( "" ) ) ) {
      log.warning( headerLog + "Failed to process random code "
          + ", found blank variable name" );
      return result;
    }

    // get script source
    pBean = EventTransQueueReservedVariables.replaceReservedVariables(
        headerLog , log , pBean , tqBean );
    pBean = EventOutboundReservedVariables.replaceReservedVariables( headerLog ,
        log , pBean , tqBean , imsg );
    String scriptSource = EventResponse.buildResponse( log , pBean );
    log.debug( headerLog + "Resolved script source : "
        + StringEscapeUtils.escapeJava( scriptSource ) );

    // prepare for script random code data
    ScriptRandomCodeData scriptRandomCodeData = ScriptRandomCodeDataFactory
        .createScriptRandomCodeData( DEFAULT_FORMAT , DEFAULT_LENGTH );

    // execute the script
    ScriptRandomCodeExec scriptRandomCodeExec = new ScriptRandomCodeExec(
        headerLog , scriptSource );
    if ( !scriptRandomCodeExec.execute( scriptRandomCodeData ) ) {
      log.warning( headerLog + "Failed to process random code "
          + ", found failed to execute the script" );
      return result;
    }

    // clean format and length
    String format = scriptRandomCodeData.getFormat();
    if ( ( format == null ) || ( format.equals( "" ) ) ) {
      format = ScriptRandomCodeData.FORMAT_ALPHANUMERIC;
    }
    int length = scriptRandomCodeData.getLength();
    length = ( length < 1 ) ? DEFAULT_LENGTH : length;

    // generate random code based on format and length
    String randCode = null;
    if ( format.equalsIgnoreCase( ScriptRandomCodeData.FORMAT_NUMERIC ) ) {
      randCode = RandomStringUtils.randomNumeric( length );
    } else if ( format
        .equalsIgnoreCase( ScriptRandomCodeData.FORMAT_ALPHABETIC ) ) {
      randCode = RandomStringUtils.randomAlphabetic( length );
    } else {
      randCode = RandomStringUtils.randomAlphanumeric( length );
    }

    // validate random code
    if ( ( randCode == null ) || ( randCode.equals( "" ) ) ) {
      log.warning( headerLog + "Failed to process random code "
          + ", found failed to generate random code" );
      return result;
    }

    // add input message variables
    imsg.addMessageParam(
        TransactionMessageParam.HDR_PREFIX_SET_RESERVED_VARIABLE
            .concat( varName ) , randCode );

    // log it
    log.debug( headerLog + "Generated random code : varName = " + varName
        + " , format = " + format + " , length = " + length + " , randCode = "
        + randCode );

    result = true;
    return result;
  }

}
