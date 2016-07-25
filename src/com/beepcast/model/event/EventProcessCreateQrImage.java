package com.beepcast.model.event;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

import edu.emory.mathcs.backport.java.util.Arrays;

public class EventProcessCreateQrImage {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // validate list outbound message(s)
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null output messages" );
      return result;
    }

    // create new message id when it's not a first message
    boolean createNewMessageId = omsgs.size() > 0;

    // execute process for qr image
    TransactionOutputMessage omsg = process( headerLog , support , log ,
        tqBean , pBean , imsg , createNewMessageId );
    if ( omsg == null ) {
      log.warning( headerLog + "Failed to process "
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
      log.warning( headerLog + "Failed to process "
          + ", found null trans queue bean" );
      return omsg;
    }
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null process bean" );
      return omsg;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null input message" );
      return omsg;
    }

    // prepare param label and names
    String paramLabel = pBean.getParamLabel();
    String[] arrNames = pBean.getNames();

    if ( ( paramLabel == null )
        || ( !paramLabel.equalsIgnoreCase( "FILENAME,SIZE=" ) ) ) {
      log.warning( headerLog + "Failed to process "
          + ", found invalid param label = " + paramLabel );
      return omsg;
    }
    if ( arrNames == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null event's process's array names" );
      return omsg;
    }

    String[] arrNamesResolved = resolveEventProcessNames( headerLog , log ,
        tqBean , imsg , arrNames );

    log.debug( headerLog + "Resolved paramLabel = " + pBean.getParamLabel()
        + " , arrNames.length = " + arrNames.length + " , arrNames.data = "
        + Arrays.asList( arrNames ) + " , arrNamesResolved.data = "
        + Arrays.asList( arrNamesResolved ) );

    if ( arrNamesResolved == null ) {
      log.warning( headerLog + "Failed to process "
          + ", failed to resolve null event's process's array names" );
      return omsg;
    }

    // resolve file name , and size
    String imageFileName = "";
    String imageFileSize = "";
    try {
      if ( arrNamesResolved.length > 0 ) {
        imageFileName = arrNamesResolved[0];
      }
      if ( arrNamesResolved.length > 1 ) {
        imageFileSize = arrNamesResolved[1];
      }
    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to resolve the file name and size , "
          + e );
    }
    log.debug( headerLog + "Resolved image file : name = " + imageFileName
        + " , size = " + imageFileSize );

    // prepare the message content
    String messageContent = createMessageContent( headerLog , log , tqBean ,
        pBean , imsg );
    if ( StringUtils.isBlank( messageContent ) ) {
      log.warning( headerLog + "Failed to process sms to sms step "
          + ", found empty message content" );
      return omsg;
    }
    log.debug( headerLog + "Created message content : "
        + StringEscapeUtils.escapeJava( messageContent ) );

    // set as qr image message with image type based on file extension
    int messageType = MessageType.QRPNG_TYPE;
    if ( imageFileName != null ) {
      if ( imageFileName.toLowerCase().endsWith( ".png" ) ) {
        messageType = MessageType.QRPNG_TYPE;
      }
      if ( imageFileName.toLowerCase().endsWith( ".gif" ) ) {
        messageType = MessageType.QRGIF_TYPE;
      }
      if ( imageFileName.toLowerCase().endsWith( ".jpg" ) ) {
        messageType = MessageType.QRJPG_TYPE;
      }
    }

    // build new output message
    omsg = support.createReplyMessage( imsg , messageContent , messageType ,
        createNewMessageId );

    // set image properties
    omsg.addMessageParam( TransactionMessageParam.HDR_QR_IMAGE_FILE_NAME ,
        imageFileName );
    omsg.addMessageParam( TransactionMessageParam.HDR_QR_IMAGE_FILE_SIZE ,
        imageFileSize );

    // log it
    log.debug( headerLog + "Created qr image message : messageType = "
        + MessageType.messageTypeToString( omsg.getMessageType() )
        + " , messageContent = "
        + StringEscapeUtils.escapeJava( omsg.getMessageContent() )
        + " , imageFileName = "
        + omsg.getMessageParam( TransactionMessageParam.HDR_QR_IMAGE_FILE_NAME )
        + " , imageFileSize = "
        + omsg.getMessageParam( TransactionMessageParam.HDR_QR_IMAGE_FILE_SIZE ) );

    return omsg;
  }

  private static String[] resolveEventProcessNames( String headerLog ,
      TransactionLog log , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg , String[] arrNamesCur ) {
    String[] arrNamesNew = null;
    if ( arrNamesCur == null ) {
      return arrNamesNew;
    }
    String strNames = StringUtils.join( arrNamesCur , "," );
    strNames = EventTransQueueReservedVariables.replaceReservedVariables(
        headerLog , log , strNames , tqBean );
    strNames = EventOutboundReservedVariables.replaceReservedVariables(
        headerLog , log , strNames , imsg );
    arrNamesNew = strNames.split( "," );
    return arrNamesNew;
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
