package com.beepcast.model.transaction;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beepcast.model.transaction.alert.AlertClientLowBalanceFactory;
import com.beepcast.model.transaction.alert.AlertClientLowBalanceService;
import com.beepcast.model.transaction.provider.ProviderFeature;
import com.beepcast.model.transaction.provider.ProviderFeatureFactory;
import com.beepcast.model.transaction.provider.ProviderFeatureName;
import com.beepcast.model.transaction.route.RouteOrder;
import com.beepcast.model.transaction.route.RouteProvider;
import com.beepcast.util.properties.GlobalEnvironment;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;
import com.firsthop.common.util.xml.TreeUtil;

public class TransactionConfFactory {

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // ////////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionConf" );

  static final GlobalEnvironment globalEnv = GlobalEnvironment.getInstance();

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public static TransactionConf generateTransactionConf( String propertyFile ) {
    TransactionConf transConf = new TransactionConf();

    if ( ( propertyFile == null ) || ( propertyFile.equals( "" ) ) ) {
      return transConf;
    }

    DLog.debug( lctx , "Loading from property = " + propertyFile );

    Element element = globalEnv.getElement( TransactionConf.class.getName() ,
        propertyFile );
    if ( element != null ) {
      boolean result = validateTag( element );
      if ( result ) {
        extractElement( element , transConf );
      }
    }

    return transConf;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  private static boolean validateTag( Element element ) {
    boolean result = false;

    if ( element == null ) {
      DLog.warning( lctx , "Found empty in element xml" );
      return result;
    }

    Node node = TreeUtil.first( element , "transaction" );
    if ( node == null ) {
      DLog.warning( lctx , "Can not find root tag <transaction>" );
      return result;
    }

    result = true;
    return result;
  }

  private static boolean extractElement( Element element ,
      TransactionConf transConf ) {
    boolean result = false;

    Node nodeTransaction = TreeUtil.first( element , "transaction" );
    if ( nodeTransaction == null ) {
      return result;
    } else {
      String stemp = TreeUtil.getAttribute( nodeTransaction , "debug" );
      if ( ( stemp != null ) && ( stemp.equalsIgnoreCase( "true" ) ) ) {
        transConf.setDebug( true );
      }
    }

    Node nodePropertyProcessSteps = TreeUtil.first( nodeTransaction ,
        "propertyProcessSteps" );
    if ( nodePropertyProcessSteps != null ) {
      extractNodePropertyProcessSteps( nodePropertyProcessSteps , transConf );
    }

    Node nodeRouteOrders = TreeUtil.first( nodeTransaction , "routeOrders" );
    if ( nodeRouteOrders != null ) {
      extractNodeRouteOrders( nodeRouteOrders , transConf );
    }

    Node nodeRouteProviders = TreeUtil.first( nodeTransaction ,
        "routeProviders" );
    if ( nodeRouteProviders != null ) {
      extractNodeRouteProviders( nodeRouteProviders , transConf );
    }

    Node nodeSessionParams = TreeUtil.first( nodeTransaction , "sessionParams" );
    if ( nodeSessionParams != null ) {
      extractNodeSessionParams( nodeSessionParams , transConf );
    }

    Node nodeProviderFeatures = TreeUtil.first( nodeTransaction ,
        "providerFeatures" );
    if ( nodeProviderFeatures != null ) {
      extractNodeProviderFeatures( nodeProviderFeatures , transConf );
    }

    Node nodeAlertTasks = TreeUtil.first( nodeTransaction , "alertTasks" );
    if ( nodeAlertTasks != null ) {
      extractNodeAlertTasks( nodeAlertTasks , transConf );
    }

    Node nodeScheduleTasks = TreeUtil.first( nodeTransaction , "scheduleTasks" );
    if ( nodeScheduleTasks != null ) {
      extractNodeScheduleTasks( nodeScheduleTasks , transConf );
    }

    result = true;
    return result;
  }

  private static void extractNodePropertyProcessSteps(
      Node nodePropertyProcessSteps , TransactionConf transConf ) {

    Node nodePropertyProcessStep = TreeUtil.first( nodePropertyProcessSteps ,
        "propertyProcessStep" );
    while ( nodePropertyProcessStep != null ) {
      String psName = TreeUtil.getAttribute( nodePropertyProcessStep , "name" );
      if ( !StringUtils.isBlank( psName ) ) {
        Node nodeProperty = TreeUtil.first( nodePropertyProcessStep ,
            "property" );
        while ( nodeProperty != null ) {
          String pName = TreeUtil.getAttribute( nodeProperty , "name" );
          String pValue = TreeUtil.getAttribute( nodeProperty , "value" );
          if ( !StringUtils.isBlank( pName ) && !StringUtils.isBlank( pValue ) ) {
            String key = psName + TransactionConf.FIELD_SEPARATOR + pName;
            String value = pValue;
            transConf.getPropertyProcessSteps().put( key , value );
          }
          nodeProperty = TreeUtil.next( nodeProperty , "property" );
        }
      }
      nodePropertyProcessStep = TreeUtil.next( nodePropertyProcessStep ,
          "propertyProcessStep" );
    }

  }

  private static void extractNodeRouteOrders( Node nodeRouteOrders ,
      TransactionConf transConf ) {
    String stemp = TreeUtil.getAttribute( nodeRouteOrders , "default" );
    if ( ( stemp != null ) && ( !stemp.equals( "" ) ) ) {
      transConf.setDefaultRouteOrder( stemp );
    }
    Node nodeRouteOrder = TreeUtil.first( nodeRouteOrders , "routeOrder" );
    while ( nodeRouteOrder != null ) {
      extractNodeRouteOrder( nodeRouteOrder , transConf );
      nodeRouteOrder = TreeUtil.next( nodeRouteOrder , "routeOrder" );
    }
  }

  private static void extractNodeRouteOrder( Node nodeRouteOrder ,
      TransactionConf transConf ) {
    String stemp = TreeUtil.getAttribute( nodeRouteOrder , "name" );
    if ( ( stemp == null ) || ( stemp.equals( "" ) ) ) {
      return;
    }
    RouteOrder routeOrder = new RouteOrder();
    routeOrder.setName( stemp );
    Node nodeProvider = TreeUtil.first( nodeRouteOrder , "provider" );
    while ( nodeProvider != null ) {
      String strProviderId = TreeUtil.getAttribute( nodeProvider , "id" );
      String strPriority = TreeUtil.getAttribute( nodeProvider , "priority" );
      String strEnable = TreeUtil.getAttribute( nodeProvider , "enable" );
      boolean invalid = false;
      if ( ( strProviderId == null ) || ( strProviderId.equals( "" ) ) ) {
        invalid = true;
      }
      if ( ( strEnable == null ) || ( !strEnable.equalsIgnoreCase( "true" ) ) ) {
        invalid = true;
      }
      int priority = 0;
      try {
        priority = Integer.parseInt( strPriority );
      } catch ( NumberFormatException e ) {
        invalid = true;
      }
      if ( !invalid ) {
        routeOrder.putProvider( strProviderId , new Integer( priority ) );
      }
      nodeProvider = TreeUtil.next( nodeProvider , "provider" );
    }
    transConf.getRouteOrders().put( routeOrder.getName() , routeOrder );
  }

  private static void extractNodeRouteProviders( Node nodeRouteProviders ,
      TransactionConf transConf ) {
    Node nodeRouteProvider = TreeUtil.first( nodeRouteProviders ,
        "routeProvider" );
    while ( nodeRouteProvider != null ) {
      extractNodeRouteProvider( nodeRouteProvider , transConf );
      nodeRouteProvider = TreeUtil.next( nodeRouteProvider , "routeProvider" );
    }
  }

  private static void extractNodeRouteProvider( Node nodeRouteProvider ,
      TransactionConf transConf ) {
    String inboundProvider = TreeUtil.getAttribute( nodeRouteProvider ,
        "inboundProvider" );
    String routeOrderName = TreeUtil.getAttribute( nodeRouteProvider ,
        "routeOrder" );
    if ( ( inboundProvider == null ) || ( inboundProvider.equals( "" ) ) ) {
      return;
    }
    if ( ( routeOrderName == null ) || ( routeOrderName.equals( "" ) ) ) {
      return;
    }
    RouteProvider routeProvider = new RouteProvider();
    routeProvider.setInboundProvider( inboundProvider );
    routeProvider.setRouteOrderName( routeOrderName );
    transConf.getRouteProviders().put( routeProvider.getInboundProvider() ,
        routeProvider );
  }

  private static void extractNodeSessionParams( Node nodeSessionParams ,
      TransactionConf transConf ) {
    Node nodeSessionParam = TreeUtil.first( nodeSessionParams , "sessionParam" );
    while ( nodeSessionParam != null ) {
      String key = TreeUtil.getAttribute( nodeSessionParam ,
          TransactionConf.SESSION_PARAM_ID );
      String value = TreeUtil.getAttribute( nodeSessionParam ,
          TransactionConf.SESSION_PARAM_VALUE );
      if ( ( key != null ) && ( !key.equals( "" ) ) && ( value != null )
          && ( StringUtils.isNumeric( value ) ) ) {
        int v = 0;
        try {
          v = Integer.parseInt( value );
        } catch ( NumberFormatException e ) {
        }
        if ( key.equals( TransactionConf.SESSION_PARAM_ID_LIMIT_DAYS ) ) {
          if ( v < 1 ) {
            v = 30 * 12;
          }
          transConf.getSessionParams().put( key , new Integer( v ) );
        }
        if ( key.equals( TransactionConf.SESSION_PARAM_ID_LIMIT_RECORDS ) ) {
          if ( v < 1 ) {
            v = 1000;
          }
          transConf.getSessionParams().put( key , new Integer( v ) );
        }
        if ( key.equals( TransactionConf.SESSION_PARAM_ID_EXPIRY_DAYS ) ) {
          if ( v < 1 ) {
            v = 30;
          }
          transConf.getSessionParams().put( key , new Integer( v ) );
        }
      }
      nodeSessionParam = TreeUtil.next( nodeSessionParam , "sessionParam" );
    }
  }

  private static void extractNodeProviderFeatures( Node nodeProviderFeatures ,
      TransactionConf transConf ) {
    Node nodeProviderFeature = TreeUtil.first( nodeProviderFeatures ,
        "providerFeature" );
    while ( nodeProviderFeature != null ) {
      String providerFeatureName = TreeUtil.getAttribute( nodeProviderFeature ,
          "name" );
      if ( providerFeatureName != null ) {
        // provider feature name must has valid name
        if ( StringUtils.equalsIgnoreCase(
            ProviderFeatureName.SplitLongSmsMessageByProvider ,
            providerFeatureName ) ) {

          // create map features
          Map mapFeatures = new HashMap();

          // load list of providers into the map
          Node nodeProvider = TreeUtil.first( nodeProviderFeature , "provider" );
          while ( nodeProvider != null ) {
            String providerId = TreeUtil.getAttribute( nodeProvider , "id" );
            String value = TreeUtil.getAttribute( nodeProvider , "value" );
            if ( ( !StringUtils.isBlank( providerId ) )
                && ( !StringUtils.isBlank( value ) ) ) {
              mapFeatures.put( providerId , value );
            }
            nodeProvider = TreeUtil.next( nodeProvider , "provider" );
          }

          // create and set provider feature object
          ProviderFeature providerFeature = ProviderFeatureFactory
              .createProviderFeature( providerFeatureName , mapFeatures );
          if ( providerFeature != null ) {
            transConf.getProviderFeatures().put( providerFeature.getName() ,
                providerFeature );
          }

        }
      }
      nodeProviderFeature = TreeUtil.next( nodeProviderFeature ,
          "providerFeature" );
    }
  }

  private static void extractNodeAlertTasks( Node nodeAlertTasks ,
      TransactionConf transConf ) {
    AlertClientLowBalanceService service = new AlertClientLowBalanceService(
        transConf.getAlertClientLowBalances() );
    Node nodeAlertTask = TreeUtil.first( nodeAlertTasks , "alertTask" );
    while ( nodeAlertTask != null ) {
      String id = TreeUtil.getAttribute( nodeAlertTask , "id" );
      if ( ( id != null ) && ( !id.equals( "" ) ) ) {
        Node nodeAlertProperty = TreeUtil.first( nodeAlertTask ,
            "alertProperty" );
        while ( nodeAlertProperty != null ) {
          String name = TreeUtil.getAttribute( nodeAlertProperty , "name" );
          String value = TreeUtil.getAttribute( nodeAlertProperty , "value" );
          if ( !StringUtils.isBlank( name ) && !StringUtils.isBlank( value ) ) {
            if ( name.equals( "thresholdUnit" ) ) {
              try {
                double thresholdUnit = Double.parseDouble( value );
                if ( thresholdUnit > -1 ) {
                  service.addNewAlert( AlertClientLowBalanceFactory
                      .createAlertClientLowBalanceBean( id , thresholdUnit ) );
                }
              } catch ( NumberFormatException e ) {
              }
            }
          }
          nodeAlertProperty = TreeUtil.next( nodeAlertProperty ,
              "alertProperty" );
        }
      }
      nodeAlertTask = TreeUtil.next( nodeAlertTask , "alertTask" );
    }
  }

  private static void extractNodeScheduleTasks( Node nodeScheduleTasks ,
      TransactionConf transConf ) {
    Node nodeScheduleTask = TreeUtil.first( nodeScheduleTasks , "scheduleTask" );
    while ( nodeScheduleTask != null ) {
      String id = TreeUtil.getAttribute( nodeScheduleTask ,
          TransactionConf.SCHEDULE_TASK_ID );
      if ( ( id != null ) && ( !id.equals( "" ) ) ) {
        transConf.getScheduleTaskIds().add( id );
        String javacls = TreeUtil.getAttribute( nodeScheduleTask ,
            TransactionConf.SCHEDULE_TASK_JAVACLS );
        if ( ( javacls != null ) && ( !javacls.equals( "" ) ) ) {
          transConf.getScheduleTasks().put(
              id + TransactionConf.FIELD_SEPARATOR
                  + TransactionConf.SCHEDULE_TASK_JAVACLS , javacls );
        }
        String cronexp = TreeUtil.getAttribute( nodeScheduleTask ,
            TransactionConf.SCHEDULE_TASK_CRONEXP );
        if ( ( cronexp != null ) && ( !cronexp.equals( "" ) ) ) {
          transConf.getScheduleTasks().put(
              id + TransactionConf.FIELD_SEPARATOR
                  + TransactionConf.SCHEDULE_TASK_CRONEXP , cronexp );
        }
      }
      nodeScheduleTask = TreeUtil.next( nodeScheduleTask , "scheduleTask" );
    }
  }

}
