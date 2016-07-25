package com.beepcast.model.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.encrypt.EncryptApp;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.util.DateTimeFormat;
import com.beepcast.subscriber.ClientSubscriberBean;
import com.beepcast.subscriber.ClientSubscriberCustomBean;
import com.beepcast.util.properties.GlobalEnvironment;

public class EventOutboundReservedVariables {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static String replaceReservedVariableWithValue( String text ,
      String var , String value ) {

    String temp = "";
    int index = 0;

    while ( true ) {

      int p1 = text.indexOf( "<#" , index );
      if ( p1 == -1 ) {
        break;
      }

      p1 += 2;
      int p2 = text.indexOf( "#>" , p1 );
      if ( p2 == -1 ) {
        break;
      }

      String thisVar = text.substring( p1 , p2 );
      if ( !thisVar.equals( var ) ) {
        break;
      }

      temp += text.substring( index , p1 - 2 );
      index = p2 + 2;

      temp += value;
    }

    temp += text.substring( index , text.length() );
    return temp;
  }

  public static ProcessBean replaceReservedVariables( TransactionLog log ,
      ProcessBean pBean , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg ) {
    return replaceReservedVariables( null , log , pBean , tqBean , imsg );
  }

  public static ProcessBean replaceReservedVariables( String headerLog ,
      TransactionLog log , ProcessBean pBean , TransactionQueueBean tqBean ,
      TransactionInputMessage imsg ) {
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
            response , imsg );
        pBean.setResponse( newResponse );
      }
      // replace rfa reserved vars
      String rfa = pBean.getRfa();
      if ( !StringUtils.isBlank( rfa ) ) {
        log.debug( headerLog + "Trying to replace processBean's rfa "
            + "with reserved vars" );
        String newRfa = replaceReservedVariables( headerLog , log , rfa , imsg );
        pBean.setRfa( newRfa );
      }
    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to replace reserved variables , " + e );
    }
    return pBean;
  }

  public static String replaceReservedVariables( TransactionLog log ,
      String strResponse , TransactionInputMessage imsg ) {
    return replaceReservedVariables( null , log , strResponse , imsg );
  }

  public static String replaceReservedVariables( String headerLog ,
      TransactionLog log , String strResponse , TransactionInputMessage imsg ) {
    String strResult = null;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params

    if ( strResponse == null ) {
      return strResult;
    }
    if ( imsg == null ) {
      log.warning( headerLog + "Failed to replace reserved variables "
          + ", found null input trans message" );
      strResult = strResponse;
      return strResult;
    }

    // compose regex pattern

    Pattern pattern1 = Pattern
        .compile( "<#([a-zA-Z0-9_]+)[,.]*([a-zA-Z0-9().,]*)#>" );
    Pattern pattern2 = Pattern
        .compile( "([a-zA-Z]*)[(),]*([0-9]*)[(),]*([0-9]*)" );

    // <#TEST_A#> : varName = TEST_A , parName = , funcName = null ,
    // funcParamA = null , funcParamB = null
    // <#TEST_A,3#> : varName = TEST_A , parName = 3 , funcName = ,
    // funcParamA = 3 , funcParamB =
    // <#TEST_A(3)#> : varName = TEST_A , parName = (3) , funcName = ,
    // funcParamA = 3 , funcParamB =
    // <#TEST_A.LEFT(3)#> : varName = TEST_A , parName = LEFT(3) , funcName
    // = LEFT , funcParamA = 3 , funcParamB =
    // <#TEST_A.SUB(1,3)#> : varName = TEST_A , parName = SUB(1,3) ,
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
            funcName , funcParamA , funcParamB , imsg );
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
      String funcParamB , TransactionInputMessage imsg ) {
    String result = null;

    if ( varName == null ) {
      return result;
    }

    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      result = replaceReservedVariableSystem( log , varName , funcParamA ,
          funcParamB , imsg );
    }
    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      result = replaceReservedVariableInputMessageParam( log , varName , imsg );
    }
    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      result = replaceReservedVariableMobileUser( log , varName , imsg );
    }
    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      result = replaceReservedVariableContactList( log , varName , imsg );
    }
    if ( ( result == null ) || ( result.equals( "" ) ) ) {
      result = replaceReservedVariableTransaction( log , varName , imsg );
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

    if ( funcName.equalsIgnoreCase( "QR" ) ) {
      try {
        String resultInBase64 = null;
        if ( result != null ) {
          EncryptApp encryptApp = EncryptApp.getInstance();
          resultInBase64 = encryptApp.base64Encode( result );
        }
        String qrGeneratorBaseUrl = null;
        if ( resultInBase64 != null ) {
          GlobalEnvironment globalEnv = GlobalEnvironment.getInstance();
          qrGeneratorBaseUrl = globalEnv
              .getProperty( "platform.qr-generator.base" );
        }
        if ( qrGeneratorBaseUrl != null ) {
          result = qrGeneratorBaseUrl.concat( "?textBase64=" ).concat(
              resultInBase64 );
        }
        if ( ( result != null ) && ( funcParamA != null )
            && ( !funcParamA.equals( "" ) ) ) {
          int qrSize = Integer.parseInt( funcParamA );
          result = result.concat( "&height=" )
              .concat( Integer.toString( qrSize ) ).concat( "&width=" )
              .concat( Integer.toString( qrSize ) );
        }
      } catch ( Exception e ) {
        log.warning( "Failed to replace variable with : funcName = " + funcName
            + " , funcParamA = " + funcParamA + " , funcParamB = " + funcParamB
            + " , " + e );
      }
    }

    return result;
  }

  private static String replaceReservedVariableSystem( TransactionLog log ,
      String varName , String funcParamA , String funcParamB ,
      TransactionInputMessage imsg ) {
    String result = null;
    if ( StringUtils.isBlank( varName ) ) {
      return result;
    }

    // system function for random codes
    if ( StringUtils.startsWithIgnoreCase( varName , "RANDOM_" ) ) {
      // setup random code length
      int randLength = 5;
      if ( ( funcParamA != null ) && ( !funcParamA.equals( "" ) ) ) {
        try {
          randLength = Integer.parseInt( funcParamA );
        } catch ( Exception e ) {
        }
      }
      // setup random code based on format
      if ( varName.equalsIgnoreCase( "RANDOM_NUMERIC" ) ) {
        result = RandomStringUtils.randomNumeric( randLength );
        return result;
      }
      if ( varName.equalsIgnoreCase( "RANDOM_ALPHABETIC" ) ) {
        result = RandomStringUtils.randomAlphabetic( randLength );
        return result;
      }
      if ( varName.equalsIgnoreCase( "RANDOM_ALPHANUMERIC" ) ) {
        result = RandomStringUtils.randomAlphanumeric( randLength );
        return result;
      }
    }

    return result;
  }

  private static String replaceReservedVariableInputMessageParam(
      TransactionLog log , String varName , TransactionInputMessage imsg ) {
    String result = null;
    if ( StringUtils.isBlank( varName ) ) {
      return result;
    }
    try {
      varName = TransactionMessageParam.HDR_PREFIX_SET_RESERVED_VARIABLE
          .concat( varName );
      result = (String) imsg.getMessageParam( varName );
    } catch ( Exception e ) {
    }
    return result;
  }

  private static String replaceReservedVariableMobileUser( TransactionLog log ,
      String varName , TransactionInputMessage imsg ) {
    String result = null;
    if ( !StringUtils.startsWithIgnoreCase( varName , "USER_" ) ) {
      return result;
    }

    MobileUserBean muBean = (MobileUserBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_MOBILE_USER_BEAN );
    if ( muBean == null ) {
      log.warning( "Failed to replace reserved variable mobile user for "
          + varName + " , found null mobile user bean" );
      return result;
    }

    if ( varName.equalsIgnoreCase( "USER_PHONE" ) ) {
      result = muBean.getPhone();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_PASSWORD" ) ) {
      result = muBean.getPassword();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_NAME" ) ) {
      result = muBean.getName();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_FIRST_NAME" ) ) {
      result = muBean.getName();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_EMAIL" ) ) {
      result = muBean.getEmail();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_PERSONAL_BEEP_ID" ) ) {
      result = muBean.getPersonalBeepID();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_CLIENT_BEEP_ID" ) ) {
      result = muBean.getClientBeepID();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_BIRTH_DATE" ) ) {
      result = DateTimeFormat.convertToString( muBean.getBirthDate() );
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_GENDER" ) ) {
      result = muBean.getGender();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_MARITAL_STATUS" ) ) {
      result = muBean.getMaritalStatus();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_HOST" ) ) {
      result = muBean.getHost();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_LAST_NAME" ) ) {
      result = muBean.getLastName();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_FAMILY_NAME" ) ) {
      result = muBean.getLastName();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_LAST_CODE" ) ) {
      result = muBean.getLastCode();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_COMPANY_NAME" ) ) {
      result = muBean.getCompanyName();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_IC" ) ) {
      result = muBean.getIc();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_ID" ) ) {
      result = muBean.getIc();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_MONTHLY_INCOME" ) ) {
      result = muBean.getMonthlyIncome();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_INDUSTRY" ) ) {
      result = muBean.getIndustry();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_OCCUPATION" ) ) {
      result = muBean.getOccupation();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_EDUCATION" ) ) {
      result = muBean.getEducation();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_MOBILE_MODEL" ) ) {
      result = muBean.getMobileModel();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_MOBILE_BRAND" ) ) {
      result = muBean.getMobileBrand();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_MOBILE_OPERATOR" ) ) {
      result = muBean.getMobileOperator();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_NUM_CHILDREN" ) ) {
      result = Integer.toString( muBean.getNumChildren() );
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_COUNTRY" ) ) {
      result = muBean.getCountry();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_DWELLING" ) ) {
      result = muBean.getDwelling();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_OFFICE_ZIP" ) ) {
      result = muBean.getOfficeZip();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_OFFICE_STREET" ) ) {
      result = muBean.getOfficeStreet();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_OFFICE_UNIT" ) ) {
      result = muBean.getOfficeUnit();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_OFFICE_BLK" ) ) {
      result = muBean.getOfficeBlk();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_HOME_ZIP" ) ) {
      result = muBean.getHomeZip();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_HOME_STREET" ) ) {
      result = muBean.getHomeStreet();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_HOME_UNIT" ) ) {
      result = muBean.getHomeUnit();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_HOME_BLK" ) ) {
      result = muBean.getHomeBlk();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_NATIONALITY" ) ) {
      result = muBean.getNationality();
      return result;
    }
    if ( varName.equalsIgnoreCase( "USER_SALUTATION" ) ) {
      result = muBean.getSalutation();
      return result;
    }

    return result;
  }

  private static String replaceReservedVariableContactList( TransactionLog log ,
      String varName , TransactionInputMessage imsg ) {
    String result = null;
    if ( !StringUtils.startsWithIgnoreCase( varName , "LIST_" ) ) {
      return result;
    }

    ClientSubscriberBean csrBean = (ClientSubscriberBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_BEAN );
    if ( csrBean == null ) {
      log.warning( "Failed to replace reserved variable contact list for "
          + varName + " , found null client subscriber bean" );
      return result;
    }

    if ( varName.equalsIgnoreCase( "LIST_CUSTREF_ID" ) ) {
      result = csrBean.getCustomerReferenceId();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTREFID" ) ) {
      result = csrBean.getCustomerReferenceId();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTREF_CODE" ) ) {
      result = csrBean.getCustomerReferenceCode();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTREFCODE" ) ) {
      result = csrBean.getCustomerReferenceCode();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_DATE_SEND" ) ) {
      result = DateTimeFormat.convertToString( csrBean.getDateSend() );
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_DATESEND" ) ) {
      result = DateTimeFormat.convertToString( csrBean.getDateSend() );
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_DESC" ) ) {
      result = csrBean.getDescription();
      return result;
    }

    ClientSubscriberCustomBean cscBean = (ClientSubscriberCustomBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_SUBSCRIBER_CUSTOM_BEAN );
    if ( cscBean == null ) {
      cscBean = csrBean.getCsCustomBean();
    }
    if ( cscBean == null ) {
      log.warning( "Failed to replace reserved variable contact list for "
          + varName + " , found null client subscriber custom bean" );
      return result;
    }

    if ( varName.equalsIgnoreCase( "LIST_CUSTOM0" ) ) {
      result = cscBean.getCustom0();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM1" ) ) {
      result = cscBean.getCustom1();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM2" ) ) {
      result = cscBean.getCustom2();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM3" ) ) {
      result = cscBean.getCustom3();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM4" ) ) {
      result = cscBean.getCustom4();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM5" ) ) {
      result = cscBean.getCustom5();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM6" ) ) {
      result = cscBean.getCustom6();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM7" ) ) {
      result = cscBean.getCustom7();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM8" ) ) {
      result = cscBean.getCustom8();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM9" ) ) {
      result = cscBean.getCustom9();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM10" ) ) {
      result = cscBean.getCustom10();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM11" ) ) {
      result = cscBean.getCustom11();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM12" ) ) {
      result = cscBean.getCustom12();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM13" ) ) {
      result = cscBean.getCustom13();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM14" ) ) {
      result = cscBean.getCustom14();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM15" ) ) {
      result = cscBean.getCustom15();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM16" ) ) {
      result = cscBean.getCustom16();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM17" ) ) {
      result = cscBean.getCustom17();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM18" ) ) {
      result = cscBean.getCustom18();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM19" ) ) {
      result = cscBean.getCustom19();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM20" ) ) {
      result = cscBean.getCustom20();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM21" ) ) {
      result = cscBean.getCustom21();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM22" ) ) {
      result = cscBean.getCustom22();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM23" ) ) {
      result = cscBean.getCustom23();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM24" ) ) {
      result = cscBean.getCustom24();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM25" ) ) {
      result = cscBean.getCustom25();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM26" ) ) {
      result = cscBean.getCustom26();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM27" ) ) {
      result = cscBean.getCustom27();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM28" ) ) {
      result = cscBean.getCustom28();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM29" ) ) {
      result = cscBean.getCustom29();
      return result;
    }
    if ( varName.equalsIgnoreCase( "LIST_CUSTOM30" ) ) {
      result = cscBean.getCustom30();
      return result;
    }

    return result;
  }

  private static String replaceReservedVariableTransaction( TransactionLog log ,
      String varName , TransactionInputMessage imsg ) {
    String result = null;
    if ( !StringUtils.startsWithIgnoreCase( varName , "TMSG_" ) ) {
      return result;
    }

    if ( imsg == null ) {
      log.warning( "Failed to replace reserved variable transaction for "
          + varName + " , found null inbound message" );
      return result;
    }

    if ( varName.equalsIgnoreCase( "TMSG_MESSAGE_CONTENT" ) ) {
      result = (String) imsg
          .getMessageParam( TransactionMessageParam.HDR_ORI_MESSAGE_CONTENT );
      if ( result == null ) {
        result = imsg.getMessageContent();
      }
      return result;
    }
    if ( varName.equalsIgnoreCase( "TMSG_MESSAGE_ID" ) ) {
      result = imsg.getMessageId();
      return result;
    }
    if ( varName.equalsIgnoreCase( "TMSG_ORIGINAL_ADDRESS" ) ) {
      result = imsg.getOriginalAddress();
      return result;
    }
    if ( varName.equalsIgnoreCase( "TMSG_DESTINATION_ADDRESS" ) ) {
      result = imsg.getDestinationAddress();
      return result;
    }

    return result;
  }

}
