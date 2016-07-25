package com.beepcast.model.transaction;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.channel.ChannelApp;
import com.beepcast.channel.ChannelLogBean;
import com.beepcast.channel.ChannelSessionBean;
import com.beepcast.channel.ChannelSessionService;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.dbmanager.common.EventCommon;
import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.dbmanager.table.TEvent;
import com.beepcast.dbmanager.table.TProviderToEvent;
import com.beepcast.idgen.IdGenApp;
import com.beepcast.model.beepcode.BeepcodeBean;
import com.beepcast.model.beepcode.BeepcodeService;
import com.beepcast.model.beepid.BeepIDBean;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventOutboundReservedVariables;
import com.beepcast.model.event.EventProcessAutoSend;
import com.beepcast.model.event.EventProcessCreateQrImage;
import com.beepcast.model.event.EventProcessDelaySend;
import com.beepcast.model.event.EventProcessEmail;
import com.beepcast.model.event.EventProcessEmailClient;
import com.beepcast.model.event.EventProcessEmailTo;
import com.beepcast.model.event.EventProcessIfDate;
import com.beepcast.model.event.EventProcessLog;
import com.beepcast.model.event.EventProcessRandomCode;
import com.beepcast.model.event.EventProcessSendIf;
import com.beepcast.model.event.EventProcessSmsToEmail;
import com.beepcast.model.event.EventProcessSmsToSms;
import com.beepcast.model.event.EventProcessSubscribe;
import com.beepcast.model.event.EventProcessSubscribeList;
import com.beepcast.model.event.EventProcessWappush;
import com.beepcast.model.event.EventProcessWebhook;
import com.beepcast.model.event.EventService;
import com.beepcast.model.event.EventSupport;
import com.beepcast.model.event.EventTransQueueReservedVariables;
import com.beepcast.model.event.LuckDrawEvent;
import com.beepcast.model.event.PingCountEvent;
import com.beepcast.model.event.ProcessBean;
import com.beepcast.model.event.TellAFriendEvent;
import com.beepcast.model.exchange.ExchangeBean;
import com.beepcast.model.exchange.ExchangeDispatcher;
import com.beepcast.model.exchange.ExchangeSupport;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.MobileUserSupport;
import com.beepcast.model.mobileUser.TelcoService;
import com.beepcast.model.reservedCode.ReservedCodeBean;
import com.beepcast.model.reservedCode.ReservedCodeDispatcher;
import com.beepcast.model.specialMessage.SpecialMessageBean;
import com.beepcast.model.specialMessage.SpecialMessageService;
import com.beepcast.model.specialMessage.SpecialMessageType;
import com.beepcast.model.transaction.provider.DestinationProviderService;
import com.beepcast.model.transaction.provider.ProviderFeatureName;
import com.beepcast.model.transaction.provider.ProviderFeatureSupport;
import com.beepcast.model.transaction.xipme.XipmeSupport;
import com.beepcast.model.transaction.xipme.XipmeTranslator;
import com.beepcast.model.util.DateTimeFormat;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.beepcast.reminder.ReminderApp;
import com.beepcast.router.RouterApp;
import com.beepcast.smsenc.impl.WapPush;
import com.beepcast.subscriber.ClientSubscriberBean;
import com.beepcast.subscriber.ClientSubscriberCustomBean;
import com.beepcast.subscriber.SubscriberApp;
import com.beepcast.subscriber.SubscriberGroupBean;
import com.beepcast.util.StrTok;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionSupport {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionSupport" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private IdGenApp idGenApp;
  private DatabaseLibrary dbLib;
  private DBManagerApp dbMan;
  private OnlinePropertiesApp oprops;
  private SubscriberApp subscriberApp;
  private BillingApp billingApp;
  private ClientApp clientApp;
  private ProviderApp providerApp;
  private RouterApp routerApp;

  private EventService eventService = null;
  private TransactionQueueService transQueueService = null;
  private TransactionLogService transLogService = null;
  private BogusRequestService bogusReqService = null;

  private GatewayLogService gatewayLogService = null;

  private TransactionProcessBasic trans;
  private TransactionConf conf;
  private TransactionLog log;
  private TransactionSession session;

  private EventSupport eventSupport = null;
  private LuckDrawEvent luckDrawEventService = null;
  private PingCountEvent pingCountEventService = null;
  private TellAFriendEvent tellAFriendEventService = null;
  private MobileUserSupport mobileUserService = null;
  private DestinationProviderService destinationProviderService = null;

  private XipmeTranslator xipmeTranslator;

  private ResolvedTransactionQueueService resolvedTransactionQueueService;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionSupport( TransactionProcessBasic trans ) {

    idGenApp = IdGenApp.getInstance();
    dbLib = DatabaseLibrary.getInstance();
    dbMan = DBManagerApp.getInstance();
    oprops = OnlinePropertiesApp.getInstance();
    subscriberApp = SubscriberApp.getInstance();
    billingApp = BillingApp.getInstance();
    clientApp = ClientApp.getInstance();
    providerApp = ProviderApp.getInstance();
    routerApp = RouterApp.getInstance();

    eventService = new EventService();
    transQueueService = new TransactionQueueService();
    transLogService = new TransactionLogService();
    bogusReqService = new BogusRequestService();

    gatewayLogService = new GatewayLogService();

    this.trans = trans;
    this.conf = trans.conf();
    this.log = trans.log();
    this.session = trans.session();

    eventSupport = new EventSupport( trans );
    luckDrawEventService = new LuckDrawEvent( trans );
    pingCountEventService = new PingCountEvent( trans );
    tellAFriendEventService = new TellAFriendEvent( trans );
    mobileUserService = new MobileUserSupport( trans );
    destinationProviderService = new DestinationProviderService( trans );

    xipmeTranslator = new XipmeTranslator();

    resolvedTransactionQueueService = new ResolvedTransactionQueueService(
        trans );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Delegated Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionQueueService transQueueService() {
    return transQueueService;
  }

  public TransactionLogService transLogService() {
    return transLogService;
  }

  public EventSupport eventSupport() {
    return eventSupport;
  }

  public LuckDrawEvent luckDrawEventService() {
    return luckDrawEventService;
  }

  public PingCountEvent pingCountEventService() {
    return pingCountEventService;
  }

  public TellAFriendEvent tellAFriendEventService() {
    return tellAFriendEventService;
  }

  public MobileUserSupport mobileUserService() {
    return mobileUserService;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean insertIncomingMessageToClientApiMsg(
      TransactionInputMessage imsg ) {
    boolean result = false;

    if ( imsg == null ) {
      log.warning( "Failed to insert into client api msg , found null imsg" );
      return result;
    }

    String messageId = imsg.getMessageId();
    int eventId = imsg.getEventId();
    String phoneNumber = imsg.getOriginalAddress();
    String modemNumber = imsg.getDestinationAddress();
    String channel = com.beepcast.api.client.data.Channel.MODEM;
    String messageType = com.beepcast.api.client.data.MessageType.SMS;

    String messageContent = (String) imsg
        .getMessageParam( TransactionMessageParam.HDR_ORI_MESSAGE_CONTENT );
    if ( messageContent == null ) {
      messageContent = imsg.getMessageContent();
    }

    int storeMsgStatus = clientApp.storeMoMessage( messageId , eventId ,
        phoneNumber , modemNumber , channel , messageType , messageContent );

    log.debug( "Store mo message into client api msg , result = "
        + clientApp.storeMsgStatusToString( storeMsgStatus ) );

    return result;
  }

  public boolean insertIncomingMessageToGatewayLog( TransactionInputMessage imsg ) {
    return insertIncomingMessageToGatewayLog( imsg , null );
  }

  public boolean insertIncomingMessageToGatewayLog(
      TransactionInputMessage imsg , String messageStatus ) {
    boolean result = false;
    if ( imsg == null ) {
      log.warning( "Failed to insert into gateway log "
          + ", found null input message" );
      return result;
    }

    String messageContent = (String) imsg
        .getMessageParam( TransactionMessageParam.HDR_ORI_MESSAGE_CONTENT );
    if ( messageContent == null ) {
      messageContent = imsg.getMessageContent();
    }

    // default messageStatus ~ DELIVERED
    if ( ( messageStatus == null ) || ( messageStatus.equals( "" ) ) ) {
      messageStatus = "DELIVERED";
      log.debug( "Found empty messageStatus , set default as " + messageStatus );
    }

    result = gatewayLogService.insertIncomingMessage( imsg.getMessageId() ,
        MessageType.messageTypeToString( imsg.getMessageType() ) ,
        imsg.getMessageCount() , imsg.getEventId() ,
        imsg.getOriginalProvider() , imsg.getDebitAmount() ,
        imsg.getDateCreated() , imsg.getOriginalAddress() ,
        TransactionCountryUtil.getCountryId( imsg.getOriginalAddress() ) ,
        imsg.getDestinationAddress() , messageContent , messageStatus );

    return result;
  }

  public boolean insertOutgoingMessageToGatewayLog(
      TransactionOutputMessage omsg , String messageStatus ) {
    boolean result = false;
    if ( omsg == null ) {
      log.warning( "Failed to insert into gateway log "
          + ", found null outgoing message" );
      return result;
    }
    try {
      // trap delta time
      long deltaTime = System.currentTimeMillis();
      // default messageStatus ~ FAILED
      if ( ( messageStatus == null ) || ( messageStatus.equals( "" ) ) ) {
        messageStatus = "FAILED";
        log.debug( "Found empty messageStatus , set default as "
            + messageStatus );
      }
      String messageType = MessageType.messageTypeToString( omsg
          .getMessageType() );
      int countryId = TransactionCountryUtil.getCountryId( omsg
          .getDestinationAddress() );
      log.debug( "Inserting outgoing message into gateway log : "
          + "messageType = " + messageType + " , messageCount = "
          + omsg.getMessageCount() + " , messageContent = "
          + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );
      log.debug( "Inserting outgoing message into gateway log : "
          + "messageStatus = " + messageStatus + " , eventId = "
          + omsg.getEventId() + " , channelSessionId = "
          + omsg.getChannelSessionId() + " , destinationProvider = "
          + omsg.getDestinationProvider() + " , debitAmount = "
          + omsg.getDebitAmount() + " , destinationAddress = "
          + omsg.getDestinationAddress() + " , countryId = " + countryId );
      if ( !gatewayLogService.insertOutgoingMessage( omsg.getMessageId() ,
          messageType , omsg.getMessageCount() , omsg.getEventId() ,
          omsg.getChannelSessionId() , omsg.getDestinationProvider() ,
          omsg.getDebitAmount() , omsg.getDateCreated() ,
          omsg.getDestinationAddress() , countryId , omsg.getOriginalAddress() ,
          omsg.getMessageContent() , messageStatus ) ) {
        log.warning( "Failed to insert outgoing message into gateway log" );
        return result;
      }
      deltaTime = System.currentTimeMillis() - deltaTime;
      log.debug( "Successfully inserted outgoing message into gateway log "
          + ", takes " + deltaTime + " ms" );
      result = true;
    } catch ( Exception e ) {
      log.warning( "Failed to insert outgoing message into gateway log , " + e );
    }
    return result;
  }

  public boolean persistMobileUserBean( TransactionInputMessage imsg ) {
    boolean result = false;

    MobileUserBean mobileUserBean = mobileUserService.persistMobileUser(
        imsg.getClientId() , imsg.getOriginalAddress() );

    if ( mobileUserBean == null ) {
      log.warning( "Failed to persist mobile user profile" );
      return result;
    }

    imsg.addMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN ,
        mobileUserBean );

    List listTelcoCodes = TelcoService.resolveListTelcoCodes( mobileUserBean );

    log.debug( "Added input msg params of mobile user profile "
        + " , with : id = " + mobileUserBean.getId() + " , phone number = "
        + mobileUserBean.getPhone() + " , listTelcoCodes = " + listTelcoCodes );

    result = true;
    return result;
  }

  public boolean persistMobileUserBean( TransactionOutputMessage omsg ) {
    boolean result = false;
    MobileUserBean mobileUserBean = mobileUserService.persistMobileUser(
        omsg.getClientId() , omsg.getDestinationAddress() );
    if ( mobileUserBean == null ) {
      log.warning( "Failed to persist mobile user profile" );
      return result;
    }
    omsg.addMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN ,
        mobileUserBean );
    log.debug( "Added output msg params of mobile user profile "
        + " , with : id = " + mobileUserBean.getId() + " , phone number = "
        + mobileUserBean.getPhone() );
    result = true;
    return result;
  }

  public boolean persistCountryBean( TransactionInputMessage imsg ) {
    boolean result = false;
    TCountry countryBean = TransactionCountryUtil.getCountryBean( imsg
        .getOriginalAddress() );
    if ( countryBean == null ) {
      log.warning( "Failed to persist country profile" );
      return result;
    }
    imsg.addMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN ,
        countryBean );
    log.debug( "Added input msg params of country profile , with : id = "
        + countryBean.getId() + " , country name = " + countryBean.getName() );
    result = true;
    return result;
  }

  public boolean persistCountryBean( TransactionOutputMessage omsg ) {
    boolean result = false;
    TCountry countryBean = TransactionCountryUtil.getCountryBean( omsg
        .getDestinationAddress() );
    if ( countryBean == null ) {
      log.warning( "Failed to persist country profile" );
      return result;
    }
    omsg.addMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN ,
        countryBean );
    log.debug( "Added output msg params of country profile , with : id = "
        + countryBean.getId() + " , country name = " + countryBean.getName() );
    result = true;
    return result;
  }

  public boolean persistContactList( TransactionInputMessage imsg ) {
    boolean result = false;

    // prepare for the module apps

    ChannelApp channelApp = ChannelApp.getInstance();
    SubscriberApp subscriberApp = SubscriberApp.getInstance();

    // read and validate event profile

    EventBean eventBean = (EventBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( eventBean == null ) {
      log.warning( "Failed to persist contact list , found empty event bean" );
      return result;
    }

    // prepare clientSubscriberId and groupSubscriberId

    int clientSubscriberId = 0;
    int groupSubscriberId = 0;

    // read client subscriber id based on channel log table

    if ( ( eventBean.isChannel() ) && ( clientSubscriberId < 1 ) ) {
      ChannelLogBean clgBean = (ChannelLogBean) imsg
          .getMessageParam( TransactionMessageParam.HDR_CHANNEL_LOG_BEAN );
      if ( clgBean == null ) {
        clgBean = channelApp.getChannelLogBean( imsg.getEventId() ,
            imsg.getOriginalAddress() );
        if ( clgBean != null ) {
          imsg.addMessageParam( TransactionMessageParam.HDR_CHANNEL_LOG_BEAN ,
              clgBean );
          log.debug( "Added input msg params of channel log "
              + "profile , with id = " + clgBean.getId() );
        }
      }
      if ( clgBean != null ) {
        clientSubscriberId = clgBean.getClientSubscriberId();
      }
    }

    // read group subscriber id based on last channel session

    if ( ( eventBean.isChannel() ) && ( clientSubscriberId < 1 ) ) {
      ChannelSessionBean csnBean = (ChannelSessionBean) imsg
          .getMessageParam( TransactionMessageParam.HDR_CHANNEL_SESSION_BEAN );
      if ( csnBean == null ) {
        csnBean = channelApp.getLastChannelSession( imsg.getEventId() );
        if ( csnBean != null ) {
          imsg.addMessageParam(
              TransactionMessageParam.HDR_CHANNEL_SESSION_BEAN , csnBean );
          log.debug( "Added input msg params of channel session "
              + "profile , with id = " + csnBean.getId()
              + " , groupSubscriberId = " + csnBean.getGroupSubscriberId() );
        }
      }
      if ( csnBean != null ) {
        groupSubscriberId = csnBean.getGroupSubscriberId();
      }
    }

    // prepare for the subscriber group bean

    SubscriberGroupBean srgBean = (SubscriberGroupBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_SUBSCRIBER_GROUP_BEAN );
    if ( ( srgBean == null ) && ( groupSubscriberId > 0 ) ) {
      srgBean = subscriberApp.getSubscriberGroupBean( groupSubscriberId );
    }
    if ( srgBean != null ) {
      imsg.addMessageParam( TransactionMessageParam.HDR_SUBSCRIBER_GROUP_BEAN ,
          srgBean );
      log.debug( "Added input msg params of subscriber group "
          + "profile , with id = " + srgBean.getId() + " , name = "
          + srgBean.getGroupName() );
    }

    // prepare for the client subscriber bean

    ClientSubscriberBean csrBean = (ClientSubscriberBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN );
    if ( csrBean == null ) {
      if ( clientSubscriberId > 0 ) {
        csrBean = subscriberApp.getClientSubscriberBean( clientSubscriberId );
      } else {
        if ( groupSubscriberId > 0 ) {
          csrBean = subscriberApp.getClientSubscriberBean( imsg.getClientId() ,
              groupSubscriberId , imsg.getOriginalAddress() );
        }
      }
      if ( csrBean != null ) {
        imsg.addMessageParam(
            TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN , csrBean );
        log.debug( "Added input msg params of client subscriber "
            + "profile , with id = " + csrBean.getId() + " , phone = "
            + csrBean.getPhone() );
      }
    }
    if ( csrBean == null ) {
      return result;
    }

    // prepare for the client subscriber custom bean

    ClientSubscriberCustomBean cscBean = (ClientSubscriberCustomBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN );
    if ( cscBean == null ) {
      cscBean = csrBean.getCsCustomBean();
      if ( cscBean != null ) {
        imsg.addMessageParam(
            TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN , cscBean );
        log.debug( "Added input msg params of client subscriber custom "
            + "profile , with id = " + cscBean.getId()
            + " , clientSubscriberId = " + cscBean.getClientSubscriberId() );
      }
    }
    if ( cscBean == null ) {
      return result;
    }

    result = true;
    return result;
  }

  public boolean updateMobileUserLastCode( MobileUserBean mobileUserBean ,
      String newLastCode ) {
    boolean result = false;
    if ( mobileUserBean == null ) {
      return result;
    }
    result = mobileUserService.updateMobileUserLastCode( mobileUserBean ,
        newLastCode );
    return result;
  }

  public boolean cleanInputMessage( TransactionInputMessage imsg ) {
    boolean result = false;

    if ( imsg.getMessageContent() == null ) {
      return result;
    }

    imsg.addMessageParam( TransactionMessageParam.HDR_ORI_MESSAGE_CONTENT ,
        new String( imsg.getMessageContent() ) );
    log.debug( "Input msg params of original message content : "
        + StringEscapeUtils.escapeJava( (String) imsg
            .getMessageParam( TransactionMessageParam.HDR_ORI_MESSAGE_CONTENT ) ) );

    imsg.setMessageContent( TransactionUtil.cleanMessageContent( imsg
        .getMessageContent() ) );
    log.debug( "Clean messageContent = "
        + StringEscapeUtils.escapeJava( imsg.getMessageContent() ) );

    result = true;
    return result;
  }

  public TransactionQueueBean resolveTransactionQueueBean(
      TProviderToEvent providerToEvent , Map sessionParams ,
      TransactionInputMessage imsg , Object sessionLock , boolean sessionPersist ) {
    TransactionQueueBean transQueueResult = null;

    if ( providerToEvent == null ) {
      log.warning( "Failed to resolve trans queue "
          + ", found null map provider to event" );
      return transQueueResult;
    }

    int eventId = providerToEvent.getEventId();
    if ( eventId < 1 ) {
      log.warning( "Failed to resolve trans queue , found zero event id" );
      return transQueueResult;
    }

    TEvent eventBean = EventCommon.getEvent( eventId );
    if ( eventBean == null ) {
      log.warning( "Failed to resolve trans queue "
          + ", found invalid event id" );
      return transQueueResult;
    }

    int clientId = eventBean.getClientId();
    if ( clientId < 1 ) {
      log.warning( "Failed to resolve trans queue , found zero client id" );
      return transQueueResult;
    }

    String eventCode = StringUtils.trimToEmpty( providerToEvent.getEventCode() );
    if ( StringUtils.isBlank( eventCode ) ) {
      log.warning( "Failed to resolve trans queue , found blank event code" );
      return transQueueResult;
    }

    // verify store session flag
    if ( !providerToEvent.isStoreSession() ) {

      // force to bypass process response
      imsg.setNoProcessResponse( true );
      log.debug( "Found store session property is disabled "
          + ", will not process any response ( no mt leg processed )" );

      // build new trans queue from provider to event properties
      transQueueResult = new TransactionQueueBean();
      transQueueResult.setClientID( clientId );
      transQueueResult.setEventID( eventId );
      transQueueResult.setCode( eventCode );
      transQueueResult.setDateTm( new Date() );
      log.debug( "Created new trans queue : client id = "
          + transQueueResult.getClientID() + " , event id = "
          + transQueueResult.getEventID() + " , code = "
          + transQueueResult.getCode() );

      return transQueueResult;
    }

    // resolve transaction queue
    transQueueResult = resolvedTransactionQueueService.execute( sessionParams ,
        imsg , sessionLock , sessionPersist , eventCode );

    return transQueueResult;
  }

  public TransactionQueueBean resolveTransactionQueueBean( Map sessionParams ,
      TransactionInputMessage imsg , Object sessionLock , boolean sessionPersist ) {

    return resolvedTransactionQueueService.execute( sessionParams , imsg ,
        sessionLock , sessionPersist , null );

  }

  public boolean processBogusMesssage( TransactionInputMessage imsg ,
      String description ) {
    boolean result = false;

    // create bogus req object
    BogusRequestBean bogusRequest = BogusRequestFactory.createBogusRequestBean(
        0 , 0 , imsg.getOriginalAddress() , imsg.getDestinationAddress() ,
        imsg.getMessageContent() , description );

    // debug
    log.debug( "Inserting a new bogus message , phone = "
        + bogusRequest.getPhone() + " , message = " + bogusRequest.getMessage() );

    // insert
    result = bogusReqService.insert( bogusRequest );

    return result;
  }

  public TransactionOutputMessage createInvalidReplyMessage(
      TransactionInputMessage imsg , String invalidCode ) {
    TransactionOutputMessage omsg = null;
    try {

      // find the special message based on bogusCode
      SpecialMessageService specialMessageService = new SpecialMessageService();
      SpecialMessageBean specialMessageBean = specialMessageService.select(
          SpecialMessageType.SMS , invalidCode );
      String messageContent = specialMessageBean.getContent();
      if ( ( messageContent == null ) || ( messageContent.equals( "" ) ) ) {
        DLog.warning( lctx , "Can not find special message "
            + ", based on invalidCode = " + invalidCode );
        messageContent = invalidCode;
      }

      // compose output message as bogus message
      omsg = TransactionMessageFactory.createOutputMessage( imsg ,
          MessageProfile.PROFILE_INVALID_MSG , MessageStatusCode.SC_OK ,
          invalidCode , TransactionMessageFactory.generateMessageId( "" ) ,
          MessageType.TEXT_TYPE , messageContent );

    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to create bogus message , " + e );
    }
    return omsg;
  }

  public boolean resolveMessageIdentifier( TransactionInputMessage imsg ,
      TransactionQueueBean transQueue ) {
    boolean resolved = false;

    if ( transQueue == null ) {
      return resolved;
    }

    int clientId = imsg.getClientId();
    int eventId = imsg.getEventId();

    if ( ( clientId < 1 ) || ( eventId < 1 ) ) {

      // resolved at client level from trans queue object

      int newClientId = (int) transQueue.getClientID();
      int newEventId = (int) transQueue.getEventID();

      log.debug( "Resolved message identifier , changed profile : clientId = "
          + clientId + " -> " + newClientId + " , eventId = " + eventId
          + " -> " + newEventId );

      imsg.setClientId( newClientId );
      imsg.setEventId( newEventId );

    } else {

      log.debug( "No need to resolve message identifier , found clientId = "
          + clientId + " and eventId = " + eventId
          + " are preset inside the message already" );

    }

    resolved = true;
    return resolved;
  }

  public TransactionOutputMessage processInternalMessage(
      TransactionInputMessage imsg , TransactionQueueBean transQueue ) {
    TransactionOutputMessage omsg = null;
    if ( transQueue == null ) {
      return omsg;
    }
    String code = transQueue.getCode();
    if ( ( code != null ) && ( code.startsWith( "?" ) ) ) {
      log.debug( "Process internal message" );

      // componse message content
      String messageContent = processReservedCode( transQueue.getCode() ,
          imsg.getOriginalAddress() , imsg.getMessageContent() );
      messageContent = EventTransQueueReservedVariables
          .replaceReservedVariables( log , messageContent , transQueue );
      messageContent = EventOutboundReservedVariables.replaceReservedVariables(
          log , messageContent , imsg );

      if ( ( messageContent != null ) && ( messageContent.length() > 6 )
          && ( messageContent.substring( 0 , 3 ).equals( "<<<" ) ) ) {
        StrTok st = new StrTok( messageContent.substring( 3 ) , ">>>" );
        String strCode = st.nextTok();
        messageContent = jumpToEvent( strCode , transQueue , imsg , "" , false );
      }

      log.debug( "Compose internal message reply " + messageContent );

      // compose output message as internal message
      omsg = TransactionMessageFactory.createOutputMessage( imsg ,
          MessageProfile.PROFILE_INTERNAL_MSG , MessageStatusCode.SC_OK ,
          messageContent , TransactionMessageFactory.generateMessageId( "" ) ,
          MessageType.TEXT_TYPE , messageContent );
    }
    return omsg;
  }

  public TransactionOutputMessage createReplyMessage(
      TransactionInputMessage imsg , String messageContent ) {
    return createReplyMessage( imsg , messageContent , MessageType.TEXT_TYPE ,
        false );
  }

  public TransactionOutputMessage createReplyMessage(
      TransactionInputMessage imsg , String messageContent , int messageType ,
      boolean createNewMessageId ) {
    TransactionOutputMessage omsg = null;
    if ( imsg == null ) {
      return omsg;
    }

    // clean params
    messageContent = ( messageContent == null ) ? "" : messageContent;

    // when found existing imsg's reply message , for to use it
    if ( !StringUtils.isBlank( imsg.getReplyMessageContent() ) ) {
      log.debug( "Found existing reply response inside input message "
          + ", update new message response : "
          + StringEscapeUtils.escapeJava( messageContent ) + " -> "
          + StringEscapeUtils.escapeJava( imsg.getReplyMessageContent() ) );
      messageContent = imsg.getReplyMessageContent();
    }

    // generate messageId of input message
    String newMessageId = imsg.getMessageId();
    if ( ( createNewMessageId ) || ( newMessageId == null )
        || ( newMessageId.equals( "" ) ) ) {
      newMessageId = TransactionMessageFactory.generateMessageId( "" );
      log.debug( "Generated new message id : " + imsg.getMessageId() + " -> "
          + newMessageId );
    }

    // verify the message type is text or unicode
    int newMessageType = messageType;
    if ( newMessageType == MessageType.UNKNOWN_TYPE ) {
      newMessageType = MessageType.TEXT_TYPE;
    }
    if ( ( newMessageType == MessageType.TEXT_TYPE )
        && TransactionUtil.isUnicodeMessage( messageContent ) ) {
      newMessageType = MessageType.UNICODE_TYPE;
    }
    log.debug( "Resolved new message type : "
        + MessageType.messageTypeToString( messageType ) + " -> "
        + MessageType.messageTypeToString( newMessageType ) );

    // create output message object
    omsg = TransactionMessageFactory.createOutputMessage( imsg ,
        MessageProfile.PROFILE_NORMAL_MSG , MessageStatusCode.SC_OK , "" ,
        newMessageId , newMessageType , messageContent );
    if ( omsg != null ) {

      // additional message params
      omsg.addMessageParam( TransactionMessageParam.HDR_ORI_PROVIDER ,
          imsg.getOriginalProvider() );
      log.debug( "Added output message params : oriProvider = "
          + imsg.getOriginalProvider() );

      // log it
      log.debug( "Successfully created output message "
          + ", with messageProfile = " + omsg.getMessageProfile()
          + " , originalNode = " + omsg.getOriginalNode()
          + " , destinationNode = " + omsg.getDestinationNode()
          + " , statusCodeDefault = " + omsg.getMessageStatusCode()
          + " , newMessageId = " + omsg.getMessageId() + " , messageType = "
          + MessageType.messageTypeToString( omsg.getMessageType() )
          + " , messageContent = "
          + StringEscapeUtils.escapeJava( omsg.getMessageContent() ) );
    }

    return omsg;
  }

  public TransactionQueueBean appendParameters(
      TransactionQueueBean transQueue , TransactionInputMessage imsg ,
      ProcessBean processBean , ProcessBean processSteps[] ) {

    EventBean eventBean = (EventBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( eventBean == null ) {
      log.warning( "Failed to process append parameters "
          + ", found empty event bean" );
      return transQueue;
    }

    // special handling for "Web-SMS Dialog" eventName
    String eventName = eventBean.getEventName();
    if ( eventName.equals( "Web-SMS Dialog" ) ) {
      log.debug( "Found event as WEB SMS Dialog" );
      int lastOutputIndex = 0;
      String params = transQueue.getParams();
      StrTok st = new StrTok( transQueue.getParams() , "," );
      for ( int i = 0 ; i < st.countTokens() ; i++ ) {
        StrTok st2 = new StrTok( st.nextTok() , "=" );
        String name = st2.nextTok();
        if ( name.startsWith( "INPUT" ) ) {
          lastOutputIndex = Integer.parseInt( name.substring( 5 ) );
        }
      }
      lastOutputIndex++;
      if ( params.length() > 0 ) {
        params += ",";
      }
      params += "INPUT" + lastOutputIndex + "=" + imsg.getMessageContent();
      transQueue.setParams( params );
      transQueue.setMessageCount( transQueue.getMessageCount() - 1 );
      transQueueService.update( transQueue );
      return transQueue;
    } // if ( eventName.equals( "Web-SMS Dialog" ) )

    // special handling for "CODE" type
    String type = processBean.getType();
    if ( type.equals( "CODE" ) ) {
      log.debug( "Found event process type as CODE "
          + ", bypass event append parameters" );
      return transQueue;
    } // if ( type.equals( "CODE" ) )

    try {

      // prepare expected param names for this process step
      String[] names = processBean.getNames();
      log.debug( "Found process type names = ["
          + StringUtils.join( names , "," ) + "]" );

      // read current list of params from transaction queue record
      String params = transQueue.getParams();
      if ( ( params != null ) && ( params.length() > 0 ) ) {
        params += ","; // setup for append
      }

      // parse receive record for values
      StrTok stTemp = new StrTok( imsg.getMessageContent() , " ," );
      if ( type.equals( "PARAM" ) ) {
        stTemp.nextTok();
      }
      String paramString = stTemp.nextTok( "~" ).trim();
      String delim = ( paramString.indexOf( "," ) != -1 ) ? "," : " ";
      StringTokenizer st = new StringTokenizer( paramString , delim );

      // get up to last value
      for ( int i = 0 ; i < names.length - 1 ; i++ ) {
        if ( st.hasMoreTokens() ) {
          String paramName = names[i];
          if ( type.equals( "EXPECT" ) || type.equals( "FIRST_WORD" )
              || type.equals( "CONTAIN_WORD" ) ) {
            paramName = eventSupport.getExpectParamName( processBean ,
                processSteps );
          }
          String paramValue = st.nextToken().trim();
          String param = paramName + "=" + paramValue + ",";
          params += param;
        }
      }

      // get last value (might be more than one token)
      String lastValue = "";
      while ( st.hasMoreTokens() ) {
        lastValue += st.nextToken() + " ";
      }
      if ( !lastValue.equals( "" ) ) {
        String paramName = names[names.length - 1];
        if ( type.equals( "EXPECT" ) || type.equals( "FIRST_WORD" )
            || type.equals( "CONTAIN_WORD" ) ) {
          paramName = eventSupport.getExpectParamName( processBean ,
              processSteps );
        }
        String paramValue = lastValue.trim();
        String param = paramName + "=" + paramValue;
        params += param;
      }

      // create param array
      if ( params.endsWith( "," ) ) {
        params = params.substring( 0 , params.length() - 1 );
      }

      // update params in transaction queue record
      transQueue.setParams( params );

    } catch ( Exception e ) {
      log.warning( "Failed to process event append parameters , " + e );
    }
    return transQueue;
  } // appendParameters()

  public boolean updateAppendedParameters( TransactionQueueBean transQueue ,
      ProcessBean processBean ) {
    boolean result = false;
    if ( transQueue == null ) {
      return result;
    }
    if ( processBean == null ) {
      return result;
    }
    try {
      String strParams = transQueue.getParams();
      if ( ( strParams == null ) || ( strParams.equals( "" ) ) ) {
        return result;
      }
      String[] arrParams = strParams.split( "," );
      if ( ( arrParams == null ) || ( arrParams.length < 1 ) ) {
        return result;
      }
      String[] arrNames = processBean.getNames();
      if ( ( arrNames == null ) || ( arrNames.length < 1 ) ) {
        return result;
      }
      for ( int idxNames = 0 ; idxNames < arrNames.length ; idxNames++ ) {
        for ( int idxParams = 0 ; idxParams < arrParams.length ; idxParams++ ) {
          StrTok st3 = new StrTok( arrParams[idxParams] , "=" );
          String paramName = st3.nextTok();

          // EMAIL
          if ( paramName.equalsIgnoreCase( "EMAIL" ) ) {
            int clientId = (int) transQueue.getClientID();
            String phoneNumber = transQueue.getPhone();
            String emailAddress = st3.nextTok();
            if ( !StringUtils.isBlank( emailAddress ) ) {
              log.debug( "Updating mobile user email param : phoneNumber = "
                  + phoneNumber + " , emailAddress = " + emailAddress );
              mobileUserService.updateMobileUserEmail( clientId , phoneNumber ,
                  emailAddress , transQueue );
            }
          }

          // NAME
          if ( paramName.equalsIgnoreCase( "NAME" ) ) {
            int clientId = (int) transQueue.getClientID();
            String phoneNumber = transQueue.getPhone();
            String name = st3.nextTok();
            StrTok st4 = new StrTok( name , " " );
            String firstName = st4.nextTok();
            String lastName = st4.nextTok();
            if ( !StringUtils.isBlank( firstName )
                || !StringUtils.isBlank( lastName ) ) {
              log.debug( "Updating mobile user name param : phoneNumber = "
                  + phoneNumber + " , firstName = " + firstName
                  + " , lastName = " + lastName );
              mobileUserService.updateMobileUserName( clientId , phoneNumber ,
                  firstName , lastName , transQueue );
            }
          }

          // IC
          if ( paramName.equalsIgnoreCase( "IC" ) ) {
            int clientId = (int) transQueue.getClientID();
            String phoneNumber = transQueue.getPhone();
            String ic = st3.nextTok();
            if ( !StringUtils.isBlank( ic ) ) {
              log.debug( "Updating mobile user ic param : phoneNumber = "
                  + phoneNumber + " , ic = " + ic );
              mobileUserService.updateMobileUserIc( clientId , phoneNumber ,
                  ic , transQueue );
            }
          }

        } // for ( idxParams = 0 ; idxParams < arrParams.length ; idxParams++ )
      } // for ( idxNames = 0 ; idxNames < arrNames.length ; idxNames++ )
      result = true;
    } catch ( Exception e ) {
      log.warning( "Failed to update appended parameters , " + e );
    }
    return result;
  }

  public int ifDate( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    int result = TransactionProcessBasic.NEXT_STEP_NIL;

    // is the next step expect date ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null )
        && ( nextType.equals( "IF DATE BEFORE" ) || nextType
            .equals( "IF DATE AFTER" ) ) ) {
      log.debug( headerLog + "Trigger as if date message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not if date step , continue" );
      return result;
    }

    // prepare default result
    result = TransactionProcessBasic.NEXT_STEP_END;

    // log first
    log.debug( headerLog + "Performing if date step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( "Failed to process if date step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    result = EventProcessIfDate.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs );
    return result;
  }

  public int sendIf( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    int result = TransactionProcessBasic.NEXT_STEP_NIL;

    // is the next step expect date ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && nextType.equals( "SEND IF" ) ) {
      log.debug( headerLog + "Trigger as send if message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not send if step , continue" );
      return result;
    }

    // prepare default result
    result = TransactionProcessBasic.NEXT_STEP_END;

    // log first
    log.debug( headerLog + "Performing send if step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( "Failed to process send if step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    result = EventProcessSendIf.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs );
    return result;
  }

  public boolean delaySend( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step delay send ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "DELAY_SEND" ) ) ) {
      log.debug( headerLog + "Trigger as delay send message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not delay send step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing delay send step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process delay send step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessDelaySend.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform delay send step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed delay send step" );

    result = true;
    return result;
  }

  public boolean log( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step reminder ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "LOG" ) ) ) {
      log.debug( headerLog + "Trigger as log message , with next type = "
          + nextType );
    } else {
      log.debug( headerLog + "This message is not log step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing log step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process log step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessLog.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform log step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Successfully performed log step" );
    result = true;
    return result;
  }

  public boolean addReminder( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step reminder ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null )
        && ( nextType.equals( "REMINDER" )
            || nextType.equals( "REMINDER RSVP PENDING" )
            || nextType.equals( "REMINDER-RSVP PENDING" )
            || nextType.equals( "REMINDER RSVP YES" ) || nextType
              .equals( "REMINDER-RSVP YES" ) ) ) {
      log.debug( headerLog + "Trigger as reminder message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not reminder step , continue" );
      return result;
    }

    // prepare date time format
    String format = "yyyy-MM-dd HH:mm";
    SimpleDateFormat sdf = new SimpleDateFormat( format );
    log.debug( headerLog + "Prepare simple date time formatter = " + format );

    // get next process bean
    ProcessBean nextProcessBean = null;
    {
      String strNextStep = processBean.getNextStep();
      if ( !StringUtils.isBlank( strNextStep ) ) {
        try {
          int intNextStep = Integer.parseInt( strNextStep );
          if ( intNextStep > 0 ) {
            nextProcessBean = processSteps[intNextStep - 1];
          }
        } catch ( NumberFormatException e ) {
          log.warning( headerLog + "Failed to get next process bean "
              + ", failed to read the nextStep , " + e );
          return result;
        }
      }
    }
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to get next process bean "
          + ", can not find the next step bean" );
      return result;
    }

    // get reminder date from next process names
    Date reminderDate = null;
    {
      String paramLabel = nextProcessBean.getParamLabel();
      if ( ( paramLabel == null ) || ( paramLabel.equals( "" ) ) ) {
        log.warning( headerLog + "Failed to get reminder date "
            + ", found empty in the param label" );
        return result;
      }
      if ( !paramLabel.equals( "DATE=" ) ) {
        log.warning( headerLog + "Failed to get reminder date "
            + ", found wrong paramLabel = " + paramLabel );
        return result;
      }
      String[] names = nextProcessBean.getNames();
      if ( ( names == null ) || ( names.length < 1 ) ) {
        log.warning( headerLog
            + "Failed to get reminder date , found empty names" );
        return result;
      }
      try {
        reminderDate = sdf.parse( StringUtils.join( names , " " ) );
      } catch ( ParseException e ) {
        log.warning( headerLog + "Failed to parse next process names "
            + "into simple date time format , " + e );
      }
    }
    if ( reminderDate == null ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found empty reminder date value" );
      return result;
    }
    log.debug( headerLog + "Succeed define reminder send date = "
        + DateTimeFormat.convertToString( reminderDate ) );

    // the reminder send date can not less than incoming message date
    if ( reminderDate.getTime() < imsg.getDateCreated().getTime() ) {
      log.debug( headerLog + "Bypass reminder step "
          + ", found reminder send date less than incoming message date = "
          + DateTimeFormat.convertToString( imsg.getDateCreated() ) );
      result = true;
      return result;
    }

    // get remind message
    String remindMessage = nextProcessBean.getResponse();
    if ( StringUtils.isBlank( remindMessage ) ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found failed to get remind message" );
      return result;
    }

    // create output remind message
    TransactionOutputMessage ormsg = createReplyMessage( imsg , remindMessage );
    if ( ormsg == null ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found failed to create output remind message" );
      return result;
    }

    // regenerate new message id for output remind message
    ormsg.setMessageId( TransactionMessageFactory.generateMessageId( "" ) );
    log.debug( headerLog + "Generated new message id for reminder message = "
        + ormsg.getMessageId() );

    // resolve original ( with/out masking ) address
    if ( !resolveOriginalAddressAndMask( ormsg ) ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found failed to resolve original address" );
      return result;
    }

    // resolve destination provider
    if ( !resolveDestinationProviderAndMask( imsg , ormsg ) ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found failed to resolve destination provider" );
      return result;
    }

    // resolve message content
    resolveMessageContent( ormsg );
    if ( ormsg.getMessageCount() < 1 ) {
      log.warning( headerLog + "Failed to set reminder "
          + ", found zero total message send" );
      return result;
    }

    // get country bean of imsg
    TCountry countryBean = (TCountry) imsg
        .getMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN );

    // prepare debit amount for mt leg
    double debitAmount = 0;
    if ( countryBean != null ) {
      int messageCount = ormsg.getMessageCount();
      debitAmount = countryBean.getCreditCost();
      debitAmount = debitAmount * messageCount;
    }

    // store debit amount info
    ormsg.setDebitAmount( debitAmount );
    log.debug( headerLog + "Set debit amount = " + ormsg.getDebitAmount()
        + " unit(s)" );

    // insert into reminder table
    if ( !insertOutgoingMessageToReminder( ormsg , nextType , reminderDate ) ) {
      log.warning( headerLog
          + "Failed to set reminder , found failed to insert "
          + "a reminder message into table" );
      return result;
    }

    log.debug( headerLog + "Successfully execute reminder step" );
    result = true;
    return result;
  }

  public boolean delReminder( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step no remind ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null )
        && ( nextType.equals( "NO REMINDER" )
            || nextType.equals( "NO REMINDER RSVP NO" ) || nextType
              .equals( "NO REMINDER-RSVP NO" ) ) ) {
      log.debug( headerLog + "Trigger as no reminder step "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not no reminder step , continue" );
      return result;
    }

    // prepare params
    int eventId = (int) transObj.getEventID();
    String phone = imsg.getOriginalAddress();
    log.debug( headerLog + "Defined no reminder params : event id = " + eventId
        + " , phone = " + phone );

    // execute no remind
    ReminderApp reminderApp = ReminderApp.getInstance();
    if ( reminderApp == null ) {
      log.warning( headerLog + "Failed to execute no reminder step "
          + ", found null reminder app" );
      return result;
    }

    try {

      if ( StringUtils.equals( nextType , "NO REMINDER" ) ) {
        int totalRecords = reminderApp.delReminders( eventId , phone );
        log.debug( headerLog + "Found delete total " + totalRecords
            + " reminder(s) thru reminder app" );
        result = true;
        return result;
      }

      if ( StringUtils.equals( nextType , "NO REMINDER RSVP NO" )
          || StringUtils.equals( nextType , "NO REMINDER-RSVP NO" ) ) {
        if ( !reminderApp.delReminderWithRsvpList( eventId , phone , false ) ) {
          log.debug( headerLog + "Failed to delete reminder with "
              + "update rsvp status thru reminder app" );
          result = true;
          return result;
        }
      }

    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to perform delete "
          + "from reminder table , " + e );
      return result;
    }

    log.debug( headerLog + "Successfully execute no reminder step" );
    result = true;
    return result;
  }

  public Date getPersonalRemindDate( String remindParam , Date remindDate )
      throws IOException {

    int minutes = 0;
    int hours = 0;
    int days = 0;
    int weeks = 0;
    int _number = 0;

    /*----------------------------
      parse remind param
    ----------------------------*/
    for ( int i = 0 ; i < remindParam.length() ; i++ ) {
      String thisChar = remindParam.substring( i , i + 1 );
      String lastChar = ( i > 0 ) ? remindParam.substring( i - 1 , i ) : "";
      if ( StringUtils.isNumeric( lastChar ) ) {
        _number = ( _number * 10 ) + Integer.parseInt( lastChar );
        if ( thisChar.equalsIgnoreCase( "m" ) )
          minutes = _number;
        else if ( thisChar.equalsIgnoreCase( "h" ) )
          hours = _number;
        else if ( thisChar.equalsIgnoreCase( "d" ) )
          days = _number;
        else if ( thisChar.equalsIgnoreCase( "w" ) )
          weeks = _number;
      } else
        _number = 0;
    }

    /*----------------------------
      override remind date
    ----------------------------*/
    if ( minutes + hours + days + weeks > 0 ) {
      try {
        Calendar c = new GregorianCalendar();
        c.setTime( remindDate );
        c.add( Calendar.MINUTE , ( minutes * -1 ) );
        c.add( Calendar.HOUR_OF_DAY , ( hours * -1 ) );
        c.add( Calendar.DATE , ( days * -1 ) );
        c.add( Calendar.DATE , ( weeks * -7 ) );
        remindDate = c.getTime();
      } catch ( Exception e ) {
      }
    }

    // success
    return remindDate;

  } // getPersonalRemindDate()

  public String getPersonalRemindFreq( String remindParam ) throws IOException {

    String freq = "";

    /*----------------------------
      parse remind param
    ----------------------------*/
    for ( int i = 0 ; i < remindParam.length() ; i++ ) {
      String thisChar = remindParam.substring( i , i + 1 );
      String lastChar = ( i > 0 ) ? remindParam.substring( i - 1 , i ) : "";
      if ( thisChar.equalsIgnoreCase( "r" ) ) {
        if ( lastChar.equalsIgnoreCase( "d" ) )
          freq = "DAILY";
        else if ( lastChar.equalsIgnoreCase( "w" ) )
          freq = "WEEKLY";
      }
    }

    // success
    return freq;

  } // getPersonalRemindFreq()

  public boolean sendEmail( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step email ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "EMAIL" ) ) ) {
      log.debug( headerLog + "Trigger as email message , with next type = "
          + nextType );
    } else {
      log.debug( headerLog + "This message is not email step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing email step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process email step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessEmail.process( headerLog , this , conf , log , transObj ,
        nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform email step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed email step" );
    result = true;
    return result;
  } // sendEmail()

  public boolean smsToEmail( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step sms to email ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "SMS_TO_EMAIL" ) ) ) {
      log.debug( headerLog + "Trigger as sms to email message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not sms to email step "
          + ", continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing sms to email step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process sms to email step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessSmsToEmail.process( headerLog , this , conf , log ,
        transObj , nextProcessBean , imsg ) ) {
      log.warning( headerLog + "Failed to perform sms to email step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed sms to email step" );
    result = true;
    return result;
  } // smsToEmail()

  public boolean smsToSms( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step sms to sms ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "SMS_TO_SMS" ) ) ) {
      log.debug( headerLog + "Trigger as sms to sms message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not sms to sms step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing sms to sms step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process sms to sms step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessSmsToSms.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs , false ) ) {
      log.warning( headerLog + "Failed to perform sms to sms step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed sms to sms step" );
    result = true;
    return result;
  }

  public boolean smsToSmsExcludeSender( String headerLog ,
      TransactionProcessBasic trans , TransactionQueueBean transObj ,
      ProcessBean processBean , ProcessBean processSteps[] ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step sms to sms xsender ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "SMS_TO_SMS_XSENDER" ) ) ) {
      log.debug( headerLog + "Trigger as sms to sms exclude sender message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not sms to sms exclude sender "
          + "step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing sms to sms step exclude sender" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog
          + "Failed to process sms to sms exclude sender step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessSmsToSms.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs , true ) ) {
      log.warning( headerLog + "Failed to perform sms to sms "
          + "exclude sender step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed sms to sms "
        + "exclude sender step" );
    result = true;
    return result;
  }

  public boolean emailClient( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs )
      throws IOException {
    boolean result = false;

    // is the next step email client ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "EMAIL_CLIENT" ) ) ) {
      log.debug( headerLog + "Trigger as email client message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not email client step "
          + ", continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing email client step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process email client "
          + ", found null next process bean" );
      return result;
    }
    if ( !nextProcessBean.getParamLabel().equals( "SUBJECT=" ) ) {
      log.warning( headerLog + "Failed to process email client "
          + ", can not find subject field in the next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessEmailClient.process( headerLog , this , conf , log ,
        transObj , nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform email client step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed email client step" );
    result = true;
    return result;
  } // emailClient()

  public boolean emailTo( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs )
      throws IOException {
    boolean result = false;

    // is the next step email to ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "EMAIL_TO" ) ) ) {
      log.debug( headerLog + "Trigger as email to message , with next type = "
          + nextType );
    } else {
      log.debug( headerLog + "This message is not email to step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing email to step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process email to step"
          + ", found null next process bean" );
      return result;
    }
    if ( !nextProcessBean.getParamLabel().equals( "TO=" ) ) {
      log.warning( headerLog + "Failed to process email to step"
          + ", can not find to field in the next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessEmailTo.process( headerLog , this , conf , log ,
        transObj , nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform email to step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed email to step" );
    result = true;
    return result;
  } // emailTo()

  public boolean sendRingtone( String headerLog ,
      TransactionProcessBasic trans , TransactionQueueBean transObj ,
      ProcessBean processBean , ProcessBean processSteps[] ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    return result;
  } // sendRingtone()

  public boolean phoneUser( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    return result;
  } // phoneUser()

  public boolean autoSubscribe( String headerLog ,
      TransactionProcessBasic trans , TransactionQueueBean transObj ,
      ProcessBean processBean , ProcessBean processSteps[] ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    return result;
  } // autoSubscribe()

  public boolean subscribe( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step subscribe ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "SUBSCRIBE" ) ) ) {
      log.debug( headerLog + "Trigger as subscribe message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not subscribe step "
          + ", continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing subscribe step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process subscribe step"
          + ", found null next process bean" );
      return result;
    }
    if ( !nextProcessBean.getParamLabel().equals( "CHANNEL=" ) ) {
      log.warning( headerLog + "Failed to process subscribe step"
          + ", can not find to field in the next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessSubscribe.process( headerLog , this , conf , log ,
        transObj , nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform subscribe step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed subscribe step" );
    result = true;
    return result;
  }

  public boolean subscribeList( String headerLog ,
      TransactionProcessBasic trans , TransactionQueueBean transObj ,
      ProcessBean processBean , ProcessBean processSteps[] ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step subscribe list ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "SUBSCRIBE_LIST" ) ) ) {
      log.debug( headerLog + "Trigger as subscribe list message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not subscribe list step "
          + ", continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing subscribe list step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process subscribe list step"
          + ", found null next process bean" );
      return result;
    }
    if ( !nextProcessBean.getParamLabel().equals( "LIST=" ) ) {
      log.warning( headerLog + "Failed to process subscribe list step"
          + ", can not find to field in the next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessSubscribeList.process( headerLog , this , conf , log ,
        transObj , nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform subscribe list step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed subscribe list step" );
    result = true;
    return result;
  }

  public boolean unsubscribeList( String headerLog ,
      TransactionProcessBasic trans , TransactionQueueBean transObj ,
      ProcessBean processBean , ProcessBean processSteps[] ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    String phoneNumber = null;
    long eventId = 0;
    long clientId = 0;

    if ( transObj == null ) {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", found empty in object of Transaction" );
      return result;
    } else {

      // comment for awhile
      // originalMessage = trans.originalMessage;

      phoneNumber = transObj.getPhone();
      eventId = transObj.getEventID();
      clientId = transObj.getClientID();
    }

    // validate event id
    if ( eventId == 0 ) {
      log.warning( headerLog + "Can not process unsubscribe "
          + "list message , found empty eventId" );
      return result;
    }

    // is the next step subscribe ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "UNSUBSCRIBE_LIST" ) ) ) {
      log.debug( headerLog + "Trigger as unsubscribe list message" );
    } else {
      log.debug( headerLog + "This message is not unsubscribe list message "
          + ", continue" );
      return result;
    }

    // get the subscription list
    String listName = null;
    String[] names = null;
    String paramLabel = null;
    String strNextStep = processBean.getNextStep();
    int intNextStep = 0;
    try {
      intNextStep = Integer.parseInt( strNextStep );
    } catch ( NumberFormatException e ) {
      log.warning( headerLog + "Failed to read the nextStep , " + e );
      return result;
    }
    ProcessBean nextProcessBean = null;
    if ( intNextStep > 0 ) {
      nextProcessBean = processSteps[intNextStep - 1];
    }
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Can not find the process bean" );
      return result;
    } else {
      names = nextProcessBean.getNames();
      paramLabel = nextProcessBean.getParamLabel();
    }
    if ( ( paramLabel == null ) || ( paramLabel.equals( "" ) ) ) {
      log.warning( headerLog + "Found empty in the param label" );
      return result;
    }
    if ( !paramLabel.equals( "LIST=" ) ) {
      log.warning( headerLog + "Found wrong paramLabel = " + paramLabel );
      return result;
    }
    if ( ( names != null ) && ( names.length > 0 ) ) {
      listName = names[0];
    }
    if ( ( listName == null ) || ( listName.equals( "" ) ) ) {
      log.warning( headerLog + "Found empty in the list name" );
      return result;
    }

    log.debug( headerLog + "Trying to unsubscribe phoneNumber = " + phoneNumber
        + " in the list name = " + listName );

    // get subscriber group bean
    SubscriberGroupBean sgBean = subscriberApp.getSubscriberGroupBean(
        (int) clientId , listName );
    if ( sgBean == null ) {
      log.warning( headerLog + "Can not process unsubscribe to list "
          + ", found invalid list name = " + listName );
      return result;
    }
    if ( !sgBean.isActive() ) {
      log.warning( headerLog + "Can not process unsubscribe to list "
          + ", Found deleted list name = " + listName );
      return result;
    }

    // do the subscribe list
    boolean subscribed = subscriberApp.doSubscribed( (int) clientId ,
        sgBean.getId() , phoneNumber , false , false , (int) eventId , null ,
        null , null );
    if ( !subscribed ) {
      log.warning( headerLog + "Failed to perform unsubscribed in list name = "
          + listName );
      return result;
    }

    log.debug( headerLog + "Successfully unsubscribed phoneNumber = "
        + phoneNumber + " from the list name = " + listName
        + " and tracked with from event id = " + eventId );
    result = true;
    return result;
  }

  public void unsubscribeMobileUser( TransactionProcessBasic trans ,
      String phoneNumber , long clientId , long channelId ) throws IOException {

    // find the target subscriber list based on channelId
    ChannelSessionBean csBean = null;
    ChannelSessionService csService = new ChannelSessionService();
    csBean = csService.getChannelSessionFromEventId( (int) channelId );
    if ( csBean == null ) {
      log.warning( "Failed to perform the unsubscribed "
          + ", can not find the subscriber group id" );
      return;
    }
    int subscriberGroupId = csBean.getGroupSubscriberId();
    if ( subscriberGroupId < 1 ) {
      log.warning( "Failed to perform the unsubscribed "
          + ", can not find the subscriber group id" );
      return;
    }

    log.debug( "Unsubscribe from the list , with : channel session id = "
        + csBean.getId() + " , group subscriber id = "
        + csBean.getGroupSubscriberId() + " , channel id = " + channelId );

    // unsubscribed in the client subscriber
    boolean updated = subscriberApp.doSubscribed( (int) clientId ,
        subscriberGroupId , phoneNumber , false , false , (int) channelId ,
        null , null , null );
    if ( !updated ) {
      log.warning( "Failed to unsubscribed at the channel session "
          + "for phoneNumber = " + phoneNumber + " and subscriberGroupId = "
          + subscriberGroupId );
      return;
    }

    log.debug( "Successfully unsubscribed for phoneNumber = " + phoneNumber
        + " , clientId = " + clientId );

  } // subscribeMobileUser()

  public boolean unsubscribe( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs )
      throws IOException {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    String phoneNumber = null;
    long eventId = 0;
    long clientId = 0;

    if ( transObj == null ) {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", found empty in object of Transaction" );
      return result;
    }

    // comment for awhile
    phoneNumber = transObj.getPhone();
    eventId = transObj.getEventID();
    clientId = transObj.getClientID();

    // validate phone number
    if ( ( phoneNumber == null ) || ( phoneNumber.equals( "" ) ) ) {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", found empty phone number" );
      return result;
    }

    // validate event id
    if ( eventId < 1 ) {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", found empty eventId" );
      return result;
    }

    // validate client id
    if ( clientId < 1 ) {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", found empty clientId" );
      return result;
    }

    // is the next step unsubscribe ?
    if ( processBean.getNextType().equals( "UNSUBSCRIBE" ) ) {
      log.debug( headerLog + "Trigger as unsubscribe message" );
    } else {
      log.debug( headerLog + "This message is not unsubscribe message "
          + ", continue" );
      return result;
    }

    // get event bean
    EventBean eventBean = (EventBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( eventBean == null ) {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", can not find event bean object" );
      return result;
    }

    // get channel id
    long channelId = 0;
    if ( eventBean.getChannel() ) {
      channelId = eventBean.getEventID();
      log.debug( headerLog + "Found current event as outgoing "
          + ", trying to unsubscribe on the one" );
    } else {
      log.warning( headerLog + "Can not process unsubscribe message "
          + ", found current event as incoming" );
      return result;
    }

    // try to subscribe
    log.debug( headerLog + "Trying to unsubscribe based on channel id = "
        + channelId );
    unsubscribeMobileUser( trans , phoneNumber , clientId , channelId );

    {
      // store subscribe message into client api
      String messageId = imsg.getMessageId();
      String eventCode = null;
      String modemNumber = imsg.getDestinationAddress();
      boolean subscribeStatus = false;
      String channel = com.beepcast.api.client.data.Channel.MODEM;
      String messageContent = imsg.getMessageContent();
      int storeMsgStatus = clientApp.storeSubscriptionMessage( messageId ,
          (int) eventId , eventCode , phoneNumber , modemNumber ,
          subscribeStatus , channel , messageContent );
      log.debug( headerLog
          + "Store unsubscription message to client msg api , result = "
          + clientApp.storeMsgStatusToString( storeMsgStatus ) );
    }

    result = true;
    return result;
  } // unsubscribe()

  public boolean autoSend( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs )
      throws IOException {
    boolean result = false;

    // is the next step auto send ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "AUTO_SEND" ) ) ) {
      log.debug( headerLog + "Trigger as auto send message , with next type = "
          + nextType );
    } else {
      log.debug( headerLog + "This message is not auto send step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing auto send step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process auto send step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessAutoSend.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform auto send step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed auto send step" );
    result = true;
    return result;
  } // autoSend()

  public boolean wapPush( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean )
      throws IOException {
    boolean result = false;

    // is the next step wappush ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "WAP_PUSH" ) ) ) {
      log.debug( headerLog + "Trigger as wappush message , with next type = "
          + nextType );
    } else {
      log.debug( headerLog + "This message is not wappush step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing wappush step" );

    // execute
    if ( !EventProcessWappush.process( headerLog , this , log , transObj ,
        processBean ) ) {
      log.warning( headerLog + "Failed to perform wappush step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed wappush step" );

    result = true;
    return result;
  } // wapPush()

  public boolean createQrImage( String headerLog ,
      TransactionProcessBasic trans , TransactionQueueBean transObj ,
      ProcessBean processBean , ProcessBean processSteps[] ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step create qr image ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "CREATE_QR_IMAGE" ) ) ) {
      log.debug( headerLog + "Trigger as create qr image message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not create qr image step "
          + ", continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing create qr image step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process create qr image step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessCreateQrImage.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform create qr image step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed create qr image step" );
    result = true;
    return result;
  }

  public boolean webhook( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    // is the next step web hook ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "WEBHOOK" ) ) ) {
      log.debug( headerLog + "Trigger as webhook message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not webhook step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing webhook step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process webhook step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessWebhook.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg , omsgs ) ) {
      log.warning( headerLog + "Failed to perform webhook step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed webhook step" );
    result = true;
    return result;
  }

  public boolean randomCode( String headerLog , TransactionProcessBasic trans ,
      TransactionQueueBean transObj , ProcessBean processBean ,
      ProcessBean processSteps[] , TransactionInputMessage imsg ) {
    boolean result = false;

    // is the next step random code ?
    String nextType = processBean.getNextType();
    if ( ( nextType != null ) && ( nextType.equals( "RANDOM_CODE" ) ) ) {
      log.debug( headerLog + "Trigger as random code message "
          + ", with next type = " + nextType );
    } else {
      log.debug( headerLog + "This message is not random code step , continue" );
      return result;
    }

    // log first
    log.debug( headerLog + "Performing random code step" );

    // get next process step
    ProcessBean nextProcessBean = processSteps[Integer.parseInt( processBean
        .getNextStep() ) - 1];
    if ( nextProcessBean == null ) {
      log.warning( headerLog + "Failed to process random code step "
          + ", found null next process bean" );
      return result;
    }

    // execute
    if ( !EventProcessRandomCode.process( headerLog , this , log , transObj ,
        nextProcessBean , imsg ) ) {
      log.warning( headerLog + "Failed to perform random code step" );
      return result;
    }

    // log succeed
    log.debug( headerLog + "Sucessfully performed random code step" );
    result = true;
    return result;
  }

  public String processReservedCode( String code , String phoneNumber ,
      String messageContent ) {
    String messageResponse = "";
    try {
      ReservedCodeBean reservedCode = new ReservedCodeBean();
      reservedCode.setCode( code );
      reservedCode.setPhone( phoneNumber );

      StringTokenizer st = new StringTokenizer( messageContent , " ," );

      st.nextToken(); // discard code
      String params[] = new String[st.countTokens()];
      for ( int i = 0 ; i < params.length ; i++ ) {
        params[i] = st.nextToken();
      }
      reservedCode.setParams( params );

      // execute the code
      ReservedCodeDispatcher reservedCodeDispatcher = new ReservedCodeDispatcher(
          reservedCode );

      messageResponse = reservedCodeDispatcher.getResponse();

    } catch ( Exception e ) {
      log.warning( "Failed to process reserved code , " + e );
    }
    return messageResponse;
  }

  public String processExchange( TransactionQueueBean transObj ,
      String messageContent , String originalProvider ) {

    String response = "";
    String message = messageContent;
    String params = transObj.getParams();

    try {
      /*-------------------------
        process X <xxx> <xxx>
      -------------------------*/
      if ( message.toUpperCase().startsWith( "X" ) && message.length() > 1 ) {
        ExchangeBean exchange = new ExchangeBean();
        exchange.setPhone( transObj.getPhone() );

        // extract parameters
        StringTokenizer st = new StringTokenizer( messageContent , " ," );
        st.nextToken(); // discard code
        String Xparams[] = new String[st.countTokens()];
        for ( int i = 0 ; i < Xparams.length ; i++ )
          Xparams[i] = st.nextToken();
        exchange.setParams( Xparams );

        // execute the exchange
        response = new ExchangeDispatcher( (int) transObj.getClientID() ,
            exchange , originalProvider ).getResponse();
      }

      /*-------------------------
        process MODE=E
      -------------------------*/
      else if ( params != null && params.indexOf( "MODE=E" ) != -1 ) {
        ExchangeBean exchange = new ExchangeBean();
        exchange.setPhone( transObj.getPhone() );
        exchange.setMessage( message );
        String paramArray[] = StringUtils.split( params , "," );
        for ( int i = 0 ; i < paramArray.length ; i++ ) {
          String param = paramArray[i];
          StrTok st = new StrTok( param , "=" );
          String paramName = st.nextTok();
          String paramValue = st.nextTok();
          if ( paramName.equals( "NICKNAME" ) )
            exchange.setNickname( paramValue );
          else if ( paramName.equals( "PASSWORD" ) )
            exchange.setPassword( paramValue );
        }
        response = new ExchangeSupport().updateMessage( exchange );
      }

      /*-------------------------
        else its the default "X Setup" event
      -------------------------*/
      else
        response = "default";

    } catch ( Exception e ) {
      log.warning( "Failed to process exchange , " + e );
    }

    // success
    return response;

  } // processExchange()

  public void closeDanglingTransactions( ServletContext context )
      throws IOException {

    // int age = 168; // hours in a week
    int age = 720; // hours in a month

    /*---------------------------
      setup date range
    ---------------------------*/
    Calendar c = new GregorianCalendar();
    c.setTime( new Date() );
    c.add( Calendar.HOUR_OF_DAY , -age );
    Date toDate = c.getTime();
    c.add( Calendar.YEAR , -1 );
    Date fromDate = c.getTime();

    /*---------------------------
      get all aging trans objects
    ---------------------------*/
    Vector transObjects = transQueueService.select( fromDate , toDate , null );

    /*---------------------------
      close transactions
    ---------------------------*/
    for ( int i = 0 ; i < transObjects.size() ; i++ ) {
      TransactionQueueBean transObj = (TransactionQueueBean) transObjects
          .elementAt( i );
      try {
        String phone = transObj.getPhone();
        if ( phone.length() == 8 ) {
          phone = "+65" + phone;
          new TransactionQueueDAO().updatePhone( transObj , phone );
          transObj.setPhone( phone );
        }
        transLogService.logTransaction( transObj ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );
        transQueueService.delete( transObj );
      } catch ( Exception e ) {
        context.log( "Closing dangling transactions: error: " + e.getMessage() );
      }
    }

  } // closeDanglingTransactions()

  public String jumpToEvent( String code , TransactionQueueBean transObj ,
      TransactionInputMessage imsg , String type , boolean simulation ) {
    String response = "";
    try {

      if ( StringUtils.isBlank( code ) ) {
        log.warning( "Failed perform jump to event , found blank code" );
        return response;
      }

      // set calling event id if "CALL" type
      long callingEventID = 0;
      if ( ( type != null ) && ( type.equals( "CALL" ) ) ) {
        callingEventID = transObj.getEventID();
        log.debug( "Set property calling event id = " + callingEventID );
      }

      // get event id from beepcode / beepid 's code
      long eventID = 0;
      long locationID = 0;
      {
        BeepcodeBean beepcode = new BeepcodeService().select( code );
        if ( beepcode != null ) {
          eventID = beepcode.getEventID();
          locationID = beepcode.getLocationID();
        } else {
          BeepIDBean beepIDBean = new BeepIDBean().select( code , true );
          if ( beepIDBean != null ) {
            eventID = beepIDBean.getEventID();
          }
        }
      }
      if ( eventID < 1 ) {
        log.warning( "Failed to find beep code and/or id bean "
            + ", based on code = " + code );
        return response;
      }

      // get event bean from event id
      EventBean jumpToEventBean = eventService.select( eventID );
      log.debug( "Define jumpto event : id = " + jumpToEventBean.getEventID()
          + " , name = " + jumpToEventBean.getEventName() + " , processType = "
          + jumpToEventBean.getProcessType() );

      // prepare for event proces step
      ProcessBean processBean = null;
      ProcessBean[] processSteps = eventSupport.extractProcessClob(
          jumpToEventBean , true );

      // update event ping count
      long pingCount = jumpToEventBean.getPingCount();
      pingCount = pingCount + 1;
      jumpToEventBean.setPingCount( pingCount );
      eventService.update( jumpToEventBean );
      log.debug( "Updated jumpto event ping count = " + pingCount );

      // get ping count response
      if ( jumpToEventBean.getProcessType() == EventBean.PING_COUNT_TYPE ) {
        for ( int i = 0 ; i < processSteps.length ; i++ ) {
          processBean = processSteps[i];
          StrTok st = new StrTok( processBean.getRfa() , ":" );
          int startRange = Integer.parseInt( st.nextTok() );
          String temp = st.nextTok();
          int endRange = ( temp.equals( "" ) ) ? 999999 : Integer
              .parseInt( temp );
          if ( pingCount >= startRange && pingCount <= endRange ) {
            if ( processBean.getType().equals( "JUMP TO" ) ) {
              String temp2[] = processBean.getNames();
              String eventName = temp2[0];
              EventBean _ev = eventService.select( eventName ,
                  transObj.getClientID() );
              String codes[] = StringUtils.split( _ev.getCodes() , "," );
              String code2 = codes[0];
              // do recursive
              return jumpToEvent( code2 , transObj , imsg , "" , simulation );
            } else {
              response = processBean.getResponse();
              processBean.setRfa( "" ); // don't show range
            }
            break;
          }
        }
      }

      // get tell a friend response
      else if ( jumpToEventBean.getProcessType() == EventBean.TELL_A_FRIEND_TYPE ) {
        transObj.setCallingEventID( callingEventID );
        response = tellAFriendEventService.tellAFriend( processSteps ,
            transObj , imsg , null );
        return response;
      }

      // check if step 2 is jumpto
      else if ( processSteps.length > 1 ) {
        processBean = processSteps[1];
        log.debug( "Set process bean from second process step" );
        if ( processBean.getType().equals( "JUMP TO" ) ) {
          // ReceiveBufferBean receiveRec = new ReceiveBufferBean();
          // receiveRec.setPhone( transObj.getPhone() );
          // receiveRec.setMessage( code );
          // receiveRec.setProvider( imsg.getOriginalProvider() );
          try {
            // comment for a while
            // new Transaction( receiveRec ).getResponse();
          } catch ( Exception e ) {
          }
          log.warning( "Failed to process jump event "
              + ", found jump to under second process step" );
          return response;
        }
        processBean = processSteps[0];
        log.debug( "Set process bean from first process step" );
        response = processBean.getResponse();
        log.debug( "Updated jumpto response : "
            + StringEscapeUtils.escapeJava( response ) );
      }

      // else get process step 1 response
      else {
        processBean = processSteps[0];
        log.debug( "Set process bean from first process step" );
        response = processBean.getResponse();
        log.debug( "Updated jumpto response : "
            + StringEscapeUtils.escapeJava( response ) );
      }

      // verify process bean
      if ( processBean == null ) {
        log.warning( "Failed to process jumpto event "
            + ", found null process bean" );
        return response;
      }

      // build response
      if ( response == null ) {
        response = "";
      }
      String rfa = processBean.getRfa();
      if ( rfa == null ) {
        rfa = "";
      }
      String cr = "";
      if ( ( response.length() > 0 ) && ( rfa.length() > 0 ) ) {
        cr = "\n";
      }
      response += cr + rfa;
      log.debug( "Composed jumpto response : "
          + StringEscapeUtils.escapeJava( response ) );

      // replace reserved variables
      response = EventTransQueueReservedVariables.replaceReservedVariables(
          log , response , transObj );
      response = EventOutboundReservedVariables.replaceReservedVariables( log ,
          response , imsg );
      log.debug( "Replaced reserved var for jumpto response : "
          + StringEscapeUtils.escapeJava( response ) );

      /*-------------------------
        set reminder
      -------------------------*/
      transObj.setEventID( eventID );
      // setReminder( transObj , processBean , simulation );
      // log.debug( "Update event reminder , with event id = " +
      // transObj.getEventID() );

      /*--------------------------
        log trans for ?MENU
      --------------------------*/
      if ( transObj.getCode().equals( "?MENU" ) ) {
        BeepcodeBean b = new BeepcodeService().select( "NAVIGATE" );
        transObj.setEventID( b.getEventID() );
        transObj.setClientID( b.getEventID() );
        transObj.setCode( b.getCode() );
        transLogService.logTransaction( transObj ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );
      }

      // close current session
      {
        TransactionQueueBean currentSession = transQueueService
            .select( transObj.getPhone() );
        if ( currentSession != null ) {
          transQueueService.delete( currentSession );
          transLogService.logTransaction( currentSession ,
              TransactionLogConstanta.CLOSED_REASON_DIFF_EVENT );
          log.debug( "Closed current session : eventId = "
              + currentSession.getEventID() + " , code = "
              + currentSession.getCode() + " , clientId = "
              + currentSession.getClientID() + " , reason = diff event" );
        }
      }

      // transfer VAR / PARAM
      String params = "";
      StrTok st = new StrTok( transObj.getParams() , "," );
      while ( true ) {
        String param = st.nextTok();
        if ( param.equals( "" ) ) {
          break;
        }
        if ( param.indexOf( "MENU" ) == -1 ) {
          params += param + ",";
        }
      }
      if ( params.endsWith( "," ) ) {
        params = params.substring( 0 , params.length() - 1 );
      }
      transObj.setParams( params );
      log.debug( "Set new params = " + params );

      // setup new session
      transObj.setCallingEventID( callingEventID );
      transObj.setEventID( eventID );
      transObj.setClientID( jumpToEventBean.getClientID() );
      transObj.setDateTm( new Date() );
      transObj.setNextStep( 2 );
      transObj.setJumpCount( transObj.getMessageCount() );
      transObj.setMessageCount( 1 );
      transObj.setCode( code );
      transObj.setLocationID( locationID );
      transQueueService.insert( transObj );
      log.debug( "Created a new session : eventId = " + transObj.getEventID()
          + " , code = " + transObj.getCode() + " , clientId = "
          + transObj.getClientID() + " , callingEventID = "
          + transObj.getCallingEventID() );

      // end transaction
      if ( "END".indexOf( processBean.getNextStep() ) == -1 ) {
        if ( "REMIND,EMAIL,SUBSCRIBE,UNSUBSCRIBE,RINGTONE".indexOf( processBean
            .getNextType() ) != -1 ) {
          processBean.setNextStep( processSteps[Integer.parseInt( processBean
              .getNextStep() ) - 1].getNextStep() );
        }
      }

      if ( "END".indexOf( processBean.getNextStep() ) != -1 ) {
        transLogService.logTransaction( transObj , simulation ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );
        if ( callingEventID != 0 ) {
          transObj.setEventID( callingEventID );
          transObj
              .setCode( ( StringUtils.split(
                  ( eventService.select( callingEventID ) ).getCodes() , "," ) )[0] );
          transObj.setCallingEventID( 0 );
          transObj.setMessageCount( 0 );
          transObj.setNextStep( 2 );
          transQueueService.update( transObj );
        } else {
          transQueueService.delete( transObj );
        }
        log.debug( "Found next step END , closed session : eventId = "
            + transObj.getEventID() + " , code = " + transObj.getCode()
            + " , clientId = " + transObj.getClientID() );
      }

      if ( jumpToEventBean.getProcessType() == EventBean.PING_COUNT_TYPE ) {
        transQueueService.delete( transObj );
        transLogService.logTransaction( transObj , simulation ,
            TransactionLogConstanta.CLOSED_REASON_NORMAL );
        log.debug( "Found process type as PING COUNT TYPE "
            + ", closed session : eventId = " + transObj.getEventID()
            + " , code = " + transObj.getCode() + " , clientId = "
            + transObj.getClientID() );
      }

    } catch ( Exception e ) {
      log.warning( "Failed perform jump to another event , " + e );
    }

    return response;
  }

  public boolean resolveOriginalAddressAndMask( TransactionOutputMessage omsg ) {
    return TransactionAddressSupport.resolveOriginalAddressAndMask( log , omsg );
  }

  public boolean resolveDestinationProviderAndMask(
      TransactionInputMessage imsg , TransactionOutputMessage omsg ) {
    return destinationProviderService.resolveDestinationProviderAndMask( imsg ,
        omsg );
  }

  public boolean resolveMessageType( TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      // read message properties
      int messageTypeCur = omsg.getMessageType();
      String messageContentCur = omsg.getMessageContent();

      // validate message content
      if ( StringUtils.isBlank( messageContentCur ) ) {
        log.warning( "Failed to resolve message type "
            + ", found blank message content" );
        return result;
      }

      // prepare default message properties
      int messageTypeNew = MessageType.UNKNOWN_TYPE;
      String messageContentNew = null;

      // resolving message type for default text only
      if ( messageTypeCur == MessageType.TEXT_TYPE ) {
        // validate for wappush message type
        if ( messageContentCur.startsWith( "WAP_PUSH=" ) ) {
          messageTypeNew = MessageType.WAPPUSH_TYPE;
          messageContentNew = messageContentCur
              .substring( "WAP_PUSH=".length() );
        } else {
          if ( TransactionUtil.isUnicodeMessage( messageContentCur ) ) {
            messageTypeNew = MessageType.UNICODE_TYPE;
          }
        }
      }

      // update new message format
      if ( messageTypeNew != MessageType.UNKNOWN_TYPE ) {
        log.debug( "Resolved new message type : "
            + MessageType.messageTypeToString( messageTypeCur ) + " -> "
            + MessageType.messageTypeToString( messageTypeNew ) );
        omsg.setMessageType( messageTypeNew );
      }

      // update new message content
      if ( messageContentNew != null ) {
        log.debug( "Resolved new message content : "
            + StringEscapeUtils.escapeJava( messageContentCur ) + " -> "
            + StringEscapeUtils.escapeJava( messageContentNew ) );
        omsg.setMessageContent( messageContentNew );
      }

      result = true;

    } catch ( Exception e ) {
      log.warning( "Failed to resolve message type , " + e );
    }
    return result;
  }

  public boolean resolveShortenerLink( TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      // read and verify message content
      String curMessageContent = omsg.getMessageContent();
      if ( StringUtils.isBlank( curMessageContent ) ) {
        log.warning( "Failed to resolve shortener link "
            + ", found blank message content" );
        return result;
      }

      // read and verify destination address
      String destinationAddress = omsg.getDestinationAddress();
      if ( StringUtils.isBlank( destinationAddress ) ) {
        log.warning( "Failed to resolve shortener link "
            + ", found blank destination address" );
        return result;
      }

      // prepared for gatewayLog to xipMe link id
      String gatewayXipmeId = omsg.getMessageId();
      log.debug( "Setup gatewayXipmeId as outbound message's id : "
          + gatewayXipmeId );

      // prepared mobile user bean
      MobileUserBean muBean = (MobileUserBean) omsg
          .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );

      // prepared channel log bean
      ChannelLogBean clBean = (ChannelLogBean) omsg
          .getMessageParam( TransactionMessageParam.HDR_CHANNEL_LOG_BEAN );

      // prepared xipme codes map
      Map xipmeCodesMap = (Map) omsg
          .getMessageParam( TransactionMessageParam.HDR_XIPME_CODES_MAP );

      // prepared client subscriber bean
      ClientSubscriberBean csBean = (ClientSubscriberBean) omsg
          .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN );

      // prepared client subscriber custom bean
      ClientSubscriberCustomBean cscBean = null;
      if ( ( cscBean == null ) && ( csBean != null ) ) {
        cscBean = csBean.getCsCustomBean();
      }
      if ( cscBean == null ) {
        cscBean = (ClientSubscriberCustomBean) omsg
            .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN );
      }

      // prepare event bean
      EventBean evBean = (EventBean) omsg
          .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );

      // prepared xipme clone params
      Map mapXipmeCloneParams = (Map) omsg
          .getMessageParam( TransactionMessageParam.HDR_XIPME_CLONE_PARAMS_MAP );

      // resolved as new message content
      String newMessageContent = null;
      if ( ( newMessageContent == null ) && ( xipmeCodesMap != null ) ) {
        // resolved with offline xipme codes
        log.debug( "Resolving shortener link from current buffer "
            + ", with : codes = " + xipmeCodesMap );
        newMessageContent = xipmeTranslator.resolveMessageContent(
            log.header() , curMessageContent , xipmeCodesMap );
        if ( newMessageContent == null ) {
          log.warning( "Failed to resolve shortener link from current buffer" );
        } else {
          gatewayXipmeId = XipmeSupport.readGatewayXipmeId( xipmeCodesMap );
        }
      }
      if ( newMessageContent == null ) {
        // resolved to online xipme server
        log.debug( "Resolving shortener link from xipme server "
            + ", with : destinationAddress = " + destinationAddress
            + " , gatewayXipmeId = " + gatewayXipmeId
            + " , mobileUserBean.id = "
            + ( muBean != null ? muBean.getId() : -1 )
            + " , channeLogBean.id = "
            + ( clBean != null ? clBean.getId() : -1 )
            + " , clientSubscriberBean.id = "
            + ( csBean != null ? csBean.getId() : -1 )
            + " , clientSubscriberCustomBean.id = "
            + ( cscBean != null ? cscBean.getId() : -1 ) + " , eventBean.id = "
            + ( evBean != null ? evBean.getEventID() : -1 )
            + " , mapXipmeCloneParams.size = "
            + ( mapXipmeCloneParams != null ? mapXipmeCloneParams.size() : -1 ) );
        newMessageContent = xipmeTranslator.resolveMessageContent(
            log.header() , curMessageContent , destinationAddress ,
            gatewayXipmeId , muBean , clBean , csBean , cscBean , evBean ,
            mapXipmeCloneParams );
      }
      // verify for final result
      if ( StringUtils.isBlank( newMessageContent ) ) {
        log.warning( "Failed to resolve shortener link "
            + ", found failed to resolve message thru xipme support" );
        return result;
      }

      // update new message content back into the outgoing message
      if ( newMessageContent.equals( omsg.getMessageContent() ) ) {
        log.warning( "Failed to resolve shortener link " );
        return result;
      }

      // update the gateway log to xipme link id back
      if ( gatewayXipmeId != null ) {
        omsg.addMessageParam( TransactionMessageParam.HDR_GATEWAY_XIPME_ID ,
            gatewayXipmeId );
        log.debug( "Added message param : "
            + TransactionMessageParam.HDR_GATEWAY_XIPME_ID + " = "
            + gatewayXipmeId );
      }

      log.debug( "Resolved shortener link , updated new message content : "
          + StringEscapeUtils.escapeJava( newMessageContent ) );
      omsg.setMessageContent( newMessageContent );

      // result as true
      result = true;

    } catch ( Exception e ) {
      log.warning( "Failed to resolve shortener link , " + e );
    }
    return result;
  }

  public int resolveMessageContent( TransactionOutputMessage omsg ) {
    int totalMessageSend = 0;

    // prepare for message parameters
    int messageTypeInt = omsg.getMessageType();
    String messageContent = omsg.getMessageContent();
    String providerId = omsg.getDestinationProvider();

    // validation for message content
    if ( StringUtils.isBlank( messageContent ) ) {
      log.warning( "Failed to resolve message content , found empty" );
      return totalMessageSend;
    }

    // trim message content
    messageContent = messageContent.trim();

    // read message type string
    String messageTypeStr = MessageType.messageTypeToString( messageTypeInt );

    // log first
    log.debug( "Resolving message content , with : destinationProviderId = "
        + providerId + " , messageType = " + messageTypeStr
        + " , messageContent = "
        + StringEscapeUtils.escapeJava( messageContent ) );

    // calculate total message send based on message type
    switch ( messageTypeInt ) {
    case MessageType.TEXT_TYPE :
    case MessageType.RINGTONE_TYPE :
    case MessageType.PICTURE_TYPE :
    case MessageType.UNICODE_TYPE :
      totalMessageSend = TransactionUtil.calculateTotalSms( messageTypeInt ,
          messageContent );
      break;
    case MessageType.WAPPUSH_TYPE :
      totalMessageSend = StringUtils.split( messageContent ,
          WapPush.DELIMITER_PDU_PDU ).length;
      break;
    case MessageType.MMS_TYPE :
      totalMessageSend = 1;
      log.debug( "All the mms message type will calculated as one message" );
      break;
    case MessageType.QRPNG_TYPE :
    case MessageType.QRGIF_TYPE :
    case MessageType.QRJPG_TYPE :
      totalMessageSend = 1;
      log.debug( "All the qr message type will calculated as one message" );
      break;
    case MessageType.WEBHOOK_TYPE :
      totalMessageSend = 1;
      log.debug( "All the webhook message type will calculated as one message" );
      break;
    }

    // split sms message type based on provider ?
    switch ( messageTypeInt ) {
    case MessageType.TEXT_TYPE :
    case MessageType.RINGTONE_TYPE :
    case MessageType.PICTURE_TYPE :
    case MessageType.UNICODE_TYPE :
    case MessageType.WAPPUSH_TYPE :
      String featureValue = ProviderFeatureSupport.getProviderFeatureValue(
          conf.getProviderFeatures() ,
          ProviderFeatureName.SplitLongSmsMessageByProvider , providerId );
      log.debug( "Read feature "
          + ProviderFeatureName.SplitLongSmsMessageByProvider + " = "
          + featureValue + " , but nothing to do yet with this one ." );
      break;
    }

    // re-update message count and content
    omsg.setMessageCount( totalMessageSend );
    omsg.setMessageContent( messageContent );
    log.debug( "Resolved total message will send , count = "
        + omsg.getMessageCount() + " msg(s)" );

    return totalMessageSend;
  }

  public boolean insertOutgoingMessageToSendBuffer(
      TransactionOutputMessage omsg ) {
    boolean result = false;

    if ( omsg == null ) {
      log.warning( "Failed to insert an outgoing message "
          + "into send buffer , found null message" );
      return result;
    }

    // set date send
    Date dateSend = DateTimeFormat.convertToDate( (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_SET_SENDDATESTR ) );
    if ( dateSend != null ) {
      log.debug( "Set date to send message : "
          + DateTimeFormat.convertToString( dateSend ) );
    }

    // re-compose related params for send buffer
    Map params = new HashMap();
    String oriProvider = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_ORI_PROVIDER );
    if ( !StringUtils.isBlank( oriProvider ) ) {
      params.put( TransactionMessageParam.HDR_ORI_PROVIDER , oriProvider );
    }
    Boolean bypassMtDebit = (Boolean) omsg
        .getMessageParam( TransactionMessageParam.HDR_BYPASS_MT_DEBIT );
    if ( bypassMtDebit != null ) {
      params.put( TransactionMessageParam.HDR_BYPASS_MT_DEBIT , bypassMtDebit );
    }
    Boolean bypassSendProvider = (Boolean) omsg
        .getMessageParam( TransactionMessageParam.HDR_BYPASS_SEND_PROVIDER );
    if ( bypassSendProvider != null ) {
      params.put( TransactionMessageParam.HDR_BYPASS_SEND_PROVIDER ,
          bypassSendProvider );
    }
    Boolean bypassGatewayLog = (Boolean) omsg
        .getMessageParam( TransactionMessageParam.HDR_BYPASS_GATEWAY_LOG );
    if ( bypassGatewayLog != null ) {
      params.put( TransactionMessageParam.HDR_BYPASS_GATEWAY_LOG ,
          bypassGatewayLog );
    }
    String gatewayXipmeId = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_GATEWAY_XIPME_ID );
    if ( gatewayXipmeId != null ) {
      params
          .put( TransactionMessageParam.HDR_GATEWAY_XIPME_ID , gatewayXipmeId );
    }
    String qrImageFileName = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_QR_IMAGE_FILE_NAME );
    if ( qrImageFileName != null ) {
      params.put( TransactionMessageParam.HDR_QR_IMAGE_FILE_NAME ,
          qrImageFileName );
    }
    String qrImageFileSize = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_QR_IMAGE_FILE_SIZE );
    if ( qrImageFileSize != null ) {
      params.put( TransactionMessageParam.HDR_QR_IMAGE_FILE_SIZE ,
          qrImageFileSize );
    }
    String webhookMethod = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_WEBHOOK_METHOD );
    if ( webhookMethod != null ) {
      params.put( TransactionMessageParam.HDR_WEBHOOK_METHOD , webhookMethod );
    }
    String webhookUri = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_WEBHOOK_URI );
    if ( webhookUri != null ) {
      params.put( TransactionMessageParam.HDR_WEBHOOK_URI , webhookUri );
    }

    if ( params.size() > 0 ) {
      log.debug( "Prepared total " + params.size() + " param(s) "
          + "from outgoing message before go to send buffer : " + params );
    }

    // send mt message thru router app
    if ( !routerApp.sendMtMessage( omsg.getMessageId() , omsg.getMessageType() ,
        omsg.getMessageCount() , omsg.getMessageContent() ,
        omsg.getDebitAmount() , omsg.getDestinationAddress() ,
        omsg.getDestinationProvider() , omsg.getEventId() ,
        omsg.getChannelSessionId() , omsg.getOriginalMaskingAddress() ,
        omsg.getPriority() , 0 , dateSend , params ) ) {
      log.warning( "Failed to insert an outgoing message into send buffer "
          + ", found failed send mt message thru router app" );
      return result;
    }

    log.debug( "Successfully inserted an outgoing message into send buffer" );
    result = true;
    return result;
  }

  public boolean insertOutgoingMessageToReminder(
      TransactionOutputMessage omsg , String reminderType , Date remindDateSend ) {
    boolean result = false;

    // validate params

    if ( omsg == null ) {
      log.warning( "Failed to insert omsg into reminder "
          + ", found null omsg " );
      return result;
    }

    if ( remindDateSend == null ) {
      log.warning( "Failed to insert omsg into reminder "
          + ", found null remind date send" );
      return result;
    }

    // prepare reminder app

    ReminderApp reminderApp = ReminderApp.getInstance();
    if ( reminderApp == null ) {
      log.warning( "Failed to insert omsg into reminder "
          + ", found reminder app is not ready yet" );
      return result;
    }

    try {

      // store reminder based on type

      if ( StringUtils.equals( reminderType , "REMINDER" ) ) {
        if ( !reminderApp.addReminder( omsg.getEventId() ,
            omsg.getChannelSessionId() , omsg.getDestinationProvider() ,
            omsg.getOriginalMaskingAddress() , omsg.getDestinationAddress() ,
            omsg.getMessageId() , omsg.getMessageType() ,
            omsg.getMessageCount() , omsg.getMessageContent() ,
            omsg.getDebitAmount() , omsg.getPriority() , remindDateSend ) ) {
          log.warning( "Failed to insert omsg into reminder table" );
          return result;
        }
      }

      if ( StringUtils.equals( reminderType , "REMINDER RSVP PENDING" )
          || StringUtils.equals( reminderType , "REMINDER-RSVP PENDING" ) ) {
        if ( !reminderApp.addReminderWithSubscribeList( omsg.getEventId() ,
            omsg.getChannelSessionId() , omsg.getDestinationProvider() ,
            omsg.getOriginalMaskingAddress() , omsg.getDestinationAddress() ,
            omsg.getMessageId() , omsg.getMessageType() ,
            omsg.getMessageCount() , omsg.getMessageContent() ,
            omsg.getDebitAmount() , omsg.getPriority() , remindDateSend ) ) {
          log.warning( "Failed to insert omsg into reminder table "
              + "with subscribe into the list" );
          return result;
        }
      }

      if ( StringUtils.equals( reminderType , "REMINDER RSVP YES" )
          || StringUtils.equals( reminderType , "REMINDER-RSVP YES" ) ) {
        if ( !reminderApp.addReminderWithRsvpList( omsg.getEventId() ,
            omsg.getChannelSessionId() , omsg.getDestinationProvider() ,
            omsg.getOriginalMaskingAddress() , omsg.getDestinationAddress() ,
            omsg.getMessageId() , omsg.getMessageType() ,
            omsg.getMessageCount() , omsg.getMessageContent() ,
            omsg.getDebitAmount() , omsg.getPriority() , remindDateSend , true ) ) {
          log.warning( "Failed to insert omsg into reminder table "
              + "with update rsvp yes into the list" );
          return result;
        }
      }

      log.debug( "Successfully stored a reminder message : eventId = "
          + omsg.getEventId() + " , channelSessionId = "
          + omsg.getChannelSessionId() + " , destinationProvider = "
          + omsg.getDestinationProvider() + " , originalMaskingAddress = "
          + omsg.getOriginalMaskingAddress() + " , destinationAddress = "
          + omsg.getDestinationAddress() + " , messageType = "
          + omsg.getMessageType() + " , messageCount = "
          + omsg.getMessageCount() + " , debitAmount = "
          + omsg.getDebitAmount() + " , priority = " + omsg.getPriority()
          + " , reminderType = " + reminderType );

    } catch ( Exception e ) {
      log.warning( "Failed to insert omsg into reminder table , " + e );
    }

    result = true;
    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

}
