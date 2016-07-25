package com.beepcast.model.transaction.provider;

import java.util.List;

import com.beepcast.dbmanager.table.TCountry;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.MobileUserService;
import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.Node;
import com.beepcast.model.transaction.TransactionCountryUtil;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionOutputMessage;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionProcessFactory;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class DestinationProviderSimulation {

  static final DLogContext lctx = new SimpleContext(
      "DestinationProviderSimulation" );

  public static TransactionOutputMessage execute( int eventId ,
      String phoneNumber ) {
    return execute( eventId , phoneNumber , null );
  }

  public static TransactionOutputMessage execute( int eventId ,
      String phoneNumber , List listExcludeProviderIds ) {
    TransactionOutputMessage omsg = null;

    try {

      // log it
      DLog.debug( lctx , "Executed with : eventId = " + eventId
          + " , phoneNumber = " + phoneNumber );

      // clean phone number
      if ( ( phoneNumber != null ) && ( !phoneNumber.startsWith( "+" ) ) ) {
        phoneNumber = "+".concat( phoneNumber );
      }

      // prepare default parameters for transaction outgoing message
      String correlationId = "TST0001";
      String messageProfile = "SMS.MT";
      String messageStatusCode = "OK";
      String messageStatusDescription = "OK";
      String messageId = "TST0002";
      int messageType = MessageType.TEXT_TYPE;
      String messageContent = "";
      String originalNode = Node.DTM;
      String originalAddress = "";
      String originalMaskingAddress = "";
      String originalProvider = "";
      String destinationNode = Node.DTM;
      String destinationAddress = phoneNumber;
      String destinationProvider = "";
      int clientId = 0;
      int channelSessionId = 0;
      int priority = 0;

      // build result as transaction outgoing message
      omsg = TransactionMessageFactory.createOutputMessage( correlationId ,
          messageProfile , messageStatusCode , messageStatusDescription ,
          messageId , messageType , messageContent , originalNode ,
          originalAddress , originalMaskingAddress , originalProvider ,
          destinationNode , destinationAddress , destinationProvider ,
          clientId , eventId , channelSessionId , priority );

      // setup event bean
      EventBean eventBean = null;
      if ( eventId > 0 ) {
        EventService eventService = new EventService();
        eventBean = eventService.select( eventId );
      }
      if ( eventBean != null ) {
        omsg.addMessageParam( TransactionMessageParam.HDR_EVENT_BEAN ,
            eventBean );
        DLog.debug( lctx , "Resolved event : id = " + eventBean.getEventID()
            + " , name = " + eventBean.getEventName() );

        // resolve original address
        originalAddress = eventBean.getOutgoingNumber();
        omsg.setOriginalAddress( originalAddress );
        DLog.debug( lctx ,
            "Resolved original address : " + omsg.getOriginalAddress() );

        // resolve client id
        DLog.debug( lctx , "Resolved client : id = " + clientId + " -> "
            + eventBean.getClientID() );
        clientId = (int) eventBean.getClientID();
        omsg.setClientId( clientId );

      }

      // setup client bean
      ClientBean clientBean = null;
      if ( clientId > 0 ) {
        ClientService clientService = new ClientService();
        clientBean = clientService.select( clientId );
      }
      if ( clientBean != null ) {
        omsg.addMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN ,
            clientBean );
        DLog.debug( lctx , "Resolved client : id = " + clientBean.getClientID()
            + " , companyName = " + clientBean.getCompanyName() );
      }

      // setup mobile user bean
      MobileUserService mobileUserService = new MobileUserService();
      MobileUserBean mobileUserBean = mobileUserService.select( clientId ,
          phoneNumber );
      if ( mobileUserBean != null ) {
        omsg.addMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN ,
            mobileUserBean );
        DLog.debug( lctx ,
            "Resolve mobileUser : id = " + mobileUserBean.getId()
                + " , telcoCodes = " + mobileUserBean.getMobileCcnc() );
      }

      // setup country bean
      TCountry countryBean = TransactionCountryUtil
          .getCountryBean( phoneNumber );
      if ( countryBean != null ) {
        omsg.addMessageParam( TransactionMessageParam.HDR_COUNTRY_BEAN ,
            countryBean );
        DLog.debug( lctx , "Resolved country : id = " + countryBean.getId()
            + " , name = " + countryBean.getName() );
      }

      // added list prohibit providerIds
      if ( ( listExcludeProviderIds != null )
          && ( listExcludeProviderIds.size() > 0 ) ) {
        omsg.addMessageParam(
            TransactionMessageParam.HDR_LIST_PROHIBIT_PROVIDER_IDS ,
            listExcludeProviderIds );
        DLog.debug( lctx , "Resolved list prohibit provider ids : "
            + listExcludeProviderIds );
      }

      // create transaction process standard
      TransactionProcessBasic transProcBasic = (TransactionProcessBasic) TransactionProcessFactory
          .generateTransactionProcessStandard( true );
      DLog.debug( lctx , "Composed transaction process standard" );

      // build destination provider service
      DestinationProviderService destProvService = new DestinationProviderService(
          transProcBasic );
      DLog.debug( lctx , "Composed destination provider service" );

      // resolve destination provider and mask
      if ( !destProvService.resolveDestinationProviderAndMask( null , omsg ) ) {
        DLog.warning( lctx , "Failed to resolve destination provider and mask" );
        return omsg;
      }

      // return as true
      DLog.debug( lctx , "Successfully resolved destination provider and mask" );

    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to resolve destination provider and mask , "
          + e );
    }
    return omsg;
  }
}
