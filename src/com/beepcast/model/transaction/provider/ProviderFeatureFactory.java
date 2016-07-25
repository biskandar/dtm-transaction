package com.beepcast.model.transaction.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ProviderFeatureFactory {

  static final DLogContext lctx = new SimpleContext( "ProviderFeatureFactory" );

  public static ProviderFeature createProviderFeature(
      String providerFeatureName , Map mapFeatures ) {
    ProviderFeature providerFeature = null;
    if ( StringUtils.isBlank( providerFeatureName ) ) {
      DLog.warning( lctx , "Failed to create provider feature "
          + ", found blank name" );
      return providerFeature;
    }
    providerFeature = new ProviderFeature();

    providerFeature.setName( providerFeatureName );
    if ( mapFeatures == null ) {
      mapFeatures = new HashMap();
    }
    providerFeature.setMapFeatures( mapFeatures );

    return providerFeature;
  }

}
