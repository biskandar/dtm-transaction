package com.beepcast.model.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;

public class EventInboundReservedVariables {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static int totalReservedVariables( String strVarNames ) {
    int total = 0;
    if ( strVarNames == null ) {
      return total;
    }
    Pattern pattern = Pattern.compile( "(\\$\\w+|<#\\w+#>)" );
    Matcher matcher = pattern.matcher( strVarNames );
    while ( matcher.find() ) {
      total = total + 1;
    }
    return total;
  }

  public static boolean processReservedVariable( TransactionLog log ,
      String varName , int varIndex , int varTotal ,
      TransactionInputMessage imsg ) {
    boolean result = false;

    if ( varName == null ) {
      return result;
    }

    if ( varName.equalsIgnoreCase( "$EMAIL" ) ) {
      result = EventExpectUserEmail.processExpect( log , imsg );
      return result;
    }

    if ( StringUtils.startsWithIgnoreCase( varName , "<#USER_" ) ) {
      result = processReservedVariableMobileUser( log , varName , varIndex ,
          imsg );
      return result;
    }

    if ( StringUtils.startsWithIgnoreCase( varName , "<#LIST_" ) ) {
      result = processReservedVariableContactList( log , varName , varIndex ,
          imsg );
      return result;
    }

    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private static boolean processReservedVariableMobileUser( TransactionLog log ,
      String varName , int varIndex , TransactionInputMessage imsg ) {
    boolean result = false;
    if ( varName == null ) {
      return result;
    }

    if ( varName.equalsIgnoreCase( "<#USER_EMAIL#>" ) ) {
      result = EventExpectUserEmail.processExpect( log , imsg );
      return result;
    }

    result = true;
    return result;
  }

  private static boolean processReservedVariableContactList(
      TransactionLog log , String varName , int varIndex ,
      TransactionInputMessage imsg ) {
    boolean result = false;
    if ( varName == null ) {
      return result;
    }

    result = true;
    return result;
  }

}
