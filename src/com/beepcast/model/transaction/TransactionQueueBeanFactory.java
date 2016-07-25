package com.beepcast.model.transaction;

import java.util.Date;

public class TransactionQueueBeanFactory {

  public static TransactionQueueBean createTransactionQueueBean( long eventID ,
      long clientID , long pendingEventID , Date dateTm , String phone ,
      String providerId , int nextStep , int messageCount , String code ,
      String pendingCode , String params , boolean updateProfile ,
      boolean newUser , int jumpCount , long locationID , long callingEventID ) {
    TransactionQueueBean bean = new TransactionQueueBean();
    bean.setEventID( eventID );
    bean.setClientID( clientID );
    bean.setPendingEventID( pendingEventID );
    bean.setDateTm( dateTm );
    bean.setPhone( phone );
    bean.setProviderId( providerId );
    bean.setNextStep( nextStep );
    bean.setMessageCount( messageCount );
    bean.setCode( code );
    bean.setPendingCode( pendingCode );
    bean.setParams( params );
    bean.setUpdateProfile( updateProfile );
    bean.setNewUser( newUser );
    bean.setJumpCount( jumpCount );
    bean.setLocationID( locationID );
    bean.setCallingEventID( callingEventID );
    return bean;
  }

}
