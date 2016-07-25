package com.beepcast.model.transaction;

import java.util.LinkedList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.idgen.IdGenApp;
import com.beepcast.keyword.KeywordApp;
import com.beepcast.model.event.ProcessBean;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.beepcast.subscriber.SubscriberApp;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public abstract class TransactionProcessBasic extends TransactionProcess {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionProcessBasic" );

  public static final int NEXT_STEP_NIL = 0;
  public static final int NEXT_STEP_END = 1;
  public static final int NEXT_STEP_FAL = 2;
  public static final int NEXT_STEP_TRU = 3;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private boolean initialized;

  protected IdGenApp idGenApp;
  protected DatabaseLibrary dbLib;
  protected DBManagerApp dbMan;
  protected OnlinePropertiesApp oprops;
  protected KeywordApp keywordApp;
  protected SubscriberApp subscriberApp;
  protected BillingApp billingApp;
  protected ClientApp clientApp;
  protected ProviderApp providerApp;

  protected TransactionApp app;
  protected TransactionConf conf;
  protected TransactionLog log;
  protected TransactionSession session;
  protected TransactionSupport support;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionProcessBasic( boolean debugMode ) {
    super( debugMode );
    initialized = init();
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Property Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionApp app() {
    return app;
  }

  public TransactionConf conf() {
    return conf;
  }

  public TransactionLog log() {
    return log;
  }

  public TransactionSession session() {
    return session;
  }

  public TransactionSupport support() {
    return support;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  protected boolean validTransactionInputMessage( TransactionInputMessage imsg ) {
    boolean result = false;

    // validate is the input message null ?
    if ( imsg == null ) {
      DLog.warning( lctx , "Found null input message" );
      return result;
    }

    // validate is the phone number empty ?
    String phoneNumber = imsg.getOriginalAddress();
    if ( StringUtils.isBlank( phoneNumber ) ) {
      DLog.warning( lctx , "Found empty phoneNumber in the input message" );
      return result;
    }

    // trim phone number
    phoneNumber = StringUtils.trimToEmpty( phoneNumber );

    // validate the phone number format
    String number = null;
    if ( phoneNumber.startsWith( "+" ) ) {
      number = phoneNumber.substring( 1 );
    } else {
      number = phoneNumber;
    }
    if ( !StringUtils.isNumeric( number ) ) {
      DLog.warning( lctx , "Found invalid phone number format" );
      return result;
    }

    // re update phone number
    imsg.setOriginalAddress( phoneNumber );

    result = true;
    return result;
  }

  protected boolean verifyInputMessageContentWithLog(
      TransactionInputMessage imsg ) {
    boolean result = false;

    // read message content
    String messageContent = imsg.getMessageContent();

    // validate is message content empty ?
    if ( StringUtils.isBlank( messageContent ) ) {
      log.warning( "Found empty message content , log and bypass ." );
      // log input message into gateway log
      support.insertIncomingMessageToGatewayLog( imsg , "FAILED-EMPTY CONTENT" );
      return result;
    }

    // trim input message content
    messageContent = ( messageContent == null ) ? "" : messageContent.trim();
    imsg.setMessageContent( messageContent );
    log.debug( "Found input message content = "
        + StringEscapeUtils.escapeJava( messageContent ) );

    // resolve message type
    int messageType = imsg.getMessageType();
    if ( messageType == MessageType.TEXT_TYPE ) {
      if ( TransactionUtil.isUnicodeMessage( messageContent ) ) {
        messageType = MessageType.UNICODE_TYPE;
      }
    }
    log.debug( "Found input message type = "
        + MessageType.messageTypeToString( messageType ) );
    imsg.setMessageType( messageType );

    // resolve message count
    int messageCount = TransactionUtil.calculateTotalSms( messageType ,
        messageContent );
    log.debug( "Calculate total input message = " + messageCount + " sms(s)" );
    imsg.setMessageCount( messageCount );

    result = true;
    return result;
  }

  protected ProcessBean executeProcessNextSteps(
      TransactionQueueBean transQueue , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg ,
      LinkedList omsgs ) {
    if ( processSteps == null ) {
      log.warning( "Failed to execute process next steps "
          + ", found null process steps" );
      return processBean;
    }
    if ( processBean == null ) {
      log.warning( "Failed to execute process next steps "
          + ", found null process bean" );
      return processBean;
    }
    try {

      // log first
      log.debug( "Start to execute process next steps "
          + ", start from process bean : step = " + processBean.getStep()
          + " , type = " + processBean.getType() + " , nextStep = "
          + processBean.getNextStep() + " , nextType = "
          + processBean.getNextType() );

      // prepare variables
      ProcessBean istProcessBean = (ProcessBean) processBean.clone();
      ProcessBean curProcessBean = (ProcessBean) processBean.clone();
      ProcessBean nxtProcessBean = null;

      int counterExec = 0 , maximumExec = 20;
      while ( counterExec < maximumExec ) {

        String headerLog = "[Step-" + counterExec + "] ";

        // validate is the next step as an end ?
        if ( StringUtils.equals( curProcessBean.getNextStep() , "END" ) ) {
          log.debug( headerLog + "Found process with next step as END "
              + ", stop the process here." );
          break;
        }

        // validate is the next type as expecting ?
        if ( TransactionProcessBeanUtils.validProcessTypeAll( curProcessBean
            .getNextType() ) ) {
          log.debug( headerLog
              + "Found process with next type as expecting like "
              + curProcessBean.getNextType() + ", stop the process here." );
          break;
        }

        // execute and verify each of the next process step
        log.debug( headerLog + "Iterate and execute each of process next step" );
        int nextStepResult = executeProcessNextStep( headerLog , transQueue ,
            curProcessBean , processSteps , imsg , omsgs );
        if ( nextStepResult == NEXT_STEP_NIL ) {
          log.debug( headerLog + "Executed process next step "
              + "with result as NIL, stop the process here." );
          break;
        }
        counterExec = counterExec + 1;
        if ( nextStepResult == NEXT_STEP_END ) {
          log.debug( headerLog + "Executed process next step "
              + "with result as END, stop the process here." );
          break;
        }
        if ( nextStepResult == NEXT_STEP_FAL ) {
          log.debug( headerLog + "Executed process next step "
              + "with result as FALSE , next step = "
              + curProcessBean.getNextStep() + " , total steps = "
              + processSteps.length );
          if ( StringUtils.isNumeric( curProcessBean.getNextStep() ) ) {
            int curProcessBeanNextStep = Integer.parseInt( curProcessBean
                .getNextStep() ) + 1;
            ProcessBean tmpProcessBean = null;
            if ( curProcessBeanNextStep <= processSteps.length ) {
              tmpProcessBean = processSteps[curProcessBeanNextStep - 1];
            }
            if ( tmpProcessBean != null ) {
              nxtProcessBean = (ProcessBean) curProcessBean.clone();
              nxtProcessBean.setNextStep( tmpProcessBean.getStep() );
              nxtProcessBean.setNextType( tmpProcessBean.getType() );
            }
          }
        }
        if ( nextStepResult == NEXT_STEP_TRU ) {
          log.debug( headerLog + "Executed process next step "
              + "with result as TRUE , next step = "
              + curProcessBean.getNextStep() + " , total steps = "
              + processSteps.length );
          if ( StringUtils.isNumeric( curProcessBean.getNextStep() ) ) {
            int curProcessBeanNextStep = Integer.parseInt( curProcessBean
                .getNextStep() );
            if ( curProcessBeanNextStep <= processSteps.length ) {
              nxtProcessBean = processSteps[curProcessBeanNextStep - 1];
            }
          }
        }

        // validate the next process bean
        if ( nxtProcessBean == null ) {
          log.debug( headerLog + "Failed read the next process bean "
              + ", stop to execute next step." );
          break;
        }

        // validate is the loop forever ?
        if ( StringUtils.equals( nxtProcessBean.getStep() ,
            istProcessBean.getStep() )
            && StringUtils.equals( nxtProcessBean.getNextStep() ,
                istProcessBean.getNextStep() ) ) {
          log.warning( headerLog + "Found the process loop start "
              + "from the beginning , stop to execute next step." );
          break;
        }

        // log , and move the next process bean
        log.debug( headerLog + "Executed process bean : step = "
            + curProcessBean.getNextStep() + " , type = "
            + curProcessBean.getNextType()
            + " ; Prepared next process bean : step = "
            + nxtProcessBean.getNextStep() + " , type = "
            + nxtProcessBean.getNextType() );
        curProcessBean = nxtProcessBean;

      } // loop while true
      log.debug( "Executed all total " + counterExec + " process step(s)" );
      processBean = curProcessBean;
    } catch ( Exception e ) {
      log.warning( "Failed to execute process next steps , " + e );
    }
    return processBean;
  }

  protected int executeProcessNextStep( String headerLog ,
      TransactionQueueBean transQueue , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg ,
      LinkedList omsgs ) {
    int nextStepResult = NEXT_STEP_NIL;
    try {

      // process step of : IF DATE BEFORE , IF DATE AFTER
      nextStepResult = support.ifDate( headerLog , this , transQueue ,
          processBean , processSteps , imsg , omsgs );
      if ( nextStepResult != NEXT_STEP_NIL ) {
        log.debug( headerLog + "Trapped if date process type , result = "
            + nextStepResult );
        return nextStepResult;
      }

      // process step of : SEND IF
      nextStepResult = support.sendIf( headerLog , this , transQueue ,
          processBean , processSteps , imsg , omsgs );
      if ( nextStepResult != NEXT_STEP_NIL ) {
        log.debug( headerLog + "Trapped send if process type , result = "
            + nextStepResult );
        return nextStepResult;
      }

      // process step of : DELAY_SEND
      if ( support.delaySend( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // process step of : LOG
      if ( support.log( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // process step of : REMINDER , REMINDER RSVP PENDING , REMINDER RSVP YES
      if ( support.addReminder( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // process step of : NO REMINDER , NO REMINDER RSVP NO
      if ( support.delReminder( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // send email
      if ( support.sendEmail( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // sms to email
      if ( support.smsToEmail( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // sms to sms
      if ( support.smsToSms( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // sms to sms exclude sender
      if ( support.smsToSmsExcludeSender( headerLog , this , transQueue ,
          processBean , processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // email client
      if ( support.emailClient( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // email to
      if ( support.emailTo( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // send ringtone
      if ( support.sendRingtone( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // phone user
      if ( support.phoneUser( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // auto subscribe
      if ( support.autoSubscribe( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // subscribe
      if ( support.subscribe( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // unsubscribe
      if ( support.unsubscribe( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // subscribe list
      if ( support.subscribeList( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // unsubscribe list
      if ( support.unsubscribeList( headerLog , this , transQueue ,
          processBean , processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // auto send
      if ( support.autoSend( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // wap push
      if ( support.wapPush( headerLog , this , transQueue , processBean ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // create qr image
      if ( support.createQrImage( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // webhook
      if ( support.webhook( headerLog , this , transQueue , processBean ,
          processSteps , imsg , omsgs ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

      // random code
      if ( support.randomCode( headerLog , this , transQueue , processBean ,
          processSteps , imsg ) ) {
        nextStepResult = NEXT_STEP_TRU;
        return nextStepResult;
      }

    } catch ( Exception e ) {
      log.warning( "Failed to execute process next step , " + e );
    }

    return nextStepResult;
  }

  protected boolean validTransactionOutputMessage( TransactionOutputMessage omsg ) {
    boolean result = false;

    if ( omsg == null ) {
      log.warning( "Found null output message" );
      return result;
    }

    // get message status code
    String messageStatusCode = omsg.getMessageStatusCode();
    if ( messageStatusCode == null ) {
      log.warning( "Found null messageStatusCode in the output message" );
      return result;
    }

    result = true;
    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private boolean init() {
    boolean result = false;

    idGenApp = IdGenApp.getInstance();
    dbLib = DatabaseLibrary.getInstance();
    dbMan = DBManagerApp.getInstance();
    oprops = OnlinePropertiesApp.getInstance();
    keywordApp = KeywordApp.getInstance();
    subscriberApp = SubscriberApp.getInstance();
    billingApp = BillingApp.getInstance();
    clientApp = ClientApp.getInstance();
    providerApp = ProviderApp.getInstance();

    app = TransactionApp.getInstance();
    if ( app == null ) {
      DLog.warning( lctx , "Failed to init transaction "
          + ", found transaction app is null" );
      return result;
    }
    conf = app.getTransactionConf();
    log = new TransactionLog();

    session = new TransactionSession( this );
    support = new TransactionSupport( this );

    result = true;
    return result;
  }

}
