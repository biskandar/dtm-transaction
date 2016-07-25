package com.beepcast.model.event;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.model.transaction.PropertyProcessStep;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

public class EventProcessEmailTo {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionConf conf , TransactionLog log , TransactionQueueBean tqBean ,
      ProcessBean pBean , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params

    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process email to "
          + ", found null process bean" );
      return result;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process email to "
          + ", found null input message" );
      return result;
    }
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process email to "
          + ", found null output messages" );
      return result;
    }

    // get and verify email to addresses

    String[] emailToAddresses = pBean.getNames();
    if ( ( emailToAddresses == null ) || ( emailToAddresses.length < 1 ) ) {
      log.warning( headerLog + "Failed to process email to "
          + ", found empty destination email to addresses" );
      return result;
    }

    // prepare email from address

    String emailFromAddress = PropertyProcessStep.getValue( log , headerLog ,
        conf , "EMAIL_TO" , "addressFrom" );
    if ( StringUtils.isBlank( emailFromAddress ) ) {
      log.warning( headerLog + "Failed to process email to "
          + ", found blank email from address , please check the conf file" );
      return result;
    }

    // prepare email content from the process step

    String emailContent = pBean.getResponse();
    emailContent = EventTransQueueReservedVariables.replaceReservedVariables(
        headerLog , log , emailContent , tqBean );
    emailContent = EventOutboundReservedVariables.replaceReservedVariables(
        headerLog , log , emailContent , imsg );
    emailContent = ( emailContent == null ) ? "" : emailContent.trim();
    if ( StringUtils.isBlank( emailContent ) ) {
      log.debug( headerLog + "Found empty response from process step "
          + ", trying to get from incoming message content" );
      emailContent = imsg.getMessageContent();
      emailContent = ( emailContent == null ) ? "" : emailContent.trim();
    }
    if ( StringUtils.isBlank( emailContent ) ) {
      log.warning( headerLog + "Failed to process email to "
          + ", found blank email content" );
      return result;
    }

    // prepare email subject

    String emailSubject = "Text from " + imsg.getOriginalAddress();

    // iterate and send email to addresses

    for ( int idx = 0 ; idx < emailToAddresses.length ; idx++ ) {
      String emailToAddress = emailToAddresses[idx];
      emailToAddress = EventTransQueueReservedVariables
          .replaceReservedVariables( headerLog , log , emailToAddress , tqBean );
      emailToAddress = EventOutboundReservedVariables.replaceReservedVariables(
          headerLog , log , emailToAddress , imsg );
      if ( StringUtils.isBlank( emailToAddress ) ) {
        continue;
      }
      String[] arrToAddrs = StringUtils.split( emailToAddress , ";" );
      if ( arrToAddrs == null ) {
        continue;
      }
      for ( int jdx = 0 ; jdx < arrToAddrs.length ; jdx++ ) {
        String toAddr = StringUtils.trimToEmpty( arrToAddrs[jdx] );
        if ( StringUtils.isBlank( toAddr ) ) {
          continue;
        }

        // log it first

        log.debug( headerLog + "Sending an email message : from = "
            + emailFromAddress + " , to : " + toAddr + " , subject = "
            + StringEscapeUtils.escapeJava( emailSubject )
            + " , content length = " + StringUtils.length( emailContent )
            + " char(s)" );

        // send email message

        ClientApp clientApp = ClientApp.getInstance();
        if ( !clientApp.sendEmailMessage( emailFromAddress , toAddr ,
            emailSubject , emailContent , null ) ) {
          log.warning( headerLog + "Failed to send email message "
              + "thru client app" );
        }

      }
    }

    result = true;
    return result;
  }

}
