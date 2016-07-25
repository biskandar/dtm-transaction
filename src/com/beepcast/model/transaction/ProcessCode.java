package com.beepcast.model.transaction;

public class ProcessCode {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  public static final int PROCESS_SUCCEED = 100;
  public static final int PROCESS_FAILED = 200;
  public static final int PROCESS_ERROR = 300;
  public static final int PROCESS_FATAL = 400;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static String processCodeToString( int processCode ) {
    String strProcessCode = "";

    switch ( processCode ) {
    case 100 :
      strProcessCode = "PROCESS_SUCCEED";
      break;
    case 200 :
      strProcessCode = "PROCESS_FAILED";
      break;
    case 300 :
      strProcessCode = "PROCESS_ERROR";
      break;
    case 400 :
      strProcessCode = "PROCESS_FATAL";
      break;
    }

    return strProcessCode;
  }

}
