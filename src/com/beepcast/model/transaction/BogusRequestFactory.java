package com.beepcast.model.transaction;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class BogusRequestFactory {

  static final DLogContext lctx = new SimpleContext( "BogusRequestFactory" );

  public static BogusRequestBean createBogusRequestBean( long clientID ,
      long eventID , String phone , String shortCode , String message ,
      String description ) {
    BogusRequestBean bean = null;

    if ( StringUtils.isBlank( phone ) ) {
      DLog.warning( lctx , "Failed to create bogus request bean "
          + ", found empty phone" );
      return bean;
    }

    bean = new BogusRequestBean();
    bean.setClientID( clientID );
    bean.setEventID( eventID );
    bean.setPhone( phone );
    bean.setShortCode( shortCode );
    bean.setDateTm( new Date() );
    bean.setMessage( message );
    bean.setDescription( description );
    return bean;
  }

}
