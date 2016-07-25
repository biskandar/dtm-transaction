package com.beepcast.model.transaction.script;

import com.beepcast.subscriber.ClientSubscriberCustomBean;

public class ScriptSubscribeListDataFactory {

  public static ScriptSubscribeListData createScriptSubscribeListData(
      boolean insert , ClientSubscriberCustomBean csCustomBean ) {

    ScriptSubscribeListData data = createScriptSubscribeListData( insert );
    if ( csCustomBean == null ) {
      return data;
    }

    // create with custom fields
    data = createScriptSubscribeListData( insert , csCustomBean.getCustom0() ,
        csCustomBean.getCustom1() , csCustomBean.getCustom2() ,
        csCustomBean.getCustom3() , csCustomBean.getCustom4() ,
        csCustomBean.getCustom5() , csCustomBean.getCustom6() ,
        csCustomBean.getCustom7() , csCustomBean.getCustom8() ,
        csCustomBean.getCustom9() , csCustomBean.getCustom10() ,
        csCustomBean.getCustom11() , csCustomBean.getCustom12() ,
        csCustomBean.getCustom13() , csCustomBean.getCustom14() ,
        csCustomBean.getCustom15() , csCustomBean.getCustom16() ,
        csCustomBean.getCustom17() , csCustomBean.getCustom18() ,
        csCustomBean.getCustom19() , csCustomBean.getCustom20() ,
        csCustomBean.getCustom21() , csCustomBean.getCustom22() ,
        csCustomBean.getCustom23() , csCustomBean.getCustom24() ,
        csCustomBean.getCustom25() , csCustomBean.getCustom26() ,
        csCustomBean.getCustom27() , csCustomBean.getCustom28() ,
        csCustomBean.getCustom29() , csCustomBean.getCustom30() );

    return data;
  }

  public static ScriptSubscribeListData createScriptSubscribeListData(
      boolean insert , String custom0 , String custom1 , String custom2 ,
      String custom3 , String custom4 , String custom5 , String custom6 ,
      String custom7 , String custom8 , String custom9 , String custom10 ,
      String custom11 , String custom12 , String custom13 , String custom14 ,
      String custom15 , String custom16 , String custom17 , String custom18 ,
      String custom19 , String custom20 , String custom21 , String custom22 ,
      String custom23 , String custom24 , String custom25 , String custom26 ,
      String custom27 , String custom28 , String custom29 , String custom30 ) {

    ScriptSubscribeListData data = createScriptSubscribeListData( insert );

    // update all custom fields

    data.setCustom0( custom0 );
    data.setCustom1( custom1 );
    data.setCustom2( custom2 );
    data.setCustom3( custom3 );
    data.setCustom4( custom4 );
    data.setCustom5( custom5 );
    data.setCustom6( custom6 );
    data.setCustom7( custom7 );
    data.setCustom8( custom8 );
    data.setCustom9( custom9 );

    data.setCustom10( custom10 );
    data.setCustom11( custom11 );
    data.setCustom12( custom12 );
    data.setCustom13( custom13 );
    data.setCustom14( custom14 );
    data.setCustom15( custom15 );
    data.setCustom16( custom16 );
    data.setCustom17( custom17 );
    data.setCustom18( custom18 );
    data.setCustom19( custom19 );

    data.setCustom20( custom20 );
    data.setCustom21( custom21 );
    data.setCustom22( custom22 );
    data.setCustom23( custom23 );
    data.setCustom24( custom24 );
    data.setCustom25( custom25 );
    data.setCustom26( custom26 );
    data.setCustom27( custom27 );
    data.setCustom28( custom28 );
    data.setCustom29( custom29 );

    data.setCustom30( custom30 );

    return data;
  }

  public static ScriptSubscribeListData createScriptSubscribeListData(
      boolean insert ) {
    ScriptSubscribeListData data = new ScriptSubscribeListData();
    data.setInsert( insert );
    return data;
  }

}
