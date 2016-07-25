package com.beepcast.model.transaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.beepcast.api.provider.util.RandomPriorityProviderGenerator;
import com.beepcast.model.transaction.provider.ProviderService;
import com.beepcast.model.transaction.route.RouteOrder;
import com.beepcast.model.transaction.route.RouteProvider;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionRoute {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionRoute" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private OnlinePropertiesApp opropsApp;

  private ProviderService providerService;

  private TransactionProcessBasic trans;
  private TransactionConf conf;
  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionRoute( TransactionProcessBasic trans ) {

    opropsApp = OnlinePropertiesApp.getInstance();

    providerService = new ProviderService( trans.log );

    this.trans = trans;
    this.conf = trans.conf();
    this.log = trans.log();

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public String resolveNextProviderId( String inboundProvider ,
      List listActiveProviderIds ) {
    String nextProviderId = null;

    // validate must be params
    if ( listActiveProviderIds == null ) {
      return nextProviderId;
    }

    // read map order providers
    Map mapOrderProviderIds = resolveOrderProviderIds( inboundProvider );
    if ( ( mapOrderProviderIds == null ) || ( mapOrderProviderIds.size() < 1 ) ) {
      log.warning( "Failed to resolve next provider id "
          + ", found empty map order provider ids" );
      return nextProviderId;
    }

    // prepare random engine
    RandomPriorityProviderGenerator randEngine = new RandomPriorityProviderGenerator();

    // setup list random priority providers
    List listValidProviderIds = new ArrayList();
    Set setProviderIds = mapOrderProviderIds.keySet();
    Iterator iterProviderIds = setProviderIds.iterator();
    while ( iterProviderIds.hasNext() ) {
      String providerId = (String) iterProviderIds.next();
      if ( StringUtils.isBlank( providerId ) ) {
        continue;
      }
      if ( listActiveProviderIds.indexOf( providerId ) < 0 ) {
        continue;
      }
      Integer priority = (Integer) mapOrderProviderIds.get( providerId );
      if ( priority == null ) {
        continue;
      }
      randEngine.addProvider( priority.intValue() , providerId );
      listValidProviderIds.add( providerId );
    }

    // validate total providers
    if ( listValidProviderIds.size() < 1 ) {
      log.warning( "Failed to get candidate winner from random priority "
          + ", found zero total providers loaded into random list" );
      return nextProviderId;
    }

    // log it
    log.debug( "List candidate providers read from map order providers ( "
        + listValidProviderIds.size() + " ) : " + listValidProviderIds );

    // get next random provider
    nextProviderId = randEngine.getNextProviderId();
    return nextProviderId;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private Map resolveOrderProviderIds( String inboundProvider ) {
    Map mapProviderIds = null;

    // define route order name

    String routeOrderName = null;
    RouteProvider routeProvider = getRouteProvider( inboundProvider );
    if ( routeProvider != null ) {
      routeOrderName = routeProvider.getRouteOrderName();
    }
    if ( StringUtils.isBlank( routeOrderName ) ) {
      routeOrderName = conf.getDefaultRouteOrder();
      log.debug( "Can not find router order name based on inbound provider = "
          + inboundProvider + " , trying to use default name = "
          + routeOrderName );
    }
    if ( StringUtils.isBlank( routeOrderName ) ) {
      log.warning( "Failed to resolve order provider ids "
          + ", found empty route order name" );
      return mapProviderIds;
    }
    log.debug( "Defined router order name = " + routeOrderName );

    // define route order object

    RouteOrder routeOrder = getRouteOrder( routeOrderName );
    if ( routeOrder == null ) {
      log.warning( "Failed to resolve order provider ids "
          + ", found null route order" );
      return mapProviderIds;
    }
    log.debug( "Defined router order : " + routeOrder );

    // read map providers from route order object

    mapProviderIds = routeOrder.getProviders();
    return mapProviderIds;
  }

  private RouteProvider getRouteProvider( String inboundProvider ) {
    RouteProvider routeProvider = null;

    if ( StringUtils.isBlank( inboundProvider ) ) {
      return routeProvider;
    }

    String opropsFieldName = "Transaction.RouteProvider."
        .concat( inboundProvider );
    String routeOrderName = opropsApp.getString( opropsFieldName , null );
    if ( !StringUtils.isBlank( routeOrderName ) ) {
      routeProvider = new RouteProvider();
      routeProvider.setInboundProvider( inboundProvider );
      routeProvider.setRouteOrderName( routeOrderName );
      return routeProvider;
    }

    routeProvider = conf.getRouteProvider( inboundProvider );
    return routeProvider;
  }

  private RouteOrder getRouteOrder( String routeOrderName ) {
    RouteOrder routeOrder = null;

    if ( StringUtils.isBlank( routeOrderName ) ) {
      return routeOrder;
    }

    String opropsFieldName = "Transaction.RouteOrder.".concat( routeOrderName );
    String strOrderProviderIds = opropsApp.getString( opropsFieldName , null );
    if ( !StringUtils.isBlank( strOrderProviderIds ) ) {
      String[] arrOrderProviderIds = strOrderProviderIds.split( ":" );
      routeOrder = new RouteOrder();
      routeOrder.setName( routeOrderName );
      for ( int idx = 0 ; idx < arrOrderProviderIds.length ; idx++ ) {
        String providerId = arrOrderProviderIds[idx];
        if ( StringUtils.isBlank( providerId ) ) {
          continue;
        }
        routeOrder.putProvider( providerId , new Integer(
            arrOrderProviderIds.length - idx ) );
      }
      routeOrder.refresh();
      return routeOrder;
    }

    routeOrder = conf.getRouteOrder( routeOrderName );
    return routeOrder;
  }

}
