package com.beepcast.model.transaction;

public class TransactionLogConstanta {

  public static int CLOSED_REASON_NORMAL = 0;
  public static int CLOSED_REASON_RENEW_EVENT = 1;
  public static int CLOSED_REASON_DIFF_EVENT = 2;
  public static int CLOSED_REASON_EXPIRY = 3;

  public static String closedReasonToString( int closedReasonId ) {
    String strClosedReasonId = "";
    switch ( closedReasonId ) {
    case 0 :
      strClosedReasonId = "CLOSED_REASON_NORMAL";
      break;
    case 1 :
      strClosedReasonId = "CLOSED_REASON_RENEW_EVENT";
      break;
    case 2 :
      strClosedReasonId = "CLOSED_REASON_DIFF_EVENT";
      break;
    case 3 :
      strClosedReasonId = "CLOSED_REASON_EXPIRY";
      break;
    }
    return strClosedReasonId;
  }

}
