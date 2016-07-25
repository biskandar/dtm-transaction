package com.beepcast.model.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beepcast.model.transaction.route.RouteOrder;
import com.beepcast.model.transaction.route.RouteProvider;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

import edu.emory.mathcs.backport.java.util.TreeMap;

public class TransactionConf {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionConf" );

  public static final String FIELD_SEPARATOR = "_";

  public static final String SESSION_PARAM_ID = "name";
  public static final String SESSION_PARAM_VALUE = "value";
  public static final String SESSION_PARAM_ID_LIMIT_RECORDS = "limitRecords";
  public static final String SESSION_PARAM_ID_LIMIT_DAYS = "limitDays";
  public static final String SESSION_PARAM_ID_EXPIRY_DAYS = "expiryDays";

  public static final String SCHEDULE_TASK_ID = "id";
  public static final String SCHEDULE_TASK_JAVACLS = "javacls";
  public static final String SCHEDULE_TASK_CRONEXP = "cronexp";

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  boolean debug;

  Map propertyProcessSteps;
  String defaultRouteOrder;
  Map routeOrders;
  Map routeProviders;
  Map sessionParams;
  Map providerFeatures;
  Map alertClientLowBalances;
  Map scheduleTasks;
  List scheduleTaskIds;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionConf() {
    init();
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public void init() {
    debug = false;
    propertyProcessSteps = new HashMap();
    defaultRouteOrder = null;
    routeOrders = new HashMap();
    routeProviders = new HashMap();
    sessionParams = new HashMap();
    providerFeatures = new HashMap();
    alertClientLowBalances = new TreeMap();
    scheduleTasks = new HashMap();
    scheduleTaskIds = new ArrayList();
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Set / Get Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public boolean isDebug() {
    return debug;
  }

  public void setDebug( boolean debug ) {
    this.debug = debug;
  }

  public Map getPropertyProcessSteps() {
    return propertyProcessSteps;
  }

  public String getProcessStepValue( String processStepName ,
      String processStepField ) {
    String key = processStepName + FIELD_SEPARATOR + processStepField;

    return (String) propertyProcessSteps.get( key );
  }

  public String getDefaultRouteOrder() {
    return defaultRouteOrder;
  }

  public void setDefaultRouteOrder( String defaultRouteOrder ) {
    this.defaultRouteOrder = defaultRouteOrder;
  }

  public Map getRouteOrders() {
    return routeOrders;
  }

  public RouteOrder getRouteOrder( String routeOrderName ) {
    RouteOrder routeOrder = null;
    if ( ( routeOrderName == null ) || ( routeOrderName.equals( "" ) ) ) {
      return routeOrder;
    }
    routeOrder = (RouteOrder) routeOrders.get( routeOrderName );
    return routeOrder;
  }

  public Map getRouteProviders() {
    return routeProviders;
  }

  public RouteProvider getRouteProvider( String inboundProvider ) {
    RouteProvider routeProvider = null;
    if ( ( inboundProvider == null ) || ( inboundProvider.equals( "" ) ) ) {
      return routeProvider;
    }
    routeProvider = (RouteProvider) routeProviders.get( inboundProvider );
    return routeProvider;
  }

  public Map getSessionParams() {
    return sessionParams;
  }

  public List getSessionParamIds() {
    List listSessionParamIds = null;
    if ( sessionParams != null ) {
      listSessionParamIds = new ArrayList( sessionParams.keySet() );
    }
    return listSessionParamIds;
  }

  public Map getProviderFeatures() {
    return providerFeatures;
  }

  public List getProviderFeatureIds() {
    List listProviderFeatureIds = null;
    if ( providerFeatures != null ) {
      listProviderFeatureIds = new ArrayList( providerFeatures.keySet() );
    }
    return listProviderFeatureIds;
  }

  public Map getAlertClientLowBalances() {
    return alertClientLowBalances;
  }

  public Map getScheduleTasks() {
    return scheduleTasks;
  }

  public List getScheduleTaskIds() {
    return scheduleTaskIds;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Helper
  //
  // ////////////////////////////////////////////////////////////////////////////

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "TransactionConf ( defaultRouteOrder = "
        + this.defaultRouteOrder + TAB + "sessionParamIds = "
        + this.getSessionParamIds() + TAB + "providerFeatureIds = "
        + this.getProviderFeatureIds() + TAB + "scheduleTaskIds = "
        + this.scheduleTaskIds + TAB + "alertClientLowBalances = "
        + this.alertClientLowBalances + TAB + " )";
    return retValue;
  }

}
