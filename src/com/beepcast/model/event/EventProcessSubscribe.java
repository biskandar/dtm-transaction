package com.beepcast.model.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.model.transaction.xipme.XipmeTranslator;
import com.beepcast.subscriber.ClientSubscriberBean;
import com.beepcast.subscriber.ClientSubscriberCustomBean;
import com.beepcast.subscriber.SubscriberGroupBean;

public class EventProcessSubscribe {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionConf conf , TransactionLog log , TransactionQueueBean tqBean ,
      ProcessBean pBean , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", found null process bean" );
      return result;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", found null input message" );
      return result;
    }

    // get mobile user bean
    MobileUserBean mobileUserBean = (MobileUserBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );
    if ( mobileUserBean == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", can not find mobile user bean object" );
      return result;
    }

    // get event bean
    EventBean eventBean = (EventBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( eventBean == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", can not find event bean object" );
      return result;
    }

    // event type subscribe ( assume the group and list is pre-defined )
    // get subscriber group bean , client subscriber ,
    // and client subscriber custom from
    // message param
    SubscriberGroupBean sgBean = (SubscriberGroupBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_SUBSCRIBER_GROUP_BEAN );
    if ( sgBean == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", can not find subscriber group bean object" );
      return result;
    }
    ClientSubscriberBean csBean = (ClientSubscriberBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN );
    if ( csBean == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", can not find client subscriber bean object" );
      return result;
    }
    ClientSubscriberCustomBean cscBean = (ClientSubscriberCustomBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN );
    if ( cscBean == null ) {
      log.warning( headerLog + "Failed to process subscribe "
          + ", can not find client subscriber custom bean object" );
      return result;
    }

    // get script source from event response step with
    // pre population reserved variable
    pBean = EventTransQueueReservedVariables.replaceReservedVariables(
        headerLog , log , pBean , tqBean );
    pBean = EventOutboundReservedVariables.replaceReservedVariables( headerLog ,
        log , pBean , tqBean , imsg );
    pBean = EventOutboundXipmeReservedVariables.replaceXipmeReservedVariables(
        headerLog , log , pBean , tqBean , imsg );
    String scriptSource = EventResponse.buildResponse( log , pBean );
    log.debug( headerLog + "Resolved script source : "
        + StringEscapeUtils.escapeJava( scriptSource ) );

    // resolve shortener link
    if ( scriptSource != null ) {
      XipmeTranslator xipmeTranslator = new XipmeTranslator();
      Map mapXipmeCloneParams = new HashMap();
      log.debug( headerLog + "Resolving shortener link from xipme server "
          + ", with : destinationAddress = " + mobileUserBean.getPhone()
          + " , mobileUserBean.id = " + mobileUserBean.getId()
          + " , eventBean.id = " + eventBean.getEventID() );
      scriptSource = xipmeTranslator.resolveMessageContent( log.header() ,
          scriptSource , mobileUserBean.getPhone() , null , mobileUserBean ,
          null , csBean , cscBean , eventBean , mapXipmeCloneParams );
      log.debug( headerLog
          + "Resolved script source ( after shortener link resolver ) : "
          + StringEscapeUtils.escapeJava( scriptSource ) );
    }

    // log it
    log.debug( headerLog + "Subscribe phoneNumber " + tqBean.getPhone()
        + " into the list , with : list's id = " + sgBean.getId()
        + " , list's name = " + sgBean.getGroupName() + " , channel's id = "
        + eventBean.getEventID() );

    // subscribe and execute script
    if ( !EventProcessSubscribeScript.execute( headerLog , log ,
        (int) tqBean.getClientID() , sgBean , csBean , cscBean ,
        tqBean.getPhone() , (int) eventBean.getEventID() , scriptSource ) ) {
      log.warning( headerLog + "Failed to subscribe phoneNumber to the list" );
      return result;
    }

    // log as suceed
    log.debug( headerLog + "Successfully subscribed phoneNumber to the list" );

    // subscribe client api message
    EventProcessSubscribeUtils.subscribeClientApiMessage( headerLog , support ,
        conf , log , tqBean , imsg );

    return result;
  }

}
