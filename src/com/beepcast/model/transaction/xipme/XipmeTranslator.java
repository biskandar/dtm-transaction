package com.beepcast.model.transaction.xipme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.beepcast.channel.ChannelLogBean;
import com.beepcast.loadmng.LoadManagement;
import com.beepcast.loadmng.LoadManagementApi;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.gatewayXipme.GatewayXipmeBean;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.transaction.TransactionApp;
import com.beepcast.subscriber.ClientSubscriberBean;
import com.beepcast.subscriber.ClientSubscriberCustomBean;
import com.beepcast.subscriber.xipme.SubscriberXipmeMapParam;
import com.beepcast.util.properties.GlobalEnvironment;
import com.beepcast.xipme.XipmeApp;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;
import com.xipme.api.Channel;
import com.xipme.api.MapParam;
import com.xipme.api.actions.Action;
import com.xipme.api.actions.ActionFactory;

public class XipmeTranslator {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "XipmeTranslator" );

  static LoadManagement loadMng = LoadManagement.getInstance();

  static final String XIPME_TAGS = "dtmeng";

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private boolean initialized;

  private GlobalEnvironment globalEnv;

  private XipmeApp xipmeApp;

  private boolean debug;

  private String loginUsername;
  private String loginPassword;
  private String remoteUrl;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public XipmeTranslator() {
    initialized = false;

    globalEnv = GlobalEnvironment.getInstance();
    if ( globalEnv == null ) {
      DLog.warning( lctx , "Failed to initialized , found null global env" );
      return;
    }
    xipmeApp = XipmeApp.getInstance();
    if ( xipmeApp == null ) {
      DLog.warning( lctx , "Failed to initialized , found null xipme app" );
      return;
    }

    try {
      debug = TransactionApp.getInstance().isDebug();
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to read debug info , " + e );
    }

    loginUsername = globalEnv.getProperty( "xipme.lnkshorten.login_username" );
    if ( StringUtils.isBlank( loginUsername ) ) {
      DLog.warning( lctx , "Failed to initialized "
          + ", found blank login username" );
      return;
    }
    loginPassword = globalEnv.getProperty( "xipme.lnkshorten.login_password" );
    if ( StringUtils.isBlank( loginPassword ) ) {
      DLog.warning( lctx , "Failed to initialized "
          + ", found blank login password" );
      return;
    }
    remoteUrl = globalEnv.getProperty( "xipme.lnkshorten.remote_url" );
    if ( StringUtils.isBlank( remoteUrl ) ) {
      DLog.warning( lctx , "Failed to initialized "
          + ", found blank remote url" );
      return;
    }

    initialized = true;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean setCloneMaps( String headerLog , ArrayList cloneMapsParams ,
      ArrayList listMsgLinks , String mobileNumber ,
      String gatewayLogToXipMeId , MobileUserBean muBean ,
      ChannelLogBean clBean , ClientSubscriberBean csBean ,
      ClientSubscriberCustomBean cscBean , EventBean evBean ,
      Map mapXipmeCloneParams ) {
    boolean set = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // must be params
    if ( cloneMapsParams == null ) {
      DLog.warning( lctx , headerLog + "Failed to set clone maps "
          + ", found null clone map params" );
      return set;
    }
    if ( ( listMsgLinks == null ) || ( listMsgLinks.size() < 1 ) ) {
      DLog.warning( lctx , headerLog + "Failed to set clone maps "
          + ", found empty list message links" );
      return set;
    }

    // generate list actions per message ( default )
    ArrayList listActionsPerMessage = createListActionsPerMessage(
        mobileNumber , gatewayLogToXipMeId , muBean , clBean , csBean ,
        cscBean , evBean );
    if ( listActionsPerMessage == null ) {
      DLog.warning( lctx , headerLog + "Failed to set clone maps "
          + ", found failed to create list actions per message" );
      return set;
    }

    // resolve reserved keyword in map xipme clone params
    mapXipmeCloneParams = XipmeCloneParamSupport.resolveMapXipmeCloneParams(
        headerLog , muBean , clBean , csBean , cscBean , mapXipmeCloneParams );

    // iterate and process all the message links
    List listXipmeCodes = new ArrayList();
    Iterator iterMsgLinks = listMsgLinks.iterator();
    while ( iterMsgLinks.hasNext() ) {
      String msgLink = (String) iterMsgLinks.next();
      if ( StringUtils.isBlank( msgLink ) ) {
        continue;
      }

      // generate xipme code from message link
      String xipmeCode = XipmeUtils.getCode( msgLink );
      if ( StringUtils.isBlank( xipmeCode ) ) {
        continue;
      }

      // generate xipme clone param if found any
      XipmeCloneParam xipmeCloneParam = null;
      if ( mapXipmeCloneParams != null ) {
        xipmeCloneParam = (XipmeCloneParam) mapXipmeCloneParams.get( xipmeCode );
      }

      // generate list actions per message link
      ArrayList listActionsPerLink = new ArrayList( listActionsPerMessage );
      listActionsPerLink.addAll( createListActionsPerLink( xipmeCloneParam ) );

      // add as new clone map param
      XipmeSupport.addCloneMapParam( cloneMapsParams , xipmeCode ,
          listActionsPerLink );

      // add into list xipme codes for debug purpose
      listXipmeCodes.add( xipmeCode );

    } // while ( iterMsgLinks.hasNext() )

    DLog.debug( lctx , headerLog + "Prepared list : cloneMapsParams.size = "
        + cloneMapsParams.size() + ( debug ? ( " : " + cloneMapsParams ) : "" )
        + " , xipmeCodes.size = " + listXipmeCodes.size()
        + ( debug ? ( " : " + listXipmeCodes ) : "" ) );

    set = true;
    return set;
  }

  public Map executeCloneMaps( String headerLog , ArrayList cloneMapsParams ,
      boolean tagChannelLogId ) {
    Map mapCodes = null;
    headerLog = ( headerLog == null ) ? "" : headerLog;
    if ( !initialized ) {
      DLog.warning( lctx , headerLog + "Failed to execute clone maps "
          + ", found not yet initialized" );
      return mapCodes;
    }
    if ( cloneMapsParams == null ) {
      DLog.warning( lctx , headerLog + "Failed to execute clone maps "
          + " , found null clone maps params" );
      return mapCodes;
    }
    if ( debug ) {
      DLog.debug( lctx , headerLog + "Execute clone maps with "
          + ": remoteUrl = " + remoteUrl + " , loginUsername = "
          + loginUsername + " , tagChannelLogId = " + tagChannelLogId );
    }
    mapCodes = XipmeSupport.executeCloneMaps( headerLog , remoteUrl ,
        loginUsername , loginPassword , cloneMapsParams , tagChannelLogId );
    if ( mapCodes != null ) {
      // trap into load management
      loadHit( mapCodes.size() );
    }
    return mapCodes;
  }

  public String resolveMessageContent( String headerLog , String msgInput ,
      Map mapInnerCodes ) {
    String msgResult = null;
    headerLog = ( headerLog == null ) ? "" : headerLog;
    if ( !initialized ) {
      DLog.warning( lctx , headerLog + "Failed to resolve message content "
          + ", found not yet initialized" );
      msgResult = msgInput;
      return msgResult;
    }
    List listInnerCodes = new ArrayList();
    msgResult = XipmeSupport.updateMessageLinks( headerLog , msgInput ,
        mapInnerCodes , listInnerCodes );
    if ( msgResult == null ) {
      DLog.warning( lctx , headerLog + "Failed to update any links inside "
          + "the message content , stored back with original message" );
      msgResult = msgInput;
    } else {
      Iterator iterInnerCodes = listInnerCodes.iterator();
      while ( iterInnerCodes.hasNext() ) {
        XipmeCloneResult xipmeCloneResult = (XipmeCloneResult) iterInnerCodes
            .next();
        if ( xipmeCloneResult == null ) {
          continue;
        }
        GatewayXipmeBean gatewayXipmeBean = XipmeSupport
            .xipmeCloneResultToGatewayXipmeBean( xipmeCloneResult );
        if ( gatewayXipmeBean == null ) {
          continue;
        }
        boolean processed = xipmeApp.processGatewayXipme( gatewayXipmeBean );
        DLog.debug( lctx , headerLog
            + "Processed gateway xipme bean : gatewayXipmeId = "
            + gatewayXipmeBean.getGatewayXipmeId() + " , xipmeMasterCode = "
            + gatewayXipmeBean.getXipmeMasterCode() + " , xipmeCode = "
            + gatewayXipmeBean.getXipmeCode() + " , xipmeCodeEncrypted = "
            + gatewayXipmeBean.getXipmeCodeEncrypted() + " , result = "
            + processed );
      }
    }
    return msgResult;
  }

  public String resolveMessageContent( String headerLog , String msgInput ,
      String mobileNumber , String gatewayLogToXipMeId , MobileUserBean muBean ,
      ChannelLogBean clBean , ClientSubscriberBean csBean ,
      ClientSubscriberCustomBean cscBean , EventBean evBean ,
      Map mapXipmeCloneParams ) {
    String msgResult = null;

    headerLog = ( headerLog == null ) ? "" : headerLog;
    if ( !initialized ) {
      DLog.warning( lctx , headerLog + "Failed to resolve message content "
          + ", found not yet initialized" );
      msgResult = msgInput;
      return msgResult;
    }

    // find links inside the message input content
    ArrayList listMsgLinks = XipmeSupport.extractMessageLinks( msgInput );
    if ( ( listMsgLinks == null ) || ( listMsgLinks.size() < 1 ) ) {
      DLog.warning( lctx , headerLog + "Failed to resolve message content "
          + ", found empty list message links" );
      msgResult = msgInput;
      return msgResult;
    }
    DLog.debug( lctx , headerLog + "Extracted from message content "
        + ", total " + listMsgLinks.size() + " link(s) : " + listMsgLinks );

    // clean mobile number
    mobileNumber = XipmeUtils.cleanMobileNumber( mobileNumber );

    // set clone maps
    ArrayList cloneMapsParams = new ArrayList();
    if ( !setCloneMaps( headerLog , cloneMapsParams , listMsgLinks ,
        mobileNumber , gatewayLogToXipMeId , muBean , clBean , csBean ,
        cscBean , evBean , mapXipmeCloneParams ) ) {
      DLog.warning( lctx , headerLog + "Failed to resolve message content "
          + ", found failed to set clone maps , stored back with original" );
      msgResult = msgInput;
      return msgResult;
    }

    // clone message links
    Map mapCodes = null;
    try {
      long deltaTime = System.currentTimeMillis();
      mapCodes = executeCloneMaps( headerLog , cloneMapsParams , false );
      deltaTime = System.currentTimeMillis() - deltaTime;
      if ( mapCodes == null ) {
        DLog.warning( lctx , headerLog + "Failed to resolve message content "
            + ", found failed to execute clone maps "
            + ", stored back with original ( " + deltaTime + " ms ) " );
        msgResult = msgInput;
        return msgResult;
      }
      DLog.debug( lctx , headerLog + "Executed clone maps ( " + deltaTime
          + " ms ) , result : total = " + mapCodes.size() + " record(s)" );
    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to execute clone maps , " + e );
    }

    // read the inner codes
    List listCodes = new ArrayList();
    if ( mapCodes != null ) {
      listCodes.addAll( mapCodes.values() );
    }
    Map mapInnerCodes = null;
    Iterator iterCodes = listCodes.iterator();
    if ( iterCodes.hasNext() ) {
      mapInnerCodes = (Map) iterCodes.next();
    }
    if ( mapInnerCodes == null ) {
      DLog.warning( lctx , headerLog + "Failed to resolve message content "
          + ", found empty map inner codes inside map codes "
          + ", stored back with original message" );
      msgResult = msgInput;
      return msgResult;
    }
    DLog.debug( lctx , headerLog + "Resolved clone maps result : "
        + mapInnerCodes );

    // store the result code back into the message
    msgResult = resolveMessageContent( headerLog , msgInput , mapInnerCodes );
    if ( msgResult == null ) {
      DLog.warning( lctx , headerLog + "Failed to update any links inside "
          + "the message content , stored back with original message" );
      msgResult = msgInput;
    }
    return msgResult;
  }

  public static int loadHit() {
    return loadMng.getLoad( LoadManagementApi.HDRPROF_XIPME ,
        LoadManagementApi.CONTYPE_SMSMT , "PROCESSOR" , false , false );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private void loadHit( int count ) {
    loadMng.hit( LoadManagementApi.HDRPROF_XIPME ,
        LoadManagementApi.CONTYPE_SMSMT , "PROCESSOR" , count , true , false );
  }

  private ArrayList createListActionsPerLink( XipmeCloneParam xipmeCloneParam ) {
    ArrayList listActions = new ArrayList();

    if ( xipmeCloneParam != null ) {
      String targetLink = xipmeCloneParam.getTargetLink();
      if ( ( targetLink != null ) && ( !targetLink.equals( "" ) ) ) {
        listActions.add( ActionFactory.createActionUpdate( Action.TBLNM_MAP ,
            Action.FLDNM_RESOLVEDURL , targetLink ) );
      }
    }

    return listActions;
  }

  private ArrayList createListActionsPerMessage( String mobileNumber ,
      String gatewayLogToXipMeId , MobileUserBean muBean ,
      ChannelLogBean clBean , ClientSubscriberBean csBean ,
      ClientSubscriberCustomBean cscBean , EventBean evBean ) {
    ArrayList listActions = new ArrayList();

    listActions.add( ActionFactory.createActionUpdate( Action.TBLNM_MAP ,
        Action.FLDNM_CHANNEL , Channel.SMS ) );
    if ( mobileNumber != null ) {
      listActions.add( ActionFactory.createActionUpdate( Action.TBLNM_MAP ,
          Action.FLDNM_CUSTOMERID , mobileNumber ) );
    }

    if ( gatewayLogToXipMeId != null ) {
      listActions.add( ActionFactory.createActionNew( Action.TBLNM_MAP_PARAM ,
          TransactionXipmeMapParam.GATEWAY_LOG_TO_XIP_ME_ID ,
          gatewayLogToXipMeId ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , Action.FLDNM_MSGPRM_DISPLAY ,
          TransactionXipmeMapParam.GATEWAY_LOG_TO_XIP_ME_ID ,
          MapParam.DISPLAY_API ) );
    }

    if ( muBean != null ) {
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.USER_FIRST_NAME ,
          muBean.getName() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.USER_FAMILY_NAME ,
          muBean.getLastName() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.USER_EMAIL ,
          muBean.getEmail() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.USER_ID ,
          muBean.getIc() ) );
    }

    if ( csBean != null ) {
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_PHONENUMBER ,
          csBean.getPhone() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM ,
          SubscriberXipmeMapParam.LIST_SUBSCRIBEDSTATUS ,
          csBean.getSubscribed() > 0 ? "true" : "false" ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTREFID ,
          csBean.getCustomerReferenceId() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTREFCODE ,
          csBean.getCustomerReferenceCode() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_DESCRIPTION ,
          csBean.getDescription() ) );
    }

    if ( cscBean != null ) {
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM0 ,
          cscBean.getCustom0() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM1 ,
          cscBean.getCustom1() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM2 ,
          cscBean.getCustom2() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM3 ,
          cscBean.getCustom3() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM4 ,
          cscBean.getCustom4() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM5 ,
          cscBean.getCustom5() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM6 ,
          cscBean.getCustom6() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM7 ,
          cscBean.getCustom7() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM8 ,
          cscBean.getCustom8() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM9 ,
          cscBean.getCustom9() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM10 ,
          cscBean.getCustom10() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM11 ,
          cscBean.getCustom11() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM12 ,
          cscBean.getCustom12() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM13 ,
          cscBean.getCustom13() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM14 ,
          cscBean.getCustom14() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM15 ,
          cscBean.getCustom15() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM16 ,
          cscBean.getCustom16() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM17 ,
          cscBean.getCustom17() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM18 ,
          cscBean.getCustom18() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM19 ,
          cscBean.getCustom19() ) );
      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , SubscriberXipmeMapParam.LIST_CUSTOM20 ,
          cscBean.getCustom20() ) );
    }

    if ( evBean != null ) {

      listActions.add( ActionFactory.createActionUpdateIfBlank(
          Action.TBLNM_MAP_TO_DTM , Action.FLDNM_SENDERID ,
          evBean.getOutgoingNumber() ) );
      listActions.add( ActionFactory.createActionUpdateIfBlank(
          Action.TBLNM_MAP_TO_DTM , Action.FLDNM_EVENTID ,
          Long.toString( evBean.getEventID() ) ) );

      listActions.add( ActionFactory.createActionUpdate(
          Action.TBLNM_MAP_PARAM , Action.FLDNM_MSGPRM_VALUE ,
          TransactionXipmeMapParam.EVENT_ID ,
          Long.toString( evBean.getEventID() ) ) );

    }

    String tags = new String( XIPME_TAGS );
    if ( clBean != null ) {
      tags = tags.concat( "," );
      tags = tags.concat( Integer.toString( clBean.getId() ) );
    } else {
      tags = tags.concat( "," );
      tags = tags.concat( "0" );
    }
    if ( tags != null ) {
      listActions.add( ActionFactory.createActionUpdate( Action.TBLNM_MAP ,
          Action.FLDNM_TAGS , tags ) );
    }

    return listActions;
  }

}
