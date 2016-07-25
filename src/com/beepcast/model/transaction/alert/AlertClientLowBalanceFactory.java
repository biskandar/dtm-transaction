package com.beepcast.model.transaction.alert;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class AlertClientLowBalanceFactory {

  static final DLogContext lctx = new SimpleContext(
      "AlertClientLowBalanceFactory" );

  public static AlertClientLowBalanceBean createAlertClientLowBalanceBean(
      String alertId , double thresholdUnit ) {
    AlertClientLowBalanceBean bean = null;

    if ( StringUtils.isBlank( alertId ) ) {
      DLog.warning( lctx , "Failed to create alert client "
          + "low balance bean , found null alert id" );
      return bean;
    }

    bean = new AlertClientLowBalanceBean();
    bean.setAlertId( alertId );
    bean.setThresholdUnit( thresholdUnit );

    DLog.debug( lctx ,
        "Created alert client low balance , id = " + bean.getAlertId()
            + " , threshold = " + bean.getThresholdUnit() + " unit(s)" );

    return bean;
  }

}
