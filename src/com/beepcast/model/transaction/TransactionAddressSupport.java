package com.beepcast.model.transaction;

import com.beepcast.dbmanager.common.ClientSenderIdCommon;
import com.beepcast.dbmanager.table.TClientToSenderId;
import com.beepcast.model.event.EventBean;

public class TransactionAddressSupport {

  public static boolean resolveOriginalAddressAndMask( TransactionLog log ,
      TransactionOutputMessage omsg ) {
    boolean result = false;
    try {

      log.debug( "Trying to read event profile from the output message" );
      EventBean eventBean = (EventBean) omsg
          .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
      if ( eventBean == null ) {
        log.warning( "Failed to resolve original address and mask "
            + ", found empty event profile" );
        return result;
      }
      log.debug( "Successfully read event profile : id = "
          + eventBean.getEventID() + " , name = " + eventBean.getEventName()
          + " , outgoingNumber = " + eventBean.getOutgoingNumber()
          + " , senderId = " + eventBean.getSenderID() );

      resolveOriginalAddress( log , omsg , eventBean );

      resolveOriginalMaskAddress( log , omsg , eventBean );

      result = true;
    } catch ( Exception e ) {
      log.warning( "Failed to resolve original address and mask , " + e );
    }
    return result;
  }

  public static boolean resolveOriginalAddress( TransactionLog log ,
      TransactionOutputMessage omsg , EventBean eventBean ) {
    boolean result = false;

    log.debug( "Trying to update message's original address" );

    String originalAddress = eventBean.getOutgoingNumber();
    if ( ( originalAddress == null ) || ( originalAddress.equals( "" ) ) ) {
      log.warning( "Failed to resolve original address and mask "
          + ", found empty event's outgoing number" );
      return result;
    }
    log.debug( "Updated output message's original address : "
        + omsg.getOriginalAddress() + " -> " + originalAddress );
    omsg.setOriginalAddress( originalAddress );

    result = true;
    return result;
  }

  public static boolean resolveOriginalMaskAddress( TransactionLog log ,
      TransactionOutputMessage omsg , EventBean eventBean ) {
    boolean result = false;

    log.debug( "Trying to update message's original mask address" );

    String originalMaskingAddress = eventBean.getSenderID();
    if ( ( originalMaskingAddress == null )
        || originalMaskingAddress.equals( "" )
        || originalMaskingAddress.equals( "*" ) ) {
      log.warning( "Found empty event's sender id" );
      originalMaskingAddress = null;
    }
    log.debug( "Updated output message's original mask address : "
        + omsg.getOriginalMaskingAddress() + " -> " + originalMaskingAddress );
    omsg.setOriginalMaskingAddress( originalMaskingAddress );

    String msgPrmSetOriMskAdr = (String) omsg
        .getMessageParam( TransactionMessageParam.HDR_SET_ORIMASKADDR );
    if ( ( msgPrmSetOriMskAdr != null ) && ( !msgPrmSetOriMskAdr.equals( "" ) ) ) {
      log.debug( "Found output message's param set ori mask address : "
          + msgPrmSetOriMskAdr + " , trying to validate and apply on "
          + "the output message" );
      if ( verifyOriMaskAddress( (int) eventBean.getClientID() ,
          eventBean.getOutgoingNumber() , msgPrmSetOriMskAdr ) ) {
        log.debug( "Updated output message's original mask address : "
            + omsg.getOriginalMaskingAddress() + " -> " + msgPrmSetOriMskAdr );
        omsg.setOriginalMaskingAddress( msgPrmSetOriMskAdr );
      } else {
        log.warning( "Failed to update output message's original "
            + "mask address , found as invalid mask = " + msgPrmSetOriMskAdr );
      }
    }

    result = true;
    return result;
  }

  public static boolean verifyOriMaskAddress( int clientId , String oriAddress ,
      String oriMaskAddress ) {
    boolean result = false;

    if ( oriMaskAddress == null ) {
      return result;
    }

    TClientToSenderId clientToSenderIdOut = null;

    clientToSenderIdOut = ClientSenderIdCommon.getClientSenderId( clientId ,
        oriAddress , oriMaskAddress );
    if ( clientToSenderIdOut != null ) {
      result = true;
      return result;
    }

    clientToSenderIdOut = ClientSenderIdCommon.getClientSenderId( clientId ,
        oriAddress , "*" );
    if ( clientToSenderIdOut != null ) {
      result = true;
      return result;
    }

    clientToSenderIdOut = ClientSenderIdCommon.getClientSenderId( clientId ,
        "*" , oriMaskAddress );
    if ( clientToSenderIdOut != null ) {
      result = true;
      return result;
    }

    clientToSenderIdOut = ClientSenderIdCommon.getClientSenderId( clientId ,
        "*" , "*" );
    if ( clientToSenderIdOut != null ) {
      result = true;
      return result;
    }

    return result;
  }

}
