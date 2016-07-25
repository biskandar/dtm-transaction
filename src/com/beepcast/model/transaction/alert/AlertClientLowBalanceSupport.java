package com.beepcast.model.transaction.alert;

public class AlertClientLowBalanceSupport {

  public static boolean isBalanceHitThreshold( double balanceThreshold ,
      double balanceBefore , double balanceAfter ) {
    boolean result = false;
    balanceThreshold = roundDoubleDecimals( balanceThreshold , 2 );
    balanceAfter = roundDoubleDecimals( balanceAfter , 2 );
    balanceBefore = roundDoubleDecimals( balanceBefore , 2 );
    if ( balanceThreshold < 0.00 ) {
      return result;
    }
    if ( balanceAfter > balanceThreshold ) {
      return result;
    }
    if ( balanceBefore <= balanceThreshold ) {
      return result;
    }
    result = true;
    return result;
  }

  public static double roundDoubleDecimals( double doubleCur , int digits ) {
    double doubleNew = doubleCur;
    if ( digits < 1 ) {
      return doubleNew;
    }
    long perTens = (long) Math.pow( 10 , digits );
    doubleNew = Math.round( doubleCur * perTens );
    doubleNew = doubleNew / perTens;
    return doubleNew;
  }

}
