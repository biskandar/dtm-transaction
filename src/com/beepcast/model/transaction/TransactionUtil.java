package com.beepcast.model.transaction;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.maintenance.WordFilterBean;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionUtil {

  static final DLogContext lctx = new SimpleContext( "TransactionUtil" );

  public static String cleanMessageContent( String messageContent ) {
    String messageResult = new String( messageContent );

    // remove mobile user signature
    messageResult = StringUtils.remove( messageResult , "<LF>" );

    // replace end of line character to space
    messageResult = StringUtils.replace( messageResult , "\n\r" , " " );
    messageResult = StringUtils.replace( messageResult , "\r\n" , " " );
    messageResult = StringUtils.replace( messageResult , "\n" , " " );
    messageResult = StringUtils.replace( messageResult , "\r" , " " );

    // remove chevrons and other strange characters
    // messageResult = StringUtils.remove( messageResult , "<" );
    // messageResult = StringUtils.remove( messageResult , ">" );
    // messageResult = StringUtils.remove( messageResult , "'" );
    // messageResult = StringUtils.remove( messageResult , "\"" );
    // messageResult = StringUtils.remove( messageResult , "!" );

    // clean if found "()" characters
    /*
     * if ( messageResult.indexOf( "(" ) != -1 && messageResult.indexOf( ")" )
     * != -1 ) { messageResult = StringUtils.remove( messageResult , "(" );
     * messageResult = StringUtils.remove( messageResult , ")" ); }
     */

    // clean dot char if not the correct email format
    /*
     * if ( !Util.validEmailAddress( messageResult ) ) { messageResult =
     * StringUtils.remove( messageResult , "." ); }
     */

    // strip out bogus words
    /*
     * try { WordStripBean wordStrip = new WordStripBean().select(); String
     * bogusWords[] = wordStrip.getWords(); for ( int j = 0 ; j <
     * bogusWords.length ; j++ ) { messageResult = StringUtils.remove(
     * messageResult , bogusWords[j] ); } } catch ( Exception e ) {
     * DLog.warning( lctx , "Failed to verify from the strip word list , " + e
     * ); }
     */

    return messageResult;
  }

  public static boolean filterMessageContent( String messageContent ) {
    boolean filter = true;

    // is the message content contain word filter message list
    try {
      WordFilterBean wordFilter = new WordFilterBean().select();
      String bogusWords[] = wordFilter.getWords();
      String bogusWord;
      for ( int j = 0 ; j < bogusWords.length ; j++ ) {
        bogusWord = bogusWords[j];
        if ( StringUtils.isBlank( bogusWord ) ) {
          continue;
        }
        if ( StringUtils.contains( messageContent , bogusWord ) ) {
          DLog.debug( lctx , "Found a filter word = " + bogusWord );
          filter = false;
          break;
        }
      }
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to perform filter message content , " + e );
    }

    return filter;
  }

  public static boolean isUnicodeMessage( String messageContent ) {
    boolean result = false;
    if ( messageContent == null ) {
      return result;
    }
    result = !GSM7BitCharsets.isValid( messageContent );
    return result;
  }

  public static int calculateTotalBytes( int messageType , String messageContent ) {
    int totalBytes = 0;
    if ( messageContent == null ) {
      return totalBytes;
    }
    if ( messageType == MessageType.TEXT_TYPE ) {
      totalBytes = GSM7BitCharsets.length( messageContent );
    }
    if ( messageType == MessageType.UNICODE_TYPE ) {
      totalBytes = messageContent.length();
    }
    return totalBytes;
  }

  public static int calculateTotalSms( int messageType , String messageContent ) {
    int totalSms = 0;
    if ( messageContent == null ) {
      return totalSms;
    }
    totalSms = 1;
    if ( messageType == MessageType.TEXT_TYPE ) {
      int messageLength = GSM7BitCharsets.length( messageContent );
      if ( messageLength > 160 ) {
        totalSms = (int) Math.ceil( messageLength / 153.0 );
      }
    }
    if ( messageType == MessageType.UNICODE_TYPE ) {
      int messageLength = messageContent.length();
      if ( messageLength > 70 ) {
        totalSms = (int) Math.ceil( messageLength / 67.0 );
      }
    }
    return totalSms;
  }

}
