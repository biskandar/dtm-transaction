package com.beepcast.model.mobileUser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TelcoService {

  static final DLogContext lctx = new SimpleContext( "TelcoService" );

  public static List resolveListTelcoCodes( MobileUserBean mobileUserBean ) {
    List listTelcoCodes = new ArrayList();

    if ( mobileUserBean == null ) {
      return listTelcoCodes;
    }

    // header log
    String headerLog = "[Client-" + mobileUserBean.getClientId() + "] [Mobile-"
        + mobileUserBean.getPhone() + "] ";

    // resolve
    listTelcoCodes = resolveListTelcoCodes( headerLog ,
        mobileUserBean.getMobileCcnc() );

    return listTelcoCodes;
  }

  public static List resolveListTelcoCodes( String headerLog ,
      String strMobileCcNc ) {
    List listTelcoCodes = new ArrayList();

    headerLog = ( headerLog == null ) ? "" : headerLog;

    if ( StringUtils.isBlank( strMobileCcNc ) ) {
      return listTelcoCodes;
    }

    try {

      // trim mobile cc nc
      strMobileCcNc = strMobileCcNc.trim();

      // length mobile cc nc
      int lenMobileCcNc = strMobileCcNc.length();

      // validate at least 5 characters
      if ( lenMobileCcNc < 5 ) {
        DLog.warning( lctx , headerLog + "Failed to resolve telco code "
            + ", found invalid length mobileCcNc = " + strMobileCcNc );
        return listTelcoCodes;
      }

      // split and validate string
      String[] arrMobileCcNc = strMobileCcNc.split( "\\s*[,;]\\s*" );
      if ( ( arrMobileCcNc == null ) || ( arrMobileCcNc.length < 1 ) ) {
        DLog.warning( lctx , headerLog + "Failed to resolve telco code "
            + ", found invalid value mobileCcNc = " + strMobileCcNc );
        return listTelcoCodes;
      }

      // iterate and process all the string
      String strMobileCc = null;
      for ( int idx = 0 ; idx < arrMobileCcNc.length ; idx++ ) {
        strMobileCcNc = arrMobileCcNc[idx];
        if ( StringUtils.isBlank( strMobileCcNc ) ) {
          continue;
        }

        lenMobileCcNc = strMobileCcNc.length();
        if ( lenMobileCcNc > 5 ) {
          continue;
        }

        if ( lenMobileCcNc == 5 ) {
          if ( strMobileCc == null ) {
            strMobileCc = strMobileCcNc.substring( 0 , 3 );
          }
          listTelcoCodes.add( strMobileCcNc );
          continue;
        }

        if ( strMobileCc == null ) {
          continue;
        }

        if ( lenMobileCcNc == 2 ) {
          listTelcoCodes.add( strMobileCc.concat( strMobileCcNc ) );
          continue;
        }
        if ( lenMobileCcNc == 1 ) {
          listTelcoCodes
              .add( strMobileCc.concat( "0" ).concat( strMobileCcNc ) );
          continue;
        }

      }

      // log it
      DLog.debug( lctx , headerLog + "Resolved list telco codes : "
          + listTelcoCodes );

    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to resolve list telco codes , "
          + e );
    }
    return listTelcoCodes;
  }

}
