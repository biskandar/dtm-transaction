package com.beepcast.model.transaction.script;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ScriptWebhookDataMigration {

  static final DLogContext lctx = new SimpleContext(
      "ScriptWebhookDataMigration" );

  public static boolean export( ScriptWebhookData data ,
      TransactionOutputMessage omsg ) {
    boolean result = false;

    if ( data == null ) {
      return result;
    }

    if ( omsg == null ) {
      return result;
    }

    // set method
    omsg.addMessageParam( TransactionMessageParam.HDR_WEBHOOK_METHOD ,
        data.getMethod() );

    // set uri
    omsg.addMessageParam( TransactionMessageParam.HDR_WEBHOOK_URI ,
        data.getUri() );

    // set data
    omsg.setMessageContent( exportTextData( data ) );

    result = true;
    return result;
  }

  public static String exportTextData( ScriptWebhookData data ) {
    String textData = null;

    Map parameters = data.getParameters();
    if ( parameters == null ) {
      return textData;
    }

    try {
      StringBuffer sbData = null;
      Set setKeys = parameters.keySet();
      Iterator iterKeys = setKeys.iterator();
      while ( iterKeys.hasNext() ) {

        String key = (String) iterKeys.next();
        if ( ( key == null ) || ( key.equals( "" ) ) ) {
          continue;
        }

        if ( sbData == null ) {
          sbData = new StringBuffer();
        } else {
          sbData.append( "&" );
        }
        sbData.append( key );

        String val = (String) parameters.get( key );
        if ( val == null ) {
          continue;
        }

        sbData.append( "=" );
        sbData.append( URLEncoder.encode( val , "UTF-8" ) );

      } // while ( iterKeys.hasNext() )
      if ( sbData != null ) {
        textData = sbData.toString();
      }
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to export script webhook data's"
          + " parameters to text data" );
    }

    return textData;
  }

}
