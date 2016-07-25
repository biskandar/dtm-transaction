package com.beepcast.model.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

public class EventProcessSmsToSms {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs , boolean excludeTqBeanPhone ) {
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

    // prepare the destination address
    List listPhones = readListPhones( headerLog , log , tqBean , pBean , imsg ,
        excludeTqBeanPhone );
    if ( ( listPhones == null ) || ( listPhones.size() < 1 ) ) {
      log.warning( headerLog + "Failed to process sms to sms step "
          + ", found empty list phones" );
      return result;
    }
    log.debug( headerLog + "Read total " + listPhones.size()
        + " destination phone(s) : " + listPhones );

    // iterate all the phones , and build the output message
    int totalPhones = 0;
    Iterator iterPhones = listPhones.iterator();
    while ( iterPhones.hasNext() ) {
      String phone = (String) iterPhones.next();
      if ( StringUtils.isBlank( phone ) ) {
        continue;
      }
      if ( !process( headerLog , support , log , imsg , omsgs , phone ,
          messageContent ) ) {
        continue;
      }
      totalPhones = totalPhones + 1;
    }
    log.debug( headerLog + "Processed total " + totalPhones
        + " destination phone(s)" );

    result = true;
    return result;
  }

  private static boolean process( String headerLog ,
      TransactionSupport support , TransactionLog log ,
      TransactionInputMessage imsg , List omsgs , String newDestinationAddress ,
      String newMessageContent ) {
    boolean result = false;

    // prepare for output message
    String messageId = TransactionMessageFactory.generateMessageId( "INT" );
    headerLog = headerLog + "[" + messageId + "] ";

    // built new output message
    TransactionOutputMessage omsg = support.createReplyMessage( imsg ,
        newMessageContent );
    if ( omsg == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null output message generated" );
      return result;
    }

    // regenerate new message id for output remind message
    omsg.setMessageId( messageId );

    // update the destination address
    log.debug( headerLog + "Update output message's destination address : "
        + omsg.getDestinationAddress() + " -> " + newDestinationAddress );
    omsg.setDestinationAddress( newDestinationAddress );

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

  private static List readListPhones( String headerLog , TransactionLog log ,
      TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , boolean excludeTqBeanPhone ) {
    List listPhones = new ArrayList();

    if ( pBean == null ) {
      log.warning( headerLog + "Failed to read phones "
          + ", found null process bean" );
      return listPhones;
    }

    if ( tqBean == null ) {
      log.warning( headerLog + "Failed to read phones "
          + ", found null trans queue bean" );
      return listPhones;
    }

    String[] arrPhones = pBean.getNames();
    if ( ( arrPhones == null ) || ( arrPhones.length < 1 ) ) {
      log.warning( headerLog + "Failed to read phones "
          + ", found empty array phones" );
      return listPhones;
    }

    for ( int idx = 0 ; idx < arrPhones.length ; idx++ ) {
      String phone = arrPhones[idx];
      phone = EventTransQueueReservedVariables.replaceReservedVariables(
          headerLog , log , phone , tqBean );
      phone = EventOutboundReservedVariables.replaceReservedVariables(
          headerLog , log , phone , imsg );
      if ( StringUtils.isBlank( phone ) ) {
        continue;
      }
      phone = phone.trim();
      if ( phone.length() < 5 ) {
        continue;
      }
      if ( phone.startsWith( "+" ) ) {
        phone = phone.substring( 1 );
      }
      if ( !StringUtils.isNumeric( phone ) ) {
        log.warning( headerLog + "Failed to add phone "
            + ", found invalid format : " + phone );
        continue;
      }
      phone = "+".concat( phone );
      if ( excludeTqBeanPhone && StringUtils.equals( phone , tqBean.getPhone() ) ) {
        log.warning( headerLog + "Failed to add phone , found destination "
            + "phone equals with original phone : " + phone );
        continue;
      }
      listPhones.add( phone );
    }

    return listPhones;
  }

}
