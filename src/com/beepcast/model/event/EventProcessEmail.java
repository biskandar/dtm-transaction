package com.beepcast.model.event;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.idgen.IdGenApp;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.transaction.PropertyProcessStep;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.service.email.EmailAddressFinder;
import com.beepcast.service.email.EmailBean;
import com.beepcast.service.email.EmailClobSupport;
import com.beepcast.service.email.EmailSender;
import com.beepcast.util.properties.GlobalEnvironment;

public class EventProcessEmail {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionConf conf , TransactionLog log , TransactionQueueBean tqBean ,
      ProcessBean pBean , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params

    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process email "
          + ", found null process bean" );
      return result;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process email "
          + ", found null input message" );
      return result;
    }
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process email "
          + ", found null output messages" );
      return result;
    }

    // get mobile user object

    MobileUserBean mobileUserBean = (MobileUserBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );
    if ( mobileUserBean == null ) {
      log.warning( headerLog + "Failed to process email "
          + ", found null mobile user bean" );
      return result;
    }

    // get client bean

    ClientBean clientBean = (ClientBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN );
    if ( clientBean == null ) {
      log.warning( headerLog + "Failed to process email "
          + ", found null client bean" );
      return result;
    }

    // get base upload path

    GlobalEnvironment globalEnv = GlobalEnvironment.getInstance();
    String baseUploadPath = globalEnv.getProperty( "platform.dir.upload" );
    log.debug( headerLog + "Defined base upload path = " + baseUploadPath );

    try {

      // read and parse the email bean

      EventEmailService eventEmailService = new EventEmailService();
      EventEmailBean eventEmailBean = eventEmailService.select(
          (int) tqBean.getEventID() , Integer.parseInt( pBean.getResponse() ) );
      if ( eventEmailBean == null ) {
        log.warning( headerLog + "Failed to process email "
            + ", can not find event email based on eventId = "
            + tqBean.getEventID() + " and processStep = " + pBean.getResponse() );
        return result;
      }
      log.debug( headerLog + "Found event email record : id = "
          + eventEmailBean.getId() + " , eventId = "
          + eventEmailBean.getEventId() + " , processStep = "
          + eventEmailBean.getProcessStep() + " , emailClob = "
          + StringEscapeUtils.escapeJava( eventEmailBean.getEmailClob() ) );

      EmailBean emailBean = EmailClobSupport
          .emailClobToEmailBean( eventEmailBean.getEmailClob() );
      if ( emailBean == null ) {
        log.warning( headerLog + "Failed to process email "
            + ", found failed to parse email clob " );
        return result;
      }
      log.debug( headerLog + "Parsed email bean from clob : from = "
          + emailBean.getFrom() + " , to = " + emailBean.getTo()
          + " , subject = " + emailBean.getSubject() + " , attachment = "
          + emailBean.getAttachment() );

      // set email id as transaction

      IdGenApp idGenApp = IdGenApp.getInstance();
      emailBean.setEmailId( imsg.getMessageId() + "-"
          + idGenApp.nextIdentifier() );

      // set default values

      if ( StringUtils.isBlank( emailBean.getFrom() ) ) {
        log.warning( headerLog + "Failed to process email "
            + ", found failed to define email address from" );
        return result;
      }
      if ( StringUtils.isBlank( emailBean.getTo() ) ) {
        emailBean.setTo( mobileUserBean.getEmail() );
        log.debug( headerLog + "Found email to is blank "
            + ", replaced with mobile user email : "
            + mobileUserBean.getEmail() );
      }
      if ( StringUtils.isBlank( emailBean.getTo() ) ) {
        log.warning( headerLog + "Failed to process email "
            + ", found failed to define email address to" );
        return result;
      }

      // populate email variables

      populateEmailVariables( headerLog , log , tqBean , imsg , emailBean );

      // clean email's sender address

      cleanEmailSenderAddress( headerLog , log , conf , emailBean );

      // compose attachment

      String attachment = emailBean.getAttachment();
      if ( attachment == null ) {
        // nothing to do ...
      } else if ( StringUtils.equalsIgnoreCase( attachment , "NONE" ) ) {
        attachment = null;
      } else {
        attachment = baseUploadPath + "clients/" + clientBean.getClientID()
            + "/" + attachment;
      }
      log.debug( headerLog + "Defined email attachment = " + attachment );
      emailBean.setAttachment( attachment );

      // log first

      log.debug( headerLog + "Sending email : id = " + emailBean.getEmailId()
          + " , from = " + emailBean.getFrom() + " , tos = "
          + Arrays.asList( emailBean.getToList() ) + " , subject = "
          + emailBean.getSubject() + " , body = "
          + StringEscapeUtils.escapeJava( emailBean.getBody() ) );

      // send email

      ( new EmailSender() ).send( emailBean );

      // result as true

      result = true;

    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to process email , " + e );
    }

    return result;
  }

  private static boolean populateEmailVariables( String headerLog ,
      TransactionLog log , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg , EmailBean emailBean ) {
    boolean result = false;

    {
      String to = emailBean.getTo();
      to = EventTransQueueReservedVariables.replaceReservedVariables(
          headerLog , log , to , tqBean );
      to = EventOutboundReservedVariables.replaceReservedVariables( headerLog ,
          log , to , imsg );
      emailBean.setToList( EmailAddressFinder.findEmailAddressesToArray( to ) );
    }

    {
      String from = emailBean.getFrom();
      from = EventTransQueueReservedVariables.replaceReservedVariables(
          headerLog , log , from , tqBean );
      from = EventOutboundReservedVariables.replaceReservedVariables(
          headerLog , log , from , imsg );
      emailBean.setFrom( from );
    }

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

  private static boolean cleanEmailSenderAddress( String headerLog ,
      TransactionLog log , TransactionConf conf , EmailBean emailBean ) {
    boolean result = false;

    if ( emailBean == null ) {
      return result;
    }

    String emailStr = emailBean.getFrom();

    if ( emailStr == null ) {
      return result;
    }

    String emailAddr = null;
    String emailName = null;

    Pattern pattern = Pattern
        .compile( "([<]?[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,4}[>]?)" );

    Matcher matcher = pattern.matcher( emailStr );
    if ( matcher.find() ) {
      emailAddr = matcher.group();
    }
    if ( ( emailAddr != null ) && ( emailAddr.length() > 3 ) ) {
      int indexOfEmailAddr = emailStr.indexOf( emailAddr );
      if ( emailAddr.startsWith( "<" ) ) {
        emailAddr = emailAddr.substring( 1 );
        if ( indexOfEmailAddr > 0 ) {
          emailName = emailStr.substring( 0 , indexOfEmailAddr );
        }
      }
      if ( emailAddr.endsWith( ">" ) ) {
        emailAddr = emailAddr.substring( 0 , emailAddr.length() - 1 );
      }
    }

    if ( StringUtils.isBlank( emailAddr ) ) {
      emailAddr = PropertyProcessStep.getValue( log , headerLog , conf ,
          "EMAIL" , "addressFrom" );
      if ( !StringUtils.isBlank( emailStr ) ) {
        emailName = emailStr;
      }
    }

    log.debug( headerLog + "Extracted email's from : address = "
        + emailBean.getFrom() + " -> " + emailAddr + " , name = "
        + emailBean.getFromName() + " -> " + emailName );

    emailBean.setFrom( emailAddr );
    emailBean.setFromName( emailName );

    result = true;
    return result;
  }

}
