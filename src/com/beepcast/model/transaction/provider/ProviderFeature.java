package com.beepcast.model.transaction.provider;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ProviderFeature {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "ProviderFeature" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private String name;
  private Map mapFeatures;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public ProviderFeature() {

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Set / Get Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public Map getMapFeatures() {
    return mapFeatures;
  }

  public void setMapFeatures( Map mapFeatures ) {
    this.mapFeatures = mapFeatures;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean addFeatureValue( String providerId , String value ) {
    boolean result = false;
    if ( StringUtils.isBlank( providerId ) || StringUtils.isBlank( value ) ) {
      return result;
    }
    mapFeatures.put( providerId , value );
    return result;
  }

  public String getFeatureValue( String providerId ) {
    String featureValue = null;
    if ( StringUtils.isBlank( providerId ) ) {
      return featureValue;
    }
    if ( mapFeatures != null ) {
      featureValue = (String) mapFeatures.get( providerId );
    }
    return featureValue;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Helper
  //
  // //////////////////////////////////////////////////////////////////////////

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append( "ProviderFeature ( " );
    sb.append( "name = " + name + " , " );
    sb.append( "size = " + mapFeatures.size() + " provider(s) " );
    sb.append( ") " );
    return sb.toString();
  }

}
