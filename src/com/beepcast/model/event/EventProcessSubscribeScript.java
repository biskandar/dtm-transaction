package com.beepcast.model.event;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.script.ScriptSubscribeListData;
import com.beepcast.model.transaction.script.ScriptSubscribeListDataFactory;
import com.beepcast.model.transaction.script.ScriptSubscribeListDataMigration;
import com.beepcast.model.transaction.script.ScriptSubscribeListExec;
import com.beepcast.subscriber.ClientSubscriberBean;
import com.beepcast.subscriber.ClientSubscriberCustomBean;
import com.beepcast.subscriber.SubscriberApp;
import com.beepcast.subscriber.SubscriberGroupBean;

public class EventProcessSubscribeScript {

  public static boolean execute( String headerLog , TransactionLog log ,
      int clientId , SubscriberGroupBean sgBean , ClientSubscriberBean csBean ,
      ClientSubscriberCustomBean csCustomBean , String phoneNumber ,
      int fromEventId , String scriptSource ) {
    boolean result = false;
    try {

      // prepare to allow duplicated entry ?
      boolean duplicated = false;

      // prepare the array of custom fields
      String[] arrCustoms = null;

      // execute the script if found any
      if ( !StringUtils.isBlank( scriptSource ) ) {
        ScriptSubscribeListExec exec = new ScriptSubscribeListExec( headerLog ,
            scriptSource );
        ScriptSubscribeListData data = ScriptSubscribeListDataFactory
            .createScriptSubscribeListData( false , csCustomBean );
        if ( exec.execute( data ) ) {
          // allow duplicated record ?
          duplicated = data.isInsert();
          // copy data to csCustomBean
          csCustomBean = ScriptSubscribeListDataMigration.copy( data ,
              csCustomBean );
          // convert csCustomBean to array of string customs
          arrCustoms = convertToArrayOfStrings( csCustomBean );
        }
      }

      // prepare the subscriber app
      SubscriberApp subscriberApp = SubscriberApp.getInstance();

      // subscribe phone number to the list
      if ( !subscriberApp.doSubscribed( clientId , sgBean.getId() ,
          phoneNumber , true , duplicated , fromEventId , null , null ,
          arrCustoms ) ) {
        log.warning( "Failed to subscribe phone number into the list" );
        return result;
      }

      // return as true
      result = true;

    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to execute event "
          + "process subscribe script , " + e );
    }
    return result;
  }

  private static String[] convertToArrayOfStrings(
      ClientSubscriberCustomBean csCustomBean ) {
    String[] arrCustoms = null;

    if ( csCustomBean == null ) {
      return arrCustoms;
    }

    arrCustoms = new String[31];

    arrCustoms[0] = csCustomBean.getCustom0();
    arrCustoms[1] = csCustomBean.getCustom1();
    arrCustoms[2] = csCustomBean.getCustom2();
    arrCustoms[3] = csCustomBean.getCustom3();
    arrCustoms[4] = csCustomBean.getCustom4();
    arrCustoms[5] = csCustomBean.getCustom5();
    arrCustoms[6] = csCustomBean.getCustom6();
    arrCustoms[7] = csCustomBean.getCustom7();
    arrCustoms[8] = csCustomBean.getCustom8();
    arrCustoms[9] = csCustomBean.getCustom9();

    arrCustoms[10] = csCustomBean.getCustom10();
    arrCustoms[11] = csCustomBean.getCustom11();
    arrCustoms[12] = csCustomBean.getCustom12();
    arrCustoms[13] = csCustomBean.getCustom13();
    arrCustoms[14] = csCustomBean.getCustom14();
    arrCustoms[15] = csCustomBean.getCustom15();
    arrCustoms[16] = csCustomBean.getCustom16();
    arrCustoms[17] = csCustomBean.getCustom17();
    arrCustoms[18] = csCustomBean.getCustom18();
    arrCustoms[19] = csCustomBean.getCustom19();

    arrCustoms[20] = csCustomBean.getCustom20();
    arrCustoms[21] = csCustomBean.getCustom21();
    arrCustoms[22] = csCustomBean.getCustom22();
    arrCustoms[23] = csCustomBean.getCustom23();
    arrCustoms[24] = csCustomBean.getCustom24();
    arrCustoms[25] = csCustomBean.getCustom25();
    arrCustoms[26] = csCustomBean.getCustom26();
    arrCustoms[27] = csCustomBean.getCustom27();
    arrCustoms[28] = csCustomBean.getCustom28();
    arrCustoms[29] = csCustomBean.getCustom29();

    arrCustoms[30] = csCustomBean.getCustom30();

    return arrCustoms;
  }

}
