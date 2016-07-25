package com.beepcast.model.reservedCode;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.reservedCode.internal.InternalCodeProcessor;
import com.beepcast.model.reservedCode.mobileUser.MobileUserCodeProcessor;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ReservedCodeDispatcher {

  static final DLogContext lctx = new SimpleContext( "ReservedCodeDispatcher" );

  private ReservedCodeBean reservedCode;

  public ReservedCodeDispatcher( ReservedCodeBean reservedCode ) {
    this.reservedCode = reservedCode;
  }

  public String getResponse() {
    String response = null;

    if ( reservedCode == null ) {
      DLog.warning( lctx , "Failed to get response , found null reserved code" );
      return response;
    }

    String code = reservedCode.getCode();
    if ( StringUtils.isBlank( code ) ) {
      DLog.warning( lctx , "Failed to get response , found blank code" );
      return response;
    }

    InternalCodeProcessor internalCodeProcessor = new InternalCodeProcessor(
        reservedCode );

    MobileUserCodeProcessor mobileUserCodeProcessor = new MobileUserCodeProcessor(
        reservedCode );

    try {

      // ?IP - get ip address
      // if ( code.equals( "?IP" ) ) {
      // response = internalCodeProcessor.getIPAddress();
      // }

      // ?# - month to date mobile users
      // if ( code.equals( "?#" ) ) {
      // response = internalCodeProcessor.getMTDRowCount();
      // }

      // ?$ - month to date revenue
      // if ( code.equals( "?$" ) ) {
      // response = internalCodeProcessor.getMTDRevenue();
      // }

      // ?CG# - commzgate balance
      // else if ( code.equals( "?CG#" ) ) {
      // response = internalCodeProcessor.getCommzgateBalance();
      // }

      // ?SQ - stock quote
      // if ( code.equals( "?SQ" ) ) {
      // response = mobileUserCodeProcessor.getStockQuote();
      // }

      // ?ESPN - sports scores
      // if ( code.equals( "?ESPN" ) ) {
      // response = mobileUserCodeProcessor.getSportsScores();
      // }

      // ?MENU - mobile user menu
      if ( code.equals( "?MENU" ) ) {
        DLog.debug( lctx , "Found reserved code as mobile user menu" );
        response = mobileUserCodeProcessor.mobileUserMenu();
      }

      // ?ID - beep id
      if ( code.equals( "?ID" ) ) {
        DLog.debug( lctx , "Found reserved code as beep id" );
        response = mobileUserCodeProcessor.getBeepID();
      }

    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to get response , " + e );
    }

    // unrecognized reserved code
    return response;
  }

}
