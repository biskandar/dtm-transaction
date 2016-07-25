package com.beepcast.model.transaction.xipme;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.beepcast.channel.ChannelLogBean;
import com.beepcast.model.event.EventOutboundReservedVariables;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.subscriber.ClientSubscriberBean;
import com.beepcast.subscriber.ClientSubscriberCustomBean;

public class XipmeCloneParamSupport {

  public static Map resolveMapXipmeCloneParams( String headerLog ,
      MobileUserBean muBean , ChannelLogBean clBean ,
      ClientSubscriberBean csBean , ClientSubscriberCustomBean cscBean ,
      Map mapXipmeCloneParamsCur ) {
    Map mapXipmeCloneParamsNew = null;

    if ( mapXipmeCloneParamsCur == null ) {
      return mapXipmeCloneParamsNew;
    }

    mapXipmeCloneParamsNew = new HashMap();

    String key = null;
    XipmeCloneParam xipmeCloneParam = null;
    Set setKey = mapXipmeCloneParamsCur.keySet();
    Iterator iterKey = setKey.iterator();
    while ( iterKey.hasNext() ) {
      key = (String) iterKey.next();
      if ( key == null ) {
        continue;
      }
      xipmeCloneParam = (XipmeCloneParam) mapXipmeCloneParamsCur.get( key );
      if ( xipmeCloneParam == null ) {
        continue;
      }
      xipmeCloneParam = resolveXipmeCloneParam( headerLog , muBean , clBean ,
          csBean , cscBean , xipmeCloneParam );
      if ( xipmeCloneParam == null ) {
        continue;
      }
      mapXipmeCloneParamsNew.put( xipmeCloneParam.getXipmeCode() ,
          xipmeCloneParam );
    }

    return mapXipmeCloneParamsNew;
  }

  public static XipmeCloneParam resolveXipmeCloneParam( String headerLog ,
      MobileUserBean muBean , ChannelLogBean clBean ,
      ClientSubscriberBean csBean , ClientSubscriberCustomBean cscBean ,
      XipmeCloneParam xipmeCloneParamCur ) {
    XipmeCloneParam xipmeCloneParamNew = null;

    if ( xipmeCloneParamCur == null ) {
      return xipmeCloneParamNew;
    }

    String xipmeLink = resolveText( headerLog , muBean , clBean , csBean ,
        cscBean , xipmeCloneParamCur.getXipmeLink() );
    String targetLink = resolveText( headerLog , muBean , clBean , csBean ,
        cscBean , xipmeCloneParamCur.getTargetLink() );
    String targetMobileLink = resolveText( headerLog , muBean , clBean ,
        csBean , cscBean , xipmeCloneParamCur.getTargetMobileLink() );

    String xipmeCode = XipmeUtils.getCode( xipmeLink );
    if ( xipmeCode == null ) {
      return xipmeCloneParamNew;
    }

    xipmeCloneParamNew = XipmeCloneParamFactory.createXipmeCloneParam(
        xipmeCode , xipmeLink , targetLink , targetMobileLink );

    return xipmeCloneParamNew;
  }

  public static String resolveText( String headerLog , MobileUserBean muBean ,
      ChannelLogBean clBean , ClientSubscriberBean csBean ,
      ClientSubscriberCustomBean cscBean , String textCur ) {
    String textNew = null;

    if ( textCur == null ) {
      return textNew;
    }

    // create fake transaction input message

    TransactionInputMessage imsg = TransactionMessageFactory
        .createInputMessage( null , 0 , null , null , null , null , null , 0 ,
            0 );

    // put message param of input message

    if ( muBean != null ) {
      imsg.addMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN ,
          muBean );
    }
    if ( clBean != null ) {
      imsg.addMessageParam( TransactionMessageParam.HDR_CHANNEL_LOG_BEAN ,
          clBean );
    }
    if ( csBean != null ) {
      imsg.addMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN ,
          csBean );
    }
    if ( cscBean != null ) {
      imsg.addMessageParam(
          TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN , cscBean );
    }

    // resolve the text

    textNew = EventOutboundReservedVariables.replaceReservedVariables(
        headerLog , new TransactionLog() , textCur , imsg );

    return textNew;
  }

}
