package com.beepcast.model.event;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.transaction.PropertyProcessStep;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.service.email.EmailBean;

public class EventProcessEmailClient {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionConf conf , TransactionLog log , TransactionQueueBean tqBean ,
      ProcessBean pBean , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process email client "
          + ", found null process bean" );
      return result;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process email client "
          + ", found null input message" );
      return result;
    }
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process email client "
          + ", found null output messages" );
      return result;
    }

    // get client bean
    ClientBean clientBean = (ClientBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN );
    if ( clientBean == null ) {
      log.warning( headerLog + "Failed to process email client "
          + ", found null client bean" );
      return result;
    }

    // verify process names
    String[] arrNames = pBean.getNames();
    if ( ( arrNames == null ) || ( arrNames.length < 1 ) ) {
      log.warning( headerLog + "Failed to process email client "
          + ", found empty process names" );
      return result;
    }

    // prepare email bean
    EmailBean emailBean = new EmailBean();
    emailBean.setFrom( PropertyProcessStep.getValue( log , headerLog , conf ,
        "EMAIL_CLIENT" , "addressFrom" ) );
    emailBean.setTo( clientBean.getEmail() );
    emailBean.setSubject( arrNames[0] );
    emailBean.setBody( EventResponse.buildResponse( log , pBean ) );

    // populate email variables
    populateEmailVariables( headerLog , log , tqBean , imsg , emailBean );

    // log it
    log.debug( headerLog + "Sending email client with : address from = "
        + emailBean.getFrom() + " , address to : " + emailBean.getTo()
        + " , email subject = "
        + StringEscapeUtils.escapeJava( emailBean.getSubject() )
        + " , email content length = "
        + StringUtils.length( emailBean.getBody() ) + " char(s)" );

    // send email message
    ClientApp clientApp = ClientApp.getInstance();
    if ( !clientApp.sendEmailMessage( emailBean.getFrom() , emailBean.getTo() ,
        emailBean.getSubject() , emailBean.getBody() , null ) ) {
      log.warning( headerLog + "Failed to send email message" );
    } else {
      log.debug( headerLog + "Successfully sent email message" );
    }

    result = true;
    return result;
  }

  private static boolean populateEmailVariables( String headerLog ,
      TransactionLog log , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg , EmailBean emailBean ) {
    boolean result = false;

    {
      String subject = emailBean.getSubject();
      subject = EventTransQueueReservedVariables.replaceReservedVariables(
          headerLog , log , subject , tqBean );
      subject = EventOutboundReservedVariables.replaceReservedVariables(
          headerLog , log , subject , imsg );
      emailBean.setSubject( subject );
    }

    {
      String body = emailBean.getBody();
      body = EventTransQueueReservedVariables.replaceReservedVariables(
          headerLog , log , body , tqBean );
      body = EventOutboundReservedVariables.replaceReservedVariables(
          headerLog , log , body , imsg );
      emailBean.setBody( body );
    }

    result = true;
    return result;
  }

}
