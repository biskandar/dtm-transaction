package com.beepcast.model.transaction.alert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class AlertClientLowBalanceService {

  static final DLogContext lctx = new SimpleContext(
      "AlertClientLowBalanceService" );

  private Map alertClientLowBalances;

  public AlertClientLowBalanceService( Map alertClientLowBalances ) {
    this.alertClientLowBalances = alertClientLowBalances;
  }

  public boolean isValid() {
    return ( alertClientLowBalances != null );
  }

  public boolean addNewAlert( AlertClientLowBalanceBean bean ) {
    boolean result = false;

    if ( bean == null ) {
      DLog.warning( lctx , "Failed to add new alert "
          + ", found null alert client low balance bean" );
      return result;
    }

    String alertId = bean.getAlertId();
    if ( StringUtils.isBlank( alertId ) ) {
      DLog.warning( lctx , "Failed to add new alert "
          + ", found empty alert id" );
      return result;
    }

    if ( alertClientLowBalances == null ) {
      DLog.warning( lctx , "Failed to add new alert "
          + ", found null alert client low balance map" );
      return result;
    }

    alertClientLowBalances.put( alertId , bean );

    return result;
  }

  public List listAlertIds() {
    List list = null;

    if ( alertClientLowBalances == null ) {
      DLog.warning( lctx , "Failed to get list alert id "
          + ", found null alert client low balance map" );
      return list;
    }

    list = new ArrayList( alertClientLowBalances.keySet() );

    return list;
  }

  public AlertClientLowBalanceBean queryAlert( String alertId ) {
    AlertClientLowBalanceBean bean = null;

    if ( StringUtils.isBlank( alertId ) ) {
      DLog.warning( lctx , "Failed to query an alert "
          + ", found empty alert id" );
      return bean;
    }

    if ( alertClientLowBalances == null ) {
      DLog.warning( lctx , "Failed to query an alert "
          + ", found null alert client low balance map" );
      return bean;
    }

    bean = (AlertClientLowBalanceBean) alertClientLowBalances.get( alertId );

    return bean;
  }

  public AlertClientLowBalanceBean searchAlert( double balanceUnitBefore ,
      double balanceUnitAfter ) {
    AlertClientLowBalanceBean bean = null;
    List listAlertIds = listAlertIds();
    if ( listAlertIds == null ) {
      DLog.warning( lctx , "Failed to search alert "
          + ", found null list alert id" );
      return bean;
    }
    String alertId;
    AlertClientLowBalanceBean tbean;
    Iterator iterAlertIds = listAlertIds.iterator();
    while ( iterAlertIds.hasNext() ) {
      alertId = (String) iterAlertIds.next();
      if ( StringUtils.isBlank( alertId ) ) {
        continue;
      }
      tbean = queryAlert( alertId );
      if ( tbean == null ) {
        continue;
      }
      if ( !AlertClientLowBalanceSupport.isBalanceHitThreshold(
          tbean.getThresholdUnit() , balanceUnitBefore , balanceUnitAfter ) ) {
        continue;
      }
      bean = tbean;
      break;
    }
    return bean;
  }

}
