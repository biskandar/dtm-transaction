package com.beepcast.model.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.MobileUserService;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;

public class EventExpectUserEmail {

  public static boolean processExpect( TransactionLog log ,
      TransactionInputMessage imsg ) {
    boolean result = false;

    // prepare mobile user service
    MobileUserService mobileUserService = new MobileUserService();

    // define mobile user bean
    MobileUserBean mobileUserBean = (MobileUserBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );
    if ( mobileUserBean == null ) {
      log.warning( "Failed to process expect user email "
          + ", found null mobile user bean" );
      return result;
    }

    // find email inside the message content
    String emailAddress = getFirstEmail( imsg.getMessageContent() );
    if ( StringUtils.isBlank( emailAddress ) ) {
      log.warning( "Failed to process expect user email "
          + ", found no email inside incoming message content" );
      return result;
    }

    // log it
    log.debug( "Updating mobile user new email address : "
        + mobileUserBean.getEmail() + " -> " + emailAddress );

    // update email on mobile user bean
    mobileUserBean.setEmail( emailAddress );

    // update email on mobile user table
    if ( !mobileUserService.updateEmail( mobileUserBean ) ) {
      log.warning( "Failed to process expect user email "
          + ", found failed to update mobile user table" );
      return result;
    }

    result = true;
    return result;
  }

  private static String getFirstEmail( String data ) {
    String found = null;
    if ( data != null ) {
      Pattern email = Pattern.compile( "^\\S+@\\S+$" );
      String datas[] = data.split( " " );
      String word;
      for ( int i = 0 ; i < datas.length ; i++ ) {
        word = datas[i].trim();
        Matcher fit = email.matcher( word );
        if ( fit.matches() ) {
          found = fit.group( 0 );
          break;
        }
      }
    }
    return found;
  }

}
