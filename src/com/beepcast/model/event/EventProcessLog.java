package com.beepcast.model.event;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

public class EventProcessLog {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // must be params
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

    // replace response & rfa variables with parameter values <%...%>
    log.debug( headerLog + "Trying to update response message "
        + ", by replacing variables <%...%>" );
    pBean = EventTransQueueReservedVariables.replaceReservedVariables(
        headerLog , log , pBean , tqBean );

    // replace response & rfa reserved variables <#...#>
    log.debug( headerLog + "Trying to update response message "
        + ", by replacing reserved variables <#...#>" );
    pBean = EventOutboundReservedVariables.replaceReservedVariables( headerLog ,
        log , pBean , tqBean , imsg );

    // prepare the response from process bean
    String response = EventResponse.buildResponse( log , pBean );

    // is response empty
    if ( StringUtils.isBlank( response ) ) {
      log.warning( headerLog + "Failed to process , found blank response" );
      return result;
    }

    // built new output message
    TransactionOutputMessage omsg = support
        .createReplyMessage( imsg , response );
    if ( omsg == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null output message generated" );
      return result;
    }

    // regenerate new message id for output remind message
    omsg.setMessageId( TransactionMessageFactory.generateMessageId( "INT" ) );

    // resolve original ( with/out masking ) address
    if ( !support.resolveOriginalAddressAndMask( omsg ) ) {
      log.warning( headerLog + "Failed to process , found failed to resolve "
          + "original address" );
      return result;
    }

    // resolve message content
    support.resolveMessageContent( omsg );
    if ( omsg.getMessageCount() < 1 ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found zero total message send" );
      return result;
    }

    // add message param to bypass mt debit
    omsg.addMessageParam( TransactionMessageParam.HDR_BYPASS_MT_DEBIT ,
        new Boolean( true ) );
    log.debug( headerLog + "Added input message param : "
        + TransactionMessageParam.HDR_BYPASS_MT_DEBIT + " = True " );

    // add message param to bypass send to provider agent
    omsg.addMessageParam( TransactionMessageParam.HDR_BYPASS_SEND_PROVIDER ,
        new Boolean( true ) );
    log.debug( headerLog + "Added input message param : "
        + TransactionMessageParam.HDR_BYPASS_SEND_PROVIDER + " = True " );

    // set provider as XX1
    omsg.addMessageParam( TransactionMessageParam.HDR_SET_PROVIDER , "XX1" );
    log.debug( headerLog + "Added input message param : "
        + TransactionMessageParam.HDR_SET_PROVIDER + " = XX1 " );

    // store output message
    omsgs.add( omsg );
    log.debug( headerLog + "Created output log message : messageId = "
        + omsg.getMessageId() + " , originalAddress = "
        + omsg.getOriginalAddress() + " , destinationAddress = "
        + omsg.getDestinationAddress() + " , messageCount = "
        + omsg.getMessageCount() + " , messageContent = "
        + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );

    result = true;
    return result;
  }

}
