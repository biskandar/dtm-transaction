package com.beepcast.model.event;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.model.transaction.script.ScriptWebhookData;
import com.beepcast.model.transaction.script.ScriptWebhookDataFactory;
import com.beepcast.model.transaction.script.ScriptWebhookDataMigration;
import com.beepcast.model.transaction.script.ScriptWebhookExec;

public class EventProcessWebhook {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // validate list outbound message(s)
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found null output messages" );
      return result;
    }

    // create new message id when it's not a first message
    boolean createNewMessageId = omsgs.size() > 0;

    // execute process for webhook
    TransactionOutputMessage omsg = process( headerLog , support , log ,
        tqBean , pBean , imsg , createNewMessageId );
    if ( omsg == null ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found failed to generate outbound message" );
      return result;
    }

    // store output message
    omsgs.add( omsg );

    result = true;
    return result;
  }

  public static TransactionOutputMessage process( String headerLog ,
      TransactionSupport support , TransactionLog log ,
      TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , boolean createNewMessageId ) {
    TransactionOutputMessage omsg = null;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( tqBean == null ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found null trans queue bean" );
      return omsg;
    }
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found null process bean" );
      return omsg;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found null input message" );
      return omsg;
    }

    // get script source
    pBean = EventTransQueueReservedVariables.replaceReservedVariables(
        headerLog , log , pBean , tqBean );
    pBean = EventOutboundReservedVariables.replaceReservedVariables( headerLog ,
        log , pBean , tqBean , imsg );
    String scriptSource = EventResponse.buildResponse( log , pBean );
    log.debug( headerLog + "Resolved script source : "
        + StringEscapeUtils.escapeJava( scriptSource ) );

    // prepare for script web hook data
    ScriptWebhookData scriptWebhookData = ScriptWebhookDataFactory
        .createScriptWebhookData( "POST" , null , new HashMap() );

    // execute the script
    ScriptWebhookExec scriptWebhookExec = new ScriptWebhookExec( headerLog ,
        scriptSource );
    if ( !scriptWebhookExec.execute( scriptWebhookData ) ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found failed to execute the script" );
      return omsg;
    }

    // build new output message
    omsg = support.createReplyMessage( imsg , null , MessageType.WEBHOOK_TYPE ,
        createNewMessageId );

    // convert
    if ( !ScriptWebhookDataMigration.export( scriptWebhookData , omsg ) ) {
      log.warning( headerLog + "Failed to process webhook "
          + ", found failed to export script webhook data to message" );
      omsg = null;
      return omsg;
    }

    // log it
    log.debug( headerLog + "Created webhook message : messageType = "
        + MessageType.messageTypeToString( omsg.getMessageType() )
        + " , messageContent = "
        + StringEscapeUtils.escapeJava( omsg.getMessageContent() )
        + " , webhookMethod = "
        + omsg.getMessageParam( TransactionMessageParam.HDR_WEBHOOK_METHOD )
        + " , webhookUri = "
        + omsg.getMessageParam( TransactionMessageParam.HDR_WEBHOOK_URI ) );

    return omsg;
  }

}
