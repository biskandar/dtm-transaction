package com.beepcast.model.transaction;

import java.util.Calendar;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.model.event.ProcessBean;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionProcessBeanUtils {

  // ///////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // ///////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "TransactionProcessBeanUtils" );

  public static final String STR_STEP_END = "END";
  public static final int INT_STEP_END = -1;
  public static final int INT_STEP_UNKNOWN = -2;

  public static final String PROCESS_TYPE_GROUP_ALL = "ALL";
  public static final String PROCESS_TYPE_GROUP_WORDS = "WORDS";
  public static final String PROCESS_TYPE_GROUP_CODES = "CODES";

  public static final String PROCESS_TYPE_VAR = "VAR";
  public static final String PROCESS_TYPE_EXPECT = "EXPECT";
  public static final String PROCESS_TYPE_FIRST_WORD = "FIRST_WORD";
  public static final String PROCESS_TYPE_CONTAIN_WORD = "CONTAIN_WORD";

  public static final String PROCESS_TYPE_CODE = "CODE";
  public static final String PROCESS_TYPE_CODE_ON_WEEKDAYS = "CODE ON WEEKDAYS";
  public static final String PROCESS_TYPE_CODE_ON_WEEKEND = "CODE ON WEEKEND";
  public static final String PROCESS_TYPE_PARAM = "PARAM";

  public static final String PROCESS_TYPE_CREATE_QR_IMAGE = "CREATE_QR_IMAGE";

  public static final String PROCESS_NAME_DEFAULT = "DEFAULT";

  // ///////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // ///////////////////////////////////////////////////////////////////////////

  public static int getProcessStep( TransactionLog log , ProcessBean processBean ) {
    int processStep = INT_STEP_UNKNOWN;
    if ( processBean == null ) {
      log.warning( "Failed to get process step , found null process bean" );
      return processStep;
    }
    String strStep = processBean.getStep();
    if ( StringUtils.isBlank( strStep ) ) {
      log.warning( "Failed to get process step , found blank process step" );
      return processStep;
    }
    if ( StringUtils.equalsIgnoreCase( strStep , STR_STEP_END ) ) {
      processStep = INT_STEP_END;
      return processStep;
    }
    try {
      int intStep = Integer.parseInt( strStep );
      if ( intStep >= 0 ) {
        processStep = intStep - 1;
      }
    } catch ( NumberFormatException e ) {
      log.warning( "Failed to get process step , " + e );
      return processStep;
    }
    return processStep;
  }

  public static int getProcessNextStep( TransactionLog log ,
      ProcessBean processBean ) {
    int processNextStep = INT_STEP_UNKNOWN;
    if ( processBean == null ) {
      log.warning( "Failed to get process next step "
          + ", found null process bean" );
      return processNextStep;
    }
    String strNextStep = processBean.getNextStep();
    if ( StringUtils.isBlank( strNextStep ) ) {
      log.warning( "Failed to get process next step "
          + ", found blank process next step" );
      return processNextStep;
    }
    if ( StringUtils.equalsIgnoreCase( strNextStep , STR_STEP_END ) ) {
      processNextStep = INT_STEP_END;
      return processNextStep;
    }
    try {
      int intNextStep = Integer.parseInt( strNextStep );
      if ( intNextStep >= 0 ) {
        processNextStep = intNextStep - 1;
      }
    } catch ( NumberFormatException e ) {
      log.warning( "Failed to get process next step , " + e );
      return processNextStep;
    }
    return processNextStep;
  }

  public static int firstScopeProcessStep( TransactionLog log ,
      int currentStep , String processTypeGroup , ProcessBean[] processSteps ) {
    int step = currentStep;
    for ( ; step > -1 ; step-- ) {
      ProcessBean pb = processSteps[step];
      if ( pb == null ) {
        break;
      }
      String pt = pb.getType();
      if ( StringUtils.isBlank( pt ) ) {
        break;
      }
      if ( StringUtils.equals( processTypeGroup , PROCESS_TYPE_GROUP_ALL ) ) {
        if ( !validProcessTypeCodes( pt ) && !validProcessTypeWords( pt ) ) {
          step = step + 1;
          break;
        }
      }
      if ( StringUtils.equals( processTypeGroup , PROCESS_TYPE_GROUP_CODES ) ) {
        if ( !validProcessTypeCodes( pt ) ) {
          step = step + 1;
          break;
        }
      }
      if ( StringUtils.equals( processTypeGroup , PROCESS_TYPE_GROUP_WORDS ) ) {
        if ( !validProcessTypeWords( pt ) ) {
          step = step + 1;
          break;
        }
      }
    }
    if ( step < 0 ) {
      step = 0;
    }
    return step;
  }

  public static int lastScopeProcessStep( TransactionLog log , int currentStep ,
      String processTypeGroup , ProcessBean[] processSteps ) {
    int step = currentStep;
    for ( ; step < processSteps.length ; step++ ) {
      ProcessBean pb = processSteps[step];
      if ( pb == null ) {
        break;
      }
      String pt = pb.getType();
      if ( StringUtils.isBlank( pt ) ) {
        break;
      }
      if ( StringUtils.equals( processTypeGroup , PROCESS_TYPE_GROUP_ALL ) ) {
        if ( !validProcessTypeCodes( pt ) && !validProcessTypeWords( pt ) ) {
          step = step - 1;
          break;
        }
      }
      if ( StringUtils.equals( processTypeGroup , PROCESS_TYPE_GROUP_CODES ) ) {
        if ( !validProcessTypeCodes( pt ) ) {
          step = step - 1;
          break;
        }
      }
      if ( StringUtils.equals( processTypeGroup , PROCESS_TYPE_GROUP_WORDS ) ) {
        if ( !validProcessTypeWords( pt ) ) {
          step = step - 1;
          break;
        }
      }
    }
    if ( step > ( processSteps.length - 1 ) ) {
      step = processSteps.length - 1;
    }
    return step;
  }

  public static void debugScopeProcessStep( TransactionLog log ,
      ProcessBean[] processSteps , int idxLeft , int idxRight ) {
    int totalStep = 0;
    StringBuffer sbTypeNames = null;
    for ( int idxStep = idxLeft ; idxStep <= idxRight ; idxStep++ ) {
      ProcessBean p = processSteps[idxStep];
      if ( p == null ) {
        continue;
      }
      String type = p.getType();
      if ( type == null ) {
        continue;
      }
      String names = StringUtils.join( p.getNames() , "," );
      if ( names == null ) {
        continue;
      }
      if ( sbTypeNames == null ) {
        sbTypeNames = new StringBuffer();
      } else {
        sbTypeNames.append( ", " );
      }
      totalStep = totalStep + 1;
      sbTypeNames.append( "[" + type + " : " + names + "] " );
    }
    String strTypeNames = null;
    if ( sbTypeNames != null ) {
      strTypeNames = sbTypeNames.toString();
    }
    log.debug( "Defined scope process steps , step : total = " + totalStep
        + " , left = " + idxLeft + " , right = " + idxRight
        + " ; type names = " + strTypeNames );
  }

  public static boolean validProcessTypeAll( String processType ) {
    boolean result = false;
    if ( processType == null ) {
      return result;
    }
    result = validProcessTypeCodes( processType );
    if ( result ) {
      return result;
    }
    result = validProcessTypeWords( processType );
    if ( result ) {
      return result;
    }
    return result;
  }

  public static boolean validProcessTypeCodes( String processType ) {
    boolean result = false;
    if ( processType == null ) {
      return result;
    }
    if ( processType.equals( PROCESS_TYPE_CODE ) ) {
      result = true;
    }
    if ( processType.equals( PROCESS_TYPE_CODE_ON_WEEKDAYS ) ) {
      result = true;
    }
    if ( processType.equals( PROCESS_TYPE_CODE_ON_WEEKEND ) ) {
      result = true;
    }
    if ( processType.equals( PROCESS_TYPE_PARAM ) ) {
      result = true;
    }
    return result;
  }

  public static boolean validProcessTypeWords( String processType ) {
    boolean result = false;
    if ( processType == null ) {
      return result;
    }
    if ( processType.equals( PROCESS_TYPE_VAR ) ) {
      result = true;
    }
    if ( processType.equals( PROCESS_TYPE_EXPECT ) ) {
      result = true;
    }
    if ( processType.equals( PROCESS_TYPE_FIRST_WORD ) ) {
      result = true;
    }
    if ( processType.equals( PROCESS_TYPE_CONTAIN_WORD ) ) {
      result = true;
    }
    return result;
  }

  public static boolean matchProcessTypeCodes( TransactionLog log ,
      String processStep , String processType , String processName ,
      String messageRequest ) {
    boolean result = false;

    if ( StringUtils.isBlank( processType ) ) {
      log.warning( "Failed to match process step codes "
          + ", found blank process step" );
      return result;
    }

    if ( !validProcessTypeCodes( processType ) ) {
      log.warning( "Failed to match process step codes "
          + ", found invalid process step = " + processType );
      return result;
    }

    if ( StringUtils.isBlank( processName ) ) {
      log.warning( "Failed to match process step codes "
          + ", found blank process name" );
      return result;
    }

    if ( StringUtils.isBlank( messageRequest ) ) {
      log.warning( "Failed to match process step codes "
          + ", found blank message request" );
      return result;
    }

    if ( processType.equals( PROCESS_TYPE_CODE_ON_WEEKDAYS ) ) {
      Calendar nowCal = Calendar.getInstance();
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.SATURDAY ) {
        return result;
      }
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.SUNDAY ) {
        return result;
      }
      log.debug( "Resolved process type as code on weekdays" );
      result = true;
    }

    if ( processType.equals( PROCESS_TYPE_CODE_ON_WEEKEND ) ) {
      Calendar nowCal = Calendar.getInstance();
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.MONDAY ) {
        return result;
      }
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.TUESDAY ) {
        return result;
      }
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.WEDNESDAY ) {
        return result;
      }
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.THURSDAY ) {
        return result;
      }
      if ( nowCal.get( Calendar.DAY_OF_WEEK ) == Calendar.FRIDAY ) {
        return result;
      }
      log.debug( "Resolved process type as code on weekend" );
      result = true;
    }

    if ( processType.equals( PROCESS_TYPE_CODE ) ) {
      if ( StringUtils.equals( processStep , "1" ) ) {
        log.debug( "Resolved process type as code" );
        result = true;
      }
    }

    if ( processType.equals( PROCESS_TYPE_PARAM ) ) {
      if ( StringUtils.equals( processStep , "1" ) ) {
        log.debug( "Resolved process type as param" );
        result = true;
      }
    }

    if ( result ) {
      log.debug( "Found resolved process : type = " + processType
          + " , step = " + processStep );
    } else {
      log.warning( "Found unresolved process : type = " + processType
          + " , step = " + processStep );
    }

    return result;
  }

  public static boolean matchProcessTypeWords( TransactionLog log ,
      String processStep , String processType , String processName ,
      int messageType , String messageContent ) {
    boolean result = false;

    if ( StringUtils.isBlank( processType ) ) {
      log.warning( "Failed to match process step words "
          + ", found blank process step" );
      return result;
    }

    if ( !validProcessTypeWords( processType ) ) {
      log.warning( "Failed to match process step words "
          + ", found invalid process step = " + processType );
      return result;
    }

    if ( StringUtils.isBlank( processName ) ) {
      log.warning( "Failed to match process step words "
          + ", found blank process name" );
      return result;
    }

    if ( StringUtils.isBlank( messageContent ) ) {
      log.warning( "Failed to match process step words "
          + ", found blank message content" );
      return result;
    }

    String cleanMessageContent = cleanMessageContent( log , messageType ,
        messageContent );
    if ( StringUtils.isBlank( cleanMessageContent ) ) {
      log.warning( "Failed to match process step words "
          + ", found blank clean message content" );
      return result;
    }

    if ( processType.equals( PROCESS_TYPE_VAR ) ) {
      result = true; // just bypass as true
      return result;
    }

    if ( processType.equals( PROCESS_TYPE_EXPECT ) ) {
      result = StringUtils.equalsIgnoreCase( cleanMessageContent , processName );
      if ( !result ) {
        result = StringUtils.equalsIgnoreCase( processName ,
            PROCESS_NAME_DEFAULT );
      }
    }

    if ( processType.equals( PROCESS_TYPE_FIRST_WORD ) ) {
      result = StringUtils.startsWithIgnoreCase( cleanMessageContent ,
          processName );
    }

    if ( processType.equals( PROCESS_TYPE_CONTAIN_WORD ) ) {
      result = StringUtils.containsIgnoreCase( cleanMessageContent ,
          processName );
    }

    log.debug( "Found " + ( result ? "" : "un" )
        + "matched message request with process step : type = " + processType
        + " , name = " + processName + " , cleanMessageContent = "
        + StringEscapeUtils.escapeJava( cleanMessageContent ) );

    return result;
  }

  public static String cleanMessageContent( TransactionLog log ,
      int messageType , String messageContent ) {
    String result = null;
    if ( messageContent == null ) {
      return result;
    }
    if ( messageType == MessageType.TEXT_TYPE ) {
      StringBuffer sbResult = new StringBuffer();
      int len = messageContent.length();
      for ( int idx = 0 ; idx < len ; idx++ ) {
        char ch = messageContent.charAt( idx );
        if ( "~!@#$%^&*()+`=[]\\{}|;':\",.\\/<>?".indexOf( ch ) < 0 ) {
          sbResult.append( ch );
        }
      }
      result = sbResult.toString().trim();
      return result;
    } // if ( messageType == MessageType.TEXT_TYPE )
    result = messageContent.trim();
    return result;
  }

}
