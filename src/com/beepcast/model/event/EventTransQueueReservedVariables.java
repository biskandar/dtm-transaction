package com.beepcast.model.event;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueSupport;

public class EventTransQueueReservedVariables {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static String replaceReservedVariableWithValue( String text ,
      String var , String value ) {
    String result = null;

    if ( ( text == null ) || ( text.equals( "" ) ) ) {
      result = text;
      return result;
    }

    if ( ( var == null ) || ( var.equals( "" ) ) ) {
      result = text;
      return result;
    }

    var = "<%".concat( var ).concat( "%>" );
    value = ( value == null ) ? "" : value;

    int index = 0 , maxLoop = 15;
    int textLength = text.length();
    StringBuffer sbResult = new StringBuffer();
    do {

      // validate is there any reserved variable
      int p1 = text.indexOf( "<%" , index );
      if ( p1 < 0 ) {
        break;
      }
      int p2 = text.indexOf( "%>" , p1 + 2 );
      if ( p2 < 0 ) {
        break;
      }

      // resolve the var from text
      String textVar = text.substring( p1 , p2 + 2 );

      // keep the text var is clean
      textVar = ( textVar == null ) ? "" : textVar;

      // compose the result
      sbResult.append( text.substring( index , p1 ) );

      // is the var match , if not bring back the text var
      if ( textVar.equals( var ) ) {
        sbResult.append( value );
      } else {
        sbResult.append( textVar );
      }

      // go to the next index pointer
      index = p2 + 2;

      // decrease the max loop , to protect forever looping
      maxLoop = maxLoop - 1;

    } while ( maxLoop > 0 );

    sbResult.append( text.substring( index , textLength ) );

    result = sbResult.toString();
    return result;
  }

  public static ProcessBean replaceReservedVariables( TransactionLog log ,
      ProcessBean pBean , TransactionQueueBean tqBean ) {
    return replaceReservedVariables( null , log , pBean , tqBean );
  }

  public static ProcessBean replaceReservedVariables( String headerLog ,
      TransactionLog log , ProcessBean pBean , TransactionQueueBean tqBean ) {
    headerLog = ( headerLog == null ) ? "" : headerLog;
    if ( pBean == null ) {
      log.warning( headerLog + "Failed to replace reserved variables "
          + ", found null process bean " );
      return pBean;
    }
    try {
      // replace response reserved vars
      String response = pBean.getResponse();
      if ( !StringUtils.isBlank( response ) ) {
        log.debug( headerLog + "Trying to replace processBean's response "
            + "with reserved vars" );
        String newResponse = replaceReservedVariables( headerLog , log ,
            response , tqBean );
        pBean.setResponse( newResponse );
      }
      // replace rfa reserved vars
      String rfa = pBean.getRfa();
      if ( !StringUtils.isBlank( rfa ) ) {
        log.debug( headerLog + "Trying to replace processBean's rfa "
            + "with reserved vars" );
        String newRfa = replaceReservedVariables( headerLog , log , rfa ,
            tqBean );
        pBean.setRfa( newRfa );
      }
    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to replace reserved variables , " + e );
    }
    return pBean;
  }

  public static String replaceReservedVariables( TransactionLog log ,
      String strResponse , TransactionQueueBean tqBean ) {
    return replaceReservedVariables( null , log , strResponse , tqBean );
  }

  public static String replaceReservedVariables( String headerLog ,
      TransactionLog log , String strResponse , TransactionQueueBean tqBean ) {
    String strResult = null;
    if ( tqBean == null ) {
      log.warning( headerLog + "Failed to replace reserved variables "
          + ", found null trans queue bean" );
      strResult = strResponse;
      return strResult;
    }
    Map mapParams = TransactionQueueSupport.readMapParams( tqBean );
    return replaceReservedVariables( headerLog , log , strResponse , mapParams );
  }

  public static String replaceReservedVariables( String headerLog ,
      TransactionLog log , String strResponse , Map mapParams ) {
    String strResult = null;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params

    if ( strResponse == null ) {
      return strResult;
    }

    // compose regex pattern

    Pattern pattern1 = Pattern
        .compile( "<%([a-zA-Z0-9_]+)[,.]*([a-zA-Z0-9().,]*)%>" );
    Pattern pattern2 = Pattern
        .compile( "([a-zA-Z]*)[(),]*([0-9]*)[(),]*([0-9]*)" );

    // <%TEST_A%> : varName = TEST_A , parName = , funcName = null ,
    // funcParamA = null , funcParamB = null
    // <%TEST_A,3%> : varName = TEST_A , parName = 3 , funcName = ,
    // funcParamA = 3 , funcParamB =
    // <%TEST_A(3)%> : varName = TEST_A , parName = (3) , funcName = ,
    // funcParamA = 3 , funcParamB =
    // <%TEST_A.LEFT(3)%> : varName = TEST_A , parName = LEFT(3) , funcName
    // = LEFT , funcParamA = 3 , funcParamB =
    // <%TEST_A.SUB(1,3)%> : varName = TEST_A , parName = SUB(1,3) ,
    // funcName = SUB , funcParamA = 1 , funcParamB = 3

    StringBuffer sbResult = new StringBuffer();
    Matcher matcher1 = pattern1.matcher( strResponse );
    while ( matcher1.find() ) {
      String strReplace = null;
      String varName = null , parName = null;
      String funcName = null , funcParamA = null , funcParamB = null;
      try {

        // dig out is there any function under reserved variable
        varName = matcher1.group( 1 );
        parName = matcher1.group( 2 );
        if ( ( parName != null ) && ( !parName.equals( "" ) ) ) {
          Matcher matcher2 = pattern2.matcher( parName );
          if ( matcher2.find() ) {
            funcName = matcher2.group( 1 );
            funcParamA = matcher2.group( 2 );
            funcParamB = matcher2.group( 3 );
          }
        }

        // replace and clean the reserved variable
        strReplace = replaceReservedVariable( log , varName , parName ,
            funcName , funcParamA , funcParamB , mapParams );
        log.debug( headerLog + "Replaced reserved variable : varName = "
            + varName + " , parName = " + parName + " , funcName = " + funcName
            + " , funcParamA = " + funcParamA + " , funcParamB = " + funcParamB
            + " , strResult = " + StringEscapeUtils.escapeJava( strReplace ) );

      } catch ( Exception e ) {
        log.warning( headerLog
            + "Failed to replace reserved variable : varName = " + varName
            + " , parName = " + parName + " , funcName = " + funcName
            + " , funcParamA = " + funcParamA + " , funcParamB = " + funcParamB
            + " , cause : " + e );
      }

      // make sure all variable is replaceable
      strReplace = ( strReplace == null ) ? "" : strReplace;
      matcher1.appendReplacement( sbResult ,
          Matcher.quoteReplacement( strReplace ) );

    } // while ( matcher1.find() )
    matcher1.appendTail( sbResult );
    strResult = sbResult.toString();
    return strResult;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private static String replaceReservedVariable( TransactionLog log ,
      String varName , String parName , String funcName , String funcParamA ,
      String funcParamB , Map mapParams ) {
    String result = null;

    if ( varName == null ) {
      return result;
    }

    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      result = replaceReservedVariableMapParams( log , varName , mapParams );
    }

    result = ( result == null ) ? "" : result.trim();

    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      return result;
    }

    if ( ( parName == null ) || ( parName.equals( "" ) ) ) {
      return result;
    }

    if ( ( funcName == null ) || ( funcName.equals( "" ) ) ) {
      funcName = "LEFT";
    }

    if ( funcName.equalsIgnoreCase( "LEFT" ) ) {
      try {
        int intFuncParamA = Integer.parseInt( funcParamA );
        if ( intFuncParamA < result.length() ) {
          result = result.substring( 0 , intFuncParamA );
        }
      } catch ( Exception e ) {
        log.warning( "Failed to replace variable with : funcName = " + funcName
            + " , funcParamA = " + funcParamA + " , funcParamB = " + funcParamB
            + " , " + e );
      }
    }

    if ( funcName.equalsIgnoreCase( "RIGHT" ) ) {
      try {
        int intFuncParamA = Integer.parseInt( funcParamA );
        int startIdx = result.length() - intFuncParamA;
        if ( startIdx > 0 ) {
          result = result.substring( startIdx );
        }
      } catch ( Exception e ) {
        log.warning( "Failed to replace variable with : funcName = " + funcName
            + " , funcParamA = " + funcParamA + " , funcParamB = " + funcParamB
            + " , " + e );
      }
    }

    if ( funcName.equalsIgnoreCase( "SUB" ) ) {
      try {
        int intFuncParamA = Integer.parseInt( funcParamA );
        int intFuncParamB = Integer.parseInt( funcParamB );
        if ( ( intFuncParamA < intFuncParamB )
            && ( intFuncParamB < result.length() ) ) {
          result = result.substring( intFuncParamA , intFuncParamB );
        }
      } catch ( Exception e ) {
        log.warning( "Failed to replace variable with : funcName = " + funcName
            + " , funcParamA = " + funcParamA + " , funcParamB = " + funcParamB
            + " , " + e );
      }
    }

    return result;
  }

  private static String replaceReservedVariableMapParams( TransactionLog log ,
      String varName , Map mapParams ) {
    String result = null;
    if ( ( mapParams == null ) || ( mapParams.size() < 1 ) ) {
      return result;
    }
    Iterator iterKey = mapParams.keySet().iterator();
    while ( iterKey.hasNext() ) {
      String key = (String) iterKey.next();
      if ( key == null ) {
        continue;
      }
      String val = (String) mapParams.get( key );
      if ( val == null ) {
        continue;
      }
      if ( varName.equals( key ) ) {
        result = val;
        break;
      }
    }
    return result;
  }

}
