package com.beepcast.model.transaction.provider;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ProviderFeatureSupport {

  static final DLogContext lctx = new SimpleContext( "ProviderFeatureFactory" );

  public static String getProviderFeatureValue( Map providerFeatures ,
      String providerFeatureName , String providerId ) {
    String value = null;
    if ( providerFeatures == null ) {
      DLog.warning( lctx , "Failed to get provider feature value "
          + ", found null map" );
      return value;
    }
    if ( StringUtils.isBlank( providerFeatureName ) ) {
      DLog.warning( lctx , "Failed to get provider feature value "
          + ", found blank name" );
      return value;
    }
    ProviderFeature providerFeature = (ProviderFeature) providerFeatures
        .get( providerFeatureName );
    if ( providerFeature == null ) {
      DLog.warning( lctx , "Failed to get provider feature value "
          + ", can not find provider feature object" );
      return value;
    }
    value = providerFeature.getFeatureValue( providerId );
    value = StringUtils.isBlank( value ) ? null : value;
    return value;
  }

}
