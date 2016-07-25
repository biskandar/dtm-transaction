package com.beepcast.model.transaction;

import java.util.Date;

public class TransactionLogBeanFactory {

  public static TransactionLogBean createTransactionLogBean( long clientID ,
      long eventID , int nextStep , long catagoryID , Date dateTm ,
      String phone , String providerId , int messageCount , String code ,
      String params , int jumpCount , long locationID , int closedReasonId ) {
    TransactionLogBean bean = new TransactionLogBean();
    bean.setClientID( clientID );
    bean.setEventID( eventID );
    bean.setNextStep( nextStep );
    bean.setCatagoryID( catagoryID );
    bean.setDateTm( dateTm );
    bean.setPhone( phone );
    bean.setProviderId( providerId );
    bean.setMessageCount( messageCount );
    bean.setCode( code );
    bean.setParams( params );
    bean.setJumpCount( jumpCount );
    bean.setLocationID( locationID );
    bean.setClosedReasonId( closedReasonId );
    return bean;
  }

}
