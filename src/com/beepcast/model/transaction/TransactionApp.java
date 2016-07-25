package com.beepcast.model.transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.beepcast.model.transaction.alert.AlertClientLowBalanceBean;
import com.beepcast.model.transaction.alert.AlertClientLowBalanceService;
import com.beepcast.model.transaction.provider.ProviderFeature;
import com.beepcast.model.transaction.route.RouteOrder;
import com.beepcast.model.transaction.route.RouteProvider;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionApp implements Module {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionApp" );

  private static Object[] arrSessionLocks;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private TransactionConf transConf;
  private Scheduler scheduler;
  private boolean initialized;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void init( TransactionConf transConf ) {
    initialized = false;

    if ( transConf == null ) {
      DLog.warning( lctx , "Failed to initialized billing "
          + ", found null conf" );
      return;
    }

    // update trans conf
    this.transConf = transConf;
    DLog.debug( lctx , "Debug mode = " + transConf.isDebug() );

    // update property process steps
    updatePropertyProcessSteps();

    // update routeOrders
    updateRouteOrders();

    // update routeProviders
    updateRouteProviders();

    // verify session params
    verifySessionParams();

    // verify provider features
    verifyProviderFeatures();

    // verify alert task
    verifyAlertTasks();

    // create scheduler
    DLog.debug( lctx , "Trying to create scheduler" );
    try {
      SchedulerFactory sf = new StdSchedulerFactory();
      scheduler = sf.getScheduler();
    } catch ( SchedulerException e ) {
      DLog.error( lctx , "Failed to generate scheduler , " + e );
    }
    if ( scheduler == null ) {
      DLog.error( lctx , "Failed to initialized , found null scheduler" );
      return;
    }
    DLog.debug( lctx , "Successfully created scheduler" );

    // generate schedule task(s)
    DLog.debug( lctx , "Trying to generate scheduler task(s)" );
    boolean generateScheduleTasks = generateScheduleTasks( scheduler ,
        transConf.getScheduleTaskIds() , transConf.getScheduleTasks() );
    if ( !generateScheduleTasks ) {
      DLog.warning( lctx , "Failed to generate scheduler task(s)" );
    }
    DLog.debug( lctx , "Successfully generated scheduler task(s)" );

    initialized = true;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public void moduleStart() {
    if ( !initialized ) {
      DLog.error( lctx , "Failed to start transaction module "
          + ", found not yet initialized" );
      return;
    }

    // start all schedule tasks
    startScheduleTasks();

    DLog.debug( lctx , "all module(s) are started" );
  }

  public void moduleStop() {
    if ( !initialized ) {
      DLog.error( lctx , "Failed to stop transaction module "
          + ", found not yet initialized" );
      return;
    }

    // stop all schedule tasks
    stopScheduleTasks();

    DLog.debug( lctx , "all module(s) are stopped" );
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean isDebug() {
    boolean debug = false;
    if ( transConf != null ) {
      debug = transConf.isDebug();
    }
    return debug;
  }

  public Object getSessionLock( String phoneNumber ) {
    Object lockObject = new Object();
    if ( ( phoneNumber == null ) || ( phoneNumber.equals( "" ) ) ) {
      return lockObject;
    }
    char ch = phoneNumber.charAt( phoneNumber.length() - 1 );
    if ( ( ch < '0' ) || ( ch > '9' ) ) {
      return lockObject;
    }
    lockObject = arrSessionLocks[(int) ( ch - '0' )];
    return lockObject;
  }

  public TransactionConf getTransactionConf() {
    return transConf;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private void init() {

    // create array of lock sessions
    arrSessionLocks = new Object[10];
    for ( int idx = 0 ; idx < arrSessionLocks.length ; idx++ ) {
      arrSessionLocks[idx] = new Object();
    }

  }

  private void updatePropertyProcessSteps() {

    Map mapPropertyProcessSteps = transConf.getPropertyProcessSteps();
    if ( mapPropertyProcessSteps == null ) {
      DLog.warning( lctx , "Failed to update property process steps "
          + ", found null object" );
      return;
    }

    int recordCount = 0;
    List listPropertyProcessSteps = new ArrayList(
        mapPropertyProcessSteps.keySet() );
    Iterator iterPropertyProcessSteps = listPropertyProcessSteps.iterator();
    while ( iterPropertyProcessSteps.hasNext() ) {
      String propertyKey = (String) iterPropertyProcessSteps.next();
      if ( StringUtils.isBlank( propertyKey ) ) {
        continue;
      }
      String propertyValue = (String) mapPropertyProcessSteps.get( propertyKey );
      if ( StringUtils.isBlank( propertyValue ) ) {
        continue;
      }
      recordCount = recordCount + 1;
      DLog.debug( lctx , "Define property process step : " + propertyKey
          + " = " + propertyValue );
    }

    DLog.debug( lctx , "Loaded property process steps , total = " + recordCount
        + " record(s)" );

  }

  private void updateRouteOrders() {

    String defaultRouteOrder = transConf.getDefaultRouteOrder();
    DLog.debug( lctx , "Define default route order = " + defaultRouteOrder );

    Map routeOrders = transConf.getRouteOrders();
    if ( routeOrders == null ) {
      DLog.warning( lctx , "Failed to update route orders "
          + ", found null object" );
      return;
    }

    int id = 0;
    List listRouteOrderIds = new ArrayList( routeOrders.keySet() );
    Iterator iterRouteOrderIds = listRouteOrderIds.iterator();
    while ( iterRouteOrderIds.hasNext() ) {
      String routeOrderName = (String) iterRouteOrderIds.next();
      if ( ( routeOrderName == null ) || ( routeOrderName.equals( "" ) ) ) {
        continue;
      }
      RouteOrder routeOrder = (RouteOrder) routeOrders.get( routeOrderName );
      if ( routeOrder == null ) {
        continue;
      }
      routeOrder.refresh();
      DLog.debug( lctx , "Define route order [" + ( id++ ) + "] = "
          + routeOrder );
    }

    DLog.debug( lctx , "Updated total " + routeOrders.size()
        + " route order(s)" );

  }

  private void updateRouteProviders() {

    Map routeProviders = transConf.getRouteProviders();
    if ( routeProviders == null ) {
      DLog.warning( lctx , "Failed to update route providers "
          + ", found null object" );
      return;
    }

    int id = 0;
    Map routeOrders = transConf.getRouteOrders();
    Iterator iterInboundProviders = routeProviders.keySet().iterator();
    while ( iterInboundProviders.hasNext() ) {
      String inboundProvider = (String) iterInboundProviders.next();
      if ( ( inboundProvider == null ) || ( inboundProvider.equals( "" ) ) ) {
        continue;
      }
      RouteProvider routeProvider = (RouteProvider) routeProviders
          .get( inboundProvider );
      if ( routeProvider == null ) {
        continue;
      }
      String routeOrderName = routeProvider.getRouteOrderName();
      if ( routeOrderName == null ) {
        iterInboundProviders.remove();
        continue;
      }
      RouteOrder routeOrder = (RouteOrder) routeOrders.get( routeOrderName );
      if ( routeOrder == null ) {
        iterInboundProviders.remove();
        continue;
      }
      DLog.debug( lctx , "Define route provider [" + ( id++ ) + "] = "
          + routeProvider );
    }

    DLog.debug( lctx , "Updated total " + routeProviders.size()
        + " route provider(s)" );

  }

  private boolean verifySessionParams() {
    boolean result = false;
    Map sessionParams = transConf.getSessionParams();
    if ( sessionParams == null ) {
      DLog.warning( lctx , "Failed to verify session params "
          + ", found null map" );
      return result;
    }
    int id = 0;
    Iterator iterator = sessionParams.keySet().iterator();
    while ( iterator.hasNext() ) {
      String sessionParamId = (String) iterator.next();
      if ( StringUtils.isBlank( sessionParamId ) ) {
        continue;
      }
      Integer sessionParamValue = (Integer) sessionParams.get( sessionParamId );
      DLog.debug( lctx , "Define session param [" + ( id++ ) + "] , name = "
          + sessionParamId + " , value = " + sessionParamValue );
    }
    DLog.debug( lctx , "Verified total " + sessionParams.size()
        + " session param(s)" );
    return result;
  }

  private boolean verifyProviderFeatures() {
    boolean result = false;
    Map providerFeatures = transConf.getProviderFeatures();
    if ( providerFeatures == null ) {
      DLog.warning( lctx , "Failed to verify provider features "
          + ", found null map" );
      return result;
    }
    int id = 0;
    Iterator iterator = providerFeatures.keySet().iterator();
    while ( iterator.hasNext() ) {
      String providerFeatureId = (String) iterator.next();
      if ( StringUtils.isBlank( providerFeatureId ) ) {
        continue;
      }
      ProviderFeature providerFeature = (ProviderFeature) providerFeatures
          .get( providerFeatureId );
      DLog.debug( lctx , "Define provider feature [" + ( id++ ) + "] = "
          + providerFeature );
    }
    DLog.debug( lctx , "Verified total " + providerFeatures.size()
        + " provider feature(s)" );
    return result;
  }

  private boolean verifyAlertTasks() {
    boolean result = false;
    AlertClientLowBalanceService service = new AlertClientLowBalanceService(
        transConf.getAlertClientLowBalances() );
    if ( !service.isValid() ) {
      DLog.warning( lctx , "Failed to verify alert client low balances "
          + ", found null map" );
      return result;
    }
    int totalAlertTasks = 0;
    List listAlertIds = service.listAlertIds();
    if ( listAlertIds != null ) {
      Iterator iterAlertIds = listAlertIds.iterator();
      while ( iterAlertIds.hasNext() ) {
        String alertId = (String) iterAlertIds.next();
        if ( StringUtils.isBlank( alertId ) ) {
          continue;
        }
        AlertClientLowBalanceBean bean = service.queryAlert( alertId );
        if ( bean == null ) {
          continue;
        }
        totalAlertTasks = totalAlertTasks + 1;
        DLog.debug( lctx , "Defined client low balance [" + bean.getAlertId()
            + "] : threshold = " + bean.getThresholdUnit() + " unit(s) " );
      }
    }
    DLog.debug( lctx , "Verified total " + totalAlertTasks + " alert task(s)" );
    return result;
  }

  private boolean generateScheduleTasks( Scheduler sdr , List ids , Map map ) {
    boolean result = false;

    if ( sdr == null ) {
      return result;
    }

    if ( ( ids == null ) || ( ids.size() < 1 ) ) {
      return result;
    }

    if ( map == null ) {
      return result;
    }

    result = true;

    Iterator iterIds = ids.iterator();
    while ( iterIds.hasNext() ) {
      String id = (String) iterIds.next();
      if ( ( id == null ) || ( id.equals( "" ) ) ) {
        continue;
      }
      String headerLog = "[ScheduleTask-" + id + "] ";
      try {
        String javaClass = (String) map.get( id
            + TransactionConf.FIELD_SEPARATOR
            + TransactionConf.SCHEDULE_TASK_JAVACLS );
        String cronExpression = (String) map.get( id
            + TransactionConf.FIELD_SEPARATOR
            + TransactionConf.SCHEDULE_TASK_CRONEXP );
        if ( ( javaClass != null ) && ( cronExpression != null ) ) {
          String jobName = id;
          String jobGroup = "group";
          JobDetail jobDetail = new JobDetail( jobName , jobGroup ,
              generateSchedulerTaskClass( javaClass ) );
          scheduler.addJob( jobDetail , true );
          String triggerName = id;
          String triggerGroup = "group";
          CronTrigger cronTrigger = new CronTrigger( triggerName ,
              triggerGroup , jobName , jobGroup , cronExpression );
          Date ft = scheduler.scheduleJob( cronTrigger );
          DLog.debug(
              lctx ,
              headerLog + "Job has been scheduled to run at " + ft
                  + " and repeat based on expression "
                  + cronTrigger.getCronExpression() );
        }
      } catch ( Exception e ) {
        DLog.warning( lctx , headerLog + "Failed to parse schedule job , " + e );
      }
    }

    return result;
  }

  private Class generateSchedulerTaskClass( String javaClass ) {
    Class c = null;
    try {
      c = Class.forName( javaClass );
    } catch ( ClassNotFoundException e ) {
      DLog.debug( lctx , "Failed to find class , " + e );
    }
    return c;
  }

  private void startScheduleTasks() {
    if ( scheduler == null ) {
      DLog.warning( lctx , "Failed to start schedule task(s) "
          + ", found null schedule object" );
      return;
    }
    try {
      scheduler.start();
      DLog.debug( lctx , "Successfully started scheduler" );
    } catch ( SchedulerException e ) {
      DLog.warning( lctx , "Failed to start scheduler , " + e );
    }
  }

  private void stopScheduleTasks() {
    if ( scheduler == null ) {
      DLog.warning( lctx , "Failed to stop schedule task(s) "
          + ", found null schedule object" );
      return;
    }
    try {
      scheduler.shutdown( true );
      DLog.debug( lctx , "Successfully shutdown scheduler" );
    } catch ( SchedulerException e ) {
      DLog.warning( lctx , "Failed to shutdown scheduler , " + e );
    }
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Singleton Pattern
  //
  // ////////////////////////////////////////////////////////////////////////////

  private static final TransactionApp INSTANCE = new TransactionApp();

  private TransactionApp() {
    init();
  }

  public static final TransactionApp getInstance() {
    return INSTANCE;
  }

}
