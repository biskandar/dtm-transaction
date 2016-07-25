package com.beepcast.model.event;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.xipme.XipmeCloneParam;
import com.beepcast.model.transaction.xipme.XipmeCloneParamFactory;
import com.beepcast.model.transaction.xipme.XipmeUtils;

public class EventOutboundXipmeReservedVariables {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static ProcessBean replaceXipmeReservedVariables( TransactionLog log ,
      ProcessBean pBean , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg ) {
    return replaceXipmeReservedVariables( null , log , pBean , tqBean , imsg );
  }

  public static ProcessBean replaceXipmeReservedVariables( String headerLog ,
      TransactionLog log , ProcessBean pBean , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg ) {
    headerLog = ( headerLog == null ) ? "" : headerLog;
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to replace xipme reserved variables "
          + ", found null process bean " );
      return pBean;
    }
    try {
      // replace response reserved vars
      String response = pBean.getResponse();
      if ( !StringUtils.isBlank( response ) ) {
        log.debug( headerLog + "Trying to replace processBean's response "
            + "with xipme reserved vars" );
        String newResponse = replaceXipmeReservedVariables( headerLog , log ,
            response , imsg );
        pBean.setResponse( newResponse );
      }
      // replace rfa reserved vars
      String rfa = pBean.getRfa();
      if ( !StringUtils.isBlank( rfa ) ) {
        log.debug( headerLog + "Trying to replace processBean's rfa "
            + "with xipme reserved vars" );
        String newRfa = replaceXipmeReservedVariables( headerLog , log , rfa ,
            imsg );
        pBean.setRfa( newRfa );
      }
    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to replace xipme reserved variables , "
          + e );
    }
    return pBean;
  }

  public static String replaceXipmeReservedVariables( TransactionLog log ,
      String strResponse , TransactionInputMessage imsg ) {
    return replaceXipmeReservedVariables( null , log , strResponse , imsg );
  }

  public static String replaceXipmeReservedVariables( String headerLog ,
      TransactionLog log , String strResponse , TransactionInputMessage imsg ) {
    String strResult = null;

    // header log
    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( strResponse == null ) {
      return strResult;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to replace xipme reserved variables "
          + ", found null input trans message" );
      strResult = strResponse;
      return strResult;
    }

    // compose regex pattern
    Pattern pattern1 = Pattern.compile( "<#XIPME\\((.+?)\\)#>" );

    // <#XIPME(http://xip.me/abcde)#>
    // <#XIPME(http://xip.me/abcde,http://www.google.com)#>
    // <#XIPME(http://xip.me/abcde,http://www.google.com,http://www.mobile.com)#>

    StringBuffer sbResult = new StringBuffer();
    Matcher matcher1 = pattern1.matcher( strResponse );
    while ( matcher1.find() ) {

      // prepare string replacement
      String strReplace = null;

      try {

        // build array parameters
        String strParams = matcher1.group( 1 );
        String[] arrParams = strParams.split( "\\s*,\\s*" );

        // replace and clean the reserved variable
        strReplace = replaceXipmeReservedVariable( headerLog , log , arrParams ,
            imsg );
        log.debug( headerLog + "Replaced xipme reserved variable "
            + ": arrParams = " + Arrays.asList( arrParams )
            + " , strReplace = " + StringEscapeUtils.escapeJava( strReplace ) );

      } catch ( Exception e ) {
        log.warning( headerLog + "Failed to replace xipme "
            + "reserved variable , cause : " + e );
      }

      // make sure all variable is replaceable
      strReplace = ( strReplace == null ) ? "" : strReplace;
      matcher1.appendReplacement( sbResult , strReplace );

    } // while ( matcher1.find() )

    matcher1.appendTail( sbResult );
    strResult = sbResult.toString();

    return strResult;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private static String replaceXipmeReservedVariable( String headerLog ,
      TransactionLog log , String[] arrParams , TransactionInputMessage imsg ) {
    String result = null;

    // validate must be params

    if ( arrParams == null ) {
      return result;
    }

    // prepare for the xipme cloning

    String xipmeCode = null;
    String xipmeLink = null;
    String targetLink = null;
    String targetMobileLink = null;

    // extract the following params

    if ( arrParams.length > 0 ) {
      xipmeLink = arrParams[0];
    }
    if ( arrParams.length > 1 ) {
      targetLink = arrParams[1];
    }
    if ( arrParams.length > 2 ) {
      targetMobileLink = arrParams[2];
    }

    // validate must be params

    if ( ( xipmeLink == null ) || ( xipmeLink.equals( "" ) ) ) {
      return result;
    }

    // generate xipme code

    if ( xipmeLink != null ) {
      xipmeCode = XipmeUtils.getCode( xipmeLink );
    }

    // created xipme clone param

    XipmeCloneParam xipmeCloneParam = XipmeCloneParamFactory
        .createXipmeCloneParam( xipmeCode , xipmeLink , targetLink ,
            targetMobileLink );

    // add input message param

    Map mapXipmeCloneParams = (Map) imsg
        .getMessageParam( TransactionMessageParam.HDR_XIPME_CLONE_PARAMS_MAP );
    if ( mapXipmeCloneParams == null ) {
      mapXipmeCloneParams = new HashMap();
      imsg.addMessageParam( TransactionMessageParam.HDR_XIPME_CLONE_PARAMS_MAP ,
          mapXipmeCloneParams );
    }
    mapXipmeCloneParams.put( xipmeCloneParam.getXipmeCode() , xipmeCloneParam );
    log.debug( headerLog + "Input msg params of "
        + "xipmeCloneParam : xipmeCode = " + xipmeCloneParam.getXipmeCode()
        + " , xipmeLink = " + xipmeCloneParam.getXipmeLink()
        + " , targetLink = " + xipmeCloneParam.getTargetLink()
        + " , targetMobileLink = " + xipmeCloneParam.getTargetMobileLink() );

    // result as xipme link

    result = xipmeCloneParam.getXipmeLink();

    return result;
  }

}
