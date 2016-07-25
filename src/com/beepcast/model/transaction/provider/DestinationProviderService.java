package com.beepcast.model.transaction.provider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.beepcast.dbmanager.common.GroupClientConnectionCommon;
import com.beepcast.dbmanager.common.ProviderCommon;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.dbmanager.table.TCountryToProvider;
import com.beepcast.dbmanager.table.TGroupClientConnection;
import com.beepcast.dbmanager.table.TOutgoingNumberToProvider;
import com.beepcast.dbmanager.table.TProvider;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.TelcoService;
import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionRoute;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class DestinationProviderService {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "DestinationProviderService" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private OnlinePropertiesApp opropsApp;
  private ProviderService providerService;
  private TransactionRoute route;

  private TransactionProcessBasic trans;
  private TransactionConf conf;
  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public DestinationProviderService( TransactionProcessBasic trans ) {

    opropsApp = OnlinePropertiesApp.getInstance();
    providerService = new ProviderService( trans.log() );
    route = new TransactionRoute( trans );

    this.trans = trans;
    this.conf = trans.conf();
    this.log = trans.log();

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean resolveDestinationProviderAndMask(
      TransactionInputMessage imsg , TransactionOutputMessage omsg ) {
    boolean result = false;

    // resolve provider and mask based on message type

    switch ( omsg.getMessageType() ) {
    case MessageType.TEXT_TYPE :
    case MessageType.RINGTONE_TYPE :
    case MessageType.PICTURE_TYPE :
    case MessageType.UNICODE_TYPE :
      log.debug( "Resolve destination provider and mask for message type sms" );
      result = resolveDestinationProviderAndMaskForMessageTypeSms( imsg , omsg );
      break;
    case MessageType.MMS_TYPE :
      log.debug( "Resolve destination provider and mask for message type mms" );
      result = resolveDestinationProviderAndMaskForMessageTypeMms( imsg , omsg );
      break;
    case MessageType.QRPNG_TYPE :
    case MessageType.QRGIF_TYPE :
    case MessageType.QRJPG_TYPE :
      log.debug( "Resolve destination provider and mask for message type qr" );
      result = resolveDestinationProviderAndMaskForMessageTypeQr( imsg , omsg );
      break;
    case MessageType.WEBHOOK_TYPE :
      log.debug( "Resolve destination provider and mask for message type webhook" );
      result = resolveDestinationProviderAndMaskForMessageTypeWebhook( imsg ,
          omsg );
      break;
    }

    // persist message param provider bean
    if ( omsg.getDestinationProvider() != null ) {
      TProvider providerBean = ProviderCommon.getProvider( omsg
          .getDestinationProvider() );
      if ( providerBean != null ) {
        omsg.addMessageParam( TransactionMessageParam.HDR_PROVIDER_BEAN ,
            providerBean );
        log.debug( "Output msg params of provider bean : id = "
            + providerBean.getProviderId() + " , name = "
            + providerBean.getProviderName() );
      }
    }

    return result;
  }

  public boolean resolveDestinationProviderAndMaskForMessageTypeSms(
      TransactionInputMessage imsg , TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      // header log

      String headerLog = null;

      // first , try to resolve destination provider from message param

      headerLog = "[#1 Attempt] ";
      log.debug( headerLog + "Resolving destination provider from "
          + "outbound message param" );
      if ( resolveDestinationProviderAndMaskFromMessageParam( headerLog , omsg ,
          true ) ) {
        result = true;
        return result;
      }
      log.debug( headerLog + "Failed to resolve destination provider from "
          + "outbound message param" );

      // second , try to resolve destination provider from the country map

      headerLog = "[#2 Attempt] ";
      log.debug( headerLog + "Resolving destination provider from "
          + "country to provider map" );
      boolean updateDestinationProvider = resolveDestinationProviderAndMaskFromCountryToProviderMap(
          headerLog , omsg , true );
      log.debug( headerLog + "Resolved destination provider : updated = "
          + updateDestinationProvider );

      // third , try to resolve destination provider from the outgoing number
      // map

      headerLog = "[#3 Attempt] ";
      log.debug( headerLog + "Resolving destination provider from "
          + "number to provider map" );
      if ( resolveDestinationProviderAndMaskFromNumberToProviderMap( headerLog ,
          omsg , !updateDestinationProvider ) ) {
        result = true;
        return result;
      }
      log.debug( headerLog + "Failed to resolve destination provider from "
          + "number to provider map" );

      // forth , try to resolve destination provider from inbound provider

      headerLog = "[#4 Attempt] ";
      log.debug( headerLog + "Resolving destination provider from "
          + "inbound provider" );
      if ( imsg != null ) {
        if ( resolveDestinationProviderFromInboundProvider( headerLog , imsg ,
            omsg , true ) ) {
          result = true;
          return result;
        }
      }
      log.debug( headerLog + "Failed to resolve destination provider from "
          + "inbound provider" );

    } catch ( Exception e ) {
      log.warning( "Failed to resolve destination provider , " + e );
    }
    return result;
  }

  public boolean resolveDestinationProviderAndMaskForMessageTypeMms(
      TransactionInputMessage imsg , TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      // not implemented yet
      log.warning( "Not implemented yet for this feature" );

    } catch ( Exception e ) {
      log.warning( "Failed to resolve destination provider , " + e );
    }
    return result;
  }

  public boolean resolveDestinationProviderAndMaskForMessageTypeQr(
      TransactionInputMessage imsg , TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      // for temporary , default qr provider : QR
      String qrProviderId = "QR";

      // update the destination provider
      log.debug( "Updated new outbound message's destination provider : "
          + omsg.getDestinationProvider() + " -> " + qrProviderId );
      omsg.setDestinationProvider( qrProviderId );

      // set result as true
      result = true;

    } catch ( Exception e ) {
      log.warning( "Failed to resolve destination provider , " + e );
    }
    return result;
  }

  public boolean resolveDestinationProviderAndMaskForMessageTypeWebhook(
      TransactionInputMessage imsg , TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      // for temporary , default webhook provider : WB
      String webhookProviderId = "WB";

      // update the destination provider
      log.debug( "Updated new outbound message's destination provider : "
          + omsg.getDestinationProvider() + " -> " + webhookProviderId );
      omsg.setDestinationProvider( webhookProviderId );

      // set result as true
      result = true;

    } catch ( Exception e ) {
      log.warning( "Failed to resolve destination provider , " + e );
    }
    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean resolveDestinationProviderFromInboundProvider(
      String headerLog , TransactionInputMessage imsg ,
      TransactionOutputMessage omsg , boolean updateDestinationProvider ) {
    boolean result = false;

    if ( imsg == null ) {
      return result;
    }
    if ( omsg == null ) {
      return result;
    }

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // read and validate inbound provider
    String inboundProvider = imsg.getOriginalProvider();
    if ( StringUtils.isBlank( inboundProvider ) ) {
      log.warning( headerLog + "Found blank imsg original provider" );
    }

    // read list active providers
    List listActiveProviderIds = providerService.listActiveProviders();
    if ( ( listActiveProviderIds == null )
        || ( listActiveProviderIds.size() < 1 ) ) {
      log.warning( headerLog + "Failed to resolve destination provider "
          + "from inbound provider , found empty list active provider ids" );
      return result;
    }

    // read next provider id from route engine
    String nextProviderId = route.resolveNextProviderId( inboundProvider ,
        listActiveProviderIds );
    if ( StringUtils.isBlank( nextProviderId ) ) {
      log.warning( headerLog + "Failed to resolve destination provider "
          + "from inbound provider , found failed to resolve "
          + "next provider id = " + nextProviderId );
      return result;
    }

    // validate is the next provider id is in active status
    if ( !providerService.isValidProviderId( nextProviderId ,
        listActiveProviderIds ) ) {
      log.warning( headerLog + "Failed to resolve destination provider "
          + "from inbound provider , found inactive provider id = "
          + nextProviderId );
      return result;
    }

    if ( updateDestinationProvider ) {
      // update the destination provider
      log.debug( headerLog
          + "Updated new outbound message's destination provider : "
          + omsg.getDestinationProvider() + " -> " + nextProviderId );
      omsg.setDestinationProvider( nextProviderId );
    }

    result = true;
    return result;
  }

  public boolean resolveDestinationProviderAndMaskFromNumberToProviderMap(
      String headerLog , TransactionOutputMessage omsg ,
      boolean updateDestinationProvider ) {
    boolean result = false;

    if ( omsg == null ) {
      return result;
    }

    ClientBean clientBean = (ClientBean) omsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN );
    if ( clientBean == null ) {
      log.warning( headerLog
          + "Failed to resolve destination provider from number "
          + "to provider map , found invalid client bean" );
      return result;
    }

    // read group connection id

    int gcId = clientBean.getGroupConnectionId();
    if ( gcId < 1 ) {
      log.warning( headerLog
          + "Failed to resolve destination provider from number "
          + "to provider map , found empty group connection id" );
      return result;
    }
    TGroupClientConnection gcBean = GroupClientConnectionCommon
        .getGroupClientConnection( gcId , true );
    if ( gcBean == null ) {
      log.warning( headerLog
          + "Failed to resolve destination provider from number "
          + "to provider map , found invalid group connection bean" );
      return result;
    }

    // read long number

    String longNumber = omsg.getOriginalAddress();

    // read country code ( optional )

    String countryCode = null;
    {
      TCountry countryBean = (TCountry) omsg
          .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );
      if ( countryBean != null ) {
        countryCode = countryBean.getCode();
      }
    }

    // read mobile user ( optional )

    MobileUserBean mobileUserBean = (MobileUserBean) omsg
        .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );

    // read prefix number ( optional )

    String prefixNumber = null;
    if ( mobileUserBean != null ) {
      String phone = mobileUserBean.getPhone();
      if ( phone != null ) {
        int phoneLength = phone.length();
        int prefixNumberLength = (int) opropsApp.getLong(
            "Transaction.RouteProvider.PrefixNumberLength" , 6 );
        if ( phoneLength > prefixNumberLength ) {
          prefixNumber = phone.substring( 0 , prefixNumberLength );
        }
      }
    }

    // read list telco codes ( optional )

    List listTelcoCodes = TelcoService.resolveListTelcoCodes( mobileUserBean );

    // log it

    log.debug( headerLog
        + "Resolving destination provider based on : groupConnId = "
        + gcBean.getId() + " , longNumber = " + longNumber
        + " , countryCode = " + countryCode + " , prefixNumber = "
        + prefixNumber + " , listTelcoCodes = " + listTelcoCodes );

    // read list prohibit provider ids
    List listProhibitProviderIds = (List) omsg
        .getMessageParam( TransactionMessageParam.HDR_LIST_PROHIBIT_PROVIDER_IDS );
    if ( listProhibitProviderIds != null ) {
      log.debug( headerLog + "Resolved list prohibit provider ids : "
          + listProhibitProviderIds );
    }

    // prepare for the list candidate providers for debug purpose
    List listCandidateProviderIds = new ArrayList();
    omsg.addMessageParam(
        TransactionMessageParam.HDR_LIST_CANDIDATE_PROVIDER_IDS ,
        listCandidateProviderIds );

    // get provider based on outgoing number value
    TOutgoingNumberToProvider ontp = providerService
        .getProviderFromLongNumberCountryCodeTelcoCodesGroupConnection(
            headerLog , longNumber , countryCode , prefixNumber ,
            listTelcoCodes , gcBean.getId() , listProhibitProviderIds ,
            listCandidateProviderIds );
    if ( ontp == null ) {
      log.warning( headerLog + "Failed to resolve destination provider "
          + "based on outgoing number to provider map" );
      return result;
    }
    log.debug( headerLog + "Resolved destination provider : providerId = "
        + ontp.getProviderId() + " , priority = " + ontp.getPriority()
        + " , masked = " + ontp.getMasked() + " , countryCode = "
        + ontp.getCountryCode() + " , description = " + ontp.getDescription() );

    if ( updateDestinationProvider ) {
      // update the destination provider
      log.debug( headerLog
          + "Updated new outbound message's destination provider : "
          + omsg.getDestinationProvider() + " -> " + ontp.getProviderId() );
      omsg.setDestinationProvider( ontp.getProviderId() );
    }

    // update the original mask address , if any
    String originalMaskAddress = omsg.getOriginalMaskingAddress();
    if ( StringUtils.isBlank( originalMaskAddress ) ) {
      originalMaskAddress = (String) omsg
          .getMessageParam( TransactionMessageParam.HDR_SET_ORIMASKADDR );
      if ( StringUtils.isBlank( originalMaskAddress ) ) {
        originalMaskAddress = ontp.getMasked();
      }
    }
    if ( !StringUtils.isBlank( originalMaskAddress ) ) {
      log.debug( headerLog
          + "Updated new outbound message's masking address : "
          + omsg.getOriginalMaskingAddress() + " -> " + originalMaskAddress );
      omsg.setOriginalMaskingAddress( originalMaskAddress );
    }

    result = true;
    return result;
  }

  public boolean resolveDestinationProviderAndMaskFromCountryToProviderMap(
      String headerLog , TransactionOutputMessage omsg ,
      boolean updateDestinationProvider ) {
    boolean result = false;

    if ( omsg == null ) {
      return result;
    }

    TCountry countryBean = (TCountry) omsg
        .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );
    if ( countryBean == null ) {
      log.debug( headerLog
          + "Failed to resolve provider from the country destination "
          + "number , found null country bean inside outbound message param" );
      return result;
    }
    log.debug( headerLog
        + "Trying to resolve provider based from country : id = "
        + countryBean.getId() + " , code = " + countryBean.getCode()
        + " , name = " + countryBean.getName() );

    TCountryToProvider countryToProviderBean = providerService
        .getProviderFromCountry( countryBean.getCode() );
    if ( countryToProviderBean == null ) {
      log.debug( headerLog + "Failed to resolve provider from "
          + "the country destination number , found empty map" );
      return result;
    }
    log.debug( headerLog
        + "Successfully resolved provider from country code named = "
        + countryToProviderBean.getProviderId() );

    if ( updateDestinationProvider ) {
      // update the destination provider
      log.debug( headerLog
          + "Updated new outbound message's destination provider : "
          + omsg.getDestinationProvider() + " -> "
          + countryToProviderBean.getProviderId() );
      omsg.setDestinationProvider( countryToProviderBean.getProviderId() );
    }

    // update the original mask address , if any
    String originalMaskAddress = omsg.getOriginalMaskingAddress();
    if ( StringUtils.isBlank( originalMaskAddress ) ) {
      originalMaskAddress = (String) omsg
          .getMessageParam( TransactionMessageParam.HDR_SET_ORIMASKADDR );
      if ( StringUtils.isBlank( originalMaskAddress ) ) {
        originalMaskAddress = countryToProviderBean.getMasked();
      }
    }
    if ( !StringUtils.isBlank( originalMaskAddress ) ) {
      log.debug( headerLog
          + "Updated new outbound message's masking address : "
          + omsg.getOriginalMaskingAddress() + " -> " + originalMaskAddress );
      omsg.setOriginalMaskingAddress( originalMaskAddress );
    }

    result = true;
    return result;
  }

  public boolean resolveDestinationProviderAndMaskFromMessageParam(
      String headerLog , TransactionOutputMessage omsg ,
      boolean updateDestinationProvider ) {
    boolean result = false;
    if ( omsg == null ) {
      return result;
    }

    String destinationProvider = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_SET_PROVIDER );
    if ( StringUtils.isBlank( destinationProvider ) ) {
      return result;
    }

    TProvider providerOut = ProviderCommon.getProvider( destinationProvider );
    if ( providerOut == null ) {
      log.warning( headerLog
          + "Failed to resolve destination provider from message param "
          + ", found unregister provider id : " + destinationProvider );
      return result;
    }
    if ( !providerService
        .isValidProviderId( providerOut.getProviderId() , null ) ) {
      log.warning( headerLog
          + "Failed to resolve destination provider from message param "
          + ", found inactive provider id : " + providerOut.getProviderId() );
      return result;
    }
    log.debug( headerLog
        + "Successfully resolved provider from message param : id = "
        + providerOut.getId() + "providerId = " + providerOut.getProviderId()
        + " , providerName = " + providerOut.getProviderName() );

    if ( updateDestinationProvider ) {
      // update the destination provider
      log.debug( headerLog
          + "Updated new outbound message's destination provider : "
          + omsg.getDestinationProvider() + " -> "
          + providerOut.getProviderId() );
      omsg.setDestinationProvider( providerOut.getProviderId() );
    }

    // update the original mask address , if any
    String originalMaskAddress = omsg.getOriginalMaskingAddress();
    if ( StringUtils.isBlank( originalMaskAddress ) ) {
      originalMaskAddress = (String) omsg
          .getMessageParam( TransactionMessageParam.HDR_SET_ORIMASKADDR );
      if ( StringUtils.isBlank( originalMaskAddress ) ) {
        originalMaskAddress = providerOut.getShortCode();
      }
    }
    if ( !StringUtils.isBlank( originalMaskAddress ) ) {
      log.debug( headerLog
          + "Updated new outbound message's masking address : "
          + omsg.getOriginalMaskingAddress() + " -> " + originalMaskAddress );
      omsg.setOriginalMaskingAddress( originalMaskAddress );
    }

    result = true;
    return result;
  }

}
