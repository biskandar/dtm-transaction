package com.beepcast.model.event;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionMessage;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionSupport;
import com.beepcast.model.util.DateTimeFormat;

public class EventProcessDelaySend {

  public static boolean process( String headerLog , TransactionSupport support ,
      TransactionLog log , TransactionQueueBean tqBean , ProcessBean pBean ,
      TransactionInputMessage imsg , List omsgs ) {
    boolean result = false;

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params
    if ( tqBean == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null trans queue bean" );
      return result;
    }
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
        || ( !pBean.getParamLabel().equalsIgnoreCase( "DATE=" ) ) ) {
      log.warning( headerLog + "Failed to process "
          + ", found invalid param label = " + pBean.getParamLabel() );
      return result;
    }

    // initialize the date previous

    Calendar calPrev = Calendar.getInstance();
    if ( imsg.getDateCreated() != null ) {
      calPrev.setTime( imsg.getDateCreated() );
    }
    Date datePrev = DateTimeFormat.convertToDate( (String) imsg
        .getMessageParam( TransactionMessageParam.HDR_SET_SENDDATESTR ) );
    if ( datePrev != null ) {
      calPrev.setTime( datePrev );
    }
    String datePrevStr = DateTimeFormat.convertToString( calPrev.getTime() );

    // parse the delay date format
    // sample = +months:2 +days:2 hours:13 minutes:00

    String[] arrNames = null;
    try {
      String strNames = StringUtils.join( pBean.getNames() );
      strNames = strNames.replaceAll( "[ ]*:[ ]*" , ":" );
      arrNames = strNames.split( "[ ,;]+" );
      log.debug( headerLog + "Read delay date send format : "
          + Arrays.asList( arrNames ) );
    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to read delay date send format , " + e );
    }
    if ( arrNames == null ) {
      log.warning( headerLog + "Failed to process "
          + ", found null array of names" );
      return result;
    }

    for ( int idxNames = 0 ; idxNames < arrNames.length ; idxNames++ ) {
      if ( ( arrNames[idxNames] == null ) || ( arrNames[idxNames].equals( "" ) ) ) {
        continue;
      }
      String[] arrName = arrNames[idxNames].split( ":" );
      if ( ( arrName == null ) || ( arrName.length < 2 ) ) {
        continue;
      }
      String field = arrName[0];
      if ( ( field == null ) || ( field.equals( "" ) ) ) {
        continue;
      }
      String value = arrName[1];
      if ( ( value == null ) || ( value.equals( "" ) ) ) {
        continue;
      }
      try {
        int valInt = Integer.parseInt( value );

        boolean added = false;
        if ( field.startsWith( "+" ) ) {
          added = true;
          field = field.substring( 1 );
        }

        if ( field.equalsIgnoreCase( "year" )
            || field.equalsIgnoreCase( "years" ) ) {
          if ( added ) {
            calPrev.add( Calendar.YEAR , valInt );
          } else {
            calPrev.set( Calendar.YEAR , valInt );
          }
        }
        if ( field.equalsIgnoreCase( "month" )
            || field.equalsIgnoreCase( "months" ) ) {
          if ( added ) {
            calPrev.add( Calendar.MONTH , valInt );
          } else {
            calPrev.set( Calendar.MONTH , valInt );
          }
        }
        if ( field.equalsIgnoreCase( "week" )
            || field.equalsIgnoreCase( "weeks" ) ) {
          if ( added ) {
            calPrev.add( Calendar.WEEK_OF_MONTH , valInt );
          } else {
            calPrev.set( Calendar.WEEK_OF_MONTH , valInt );
          }
        }
        if ( field.equalsIgnoreCase( "day" ) || field.equalsIgnoreCase( "days" ) ) {
          if ( added ) {
            calPrev.add( Calendar.DAY_OF_MONTH , valInt );
          } else {
            calPrev.set( Calendar.DAY_OF_MONTH , valInt );
          }
        }
        if ( field.equalsIgnoreCase( "hour" )
            || field.equalsIgnoreCase( "hours" ) ) {
          if ( added ) {
            calPrev.add( Calendar.HOUR_OF_DAY , valInt );
          } else {
            calPrev.set( Calendar.HOUR_OF_DAY , valInt );
          }
        }
        if ( field.equalsIgnoreCase( "minute" )
            || field.equalsIgnoreCase( "minutes" ) ) {
          if ( added ) {
            calPrev.add( Calendar.MINUTE , valInt );
          } else {
            calPrev.set( Calendar.MINUTE , valInt );
          }
        }
        if ( field.equalsIgnoreCase( "second" )
            || field.equalsIgnoreCase( "seconds" ) ) {
          if ( added ) {
            calPrev.add( Calendar.SECOND , valInt );
          } else {
            calPrev.set( Calendar.SECOND , valInt );
          }
        }

      } catch ( Exception e ) {
        continue;
      }
    }

    // read the next date time schedule to send

    String dateNextStr = DateTimeFormat.convertToString( calPrev.getTime() );

    // log it

    log.debug( headerLog + "Updated the new date send : " + datePrevStr
        + " -> " + dateNextStr );

    // store into the last output message param if exist
    TransactionMessage transMsg = null;
    if ( omsgs.size() > 0 ) {
      transMsg = (TransactionMessage) omsgs.get( omsgs.size() - 1 );
      log.debug( headerLog + "Set transaction msg as the last of "
          + "list output messages" );
    }
    if ( transMsg == null ) {
      transMsg = imsg;
      log.debug( headerLog + "Set transaction msg as the input message" );
    }

    // store input message param if found transaction message exist
    if ( transMsg != null ) {
      transMsg.addMessageParam( TransactionMessageParam.HDR_SET_SENDDATESTR ,
          dateNextStr );
      log.debug( headerLog + "Updated transaction msg params ["
          + transMsg.getMessageId() + "] : "
          + TransactionMessageParam.HDR_SET_SENDDATESTR + " = " + dateNextStr );
    }

    result = true;
    return result;
  }

}
