package com.beepcast.model.transaction.xipme;

import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class XipmeUtils {

  static final DLogContext lctx = new SimpleContext( "XipmeUtils" );

  public static String getCode( String curLink ) {
    String code = null;
    try {
      if ( curLink == null ) {
        return code;
      }
      int idx1 = -1;
      int idx2 = curLink.indexOf( "/" );
      while ( idx2 > -1 ) {
        idx1 = idx2;
        idx2 = curLink.indexOf( "/" , idx1 + 1 );
      }
      if ( idx1 < 0 ) {
        return code;
      }
      int idx3 = curLink.indexOf( "?" );
      if ( idx3 < 0 ) {
        idx3 = curLink.length();
      }
      code = curLink.substring( idx1 + 1 , idx3 );
    } catch ( Exception e ) {
    }
    return code;
  }

  public static String cleanMobileNumber( String mobileNumber ) {
    if ( mobileNumber == null ) {
      return mobileNumber;
    }
    if ( mobileNumber.startsWith( "+" ) ) {
      mobileNumber = mobileNumber.substring( 1 );
    }
    return mobileNumber;
  }

}
