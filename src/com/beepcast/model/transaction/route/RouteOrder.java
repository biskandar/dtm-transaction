package com.beepcast.model.transaction.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RouteOrder {

  private String name;
  private Map providers;
  private List listPriorityProviderIds;

  public RouteOrder() {
    providers = new HashMap();
  }

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public Map getProviders() {
    return providers;
  }

  public boolean putProvider( String providerId , Integer priority ) {
    boolean result = false;
    if ( ( providerId == null ) || ( providerId.equals( "" ) ) ) {
      return result;
    }
    if ( priority == null ) {
      return result;
    }
    providers.put( providerId , priority );
    result = true;
    return result;
  }

  public void refresh() {
    listPriorityProviderIds = new ArrayList( providers.keySet() );
    Collections.sort( listPriorityProviderIds , new Comparator() {
      public int compare( Object left , Object right ) {
        if ( !( left instanceof String ) ) {
          return 0;
        }
        if ( !( right instanceof String ) ) {
          return 0;
        }
        String leftKey = (String) left;
        String rightKey = (String) right;
        Integer leftValue = (Integer) providers.get( leftKey );
        Integer rightValue = (Integer) providers.get( rightKey );
        return rightValue.compareTo( leftValue );
      }
    } );
  }

  public Iterator iterPriorityProviderIds() {
    Iterator iter = null;
    if ( listPriorityProviderIds == null ) {
      return iter;
    }
    iter = listPriorityProviderIds.iterator();
    return iter;
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "RouteOrder ( " + "name = " + this.name + TAB
        + "listPriorityProviderIds = " + this.listPriorityProviderIds + TAB
        + " )";
    return retValue;
  }

}
