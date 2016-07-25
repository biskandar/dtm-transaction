package com.beepcast.model.transaction.script;

import com.beepcast.subscriber.ClientSubscriberCustomBean;

public class ScriptSubscribeListDataMigration {

  public static ClientSubscriberCustomBean copy( ScriptSubscribeListData data ,
      ClientSubscriberCustomBean csCustomBean ) {
    ClientSubscriberCustomBean csCustomBeanResult = csCustomBean;

    // persist the result

    if ( csCustomBeanResult == null ) {
      csCustomBeanResult = new ClientSubscriberCustomBean();
    }

    // validate must be params

    if ( data == null ) {
      return csCustomBeanResult;
    }

    // copy data params

    csCustomBeanResult.setCustom0( data.getCustom0() );
    csCustomBeanResult.setCustom1( data.getCustom1() );
    csCustomBeanResult.setCustom2( data.getCustom2() );
    csCustomBeanResult.setCustom3( data.getCustom3() );
    csCustomBeanResult.setCustom4( data.getCustom4() );
    csCustomBeanResult.setCustom5( data.getCustom5() );
    csCustomBeanResult.setCustom6( data.getCustom6() );
    csCustomBeanResult.setCustom7( data.getCustom7() );
    csCustomBeanResult.setCustom8( data.getCustom8() );
    csCustomBeanResult.setCustom9( data.getCustom9() );

    csCustomBeanResult.setCustom10( data.getCustom10() );
    csCustomBeanResult.setCustom11( data.getCustom11() );
    csCustomBeanResult.setCustom12( data.getCustom12() );
    csCustomBeanResult.setCustom13( data.getCustom13() );
    csCustomBeanResult.setCustom14( data.getCustom14() );
    csCustomBeanResult.setCustom15( data.getCustom15() );
    csCustomBeanResult.setCustom16( data.getCustom16() );
    csCustomBeanResult.setCustom17( data.getCustom17() );
    csCustomBeanResult.setCustom18( data.getCustom18() );
    csCustomBeanResult.setCustom19( data.getCustom19() );

    csCustomBeanResult.setCustom20( data.getCustom20() );
    csCustomBeanResult.setCustom21( data.getCustom21() );
    csCustomBeanResult.setCustom22( data.getCustom22() );
    csCustomBeanResult.setCustom23( data.getCustom23() );
    csCustomBeanResult.setCustom24( data.getCustom24() );
    csCustomBeanResult.setCustom25( data.getCustom25() );
    csCustomBeanResult.setCustom26( data.getCustom26() );
    csCustomBeanResult.setCustom27( data.getCustom27() );
    csCustomBeanResult.setCustom28( data.getCustom28() );
    csCustomBeanResult.setCustom29( data.getCustom29() );

    csCustomBeanResult.setCustom30( data.getCustom30() );

    return csCustomBeanResult;
  }

}
