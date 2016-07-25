package com.beepcast.model.transaction;

import com.beepcast.idgen.IdGenApp;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionMessageFactory {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  private static final DLogContext lctx = new SimpleContext(
      "TransactionMessageFactory" );

  private static final String DEFAULT_PREFIX_MSGID = "INT";

  private static IdGenApp idGenApp = IdGenApp.getInstance();

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static String generateMessageId( String prefix ) {
    if ( ( prefix == null ) || ( prefix.equals( "" ) ) ) {
      prefix = DEFAULT_PREFIX_MSGID;
    }
    StringBuffer sbId = new StringBuffer();
    sbId.append( prefix );
    sbId.append( idGenApp.nextIdentifier() );
    return sbId.toString();
  }

  public static boolean generateMessageId( TransactionMessage msg ) {
    boolean result = false;
    if ( msg == null ) {
      DLog.warning( lctx , "Failed to generate messageId "
          + ", found null input message" );
      return result;
    }
    String messageId = msg.getMessageId();
    // when found empty messageId than generate the new one
    if ( ( messageId == null ) || ( messageId.equals( "" ) ) ) {
      result = true;
      messageId = generateMessageId( "" );
      msg.setMessageId( messageId );
      DLog.debug( lctx , "Generate a new messageId = " + messageId );
    }
    return result;
  }

  public static TransactionInputMessage createInputMessage( String messageId ,
      int messageType , String messageContent , String originalNode ,
      String originalAddress , String originalProvider ,
      String destinationAddress , int channelSessionId , int priority ) {
    TransactionInputMessage imsg = new TransactionInputMessage();
    imsg.setMessageId( messageId );
    imsg.setMessageType( messageType );
    imsg.setMessageContent( messageContent );
    imsg.setOriginalNode( originalNode );
    imsg.setOriginalAddress( originalAddress );
    imsg.setOriginalProvider( originalProvider );
    imsg.setDestinationAddress( destinationAddress );
    imsg.setChannelSessionId( channelSessionId );
    imsg.setPriority( priority );
    return imsg;
  }

  public static TransactionOutputMessage createOutputMessage(
      String correlationId , String messageProfile , String messageStatusCode ,
      String messageStatusDescription , String messageId , int messageType ,
      String messageContent , String originalNode , String originalAddress ,
      String originalMaskingAddress , String originalProvider ,
      String destinationNode , String destinationAddress ,
      String destinationProvider , int clientId , int eventId ,
      int channelSessionId , int priority ) {
    TransactionOutputMessage omsg = new TransactionOutputMessage();

    omsg.setCorrelationId( correlationId );
    omsg.setMessageProfile( messageProfile );
    omsg.setMessageStatusCode( messageStatusCode );
    omsg.setMessageStatusDescription( messageStatusDescription );

    omsg.setMessageId( messageId );
    omsg.setMessageType( messageType );
    omsg.setMessageContent( messageContent );

    omsg.setOriginalNode( originalNode );
    omsg.setOriginalAddress( originalAddress );
    omsg.setOriginalMaskingAddress( originalMaskingAddress );
    omsg.setOriginalProvider( originalProvider );

    omsg.setDestinationNode( destinationNode );
    omsg.setDestinationAddress( destinationAddress );
    omsg.setDestinationProvider( destinationProvider );

    omsg.setClientId( clientId );
    omsg.setEventId( eventId );
    omsg.setChannelSessionId( channelSessionId );

    omsg.setPriority( priority );

    return omsg;
  }

  public static TransactionOutputMessage createOutputMessage(
      TransactionInputMessage imsg , String messageProfile ,
      String messageStatusCode , String messageStatusDescription ,
      String messageId , int messageType , String messageContent ) {
    TransactionOutputMessage omsg = null;

    if ( imsg == null ) {
      DLog.debug( lctx , "Failed to create output message "
          + ", found null input message" );
      return omsg;
    }

    // create output message based on input message

    omsg = TransactionMessageFactory.createOutputMessage( imsg.getMessageId() ,
        messageProfile , messageStatusCode , messageStatusDescription ,
        messageId , messageType , messageContent , Node.DTM , "" , "" ,
        imsg.getOriginalProvider() , Node.DTM , imsg.getOriginalAddress() , "" ,
        imsg.getClientId() , imsg.getEventId() , imsg.getChannelSessionId() ,
        imsg.getPriority() );
    if ( omsg == null ) {
      DLog.debug( lctx , "Failed to create output message" );
      return omsg;
    }

    // copy message params

    String copiedMsgParams = copyMessageParams( imsg , omsg );

    // debug

    DLog.debug(
        lctx ,
        "Created output message , based on input message : messageId = "
            + omsg.getMessageId() + " , clientId = " + omsg.getClientId()
            + " , eventId = " + omsg.getEventId() + " , channelSessionId = "
            + omsg.getChannelSessionId() + " , priority = "
            + omsg.getPriority() + " , messageParams = [" + copiedMsgParams
            + "]" );

    return omsg;
  }

  public static String copyMessageParams( TransactionInputMessage imsg ,
      TransactionOutputMessage omsg ) {
    String result = null;

    if ( imsg == null ) {
      return result;
    }
    if ( omsg == null ) {
      return result;
    }

    // copy canonical message params

    String[] arrKeys = new String[] { TransactionMessageParam.HDR_COUNTRY_BEAN ,
        TransactionMessageParam.HDR_MOBILE_USER_BEAN ,
        TransactionMessageParam.HDR_CLIENT_BEAN ,
        TransactionMessageParam.HDR_EVENT_BEAN ,
        TransactionMessageParam.HDR_CHANNEL_LOG_BEAN ,
        TransactionMessageParam.HDR_CHANNEL_SESSION_BEAN ,
        TransactionMessageParam.HDR_SUBSCRIBER_GROUP_BEAN ,
        TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN ,
        TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN ,
        TransactionMessageParam.HDR_XIPME_CODES_MAP ,
        TransactionMessageParam.HDR_HAS_EXPECT_INPUT ,
        TransactionMessageParam.HDR_HAS_EVENT_MENU_TYPE ,
        TransactionMessageParam.HDR_BYPASS_MT_DEBIT ,
        TransactionMessageParam.HDR_BYPASS_SEND_PROVIDER ,
        TransactionMessageParam.HDR_BYPASS_GATEWAY_LOG ,
        TransactionMessageParam.HDR_SET_ORIMASKADDR ,
        TransactionMessageParam.HDR_XIPME_CLONE_PARAMS_MAP };

    if ( ( arrKeys != null ) && ( arrKeys.length > 0 ) ) {

      StringBuffer sbCopiedMsgParamKeys = null;
      for ( int idx = 0 ; idx < arrKeys.length ; idx++ ) {

        String strKey = arrKeys[idx];
        if ( ( strKey == null ) || ( strKey.equals( "" ) ) ) {
          continue;
        }

        Object val = imsg.getMessageParam( strKey );
        if ( val == null ) {
          continue;
        }

        // copied message param
        omsg.addMessageParam( strKey , val );

        // log purpose
        if ( sbCopiedMsgParamKeys == null ) {
          sbCopiedMsgParamKeys = new StringBuffer();
        } else {
          sbCopiedMsgParamKeys.append( "," );
        }
        sbCopiedMsgParamKeys.append( strKey );

      } // for ( int idx = 0 ; idx < arrKeys.length ; idx++ )

      if ( sbCopiedMsgParamKeys != null ) {
        result = sbCopiedMsgParamKeys.toString();
      }

    } // if ( ( arrKeys != null ) && ( arrKeys.length > 0 ) )

    return result;
  }

}
