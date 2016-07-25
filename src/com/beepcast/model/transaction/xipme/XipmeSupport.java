package com.beepcast.model.transaction.xipme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.event.EventOutboundXipmeReservedVariables;
import com.beepcast.model.gatewayXipme.GatewayXipmeBean;
import com.beepcast.model.gatewayXipme.GatewayXipmeBeanFactory;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;
import com.xipme.api.MapParam;
import com.xipme.api.StatusCode;
import com.xipme.api.commands.CommandResponseCloneMap;
import com.xipme.api.commons.CloneMapsCommon;
import com.xipme.api.commons.CloneMapsParam;

public class XipmeSupport {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  private static final DLogContext lctx = new SimpleContext( "XipmeSupport" );

  private static OnlinePropertiesApp opropsApp = OnlinePropertiesApp
      .getInstance();

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static ArrayList extractMessageCodes( String messageContent ) {
    ArrayList listMsgCodes = new ArrayList();

    ArrayList listMsgLinks = extractMessageLinks( messageContent );
    if ( ( listMsgLinks == null ) || ( listMsgLinks.size() < 1 ) ) {
      return listMsgCodes;
    }

    Iterator iterMsgLinks = listMsgLinks.iterator();
    while ( iterMsgLinks.hasNext() ) {
      String msgLink = (String) iterMsgLinks.next();
      if ( StringUtils.isBlank( msgLink ) ) {
        continue;
      }
      String msgCode = XipmeUtils.getCode( msgLink );
      if ( StringUtils.isBlank( msgCode ) ) {
        continue;
      }
      listMsgCodes.add( msgCode );
    }

    return listMsgCodes;
  }

  public static ArrayList extractMessageLinks( String messageContent ) {
    ArrayList listMsgLinks = new ArrayList();
    if ( StringUtils.isBlank( messageContent ) ) {
      DLog.warning( lctx , "Failed to extract message links "
          + ", found blank msg input" );
      return listMsgLinks;
    }
    try {
      String found = null;
      Pattern pattern = Pattern.compile( opropsApp.getString(
          "Transaction.ShortenerLink" , null ) );
      Matcher matcher = pattern.matcher( messageContent );
      while ( matcher.find() ) {
        found = matcher.group( matcher.groupCount() );
        if ( ( found == null ) || ( found.equals( "" ) ) ) {
          continue;
        }
        if ( listMsgLinks.indexOf( found ) < 0 ) {
          listMsgLinks.add( found );
        }
      }
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to extract message links , " + e );
    }
    return listMsgLinks;
  }

  public static String extractMapXipmeCloneParams( String messageContent ,
      Map mapXipmeCloneParams ) {
    String messageContentNew = messageContent;
    if ( StringUtils.isBlank( messageContent ) ) {
      DLog.warning( lctx , "Failed to extract map xipme clone params "
          + ", found blank msg input" );
      return messageContentNew;
    }
    if ( mapXipmeCloneParams == null ) {
      DLog.warning( lctx , "Failed to extract map xipme clone params "
          + ", found blank msg input" );
      return messageContentNew;
    }

    // create fake transaction input message
    TransactionInputMessage imsg = TransactionMessageFactory
        .createInputMessage( null , 0 , null , null , null , null , null , 0 ,
            0 );

    // process to generate map xipme clone params
    messageContentNew = EventOutboundXipmeReservedVariables
        .replaceXipmeReservedVariables( new TransactionLog() , messageContent ,
            imsg );

    // read map xipme clone params from input transation message
    Map mapXipmeCloneParamsNew = (Map) imsg
        .getMessageParam( TransactionMessageParam.HDR_XIPME_CLONE_PARAMS_MAP );

    // store new map xipme clone params into the result
    if ( mapXipmeCloneParamsNew != null ) {
      mapXipmeCloneParams.putAll( mapXipmeCloneParamsNew );
    }

    return messageContentNew;
  }

  public static String readGatewayXipmeId( Map mapInnerCodes ) {
    String gatewayXipmeId = null;
    if ( mapInnerCodes == null ) {
      DLog.warning( lctx , "Failed to read gateway xipme id "
          + ", found nul map inner codes" );
      return gatewayXipmeId;
    }

    // convert map inner codes into list inner codes
    List listInnerCodes = new ArrayList( mapInnerCodes.values() );

    // find first xipmeCloneResult record
    XipmeCloneResult xipmeCloneResult = null;
    Iterator iterInnerCodes = listInnerCodes.iterator();
    if ( iterInnerCodes.hasNext() ) {
      xipmeCloneResult = (XipmeCloneResult) iterInnerCodes.next();
    }
    if ( xipmeCloneResult == null ) {
      DLog.warning( lctx , "Failed to read gateway xipme id "
          + ", found empty map inner codes" );
      return gatewayXipmeId;
    }

    // read gatewayXipmeId from xipmeCloneResult
    gatewayXipmeId = xipmeCloneResult.getGatewayXipmeId();

    return gatewayXipmeId;
  }

  public static String updateMessageLinks( String headerLog ,
      String messageContent , Map mapInnerCodes , List listInnerCodesResult ) {

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must params
    if ( messageContent == null ) {
      DLog.warning( lctx , headerLog + "Failed to update message links "
          + ", found null message content" );
      return messageContent;
    }
    if ( ( mapInnerCodes == null ) || ( mapInnerCodes.size() < 1 ) ) {
      DLog.warning( lctx , headerLog + "Failed to update message links "
          + ", found empty map inner codes" );
      return messageContent;
    }
    if ( listInnerCodesResult == null ) {
      DLog.warning( lctx , headerLog + "Failed to update message links "
          + ", found null list inner codes result" );
      return messageContent;
    }

    // convert map inner codes into list inner codes
    List listInnerCodes = new ArrayList( mapInnerCodes.values() );

    // iterate xipmeCloneResult and process it
    XipmeCloneResult xipmeCloneResult = null;
    Iterator iterInnerCodes = listInnerCodes.iterator();
    while ( iterInnerCodes.hasNext() ) {
      xipmeCloneResult = (XipmeCloneResult) iterInnerCodes.next();
      if ( xipmeCloneResult == null ) {
        continue;
      }
      String masterMapId = xipmeCloneResult.getMasterMapId();
      if ( ( masterMapId == null ) || ( masterMapId.equals( "" ) ) ) {
        continue;
      }
      if ( messageContent.indexOf( masterMapId ) < 0 ) {
        continue;
      }
      String resolvedCode = xipmeCloneResult.getMapIdEncrypted();
      if ( ( resolvedCode == null ) || ( resolvedCode.equals( "" ) ) ) {
        continue;
      }
      messageContent = StringUtils.replace( messageContent , masterMapId ,
          resolvedCode );
      listInnerCodesResult.add( xipmeCloneResult );
      DLog.debug( lctx , headerLog + "Resolved message link code : "
          + masterMapId + " -> " + resolvedCode );
    }

    return messageContent;
  }

  public static boolean addCloneMapParam( ArrayList cloneMapsParams ,
      String xipmeCode , ArrayList listActions ) {
    boolean result = false;
    if ( cloneMapsParams == null ) {
      return result;
    }
    if ( StringUtils.isBlank( xipmeCode ) ) {
      return result;
    }
    CloneMapsParam cloneMapsParam = new CloneMapsParam( xipmeCode , listActions );
    cloneMapsParams.add( cloneMapsParam );
    result = true;
    return result;
  }

  public static Map executeCloneMaps( String headerLog , String remoteUrl ,
      String loginUsername , String loginPassword , ArrayList cloneMapsParams ,
      boolean tagChannelLogId ) {
    Map mapResult = null;

    // trap delta time
    long deltaTime = System.currentTimeMillis();

    // header log
    headerLog = ( headerLog == null ) ? "" : headerLog;

    // prepare response
    ArrayList listCmdResps = null;

    // create request
    try {
      listCmdResps = CloneMapsCommon.processRequest( remoteUrl , loginUsername ,
          loginPassword , cloneMapsParams );
    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to execute clone maps "
          + ", found failed to process clone maps common request , " + e );
    }

    // validate response
    if ( listCmdResps == null ) {
      DLog.warning( lctx , headerLog + "Failed to execute clone maps "
          + ", found null listCmdResps" );
      return mapResult;
    }

    // read response and store into map codes
    try {
      mapResult = convertIterCmdRespsToMapCodes( tagChannelLogId ,
          listCmdResps.iterator() );
    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to execute clone maps "
          + ", found failed to convert iter command responses to map codes , "
          + e );
      return mapResult;
    }

    // trap delta time
    deltaTime = System.currentTimeMillis() - deltaTime;

    // log
    DLog.debug( lctx , headerLog + "Executed clone maps ( take " + deltaTime
        + " ms ) : listCmdResps.size = " + listCmdResps.size()
        + " , mapResult.size = " + mapResult.size() );

    return mapResult;
  }

  public static Map convertIterCmdRespsToMapCodes( boolean tagChannelLogId ,
      Iterator iterCmdResps ) {
    Map mapCodes = null;

    if ( iterCmdResps == null ) {
      return mapCodes;
    }

    long deltaTime = System.currentTimeMillis();

    mapCodes = new HashMap();
    int totalCmdResp = 0 , totalCmdRespOk = 0 , totalMapInnerCodesKey = 0 , totalMapInnerCodes = 0;
    int totalXipmeCloneResult = 0 , totalDuplicatedChannelLogId = 0 , totalDuplicatedInnerCodes = 0;

    CommandResponseCloneMap cmdResp = null;
    while ( iterCmdResps.hasNext() ) {
      try {

        cmdResp = (CommandResponseCloneMap) iterCmdResps.next();
        if ( cmdResp == null ) {
          continue;
        }
        totalCmdResp = totalCmdResp + 1;
        // System.out.println( cmdResp );

        if ( !StringUtils.equals( cmdResp.getStatusCode() , StatusCode.OK ) ) {
          continue;
        }
        totalCmdRespOk = totalCmdRespOk + 1;

        // build map codes key based on channel log id
        // else will be set as default zero value
        String mapCodesKey = "0";
        if ( tagChannelLogId ) {
          int channelLogId = getTagChannelLogId( cmdResp.getTags() );
          if ( channelLogId < 0 ) {
            continue;
          }
          mapCodesKey = Integer.toString( channelLogId );
        }
        if ( mapCodesKey == null ) {
          continue;
        }
        totalMapInnerCodesKey = totalMapInnerCodesKey + 1;

        // build a map inner codes if found empty
        // and link it to the map codes
        Map mapInnerCodes = (Map) mapCodes.get( mapCodesKey );
        if ( mapInnerCodes == null ) {
          mapInnerCodes = new HashMap();
          mapCodes.put( mapCodesKey , mapInnerCodes );
        } else {
          totalDuplicatedChannelLogId = totalDuplicatedChannelLogId + 1;
        }

        // convert cmdResp to xipmeCloneResult
        XipmeCloneResult xipmeCloneResult = commandResponseCloneMapToXipmeCloneResult( cmdResp );
        if ( xipmeCloneResult == null ) {
          continue;
        }
        totalXipmeCloneResult = totalXipmeCloneResult + 1;

        // store xipmeCloneResult into the inner map codes with key of
        // masterMapId
        if ( mapInnerCodes.get( xipmeCloneResult.getMasterMapId() ) != null ) {
          totalDuplicatedInnerCodes = totalDuplicatedInnerCodes + 1;
          continue;
        }
        mapInnerCodes
            .put( xipmeCloneResult.getMasterMapId() , xipmeCloneResult );
        totalMapInnerCodes = totalMapInnerCodes + 1;

      } catch ( Exception e ) {
        DLog.warning( lctx , "Failed to process command response , " + e );
      }
    } // while ( iterCmdResps.hasNext() ) {

    int totalMapCodes = mapCodes.size();

    deltaTime = System.currentTimeMillis() - deltaTime;

    DLog.debug( lctx , "Converted iterCmdResps to mapResult ( take "
        + deltaTime + " ms ) : totalCmdResp = " + totalCmdResp
        + " , totalCmdRespOk = " + totalCmdRespOk
        + " , totalMapInnerCodesKey = " + totalMapInnerCodesKey
        + " , totalMapInnerCodes = " + totalMapInnerCodes
        + " , totalMapCodes = " + totalMapCodes + " , totalXipmeCloneResult = "
        + totalXipmeCloneResult + " , totalDuplicatedChannelLogId = "
        + totalDuplicatedChannelLogId + " , totalDuplicatedInnerCodes = "
        + totalDuplicatedInnerCodes );

    return mapCodes;
  }

  public static GatewayXipmeBean xipmeCloneResultToGatewayXipmeBean(
      XipmeCloneResult xipmeCloneResult ) {
    GatewayXipmeBean gatewayXipmeBean = null;
    if ( xipmeCloneResult == null ) {
      return gatewayXipmeBean;
    }
    gatewayXipmeBean = GatewayXipmeBeanFactory.createGatewayXipmeBean(
        xipmeCloneResult.getGatewayXipmeId() ,
        xipmeCloneResult.getMasterMapId() , xipmeCloneResult.getMapId() ,
        xipmeCloneResult.getMapIdEncrypted() );
    return gatewayXipmeBean;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private static XipmeCloneResult commandResponseCloneMapToXipmeCloneResult(
      CommandResponseCloneMap cmdResp ) {
    XipmeCloneResult xipmeCloneResult = null;
    if ( cmdResp == null ) {
      return xipmeCloneResult;
    }

    // read params from command response

    String masterMapId = cmdResp.getMasterMapId();
    String mapId = cmdResp.getMapId();
    String code = cmdResp.getCode();

    // validate params

    if ( masterMapId == null ) {
      return xipmeCloneResult;
    }
    if ( mapId == null ) {
      return xipmeCloneResult;
    }
    if ( code == null ) {
      return xipmeCloneResult;
    }

    // build xipme clone result based on params

    xipmeCloneResult = new XipmeCloneResult();
    xipmeCloneResult.setMasterMapId( masterMapId );
    xipmeCloneResult.setMapId( mapId );
    xipmeCloneResult.setMapIdEncrypted( code );

    // read the gatewayToXipmeId param

    Iterator iterMapParams = cmdResp.iterMapParams();
    if ( iterMapParams != null ) {
      while ( iterMapParams.hasNext() ) {
        MapParam mapParam = (MapParam) iterMapParams.next();
        if ( mapParam == null ) {
          continue;
        }
        String mapParamName = mapParam.getName();
        if ( mapParamName == null ) {
          continue;
        }
        String mapParamValue = mapParam.getValue();
        if ( mapParamValue == null ) {
          continue;
        }
        if ( mapParamName
            .equals( TransactionXipmeMapParam.GATEWAY_LOG_TO_XIP_ME_ID ) ) {
          xipmeCloneResult.setGatewayXipmeId( mapParamValue );
          continue;
        }
      }
    }

    return xipmeCloneResult;
  }

  private static int getTagChannelLogId( String tags ) {
    int clId = 0;
    try {
      if ( tags == null ) {
        return clId;
      }
      String[] arrTags = tags.split( "," );
      if ( arrTags == null ) {
        return clId;
      }
      if ( arrTags.length < 2 ) {
        return clId;
      }
      clId = Integer.parseInt( arrTags[1] );
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to get tag channel log id , " + e );
    }
    return clId;
  }

}
