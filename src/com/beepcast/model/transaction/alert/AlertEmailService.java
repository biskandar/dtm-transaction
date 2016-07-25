package com.beepcast.model.transaction.alert;

import org.apache.commons.lang.StringUtils;

import com.beepcast.onm.OnmApp;
import com.beepcast.onm.alert.AlertMessageFactory;
import com.beepcast.onm.data.AlertMessage;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class AlertEmailService {

  static final DLogContext lctx = new SimpleContext( "AlertEmailService" );

  public static final String ALERT_TYPE_CLIENT_LOW_BALANCE = "ALERT_CLIENT_LOW_BALANCE";

  private OnmApp onmApp;

  public AlertEmailService() {
    onmApp = OnmApp.getInstance();
  }

  public boolean doAlert( String emailAddress , String managerName ,
      String companyName , double currentAmount , String alertType ) {
    boolean result = false;

    String emailMessageSubject = generateEmailMessageSubject( alertType );
    if ( StringUtils.isBlank( emailMessageSubject ) ) {
      DLog.warning( lctx , "Failed to do alert "
          + ", failed to generate email message subject" );
      return result;
    }

    String emailMessageContent = generateEmailMessageContent( alertType ,
        managerName , companyName , currentAmount );
    if ( StringUtils.isBlank( emailMessageContent ) ) {
      DLog.warning( lctx , "Failed to do alert "
          + ", failed to generate email message content" );
      return result;
    }

    AlertMessage alertEmailMessage = AlertMessageFactory
        .createAlertEmailMessage( AlertMessage.DESTNODE_SERVICE_EMAIL ,
            emailAddress , emailMessageSubject , emailMessageContent , 0 );
    if ( alertEmailMessage == null ) {
      DLog.warning( lctx , "Failed to do alert "
          + ", failed to create alert email message" );
      return result;
    }

    if ( !onmApp.sendAlert( alertEmailMessage ) ) {
      DLog.warning( lctx , "Failed to do alert "
          + ", failed to send alert message" );
      return result;
    }

    result = true;
    return result;
  }

  private String generateEmailMessageSubject( String alertType ) {
    String subject = null;
    if ( alertType == null ) {
      return subject;
    }
    if ( alertType.equals( ALERT_TYPE_CLIENT_LOW_BALANCE ) ) {
      subject = "DirectToMobile message credit alert!";
    }
    return subject;
  }

  private String generateEmailMessageContent( String alertType ,
      String managerName , String companyName , double currentAmount ) {
    String content = null;
    if ( alertType == null ) {
      return content;
    }
    if ( alertType.equals( ALERT_TYPE_CLIENT_LOW_BALANCE ) ) {
      StringBuffer sbContent = new StringBuffer();
      sbContent.append( "Dear " + managerName + " ,\n" );
      sbContent.append( "\n" );
      sbContent.append( "Your SMS credit under company name "
          + StringUtils.upperCase( companyName ) + " is running low, you have "
          + currentAmount + " credit(s) remaining.\n" );
      sbContent.append( "\n" );
      sbContent.append( " To continue to receive and send messages"
          + ", please top up your account before your credit reaches zero.\n" );
      sbContent.append( "\n" );
      sbContent.append( "Currently to top-up your account"
          + ", you need to contact BeepCast Pte Ltd.\n" );
      sbContent.append( "Email: services@directtomobile.com\n" );
      sbContent.append( "Tel: +65 64239685\n" );
      sbContent.append( "\n" );
      sbContent.append( "Thank you,\n" );
      sbContent.append( "DirectToMobile" );
      content = sbContent.toString();
    }
    return content;
  }

}
