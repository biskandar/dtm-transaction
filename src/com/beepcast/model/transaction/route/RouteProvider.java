package com.beepcast.model.transaction.route;

public class RouteProvider {

  private String inboundProvider;
  private String routeOrderName;

  public RouteProvider() {

  }

  public String getInboundProvider() {
    return inboundProvider;
  }

  public void setInboundProvider( String inboundProvider ) {
    this.inboundProvider = inboundProvider;
  }

  public String getRouteOrderName() {
    return routeOrderName;
  }

  public void setRouteOrderName( String routeOrderName ) {
    this.routeOrderName = routeOrderName;
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "RouteProvider ( " + "inboundProvider = " + this.inboundProvider
        + TAB + "routeOrderName = " + this.routeOrderName + TAB + " )";
    return retValue;
  }

}
