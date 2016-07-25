package com.beepcast.model.event;

import com.beepcast.api.client.ClientApp;
import com.beepcast.model.transaction.TransactionConf;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

public class EventProcessSubscribeUtils {

  public static boolean subscribeClientApiMessage( String headerLog ,
      TransactionSupport support , TransactionConf conf , TransactionLog log ,
      TransactionQueueBean tqBean , TransactionInputMessage imsg ) {
    boolean result = false;

    // prepare client api message
    String messageId = imsg.getMessageId();
    int eventId = (int) tqBean.getEventID();
    String eventCode = null;
    String phoneNumber = tqBean.getPhone();
    String modemNumber = imsg.getDestinationAddress();
    boolean subscribeStatus = true;
    String channel = com.beepcast.api.client.data.Channel.MODEM;
    String messageContent = imsg.getMessageContent();

    // prepare client api app
    ClientApp clientApp = ClientApp.getInstance();

    // store as subscription message
    int storeMsgStatus = clientApp.storeSubscriptionMessage( messageId ,
        (int) eventId , eventCode , phoneNumber , modemNumber ,
        subscribeStatus , channel , messageContent );
    log.debug( headerLog + "Store client api's subscription message "
        + "to client app , result = "
        + clientApp.storeMsgStatusToString( storeMsgStatus ) );

    return result;
  }

}
