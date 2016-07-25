package com.beepcast.model.transaction;

public class MessageType {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  public static final int UNKNOWN_TYPE = -1;
  public static final int TEXT_TYPE = 0;
  public static final int RINGTONE_TYPE = 1;
  public static final int PICTURE_TYPE = 2;
  public static final int UNICODE_TYPE = 3;
  // public static final int PREMIUM_TYPE = 4;
  public static final int WAPPUSH_TYPE = 5;
  // public static final int TESTING_TYPE = 8;
  // public static final int ALL_TYPE = 9;
  public static final int MMS_TYPE = 10;
  public static final int QRPNG_TYPE = 11;
  public static final int QRGIF_TYPE = 12;
  public static final int QRJPG_TYPE = 13;
  public static final int WEBHOOK_TYPE = 14;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static String messageTypeToString( int messageType ) {
    String strMsgType = "UNKNOWN";
    switch ( messageType ) {
    case TEXT_TYPE :
      strMsgType = "SMS_TEXT";
      break;
    case RINGTONE_TYPE :
      strMsgType = "SMS_RINGTONE";
      break;
    case PICTURE_TYPE :
      strMsgType = "SMS_PICTURE";
      break;
    case UNICODE_TYPE :
      strMsgType = "SMS_UNICODE";
      break;
    case WAPPUSH_TYPE :
      strMsgType = "SMS_WAPPUSH";
      break;
    case MMS_TYPE :
      strMsgType = "MMS";
      break;
    case QRPNG_TYPE :
      strMsgType = "QR_PNG";
      break;
    case QRGIF_TYPE :
      strMsgType = "QR_GIF";
      break;
    case QRJPG_TYPE :
      strMsgType = "QR_JPG";
      break;
    case WEBHOOK_TYPE :
      strMsgType = "WEBHOOK";
      break;
    }
    return strMsgType;
  }

  public static int messageTypeToInt( String messageType ) {
    int intMsgType = UNKNOWN_TYPE;
    if ( messageType == null ) {
      return intMsgType;
    }
    if ( messageType.equalsIgnoreCase( "SMS_TEXT" ) ) {
      intMsgType = TEXT_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "SMS_RINGTONE" ) ) {
      intMsgType = RINGTONE_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "SMS_PICTURE" ) ) {
      intMsgType = PICTURE_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "SMS_UNICODE" ) ) {
      intMsgType = UNICODE_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "SMS_WAPPUSH" ) ) {
      intMsgType = WAPPUSH_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "MMS" ) ) {
      intMsgType = MMS_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "QR_PNG" ) ) {
      intMsgType = QRPNG_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "QR_GIF" ) ) {
      intMsgType = QRGIF_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "QR_JPG" ) ) {
      intMsgType = QRJPG_TYPE;
    }
    if ( messageType.equalsIgnoreCase( "WEBHOOK" ) ) {
      intMsgType = WEBHOOK_TYPE;
    }
    return intMsgType;
  }

}
