package com.beepcast.model.transaction;

import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionMessageParam {

  static final DLogContext lctx = new SimpleContext( "TransactionMessageParam" );

  // inside transaction

  public static final String HDR_PROVIDER_BEAN = "providerBean";
  public static final String HDR_COUNTRY_BEAN = "countryBean";
  public static final String HDR_MOBILE_USER_BEAN = "mobileUserBean";
  public static final String HDR_CLIENT_BEAN = "clientBean";
  public static final String HDR_EVENT_BEAN = "eventBean";
  public static final String HDR_EXPECT_CLIENT_BEAN = "expectClientBean";
  public static final String HDR_EXPECT_EVENT_BEAN = "expectEventBean";
  public static final String HDR_MODEM_NUMBER_TO_CLIENT_BEAN = "modemNumberToClientBean";

  // inside support

  public static final String HDR_ORI_MESSAGE_CONTENT = "oriMessageContent";
  public static final String HDR_ORI_PROVIDER = "oriProvider";
  public static final String HDR_CHANNEL_LOG_BEAN = "channelLogBean";
  public static final String HDR_CHANNEL_SESSION_BEAN = "channelSessionBean";
  public static final String HDR_SUBSCRIBER_GROUP_BEAN = "subscriberGroupBean";
  public static final String HDR_CLIENT_SUBSCRIBER_BEAN = "clientSubscriberBean";
  public static final String HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN = "clientSubscriberCustomBean";
  public static final String HDR_XIPME_CODES_MAP = "xipmeCodesMap";
  public static final String HDR_GATEWAY_XIPME_ID = "gatewayXipmeId";
  public static final String HDR_XIPME_CLONE_PARAMS_MAP = "xipmeCloneParamsMap";

  // output message

  public static final String HDR_HAS_EXPECT_INPUT = "hasExpectInput";
  public static final String HDR_HAS_EVENT_MENU_TYPE = "hasEventMenuType";
  public static final String HDR_BYPASS_MT_DEBIT = "bypassMtDebit";
  public static final String HDR_BYPASS_SEND_PROVIDER = "bypassSendProvider";
  public static final String HDR_BYPASS_GATEWAY_LOG = "bypassGatewayLog";
  public static final String HDR_BYPASS_SUSPENDED_EVENT = "bypassSuspendedEvent";
  public static final String HDR_SET_PROVIDER = "setProvider";
  public static final String HDR_SET_ORIMASKADDR = "setOriMaskAddr";
  public static final String HDR_SET_SENDDATESTR = "setSendDateStr";
  public static final String HDR_LIST_PROHIBIT_PROVIDER_IDS = "listProhibitProviderIds";
  public static final String HDR_LIST_CANDIDATE_PROVIDER_IDS = "listCandidateProviderIds";

  // qr image

  public static final String HDR_QR_IMAGE_FILE_NAME = "qrImageFileName";
  public static final String HDR_QR_IMAGE_FILE_SIZE = "qrImageFileSize";

  // prefix to override reserved variable

  public static final String HDR_PREFIX_SET_RESERVED_VARIABLE = "setReservedVariable.";

  // webhook

  public static final String HDR_WEBHOOK_METHOD = "webhookMethod";
  public static final String HDR_WEBHOOK_URI = "webhookUri";

}
