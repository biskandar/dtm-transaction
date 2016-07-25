package com.beepcast.model.transaction;

import java.util.HashMap;
import java.util.Map;

import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionQueueSupport {

  static final DLogContext lctx = new SimpleContext( "TransactionQueueSupport" );

  public static Map readMapParams( TransactionQueueBean transQueueBean ) {
    Map mapParams = new HashMap();
    if ( transQueueBean == null ) {
      return mapParams;
    }
    String strParams = transQueueBean.getParams();
    if ( ( strParams == null ) || ( strParams.equals( "" ) ) ) {
      return mapParams;
    }
    String[] arrParams = strParams.split( "," );
    for ( int idx = 0 ; idx < arrParams.length ; idx++ ) {
      if ( ( arrParams[idx] == null ) || ( arrParams[idx].equals( "" ) ) ) {
        continue;
      }
      String[] arrParam = arrParams[idx].split( "=" );
      if ( arrParam.length < 1 ) {
        continue;
      }
      mapParams.put( arrParam[0].trim() , arrParam[1].trim() );
    }
    return mapParams;
  }

}
