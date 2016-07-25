package com.beepcast.model.reservedCode.mobileUser;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.reservedCode.ReservedCodeBean;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class MobileUserCodeProcessor {

  static final DLogContext lctx = new SimpleContext( "MobileUserCodeProcessor" );

  private ReservedCodeBean reservedCode;

  public MobileUserCodeProcessor( ReservedCodeBean reservedCode ) {
    this.reservedCode = reservedCode;
  }

  public String mobileUserMenu() throws IOException {
    String menu = null;

    if ( reservedCode == null ) {
      DLog.warning( lctx , "Failed to get mobile user menu "
          + ", found null reserved code" );
      return menu;
    }

    int clientId = reservedCode.getClientId();
    if ( clientId < 1 ) {
      DLog.warning( lctx , "Failed to get mobile user menu "
          + ", found zero client id" );
      return menu;
    }

    String phone = reservedCode.getPhone();
    if ( StringUtils.isBlank( phone ) ) {
      DLog.warning( lctx , "Failed to get mobile user menu "
          + ", found blank phone" );
      return menu;
    }

    MobileUserMenu mobileUserMenu = new MobileUserMenu( clientId , phone );

    menu = mobileUserMenu.getMenu();

    return menu;
  }

  public String getBeepID() throws IOException {
    return new BeepID( reservedCode.getClientId() , reservedCode ).getBeepID();
  }

  public String getStockQuote() throws IOException {
    return null;
    // new StockReader( reservedCode ).getStockQuote();
  }

  public String getSportsScores() throws IOException {
    return null;
    // new SportsReader( reservedCode ).getScores();
  }

} // eof
