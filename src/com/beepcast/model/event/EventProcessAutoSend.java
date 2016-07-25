package com.beepcast.model.event;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

public class EventProcessAutoSend {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( tqBean == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null trans queue bean" );
      return result;
    }
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

    // prepare the message content
    String messageContent = createMessageContent( headerLog , log , tqBean ,
        pBean , imsg );
    if ( StringUtils.isBlank( messageContent ) ) {
      log.warning( headerLog + "Failed to process sms to sms step "
          + ", found empty message content" );
      return result;
    }
    log.debug( headerLog + "Created message content : "
        + StringEscapeUtils.escapeJava( messageContent ) );

    // prepare for output message
    String messageId = TransactionMessageFactory.generateMessageId( "INT" );

    // built new output message
    TransactionOutputMessage omsg = support.createReplyMessage( imsg ,
        messageContent );
    if ( omsg == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null output message generated" );
      return result;
    }

    // regenerate new message id for output remind message
    omsg.setMessageId( messageId );
    log.debug( headerLog + "Created new output message with new messageId = "
        + omsg.getMessageId() );

    // set new header log
    headerLog = headerLog + "[" + messageId + "] ";

    // resolve original ( with/out masking ) address
    if ( !support.resolveOriginalAddressAndMask( omsg ) ) {
      log.warning( headerLog + "Failed to process "
          + ", found failed to resolve original address" );
      return result;
    }

    // resolve message content
    support.resolveMessageContent( omsg );
    if ( omsg.getMessageCount() < 1 ) {
      log.warning( headerLog + "Failed to process "
          + ", found zero total message send" );
      return result;
    }

    // store output message
    omsgs.add( omsg );
    log.debug( headerLog + "Created output message : messageId = "
        + omsg.getMessageId() + " , originalAddress = "
        + omsg.getOriginalAddress() + " , destinationAddress = "
        + omsg.getDestinationAddress() + " , messageCount = "
        + omsg.getMessageCount() + " , messageContent = "
        + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );
    result = true;
    return result;
  }

  private static String createMessageContent( String headerLog ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg ) {
    String messageContent = null;

    // validate must be params
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to create message content "
          + ", found null process bean" );
      return messageContent;
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

    // read message content form event step's response
    messageContent = EventResponse.buildResponse( log , pBean );

    return messageContent;
  }

}
