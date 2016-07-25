package com.beepcast.model.event;

import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.smsenc.SmsTypeFactory;
import com.beepcast.smsenc.SmsTypeService;
import com.beepcast.smsenc.impl.WapPush;

public class EventProcessWappush {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ) {
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

    // read and clean the response
    String strResponse = pBean.getResponse();
    strResponse = ( strResponse == null ) ? "" : strResponse.trim();

    // read the uri and text from the response
    String strText = null;
    String strUri = null;
    {
      int i , j , n = strResponse.length();
      i = -1;
      if ( i == -1 ) {
        i = strResponse.indexOf( "http://" );
      }
      if ( i == -1 ) {
        i = strResponse.indexOf( "https://" );
      }
      if ( i == -1 ) {
        i = strResponse.indexOf( "www." );
      }
      if ( i != -1 ) {
        j = -1;
        if ( j == -1 ) {
          j = strResponse.indexOf( " " , i );
        }
        if ( j == -1 ) {
          j = n;
        }
        strUri = strResponse.substring( i , j );
      }
      if ( strUri != null ) {
        i = strResponse.indexOf( strUri );
        j = strUri.length();
        strText = strResponse.substring( 0 , i );
        strText = strText + strResponse.substring( i + j , n ).trim();
      }
    }

    { // clean and log the uri and text
      strText = ( strText == null ) ? "" : strText.trim();
      strUri = ( strUri == null ) ? "" : strUri.trim();
      if ( !strUri.startsWith( "http" ) ) {
        strUri = "http://" + strUri;
      }
      log.debug( headerLog + "Composed wappush message with text = " + strText
          + " , uri = " + strUri );
    }

    // generate wappush object
    WapPush wappush = null;
    {
      SmsTypeService service = SmsTypeFactory
          .generateSmsTypeService( "WapPush" );
      if ( service != null ) {
        if ( ( service instanceof WapPush ) ) {
          wappush = (WapPush) service;
          wappush.setText( strText );
          wappush.setUri( strUri );
        } else {
          log.warning( headerLog + "Found anonymous wappush function" );
        }
      } else {
        log.warning( headerLog + "Failed to generate wappush function" );
      }
    }

    // generate wappush pdu and store back into response
    if ( wappush != null ) {
      String pdu = wappush.convertToPdu();
      if ( pdu != null ) {
        log.debug( headerLog + "Generated wappush pdu = " + pdu );
        pBean.setResponse( "WAP_PUSH=".concat( pdu ) );
      } else {
        log.warning( headerLog + "Failed to generate wappush pdu" );
      }
    }

    result = true;
    return result;
  }

}
