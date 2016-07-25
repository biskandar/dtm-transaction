package com.beepcast.model.transaction.alert;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionApp;
import com.beepcast.model.transaction.TransactionConf;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class AlertService {

  static final DLogContext lctx = new SimpleContext( "AlertService" );

  public static boolean alertClientLowBalance( int clientId ,
      String clientEmailAddress , String clientManager ,
      String clientCompanyName , int clientBalanceThreshold ,
      double clientBalanceBefore , double clientBalanceAfter ) {
    boolean result = false;

    String headerLog = "[Client-" + clientId + "] ";

    if ( clientId < 1 ) {
      DLog.warning( lctx , headerLog + "Failed to perform alert client "
          + "low balance , found zero client id" );
      return result;
    }

    if ( StringUtils.isBlank( clientEmailAddress ) ) {
      DLog.warning( lctx , headerLog + "Failed to perform alert client "
          + "low balance , found empty client email address" );
      return result;
    }

    TransactionApp app = TransactionApp.getInstance();
    if ( app == null ) {
      DLog.warning( lctx , headerLog + "Failed to perform alert client "
          + "low balance , found null transaction app" );
      return result;
    }

    TransactionConf conf = app.getTransactionConf();
    if ( conf == null ) {
      DLog.warning( lctx , headerLog + "Failed to perform alert client "
          + "low balance , found null transaction conf" );
      return result;
    }

    boolean debug = conf.isDebug();

    boolean sendAlert = false;
    if ( ( !sendAlert )
        && verifyClientLowBalanceFromClientBalanceThreshold(
            clientBalanceThreshold , clientBalanceBefore , clientBalanceAfter ) ) {
      sendAlert = true;
      DLog.debug( lctx , headerLog + "Found client low balance "
          + "from client balance threshold " + clientBalanceThreshold );
    }
    if ( ( !sendAlert )
        && verifyClientLowBalanceFromAlertClientLowBalanceService( conf ,
            clientBalanceBefore , clientBalanceAfter ) ) {
      sendAlert = true;
      DLog.debug( lctx , headerLog + "Found client low balance "
          + "from alert client low balance map" );
    }
    if ( !sendAlert ) {
      if ( debug ) {
        DLog.debug( lctx , "Client balance doesn't reaches the limit yet "
            + ", no alert low balance need to be perform" );
      }
      return result;
    }

    DLog.debug( lctx , headerLog + "Found client balance amount ( before = "
        + clientBalanceBefore + " , after = " + clientBalanceAfter
        + " ) is reached the limit threshold , trying to send "
        + "a low balance alert to : " + clientEmailAddress );

    AlertEmailService emailService = new AlertEmailService();
    result = emailService.doAlert( clientEmailAddress , clientManager ,
        clientCompanyName , clientBalanceAfter ,
        AlertEmailService.ALERT_TYPE_CLIENT_LOW_BALANCE );

    if ( result ) {
      DLog.debug( lctx , headerLog + "Successfully send client "
          + "low balance alert message" );
    } else {
      DLog.warning( lctx , headerLog + "Failed to send client "
          + "low balance alert message" );
    }

    return result;
  }

  private static boolean verifyClientLowBalanceFromClientBalanceThreshold(
      double balanceThreshold , double balanceBefore , double balanceAfter ) {
    return AlertClientLowBalanceSupport.isBalanceHitThreshold(
        balanceThreshold , balanceBefore , balanceAfter );
  }

  private static boolean verifyClientLowBalanceFromAlertClientLowBalanceService(
      TransactionConf conf , double balanceBefore , double balanceAfter ) {
    boolean result = false;
    Map m = conf.getAlertClientLowBalances();
    if ( ( m == null ) || ( m.size() < 1 ) ) {
      return result;
    }
    AlertClientLowBalanceService s = new AlertClientLowBalanceService( m );
    result = ( s.searchAlert( balanceBefore , balanceAfter ) != null );
    return result;
  }

}
