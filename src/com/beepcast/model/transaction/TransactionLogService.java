package com.beepcast.model.transaction;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientService;
import com.beepcast.model.client.TestPhoneBean;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.event.EventSupport;
import com.beepcast.model.util.DateTimeFormat;
import com.beepcast.util.StrTok;
import com.beepcast.util.Util;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionLogService {

  static final DLogContext lctx = new SimpleContext( "TransactionLogService" );

  private EventService eventService;
  private TransactionLogDAO transactionLogDAO;
  private TransactionQueueService transactionQueueService;

  public TransactionLogService() {
    eventService = new EventService();
    transactionLogDAO = new TransactionLogDAO();
    transactionQueueService = new TransactionQueueService();
  }

  public boolean insert( TransactionLogBean bean ) {
    return transactionLogDAO.insert( bean );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ) {
    return transactionLogDAO.select( fromDate , toDate , criteria , false , 0 );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc ) {
    return transactionLogDAO.select( fromDate , toDate , criteria , desc , 0 );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc , int recLimit ) {
    return transactionLogDAO.select( fromDate , toDate , criteria , desc ,
        recLimit );
  }

  public boolean update( TransactionLogBean bean ) {
    return transactionLogDAO.update( bean );
  }

  public boolean delete( long transLogId ) {
    return transactionLogDAO.delete( transLogId );
  }

  public boolean logTransaction( TransactionQueueBean transQueue ,
      int closedReasonId ) {
    return logTransaction( transQueue , false , true , closedReasonId );
  }

  public boolean logTransaction( TransactionQueueBean transQueue ,
      boolean simulation , int closedReasonId ) {
    return logTransaction( transQueue , simulation , true , closedReasonId );
  }

  public boolean logTransaction( TransactionQueueBean transQueue ,
      boolean simulation , boolean sendFirstTimeMsg , int closedReasonId ) {
    boolean result = false;

    // don't log reserved code transaction
    String code = transQueue.getCode();
    if ( code.startsWith( "?" ) ) {
      DLog.warning( lctx , "Bypass do log the transaction "
          + ", found internal message" );
      return result;
    }

    // get event bean
    EventBean eventBean = null;
    try {
      long eventID = transQueue.getPendingEventID();
      if ( eventID == 0 ) {
        eventID = transQueue.getEventID();
      }
      eventBean = eventService.select( eventID );
      if ( eventBean == null ) {
        DLog.warning( lctx , "Failed to retrieve event object "
            + "based on eventId and/or pendingEventId = " + eventID );
        return result;
      }
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to retrieve event object , " + e );
      return result;
    }

    // setup transaction log bean
    TransactionLogBean transLog = new TransactionLogBean();
    transLog.setClientID( eventBean.getClientID() );
    transLog.setEventID( eventBean.getEventID() );
    transLog.setNextStep( transQueue.getNextStep() );
    transLog.setCatagoryID( eventBean.getCatagoryID() );
    transLog.setDateTm( transQueue.getDateTm() );
    transLog.setPhone( transQueue.getPhone() );
    transLog
        .setProviderId( StringUtils.trimToEmpty( transQueue.getProviderId() ) );
    transLog.setCode( code );
    transLog.setParams( transQueue.getParams() );
    transLog.setJumpCount( transQueue.getJumpCount() );
    transLog.setLocationID( transQueue.getLocationID() );
    transLog.setClosedReasonId( closedReasonId );

    // minimum message count is 1
    int messageCount = transQueue.getMessageCount();
    messageCount = ( messageCount == 0 ) ? 1 : messageCount;
    transLog.setMessageCount( messageCount );

    // create permanent record
    result = insert( transLog );
    if ( !result ) {
      DLog.warning( lctx , "Failed to insert a new trans log" );
    }

    return result;
  }

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter ) throws IOException {
    return transactionLogDAO.getMessageCount( clientID , eventID , fromDate ,
        toDate , iter );
  } // getMessageCount()

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter , boolean jumpTo ) throws IOException {
    return transactionLogDAO.getMessageCount( clientID , eventID , fromDate ,
        toDate , iter , jumpTo );
  } // getMessageCount()

  public int getMobileUserCount( long clientID , long eventID , Date fromDate ,
      Date toDate ) throws IOException {
    return transactionLogDAO.getMobileUserCount( clientID , eventID , fromDate ,
        toDate );
  } // getMobileUserCount()

  public int getMobileUserCount( long clientID , long eventID , Date fromDate ,
      Date toDate , boolean combineWithTransQueue ) throws IOException {

    /*------------------------
     get distinct phone numbers
     ------------------------*/
    Vector vTransLog = transactionLogDAO.getMobileUsers( clientID , eventID ,
        fromDate , toDate );
    Vector vTransQueue = transactionQueueService.getMobileUsers( clientID ,
        eventID , fromDate , toDate );

    /*------------------------
     combine transLog and transQueue
     ------------------------*/
    HashSet hUsers = new HashSet( 1000 );
    for ( int i = 0 ; i < vTransLog.size() ; i++ )
      hUsers.add( (String) vTransLog.elementAt( i ) );
    for ( int i = 0 ; i < vTransQueue.size() ; i++ )
      hUsers.add( (String) vTransQueue.elementAt( i ) );

    /*------------------------
     return combined phone count
     ------------------------*/
    return hUsers.size();

  } // getMobileUserCount()

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code ) throws IOException {

    /*------------------------
     get row count for both tables
     ------------------------*/
    int transLogCount = transactionLogDAO.getRowCount( clientID , eventID ,
        fromDate , toDate , code );
    int transQueueCount = transactionQueueService.getRowCount( clientID ,
        eventID , fromDate , toDate , code );

    /*------------------------
     return combined row count
     ------------------------*/
    return transLogCount + transQueueCount;

  } // getRowCount()

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code , boolean jumpTo ) throws IOException {

    /*------------------------
     get row count for both tables
     ------------------------*/
    int transLogCount = transactionLogDAO.getRowCount( clientID , eventID ,
        fromDate , toDate , code , jumpTo );
    int transQueueCount = transactionQueueService.getRowCount( clientID ,
        eventID , fromDate , toDate , code , jumpTo );

    /*------------------------
     return combined row count
     ------------------------*/
    return transLogCount + transQueueCount;

  } // getRowCount()

  public String getHourlyLoad( Date fromDate , Date toDate , long eventID )
      throws IOException {

    String hourlyLoad = "";
    int buckets[] = new int[24]; // hours
    String criteria = null;

    /*------------------------
     set criteria
     ------------------------*/
    if ( eventID != 0 )
      criteria = "event_id=" + eventID;

    /*------------------------
     select records within date range
     ------------------------*/
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate , criteria );
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        criteria );

    /*------------------------
     build hourly buckets for trans log
     ------------------------*/
    for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );
      Calendar c = new GregorianCalendar();
      c.setTime( transLog.getDateTm() );
      int hour = c.get( Calendar.HOUR_OF_DAY );
      buckets[hour] += transLog.getMessageCount();
    }

    /*------------------------
     build hourly buckets for trans queue
     ------------------------*/
    for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );
      Calendar c = new GregorianCalendar();
      c.setTime( transQueue.getDateTm() );
      int hour = c.get( Calendar.HOUR_OF_DAY );
      buckets[hour] += transQueue.getMessageCount();
    }

    /*------------------------
     build string of hourly counts
     ------------------------*/
    for ( int i = 0 ; i < 24 ; i++ )
      hourlyLoad += buckets[i] + ",";
    if ( hourlyLoad.length() > 0 )
      hourlyLoad = hourlyLoad.substring( 0 , hourlyLoad.length() - 1 );

    return hourlyLoad;

  } // getHourlyLoad()

  public String getMinuteLoad( Date fromDate , Date toDate , long eventID )
      throws IOException {

    String minuteLoad = "";
    int buckets[] = new int[60]; // minutes
    String criteria = null;

    /*------------------------
     set criteria
     ------------------------*/
    if ( eventID != 0 )
      criteria = "event_id=" + eventID;

    /*------------------------
     select records within date range
     ------------------------*/
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate , criteria );
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        criteria );

    /*------------------------
     build minute buckets for trans log
     ------------------------*/
    for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );
      Calendar c = new GregorianCalendar();
      c.setTime( transLog.getDateTm() );
      int minute = c.get( Calendar.MINUTE );
      buckets[minute] += transLog.getMessageCount();
    }

    /*------------------------
     build minute buckets for trans queue
     ------------------------*/
    for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );
      Calendar c = new GregorianCalendar();
      c.setTime( transQueue.getDateTm() );
      int minute = c.get( Calendar.MINUTE );
      buckets[minute] += transQueue.getMessageCount();
    }

    /*------------------------
     build string of minute counts
     ------------------------*/
    for ( int i = 0 ; i < 60 ; i++ )
      minuteLoad += buckets[i] + ",";
    if ( minuteLoad.length() > 0 )
      minuteLoad = minuteLoad.substring( 0 , minuteLoad.length() - 1 );

    return minuteLoad;

  } // getMinuteLoad()

  public String getMenuSelections( Date fromDate , Date toDate ,
      EventBean eventBean , String menuStep , String menuLetters )
      throws IOException {
    String menuSelections = "";

    // log it
    DLog.debug( lctx , "Compose menu selections with : fromDate = "
        + DateTimeFormat.convertToString( fromDate ) + " , toDate = "
        + DateTimeFormat.convertToString( toDate ) + " , menuStep = "
        + menuStep + " , menuLetters = " + menuLetters );

    // setup buckets
    StringTokenizer st = new StringTokenizer( menuLetters , "," );
    int numMenuItems = st.countTokens();
    long buckets[] = new long[numMenuItems];
    String letters[] = new String[numMenuItems];
    try {
      for ( int i = 0 ; i < numMenuItems ; i++ ) {
        letters[i] = st.nextToken();
      }
    } catch ( Exception e ) {
    }

    // select records within date range
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        "event_id=" + eventBean.getEventID() , false , 0 );
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate ,
        "event_id=" + eventBean.getEventID() , false , 0 );

    // build buckets from trans queue
    for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );
      String params = transQueue.getParams();
      StringTokenizer st2 = new StringTokenizer( params , "," );
      int numParams = st2.countTokens();
      try {
        for ( int j = 0 ; j < numParams ; j++ ) {
          String param = st2.nextToken();
          StrTok st3 = new StrTok( param , "=" );
          String name = st3.nextTok();
          if ( name.equals( "MENU" + menuStep ) ) {
            String value = st3.nextTok();
            for ( int k = 0 ; k < letters.length ; k++ ) {
              if ( value.equalsIgnoreCase( letters[k] ) )
                buckets[k]++;
            }
          }
        }
      } catch ( Exception e ) {
      }
    }

    // build buckets from trans log
    for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );
      String params = transLog.getParams();
      StringTokenizer st2 = new StringTokenizer( params , "," );
      int numParams = st2.countTokens();
      try {
        for ( int j = 0 ; j < numParams ; j++ ) {
          String param = st2.nextToken();
          StrTok st3 = new StrTok( param , "=" );
          String name = st3.nextTok();
          if ( name.equals( "MENU" + menuStep ) ) {
            String value = st3.nextTok();
            for ( int k = 0 ; k < letters.length ; k++ ) {
              if ( value.equalsIgnoreCase( letters[k] ) )
                buckets[k]++;
            }
          }
        }
      } catch ( Exception e ) {
      }
    }

    // build string of menu selections
    for ( int i = 0 ; i < numMenuItems ; i++ ) {
      menuSelections += buckets[i] + ",";
    }
    if ( menuSelections.length() > 0 ) {
      menuSelections = menuSelections.substring( 0 ,
          menuSelections.length() - 1 );
    }

    // success
    return menuSelections;
  }

  public String getMenuSelectionsForFlash( Date fromDate , Date toDate ,
      EventBean _event , String menuStep , String menuLetters )
      throws IOException {

    return getMenuSelectionsForFlash( fromDate , toDate , _event , menuStep ,
        menuLetters , false );
  }

  public String getMenuSelectionsForFlash( Date fromDate , Date toDate ,
      EventBean _event , String menuStep , String menuLetters ,
      boolean excludeTestPhones ) throws IOException {

    String menuSelections = "&";
    String testPhones = "";

    /*------------------------
     get test phones
     ------------------------*/
    if ( excludeTestPhones ) {
      try {
        String phoneArray[] = new TestPhoneBean().select( _event.getClientID() );
        testPhones = StringUtils.join( phoneArray , "," );
      } catch ( Exception e ) {
      }
    }

    /*------------------------
     setup buckets
     ------------------------*/
    StringTokenizer st = new StringTokenizer( menuLetters , "," );
    int numMenuItems = st.countTokens();
    long buckets[] = new long[numMenuItems];
    String letters[] = new String[numMenuItems];
    try {
      for ( int i = 0 ; i < numMenuItems ; i++ )
        letters[i] = st.nextToken();
    } catch ( Exception e ) {
    }

    /*------------------------
     select records within date range
     ------------------------*/
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        "event_id=" + _event.getEventID() );
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate ,
        "event_id=" + _event.getEventID() );

    /*------------------------
     build buckets from trans queue
     ------------------------*/
    nextTransQueue: for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );

      // exclude test phones?
      if ( excludeTestPhones )
        if ( testPhones.indexOf( transQueue.getPhone() ) != -1 )
          continue nextTransQueue;

      // build buckets
      String params = transQueue.getParams();
      StringTokenizer st2 = new StringTokenizer( params , "," );
      int numParams = st2.countTokens();
      try {
        for ( int j = 0 ; j < numParams ; j++ ) {
          String param = st2.nextToken();
          StrTok st3 = new StrTok( param , "=" );
          String name = st3.nextTok();
          if ( name.equals( "MENU" + menuStep ) ) {
            String value = st3.nextTok();
            for ( int k = 0 ; k < letters.length ; k++ ) {
              if ( value.equalsIgnoreCase( letters[k] ) )
                buckets[k]++;
            }
          }
        }
      } catch ( Exception e ) {
      }
    }

    /*------------------------
     build buckets from trans log
     ------------------------*/
    nextTransLog: for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );

      // exclude test phones?
      if ( excludeTestPhones )
        if ( testPhones.indexOf( transLog.getPhone() ) != -1 )
          continue nextTransLog;

      // build buckets
      String params = transLog.getParams();
      StringTokenizer st2 = new StringTokenizer( params , "," );
      int numParams = st2.countTokens();
      try {
        for ( int j = 0 ; j < numParams ; j++ ) {
          String param = st2.nextToken();
          StrTok st3 = new StrTok( param , "=" );
          String name = st3.nextTok();
          if ( name.equals( "MENU" + menuStep ) ) {
            String value = st3.nextTok();
            for ( int k = 0 ; k < letters.length ; k++ ) {
              if ( value.equalsIgnoreCase( letters[k] ) )
                buckets[k]++;
            }
          }
        }
      } catch ( Exception e ) {
      }
    }

    /*------------------------
     build string of menu selections
     ------------------------*/
    for ( int i = 0 ; i < numMenuItems ; i++ )
      menuSelections += letters[i] + "=" + buckets[i] + "&";
    if ( menuSelections.length() > 1 )
      menuSelections = menuSelections.substring( 0 ,
          menuSelections.length() - 1 );

    // success
    return menuSelections;

  } // getMenuSelectionsForFlash()

  public void updateEventUsedBudget( TransactionLogBean transLog )
      throws IOException {

    ClientService clientService = new ClientService();
    ClientBean client = clientService.select( transLog.getClientID() );
    int messageCount = transLog.getMessageCount();
    long eventID = transLog.getEventID();
    EventBean _event = eventService.select( eventID );

    /*------------------------
     get sms charge & discounts
     ------------------------*/
    double smsCharge = new Double( client.getSmsCharge() ).doubleValue();
    double discounts[] = new double[4];
    double afterDiscounts[] = new double[4];
    StrTok st = new StrTok( client.getDiscount() , "," );
    for ( int i = 0 ; i < 4 ; i++ ) {
      discounts[i] = new Double( st.nextTok() ).doubleValue();
      afterDiscounts[i] = smsCharge * ( 1 - ( discounts[i] / 100 ) );
    }

    // events jumped to get full discount
    if ( transLog.getJumpCount() > 0 ) {
      for ( int i = 0 ; i < 3 ; i++ )
        afterDiscounts[i] = afterDiscounts[3];
    }

    /*------------------------
     accumulated charges
     ------------------------*/
    double charge = 0.0;
    if ( messageCount > 0 ) {
      charge += afterDiscounts[0];
      if ( _event.getChannel() ) // adjust for channel
        charge /= 2;
    }
    if ( messageCount > 1 )
      charge += afterDiscounts[1];
    if ( messageCount > 2 )
      charge += afterDiscounts[2];
    for ( int i = 4 ; i <= messageCount ; i++ )
      charge += afterDiscounts[3];

    /*------------------------
     udate event used budget
     ------------------------*/
    double usedBudget = _event.getUsedBudget();
    usedBudget += charge;
    double usedBudgetRound = (double) Math.round( usedBudget );
    _event.setUsedBudget( usedBudgetRound );
    eventService.update( _event );

  } // updateEventUsedBudget();

  public double getTransactionCharge( int messageCounts[] , ClientBean client ,
      boolean jumpTo , boolean channel ) throws IOException {

    /*------------------------
     get sms charge & discounts
     ------------------------*/
    double smsCharge = new Double( client.getSmsCharge() ).doubleValue();
    double discounts[] = new double[4];
    double afterDiscounts[] = new double[4];
    StrTok st = new StrTok( client.getDiscount() , "," );
    for ( int i = 0 ; i < 4 ; i++ ) {
      discounts[i] = new Double( st.nextTok() ).doubleValue();
      afterDiscounts[i] = smsCharge * ( 1 - ( discounts[i] / 100 ) );
    }

    // events jumped to get full discount
    if ( jumpTo ) {
      for ( int i = 0 ; i < 3 ; i++ )
        afterDiscounts[i] = afterDiscounts[3];
    }

    /*------------------------
     accumulated charges
     ------------------------*/
    double charge = 0.0;

    // 1st message
    double msgCount = ( channel ) ? (double) messageCounts[0] / 2
        : (double) messageCounts[0];
    charge += msgCount * afterDiscounts[0];

    // 2nd message
    msgCount = ( channel ) ? (double) messageCounts[1] / 4
        : (double) messageCounts[1] / 2;
    charge += msgCount * afterDiscounts[0];
    charge += (double) messageCounts[1] / 2 * afterDiscounts[1];

    // 3rd message
    msgCount = ( channel ) ? (double) messageCounts[2] / 6
        : (double) messageCounts[2] / 3;
    charge += msgCount * afterDiscounts[0];
    charge += (double) messageCounts[2] / 3 * afterDiscounts[1];
    charge += (double) messageCounts[2] / 3 * afterDiscounts[2];

    // 4th+ message
    msgCount = ( channel ) ? (double) messageCounts[3] / 8
        : (double) messageCounts[3] / 4;
    charge += msgCount * afterDiscounts[0];
    charge += (double) messageCounts[3] / 4 * afterDiscounts[1];
    charge += (double) messageCounts[3] / 4 * afterDiscounts[2];
    charge += (double) messageCounts[3] / 4 * afterDiscounts[3];

    // success
    return charge;

  } // getTransactionCharge();

  public String[] getCodeUsers( EventBean _event , Date fromDate , Date toDate )
      throws IOException {

    long eventID = _event.getEventID();
    long clientID = _event.getClientID();
    String codes[] = StringUtils.split( _event.getCodes() , "," );
    int users[] = new int[codes.length];
    String codeUsers[] = new String[codes.length];

    /*------------------------
     for each code....
     ------------------------*/
    for ( int i = 0 ; i < codes.length ; i++ ) {

      /*------------------------
        get distinct mobile users
      ------------------------*/
      Vector vTransLog = transactionLogDAO.getMobileUsers( clientID , eventID ,
          fromDate , toDate , codes[i] );
      Vector vTransQueue = transactionQueueService.getMobileUsers( clientID ,
          eventID , fromDate , toDate , codes[i] );

      /*------------------------
        combine transLog and transQueue
      ------------------------*/
      HashSet hUsers = new HashSet( 1000 );
      for ( int j = 0 ; j < vTransLog.size() ; j++ )
        hUsers.add( (String) vTransLog.elementAt( j ) );
      for ( int j = 0 ; j < vTransQueue.size() ; j++ )
        hUsers.add( (String) vTransQueue.elementAt( j ) );

      /*------------------------
        count total users
      ------------------------*/
      users[i] = hUsers.size();
    }

    /*------------------------
     sort by quantity desc
     ------------------------*/
    for ( int i = 0 ; i < users.length ; i++ ) {
      int biggest = 0;
      int biggestIndex = 0;
      for ( int j = 0 ; j < users.length ; j++ ) {
        if ( users[j] >= biggest ) {
          biggest = users[j];
          biggestIndex = j;
        }
      }
      codeUsers[i] = codes[biggestIndex] + "=" + users[biggestIndex];
      users[biggestIndex] = -1;
    }

    // success
    return codeUsers;

  } // getCodeUsers()

  public String[] getCodeRows( EventBean _event , Date fromDate , Date toDate )
      throws IOException {

    long eventID = _event.getEventID();
    long clientID = _event.getClientID();
    String codes[] = transactionLogDAO.getEventCodes( eventID );
    int rows[] = new int[codes.length];
    String codeRows[] = new String[codes.length];

    /*------------------------
     get code row count
     ------------------------*/
    for ( int i = 0 ; i < codes.length ; i++ )
      rows[i] = getRowCount( clientID , eventID , fromDate , toDate , codes[i] ,
          false );

    /*------------------------
     sort by quantity desc
     ------------------------*/
    for ( int i = 0 ; i < rows.length ; i++ ) {
      int biggest = 0;
      int biggestIndex = 0;
      for ( int j = 0 ; j < rows.length ; j++ ) {
        if ( rows[j] >= biggest ) {
          biggest = rows[j];
          biggestIndex = j;
        }
      }
      codeRows[i] = codes[biggestIndex] + "=" + rows[biggestIndex];
      rows[biggestIndex] = -1;
    }

    // success
    return codeRows;

  } // getCodeRows()

  public long getCodeLocation( long eventID , String code ) throws IOException {

    return transactionLogDAO.getCodeLocation( eventID , code );

  } // getCodeLocation()

  public String getDailyHits( Date fromDate , Date toDate , String criteria ,
      boolean accumulate ) throws IOException {

    String dailyHits = "";

    /*------------------------
     build day buckets
     ------------------------*/
    int numDays = (int) Util.dayDiff( fromDate , toDate ) + 1;
    int buckets[] = new int[numDays]; // day buckets

    /*------------------------
     select records within date range
     ------------------------*/
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate , criteria );
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        criteria );

    /*------------------------
     build day buckets for trans log
     ------------------------*/
    for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );
      int bucket = (int) Util.dayDiff( fromDate , transLog.getDateTm() );
      buckets[bucket]++;
    }

    /*------------------------
     build day buckets for trans queue
     ------------------------*/
    for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );
      int bucket = (int) Util.dayDiff( fromDate , transQueue.getDateTm() );
      buckets[bucket]++;
    }

    /*------------------------
     accumulate hits
     ------------------------*/
    if ( accumulate )
      for ( int i = 1 ; i < numDays ; i++ )
        buckets[i] += buckets[i - 1];

    /*------------------------
     build string of daily hits
     ------------------------*/
    for ( int i = 0 ; i < numDays ; i++ )
      dailyHits += buckets[i] + ",";
    if ( dailyHits.length() > 0 )
      dailyHits = dailyHits.substring( 0 , dailyHits.length() - 1 );

    return dailyHits;

  } // getDailyHits()

  public String getMenuChoices( EventBean _event , Date fromDate , Date toDate ,
      boolean bogus ) throws IOException {

    String menuChoices = "(none),";
    HashSet hMenuChoices = new HashSet( 100 );

    /*------------------------
     get expected choice values
     ------------------------*/
    String expectedValues[] = new EventSupport( null ).getMenuChoices( _event );

    /*------------------------
     set select criteria
     ------------------------*/
    String criteria = "event_id=" + _event.getEventID()
        + " and params like '%MENU%'";

    /*------------------------
     select records within date range
     ------------------------*/
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate , criteria );
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        criteria );

    /*------------------------
     get menu choices from trans log
     ------------------------*/
    for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );
      String params = transLog.getParams();
      try {
        StringTokenizer st = new StringTokenizer( params , "," );
        int numTokens = st.countTokens();
        for ( int j = 0 ; j < numTokens ; j++ ) {
          String param = st.nextToken();
          StringTokenizer st2 = new StringTokenizer( param , "=" );
          String paramName = st2.nextToken();
          if ( !paramName.equals( "MENUITEM" ) && paramName.startsWith( "MENU" ) ) {
            String paramValue = st2.nextToken().toUpperCase();
            if ( !bogus ) {
              for ( int k = 0 ; k < expectedValues.length ; k++ ) {
                if ( paramValue.equals( expectedValues[k] ) ) {
                  hMenuChoices.add( (String) paramValue );
                  break;
                }
              }
            } else if ( bogus ) {
              boolean found = false;
              for ( int k = 0 ; k < expectedValues.length ; k++ ) {
                if ( paramValue.equals( expectedValues[k] ) ) {
                  found = true;
                  break;
                }
              }
              if ( !found )
                hMenuChoices.add( (String) paramValue );
            }
          }
        }
      } catch ( Exception e ) {
      }
    }

    /*------------------------
     get menu choices from trans queue
     ------------------------*/
    for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );
      String params = transQueue.getParams();
      try {
        StringTokenizer st = new StringTokenizer( params , "," );
        int numTokens = st.countTokens();
        for ( int j = 0 ; j < numTokens ; j++ ) {
          String param = st.nextToken();
          StringTokenizer st2 = new StringTokenizer( param , "=" );
          String paramName = st2.nextToken();
          if ( !paramName.equals( "MENUITEM" ) && paramName.startsWith( "MENU" ) ) {
            String paramValue = st2.nextToken().toUpperCase();
            if ( !bogus ) {
              for ( int k = 0 ; k < expectedValues.length ; k++ ) {
                if ( paramValue.equals( expectedValues[k] ) ) {
                  hMenuChoices.add( (String) paramValue );
                  break;
                }
              }
            } else if ( bogus ) {
              boolean found = false;
              for ( int k = 0 ; k < expectedValues.length ; k++ ) {
                if ( paramValue.equals( expectedValues[k] ) ) {
                  found = true;
                  break;
                }
              }
              if ( !found )
                hMenuChoices.add( (String) paramValue );
            }
          }
        }
      } catch ( Exception e ) {
      }
    }

    /*------------------------
     build return string
     ------------------------*/
    try {
      Iterator iter = hMenuChoices.iterator();
      while ( iter.hasNext() )
        menuChoices += (String) iter.next() + ",";
      menuChoices = menuChoices.substring( 0 , menuChoices.length() - 1 );
    } catch ( Exception e ) {
    }

    return menuChoices;

  } // getMenuChoices()

  public DownloadBean extractDownloadData( Date fromDate , Date toDate ,
      long eventID , String downloadPath ) throws IOException {

    DownloadBean downloadBean = new DownloadBean();

    /*------------------------
     get event and client beans
     ------------------------*/
    EventBean _event = eventService.select( eventID );

    ClientService clientService = new ClientService();
    ClientBean client = clientService.select( _event.getClientID() );

    /*------------------------
     select records within date range
     ------------------------*/
    Vector vTransQueue = transactionQueueService.select( fromDate , toDate ,
        "event_id=" + eventID );
    Vector vTransLog = transactionLogDAO.select( fromDate , toDate ,
        "event_id=" + eventID );
    Vector vMerged = mergeData( vTransQueue , vTransLog );

    /*------------------------
     find params
     ------------------------*/
    HashSet hParams = new HashSet( 100 );
    for ( int i = 0 ; i < vMerged.size() ; i++ ) {
      TransactionLogBean merged = (TransactionLogBean) vMerged.elementAt( i );
      StrTok st = new StrTok( merged.getParams() , "," );
      for ( int j = 0 ; j < st.countTokens() ; j++ ) {
        StrTok st2 = new StrTok( st.nextTok() , "=" );
        String paramName = st2.nextTok();
        if ( !paramName.equals( "MENUITEM" ) )
          hParams.add( paramName );
      }
    }

    /*------------------------
     create temp folder
     ------------------------*/
    long _12digit = ( ( (long) ( Math.random() * 899999999998L ) ) + 100000000001L );
    String folderPathname = downloadPath + _12digit;
    File folder = new File( folderPathname );
    if ( !folder.isDirectory() )
      folder.mkdir();

    /*------------------------
     create csv file
     ------------------------*/
    String eventName = _event.getEventName();
    eventName = eventName.replaceAll( " " , "_" );
    eventName = eventName.replaceAll( ":" , "_" );
    String csvFilename = eventName + ".csv";
    String filePathname = folderPathname + "/" + csvFilename;
    File f = new File( filePathname );
    DataOutputStream dos = new DataOutputStream( new FileOutputStream( f ) );

    /*------------------------
     write header record
     ------------------------*/
    String header = "DATE_TIME,CODE,MESSAGE_COUNT,";
    if ( !client.isBeepme() )
      header += "PHONE,";
    Iterator iter = hParams.iterator();
    while ( iter.hasNext() )
      header += (String) iter.next() + ",";
    header = header.substring( 0 , header.length() - 1 );
    header += "\r\n";
    dos.writeBytes( header );

    /*------------------------
     write data records
     ------------------------*/
    for ( int i = 0 ; i < vMerged.size() ; i++ ) {

      // write fields up through PHONE
      TransactionLogBean merged = (TransactionLogBean) vMerged.elementAt( i );
      String record = Util.strFormat( merged.getDateTm() ,
          "yyyy-mm-dd hh:nn:ss" ) + ",";
      record += merged.getCode() + "," + merged.getMessageCount() + ",";
      if ( !client.isBeepme() ) {
        String phone = merged.getPhone();
        if ( phone.startsWith( "+65" ) )
          phone = "(65) " + phone.substring( 3 );
        record += phone + ",";
      }

      // write PARAM fields
      iter = hParams.iterator();
      NextHeader: while ( iter.hasNext() ) {
        String headerName = (String) iter.next();
        StrTok st = new StrTok( merged.getParams() , "," );
        boolean found = false;
        for ( int j = 0 ; j < st.countTokens() ; j++ ) {
          StrTok st2 = new StrTok( st.nextTok() , "=" );
          String paramName = st2.nextTok();
          String paramValue = st2.nextTok();
          if ( paramName.equals( headerName ) ) {
            if ( found )
              record += "|";
            record += paramValue;
            found = true;
          }
        }
        record += ",";
      }

      // done writing one data record
      record = record.substring( 0 , record.length() - 1 );
      record += "\r\n";
      dos.writeBytes( record );
    }
    dos.close();

    /*------------------------
     create zip file
     ------------------------*/
    String zipFilename = eventName + ".zip";
    File fArray[] = new File[1];
    fArray[0] = f;
    Util.zip( folderPathname + "/" + zipFilename , fArray );

    /*------------------------
     create self extracting exe
     ------------------------*/
    Util.zip2exe( zipFilename , folderPathname );
    String exeFilename = eventName + ".exe";

    /*------------------------
     build download bean
     ------------------------*/
    downloadBean.setRecordCount( vMerged.size() );
    if ( vMerged.size() > 0 ) {
      downloadBean.setFirstRecordDate( ( (TransactionLogBean) vMerged
          .elementAt( 0 ) ).getDateTm() );
      downloadBean.setLastRecordDate( ( (TransactionLogBean) vMerged
          .elementAt( vMerged.size() - 1 ) ).getDateTm() );
    }
    downloadBean.setFolderName( "" + _12digit );
    downloadBean.setDownloadFileSize( f.length() );
    downloadBean.setCsvFilename( csvFilename );
    downloadBean.setExeFilename( exeFilename );

    /*--------------------------
     success
     --------------------------*/
    return downloadBean;

  } // extractDownloadData()

  public Vector mergeData( Vector vTransQueue , Vector vTransLog ) {
    return mergeData( vTransQueue , vTransLog , false );
  }

  public Vector mergeData( Vector vTransQueue , Vector vTransLog , boolean desc ) {

    Vector vMerged = new Vector( 100 , 100 );

    for ( int i = 0 ; i < vTransQueue.size() ; i++ ) {
      TransactionQueueBean transQueue = (TransactionQueueBean) vTransQueue
          .elementAt( i );
      TransactionLogBean transLog = new TransactionLogBean();
      transLog.setPhone( transQueue.getPhone() );
      transLog.setCode( transQueue.getCode() );
      transLog.setParams( transQueue.getParams() );
      transLog.setDateTm( transQueue.getDateTm() );
      transLog.setMessageCount( transQueue.getMessageCount() );
      vMerged.addElement( transLog );
    }

    nextTransLog: for ( int i = 0 ; i < vTransLog.size() ; i++ ) {
      TransactionLogBean transLog = (TransactionLogBean) vTransLog
          .elementAt( i );
      if ( vMerged.size() == 0 )
        vMerged.addElement( transLog );
      else {
        for ( int j = 0 ; j < vMerged.size() ; j++ ) {
          TransactionLogBean merged = (TransactionLogBean) vMerged
              .elementAt( j );
          if ( desc ) {
            if ( Util.secondDiff( transLog.getDateTm() , merged.getDateTm() ) <= 0 ) {
              vMerged.insertElementAt( transLog , j );
              continue nextTransLog;
            }
          } else {
            if ( Util.secondDiff( transLog.getDateTm() , merged.getDateTm() ) >= 0 ) {
              vMerged.insertElementAt( transLog , j );
              continue nextTransLog;
            }
          }
        }
        vMerged.addElement( transLog );
      }
    }

    // success
    return vMerged;

  } // mergeData()

}
