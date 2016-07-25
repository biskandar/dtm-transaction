package com.beepcast.model.event;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionLog;

public class EventResponse {

  public static String buildResponse( TransactionLog log , ProcessBean pBean ) {
    String strResponse = "";

    if ( pBean == null ) {
      log.warning( "Failed to build response , found null process bean" );
      return strResponse;
    }

    StringBuffer sbResponse = null;

    String pBeanResponse = pBean.getResponse();
    if ( !StringUtils.isBlank( pBeanResponse ) ) {
      pBeanResponse = pBeanResponse.trim();
      if ( sbResponse == null ) {
        sbResponse = new StringBuffer();
      } else {
        sbResponse.append( "\n" );
      }
      sbResponse.append( pBeanResponse );
    }

    String pBeanRfa = pBean.getRfa();
    if ( !StringUtils.isBlank( pBeanRfa ) ) {
      pBeanRfa = pBeanRfa.trim();
      if ( sbResponse == null ) {
        sbResponse = new StringBuffer();
      } else {
        sbResponse.append( "\n" );
      }
      sbResponse.append( pBeanRfa );
    }

    if ( sbResponse != null ) {
      strResponse = sbResponse.toString();
    }

    log.debug( "Built response from process bean ( " + strResponse.length()
        + " chars ) = " + StringEscapeUtils.escapeJava( strResponse ) );

    return strResponse;
  }

}
