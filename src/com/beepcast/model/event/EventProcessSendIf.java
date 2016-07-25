package com.beepcast.model.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;

public class EventProcessSendIf {

  public static int process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    int result = TransactionProcessBasic.NEXT_STEP_END;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null process bean" );
      return result;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null input message" );
      return result;
    }
    if ( omsgs == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null output messages" );
      return result;
    }

    if ( ( pBean.getParamLabel() == null )
        || ( !pBean.getParamLabel().equalsIgnoreCase( "CONDITION=" ) ) ) {
      log.warning( headerLog + "Failed to process "
          + ", found invalid param label = " + pBean.getParamLabel() );
      return result;
    }

    // prepare the condition
    boolean condition = false;

    // resolve the condition
    try {
      String strNames = StringUtils.join( pBean.getNames() , "," );
      strNames = strNames.trim();
      strNames = strNames.replaceAll( "[ ]*,[ ]*" , "<COMMA>" );
      strNames = strNames.replaceFirst( "[ ]+" , "<SPLITTER>" );
      strNames = strNames.replaceFirst( "[ ]+" , "<SPLITTER>" );
      strNames = EventTransQueueReservedVariables.replaceReservedVariables(
          headerLog , log , strNames , tqBean );
      strNames = EventOutboundReservedVariables.replaceReservedVariables(
          headerLog , log , strNames , imsg );
      log.debug( headerLog + "Resolved string names with reserved variables : "
          + StringEscapeUtils.escapeJava( strNames ) );
      String[] arrNames = strNames.split( "<SPLITTER>" );
      condition = resolveCondition( headerLog , log , arrNames );
    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to resolve the result condition , " + e );
    }

    // log it
    log.debug( headerLog + "Resolved the result condition as " + condition );

    // compose output message if condition true
    if ( condition ) {
      log.debug( headerLog + "Composing output message with "
          + "outsend step method" );
      EventProcessAutoSend.process( headerLog , support , log , tqBean , pBean ,
          imsg , omsgs );
    }

    // setup return result
    result = condition ? TransactionProcessBasic.NEXT_STEP_TRU
        : TransactionProcessBasic.NEXT_STEP_FAL;
    return result;
  }

  private static boolean resolveCondition( String headerLog ,
      TransactionLog log , String[] arrNames ) {
    boolean result = false;

    if ( ( arrNames == null ) || ( arrNames.length < 1 ) ) {
      log.warning( headerLog + "Failed to resolve condition "
          + ", found null array names" );
      return result;
    }
    if ( arrNames.length < 2 ) {
      result = resolveCondition( arrNames[0] , true );
    }

    String a = null , o = null , b = null;
    List la = new ArrayList() , lb = new ArrayList();

    if ( arrNames.length > 0 ) {
      a = arrNames[0];
      String[] arr = a.split( "<COMMA>" );
      for ( int idx = 0 ; idx < arr.length ; idx++ ) {
        if ( ( arr[idx] != null ) && ( !arr[idx].equals( "" ) ) ) {
          la.add( arr[idx] );
        }
      }
    }
    if ( arrNames.length > 1 ) {
      o = arrNames[1];
    }
    if ( arrNames.length > 2 ) {
      b = arrNames[2];
      String[] arr = b.split( "<COMMA>" );
      for ( int idx = 0 ; idx < arr.length ; idx++ ) {
        if ( ( arr[idx] != null ) && ( !arr[idx].equals( "" ) ) ) {
          lb.add( arr[idx] );
        }
      }
    }

    log.debug( headerLog + "Defined condition parameters : la = " + la
        + " , o = " + o + " , lb = " + lb );

    result = resolveCondition( headerLog , log , la , o , lb );
    return result;
  }

  private static boolean resolveCondition( String headerLog ,
      TransactionLog log , List la , String so , List lb ) {
    boolean result = false;
    if ( so == null ) {
      return result;
    }

    int operatorCode = -1;
    so = ",".concat( so.toLowerCase() );
    if ( ",eq,equal".indexOf( so ) > -1 ) {
      log.debug( headerLog + "Resolving condition with equal function" );
      operatorCode = 0;
    }
    if ( ",neq,not_equal".indexOf( so ) > -1 ) {
      log.debug( headerLog + "Resolving condition with not equal function" );
      operatorCode = 1;
    }
    if ( ",fw,first_word".indexOf( so ) > -1 ) {
      log.debug( headerLog + "Resolving condition with first word function" );
      operatorCode = 2;
    }
    if ( ",cw,contain_word".indexOf( so ) > -1 ) {
      log.debug( headerLog + "Resolving condition with contain word function" );
      operatorCode = 3;
    }

    Iterator ia = la.iterator();
    while ( ia.hasNext() ) {
      String a = (String) ia.next();
      Iterator ib = lb.iterator();
      while ( ib.hasNext() ) {
        String b = (String) ib.next();
        if ( operatorCode == 0 ) { // equal
          if ( ( a != null ) && ( b != null )
              && ( a.toLowerCase().equals( b.toLowerCase() ) ) ) {
            log.debug( headerLog + "Found matched equal : a = "
                + StringEscapeUtils.escapeJava( a ) + " , b = "
                + StringEscapeUtils.escapeJava( b ) );
            result = true;
            return result;
          }
        }
        if ( operatorCode == 1 ) { // not equal
          if ( ( a != null ) && ( b != null )
              && ( !a.toLowerCase().equals( b.toLowerCase() ) ) ) {
            log.debug( headerLog + "Found matched not equal : a = "
                + StringEscapeUtils.escapeJava( a ) + " , b = "
                + StringEscapeUtils.escapeJava( b ) );
            result = true;
            return result;
          }
        }
        if ( operatorCode == 2 ) { // first word
          if ( ( a != null )
              && ( b != null )
              && ( " ".concat( a ).toLowerCase().concat( " " ).startsWith( " "
                  .concat( b ).toLowerCase().concat( " " ) ) ) ) {
            log.debug( headerLog + "Found matched first word : a = "
                + StringEscapeUtils.escapeJava( a ) + " , b = "
                + StringEscapeUtils.escapeJava( b ) );
            result = true;
            return result;
          }
        }
        if ( operatorCode == 3 ) { // contain word
          if ( ( a != null )
              && ( b != null )
              && ( " ".concat( a ).toLowerCase().concat( " " )
                  .indexOf( " ".concat( b ).toLowerCase().concat( " " ) ) > -1 ) ) {
            log.debug( headerLog + "Found matched contain word : a = "
                + StringEscapeUtils.escapeJava( a ) + " , b = "
                + StringEscapeUtils.escapeJava( b ) );
            result = true;
            return result;
          }
        }
      } // while ( ib.hasNext() ) {
    } // while ( ia.hasNext() ) {

    return result;
  }

  private static boolean resolveCondition( String stringValue ,
      boolean defaultValue ) {
    boolean value = defaultValue;
    if ( StringUtils.isBlank( stringValue ) ) {
      value = false;
    } else {
      stringValue = ",".concat( stringValue.toLowerCase() );
      if ( ",true,ok,yes,1".indexOf( stringValue ) > -1 ) {
        value = true;
      }
      if ( ",false,nok,not,no,0".indexOf( stringValue ) > -1 ) {
        value = false;
      }
    }
    return value;
  }

}
