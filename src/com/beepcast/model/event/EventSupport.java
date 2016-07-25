package com.beepcast.model.event;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.billing.BillingResult;
import com.beepcast.billing.BillingStatus;
import com.beepcast.billing.profile.PaymentType;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.dbmanager.common.ClientCommon;
import com.beepcast.dbmanager.common.ClientLevelCommon;
import com.beepcast.dbmanager.table.QClientLevel;
import com.beepcast.dbmanager.table.TClientLevel;
import com.beepcast.model.beepcode.BeepcodeBean;
import com.beepcast.model.beepcode.BeepcodeService;
import com.beepcast.model.beepcode.BeepcodeSupport;
import com.beepcast.model.chart.ChartSupport;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.model.mobileUser.MobileUserService;
import com.beepcast.model.transaction.BogusRequestService;
import com.beepcast.model.transaction.TransactionInputMessage;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.model.transaction.TransactionLogBean;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionMessage;
import com.beepcast.model.transaction.TransactionMessageParam;
import com.beepcast.model.transaction.TransactionProcessBasic;
import com.beepcast.model.transaction.TransactionProcessBeanUtils;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.beepcast.model.transaction.alert.AlertService;
import com.beepcast.model.transaction.billing.AccountProfile;
import com.beepcast.model.transaction.scheduleTask.DailyClientTrackInit;
import com.beepcast.model.transaction.scheduleTask.MonthlyClientTrackInit;
import com.beepcast.util.StrTok;
import com.beepcast.util.Util;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class EventSupport {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "EventSupport" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private DatabaseLibrary dbLib;
  private DBManagerApp dbMan;
  private BillingApp billingApp;
  private ClientApp clientApp;
  private ProviderApp providerApp;

  private EventService eventService;
  private TransactionQueueService transQueueService;
  private TransactionLogService transLogService;
  private BogusRequestService bogusReqService;
  private MobileUserService mobileUserService;

  private GatewayLogService gatewayLogService;

  private TransactionProcessBasic trans;
  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public EventSupport() {
    init();
  }

  public EventSupport( TransactionProcessBasic trans ) {
    init();
    if ( trans != null ) {
      this.trans = trans;
      this.log = trans.log();
    } else {
      this.log = new TransactionLog();
    }
  }

  private void init() {

    dbLib = DatabaseLibrary.getInstance();
    dbMan = DBManagerApp.getInstance();
    billingApp = BillingApp.getInstance();
    clientApp = ClientApp.getInstance();
    providerApp = ProviderApp.getInstance();

    eventService = new EventService();
    transQueueService = new TransactionQueueService();
    transLogService = new TransactionLogService();
    bogusReqService = new BogusRequestService();
    mobileUserService = new MobileUserService();

    gatewayLogService = new GatewayLogService();

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void removeEventCode( long eventID , String code ) throws IOException {

    String codes = "";
    String currentCodes = new BeepcodeSupport().selectEventCodes( eventID );
    StringTokenizer st = new StringTokenizer( currentCodes , "," );
    int numCodes = st.countTokens();
    try {
      for ( int i = 0 ; i < numCodes ; i++ ) {
        String temp = st.nextToken();
        if ( !temp.equals( code ) )
          codes += temp + ",";
      }
      if ( codes.length() > 0 )
        codes = codes.substring( 0 , codes.length() - 1 ); // remove trailing
      // comma
    } catch ( Exception e ) {
    }

    EventBean eventBean = eventService.select( eventID );
    eventBean.setCodes( codes );
    eventBean.setNumCodes( ( numCodes > 0 ) ? numCodes - 1 : 0 );
    eventService.update( eventBean );

  } // removeEventCode()

  public ProcessBean[] extractProcessClob( EventBean eventBean ) {
    return extractProcessClob( eventBean , true );
  }

  public ProcessBean[] extractProcessClob( EventBean eventBean ,
      boolean setNextType ) {
    return extractProcessClob( eventBean , setNextType , null );
  }

  public ProcessBean[] extractProcessClob( EventBean eventBean ,
      boolean setNextType , String message ) {
    ProcessBean[] processSteps = null;

    if ( eventBean == null ) {
      DLog.warning( lctx , "Failed to extract process clob "
          + ", found null event bean" );
      return processSteps;
    }

    DLog.debug( lctx ,
        "Extracting process clob : eventId = " + eventBean.getEventID()
            + " , setNextType = " + setNextType + " , message = "
            + StringEscapeUtils.escapeJava( message ) );

    String surveyResults = "";
    long flags = eventBean.getBitFlags();

    // get process clob string
    String processClob = eventBean.getProcess();
    if ( ( processClob == null ) || ( processClob.equals( "" ) ) ) {
      DLog.warning( lctx , "Failed to extract process clob "
          + ", found empty event process value" );
      return processSteps;
    }

    // extract clob data
    StrTok st1 = new StrTok( processClob );
    int numProcessSteps = Integer.parseInt( st1.nextTok( ":" ).trim() );
    processSteps = new ProcessBean[numProcessSteps];
    for ( int i = 0 ; i < numProcessSteps ; i++ ) {
      String processStep = st1.nextTok( "~" ).trim();
      StrTok st2 = new StrTok( processStep , "^" );

      ProcessBean processBean = new ProcessBean();
      processBean.setStep( st2.nextTok().trim() );
      processBean.setType( st2.nextTok().trim() );
      processBean.setParamLabel( st2.nextTok().trim() );

      // get array of parameter names
      try {
        String paramNames = st2.nextTok().trim();
        StringTokenizer st3 = new StringTokenizer( paramNames , "," );
        int numNames = st3.countTokens();
        String names[] = new String[numNames];
        for ( int j = 0 ; j < numNames ; j++ ) {
          names[j] = st3.nextToken().trim();
        }
        processBean.setNames( names );
      } catch ( Exception e ) {
      }

      // get response
      String response = st2.nextTok().trim();

      if ( i != 0
          && ( flags & EventBean.SHOW_SURVEY_RESULTS ) == EventBean.SHOW_SURVEY_RESULTS ) {
        response = surveyResults;
      }

      processBean.setResponse( response );
      processBean.setNextStep( st2.nextTok().trim() );
      processBean.setRfa( st2.nextTok().trim() );
      processSteps[i] = processBean;

      // setup special response for survey results
      if ( i == 0
          && ( flags & EventBean.SHOW_SURVEY_RESULTS ) == EventBean.SHOW_SURVEY_RESULTS
          && ( message != null ) ) {
        boolean showTotal = ( ( flags & EventBean.SHOW_SURVEY_TOTAL ) == EventBean.SHOW_SURVEY_TOTAL ) ? true
            : false;
        surveyResults = getSurveyResults( eventBean , processBean , message ,
            showTotal );
      }

    }

    // set next process type
    if ( setNextType ) {
      for ( int i = 0 ; i < processSteps.length ; i++ ) {
        ProcessBean processBean = processSteps[i];
        if ( processBean.getNextStep().equals( "END" ) ) {
          processBean.setNextType( "" );
        } else {
          ProcessBean processBeanTemp = processSteps[Integer
              .parseInt( processBean.getNextStep() ) - 1];
          processBean.setNextType( processBeanTemp.getType() );
        }
        processSteps[i] = processBean;
      }
    }

    return processSteps;
  }

  public String buildProcessClob( ProcessBean processSteps[] ) {

    String _process = "";

    /*------------------------
      build clob
      IMPORTANT: ANY ADDITIONS MUST BE APPENDED, NOT INSERTED
    ------------------------*/
    _process += processSteps.length + ":";
    for ( int i = 0 ; i < processSteps.length ; i++ ) {
      _process += processSteps[i].getStep().trim() + "^";
      _process += processSteps[i].getType().trim() + "^";
      _process += processSteps[i].getParamLabel().trim() + "^";

      _process += StringUtils.join( processSteps[i].getNames() , "," ) + "^";

      _process += processSteps[i].getResponse().trim() + "^";
      _process += processSteps[i].getNextStep().trim() + "^";
      _process += processSteps[i].getRfa().trim() + "~"; // appended 11/24/02
    }
    _process = _process.substring( 0 , _process.length() - 1 ); // remove
    // trailing "~"

    return _process;

  } // buildProcessClob()

  public ProcessBean resolveNextProcessBean(
      TransactionProcessBasic transaction ,
      TransactionQueueBean transQueueBean , ProcessBean[] processSteps ,
      TransactionInputMessage imsg ) {
    ProcessBean processBean = null;

    // set array with zero based
    int transQueueNextStep = transQueueBean.getNextStep() - 1;
    log.debug( "Define next step index from trans queue = "
        + transQueueNextStep );

    // check for event closure
    try {
      int eventId = (int) transQueueBean.getEventID();
      String phoneNumber = transQueueBean.getPhone();
      if ( closureEvent( eventId ) ) {
        transQueueNextStep = 1; // not closed
        log.debug( "Defined as closure event , trying to "
            + "close the next process : event id = " + eventId
            + " , phone number = " + phoneNumber
            + " , set trans queue next step = " + transQueueNextStep );
        if ( eventClosed( eventId , phoneNumber ) ) {
          transQueueNextStep = 0; // closed
          log.debug( "Event closed , set trans queue next step = "
              + transQueueNextStep );
        }
      } else {
        log.debug( "No closure event defined" );
      }
    } catch ( Exception e ) {
      log.warning( "Failed to verify closure event , " + e );
      return processBean;
    }

    // get and validate next process step
    ProcessBean candidateProcessBean = processSteps[transQueueNextStep];
    if ( candidateProcessBean == null ) {
      log.warning( "Failed to resolved next process bean "
          + ", found null candidate process bean " );
      return processBean;
    }
    log.debug( "Defined candidate process bean from trans queue : index = "
        + transQueueNextStep + " , type = " + candidateProcessBean.getType()
        + " , step = " + candidateProcessBean.getStep() );

    // validate for MENUITEM shortcut
    if ( StringUtils.equals( candidateProcessBean.getType() , "PARAM" ) ) {
      String[] names = candidateProcessBean.getNames();
      if ( ( names != null ) && ( names.length > 0 )
          && ( names[0].equals( "MENUITEM" ) ) ) {
        // process menu item
        log.debug( "Found next process type as param , will process menu item" );
        candidateProcessBean = processMenuItem( transaction , transQueueBean ,
            processSteps , imsg , candidateProcessBean );
        // redefine trans queue next step
        transQueueNextStep = transQueueBean.getNextStep() - 1;
        log.debug( "Redefine next step index from trans queue = "
            + transQueueNextStep );
      }
    }
    if ( candidateProcessBean == null ) {
      log.warning( "Failed to resolved next process bean "
          + ", found null candidate process bean " );
      return processBean;
    }

    // get process current step of candidate process bean
    // verify it when found as unknown and end
    int candidateProcessBeanCurStep = TransactionProcessBeanUtils
        .getProcessStep( log , candidateProcessBean );
    if ( candidateProcessBeanCurStep == TransactionProcessBeanUtils.INT_STEP_UNKNOWN ) {
      log.warning( "Failed to resolved next process bean "
          + ", found unknown current step value on candidate process bean" );
      return processBean;
    }
    if ( candidateProcessBeanCurStep == TransactionProcessBeanUtils.INT_STEP_END ) {
      log.warning( "Failed to resolved next process bean "
          + ", found end current step value on candidate process bean" );
      return processBean;
    }

    {
      // get process current next step of candidate process bean
      // and verify it when found as unknown and end
      int candidateProcessBeanNextStep = TransactionProcessBeanUtils
          .getProcessNextStep( log , candidateProcessBean );
      if ( candidateProcessBeanNextStep == TransactionProcessBeanUtils.INT_STEP_UNKNOWN ) {
        log.warning( "Failed to resolved next process bean "
            + ", found unknown next step value on candidate process bean" );
        return processBean;
      }
      if ( candidateProcessBeanNextStep == TransactionProcessBeanUtils.INT_STEP_END ) {
        log.debug( "Found end next step value on candidate process bean" );
        processBean = candidateProcessBean;
        return processBean;
      }
    }

    // get process type of candidate process bean
    // verify it when found blank
    String candidateProcessBeanType = candidateProcessBean.getType();
    if ( StringUtils.isBlank( candidateProcessBeanType ) ) {
      log.warning( "Failed to resolved next process bean "
          + ", found blank type on candidate process bean" );
      return processBean;
    }

    if ( transQueueNextStep < 1 ) {

      // when trans queue next step is zero
      // means that this is a first step
      log.debug( "Event process as first step" );

      if ( candidateProcessBeanType
          .equalsIgnoreCase( TransactionProcessBeanUtils.PROCESS_TYPE_CREATE_QR_IMAGE ) ) {

        // found create qr image , no need to resolve all the sibling as codes
        log.debug( "Found create qr image process type "
            + ", will resolve as next process bean" );
        processBean = candidateProcessBean;

      } else {

        // other than that will try to resolved next process bean from codes
        processBean = resolveNextProcessBeanCodes( candidateProcessBean ,
            candidateProcessBeanCurStep , candidateProcessBeanType ,
            transQueueBean , processSteps , imsg );

      }

    } else {

      log.debug( "Event process as non first step" );
      processBean = resolveNextProcessBeanWords( candidateProcessBean ,
          candidateProcessBeanCurStep , candidateProcessBeanType ,
          transQueueBean , processSteps , imsg );
    }

    // log for null process bean
    if ( processBean == null ) {
      log.warning( "Failed to resolved next process bean" );
    }

    return processBean;
  }

  private ProcessBean resolveNextProcessBeanCodes( ProcessBean cProcessBean ,
      int cProcessStep , String cProcessType ,
      TransactionQueueBean transQueueBean , ProcessBean[] processSteps ,
      TransactionInputMessage imsg ) {
    ProcessBean rProcessBean = null;

    log.debug( "Processing the process type codes" );

    if ( !TransactionProcessBeanUtils.validProcessTypeCodes( cProcessType ) ) {
      log.warning( "Failed to resolved next process bean codes "
          + ", found invalid process type = " + cProcessType );
      return rProcessBean;
    }

    log.debug( "Defined valid process type codes = " + cProcessType );

    // get first step in same level with current step
    int cProcessLeftStep = TransactionProcessBeanUtils.firstScopeProcessStep(
        log , cProcessStep ,
        TransactionProcessBeanUtils.PROCESS_TYPE_GROUP_CODES , processSteps );

    // get last step in same level with current step
    int cProcessRightStep = TransactionProcessBeanUtils.lastScopeProcessStep(
        log , cProcessStep ,
        TransactionProcessBeanUtils.PROCESS_TYPE_GROUP_CODES , processSteps );

    // log it
    TransactionProcessBeanUtils.debugScopeProcessStep( log , processSteps ,
        cProcessLeftStep , cProcessRightStep );

    // find match next process bean
    rProcessBean = resolveNextProcessBeanCodes( processSteps ,
        cProcessLeftStep , cProcessRightStep , imsg.getMessageContent() ,
        transQueueBean );

    return rProcessBean;
  }

  private ProcessBean resolveNextProcessBeanWords( ProcessBean cProcessBean ,
      int cProcessStep , String cProcessType ,
      TransactionQueueBean transQueueBean , ProcessBean[] processSteps ,
      TransactionInputMessage imsg ) {
    ProcessBean rProcessBean = null;

    log.debug( "Processing the process type words" );

    // resolved next process bean from words
    if ( !TransactionProcessBeanUtils.validProcessTypeAll( cProcessType ) ) {
      log.warning( "Failed to resolved next process bean all "
          + ", found invalid process type = " + cProcessType );
      return rProcessBean;
    }

    // get first step in same level with current step
    int nextProcessBeanLeftStep = TransactionProcessBeanUtils
        .firstScopeProcessStep( log , cProcessStep ,
            TransactionProcessBeanUtils.PROCESS_TYPE_GROUP_ALL , processSteps );

    // get last step in same level with current step
    int nextProcessBeanRightStep = TransactionProcessBeanUtils
        .lastScopeProcessStep( log , cProcessStep ,
            TransactionProcessBeanUtils.PROCESS_TYPE_GROUP_ALL , processSteps );

    // log it
    TransactionProcessBeanUtils.debugScopeProcessStep( log , processSteps ,
        nextProcessBeanLeftStep , nextProcessBeanRightStep );

    // find match next process step
    rProcessBean = resolveNextProcessBeanWords( processSteps ,
        nextProcessBeanLeftStep , nextProcessBeanRightStep , imsg ,
        transQueueBean );

    return rProcessBean;
  }

  private ProcessBean resolveNextProcessBeanCodes( ProcessBean[] processSteps ,
      int idxLeft , int idxRight , String messageRequest ,
      TransactionQueueBean transQueueBean ) {
    ProcessBean processBean = null;
    log.debug( "Extract to resolve next process bean based on codes" );
    for ( int idx = idxRight ; ( ( idx >= idxLeft ) && ( processBean == null ) ) ; idx-- ) {
      ProcessBean pb = processSteps[idx];
      if ( pb == null ) {
        continue;
      }
      String step = pb.getStep();
      if ( StringUtils.isBlank( step ) ) {
        continue;
      }
      String type = pb.getType();
      if ( StringUtils.isBlank( type ) ) {
        continue;
      }
      String[] names = pb.getNames();
      if ( ( names == null ) || ( names.length < 1 ) ) {
        continue;
      }
      for ( int j = 0 ; j < names.length ; j++ ) {
        String name = StringUtils.trimToEmpty( names[j] );
        if ( StringUtils.isBlank( name ) ) {
          continue;
        }
        if ( !TransactionProcessBeanUtils.matchProcessTypeCodes( log , step ,
            type , name , messageRequest ) ) {
          continue;
        }

        // resolved process bean
        processBean = pb;
        log.debug( "Found match bean process " + ", with : step = "
            + pb.getStep() + " , names = "
            + StringUtils.join( pb.getNames() , "," ) + " , message = "
            + StringEscapeUtils.escapeJava( pb.getResponse() ) );
        break;
      }
    }
    log.debug( "Finished extract scope of process beans" );
    return processBean;
  }

  private ProcessBean resolveNextProcessBeanWords( ProcessBean[] processSteps ,
      int idxLeft , int idxRight , TransactionInputMessage imsg ,
      TransactionQueueBean transQueueBean ) {
    ProcessBean processBean = null;
    log.debug( "Resolving next process bean based on words" );
    for ( int idx = idxLeft ; ( idx <= idxRight ) && ( processBean == null ) ; idx++ ) {
      ProcessBean pb = processSteps[idx];
      if ( pb == null ) {
        continue;
      }
      String step = pb.getStep();
      if ( StringUtils.isBlank( step ) ) {
        continue;
      }
      String type = pb.getType();
      if ( StringUtils.isBlank( type ) ) {
        continue;
      }
      if ( !TransactionProcessBeanUtils.validProcessTypeWords( type ) ) {
        continue;
      }
      String[] names = pb.getNames();
      if ( ( names == null ) || ( names.length < 1 ) ) {
        continue;
      }
      int totalReservedVariables = EventInboundReservedVariables
          .totalReservedVariables( StringUtils.join( names , ' ' ) );
      for ( int j = 0 ; j < names.length ; j++ ) {
        String name = names[j];
        if ( name == null ) {
          continue;
        }
        name = name.trim();
        if ( name.equals( "" ) ) {
          continue;
        }

        if ( totalReservedVariables > 0 ) {
          // process for inbound reserved keyword
          if ( EventInboundReservedVariables.processReservedVariable( log ,
              name , j , totalReservedVariables , imsg ) ) {
            processBean = pb;
            break;
          }
        } else {
          // process for matching message content
          if ( TransactionProcessBeanUtils.matchProcessTypeWords( log , step ,
              type , name , imsg.getMessageType() , imsg.getMessageContent() ) ) {
            processBean = pb;
            break;
          }
        }

      } // for ( int j=0;j<names.length;j++)
      if ( processBean != null ) {
        break;
      }
    } // for (int idx=idxLeft;(idx<=idxRight)&&(processBean==null);idx++)

    if ( processBean == null ) {
      log.warning( "Failed to resolve next process bean based on words" );
      return processBean;
    }

    log.debug( "Found resolved next process bean based on words : step = "
        + processBean.getStep() + " , names = "
        + StringUtils.join( processBean.getNames() , "," ) + " , message = "
        + StringEscapeUtils.escapeJava( processBean.getResponse() ) );
    return processBean;
  }

  private ProcessBean processMenuItem( TransactionProcessBasic transaction ,
      TransactionQueueBean transObj , ProcessBean[] processSteps ,
      TransactionInputMessage imsg , ProcessBean processBean ) {

    log.debug( "Start to process menu item : " + processBean );

    // validation for the must be params

    if ( processSteps == null ) {
      log.warning( "Failed to process menu item , found null process steps" );
      return processBean;
    }

    if ( processSteps.length < 2 ) {
      log.warning( "Failed to process menu item , found small process steps" );
      return processBean;
    }

    // define the input parameters and menu choice

    String messageRequest = imsg.getMessageContent();
    StrTok st = new StrTok( messageRequest , " ," );
    st.nextTok();

    String parameters = StringUtils.trim( st.nextTok( "~" ) );
    if ( ( parameters == null ) || ( parameters.equals( "" ) ) ) {
      log.warning( "Failed to process menu item , found empty parameters" );
      return processBean;
    }
    parameters = parameters.toUpperCase();
    log.debug( "Defined parameters based on input message = " + parameters );

    StrTok st2 = new StrTok( parameters , " ," );
    String menuChoice = st2.nextTok();
    if ( ( menuChoice == null ) || ( menuChoice.equals( "" ) ) ) {
      log.warning( "Failed to process menu item , found empty menu choice" );
      return processBean;
    }
    log.debug( "Defined menuChoice based on parameters = " + menuChoice );

    // read the next process bean

    ProcessBean nextProcessBean = ProcessCommon.nextProcessBean( processBean ,
        processSteps );
    if ( nextProcessBean == null ) {
      log.warning( "Failed to process menu item "
          + ", found null next process step" );
      return processBean;
    }

    // prepare the next process type and names

    String type = nextProcessBean.getType();
    if ( !type.equals( "EXPECT" ) ) {
      log.warning( "Failed to process menu item "
          + ", found invalid process step" );
      return processBean;
    }
    String[] names = processBean.getNames();
    log.debug( "Defined nextProcessBean : type = " + type + " , names = "
        + Arrays.asList( names ) );

    // rename for special params

    if ( names.length >= 2 ) {
      if ( menuChoice.equalsIgnoreCase( "EMAIL" ) ) {
        names[1] = "EMAIL";
      }
      if ( menuChoice.equalsIgnoreCase( "NAME" ) ) {
        names[1] = "NAME";
      }
      if ( menuChoice.equalsIgnoreCase( "IC" ) ) {
        names[1] = "IC";
      }
      log.debug( "Updated nextProcessBean : names[1] = " + names[1] );
      processBean.setNames( names );
    }

    // capture parameters past MENUITEM

    if ( trans != null ) {
      transObj = trans.support().appendParameters( transObj , imsg ,
          processBean , processSteps );
    }

    // setup for next process step

    try {
      transObj.setNextStep( Integer.parseInt( nextProcessBean.getStep() ) );
      transQueueService.update( transObj );
    } catch ( Exception e ) {
      log.warning( "Failed to update trans queue bean , " + e );
      return processBean;
    }

    // redefine the input message content

    imsg.setMessageContent( menuChoice );
    log.debug( "Updated new input message content : "
        + StringEscapeUtils.escapeJava( imsg.getMessageContent() ) );

    // go to the next process bean

    processBean = nextProcessBean;
    log.debug( "Finished process menu item : " + processBean );

    return processBean;
  }

  public boolean closureEvent( long eventID ) throws IOException {

    EventBean _event = eventService.select( eventID );
    if ( _event != null && _event.getProcessType() == EventBean.CLOSURE_TYPE )
      return true;
    return false;

  } // closureEvent()

  public boolean eventClosed( long eventID , String phone ) throws IOException {

    EventBean _event = eventService.select( eventID );
    if ( _event == null || _event.getProcessType() != EventBean.CLOSURE_TYPE )
      return false;
    else {
      /*-------------------------
        set one year timeframe
      -------------------------*/
      Date toDate = new Date();
      Calendar c = new GregorianCalendar();
      c.setTime( toDate );
      c.add( Calendar.YEAR , -1 );
      Date fromDate = c.getTime();

      /*-------------------------
        query for event closure
      -------------------------*/
      long orgEventID = Long.parseLong( _event.getRemindFreq() );
      String criteria = "phone='" + phone + "' and event_id=" + orgEventID;
      Vector transactions = transLogService.select( fromDate , toDate ,
          criteria );
      if ( transactions.size() > 0 )
        return true;
    }
    return false;

  } // eventClosed()

  public String getExpectParamName( ProcessBean _process ,
      ProcessBean processSteps[] ) {

    // determine expect group ( zero based )
    int thisStep = Integer.parseInt( _process.getStep() ) - 1;

    // find first process step in this group
    int firstStep = thisStep;
    for ( int i = thisStep - 1 ; i >= 0 ; i-- ) {
      ProcessBean p = processSteps[i];
      if ( p.getType().equals( "EXPECT" ) || p.getType().equals( "FIRST_WORD" )
          || p.getType().equals( "CONTAIN_WORD" ) ) {
        firstStep = i;
      } else {
        break;
      }
    }

    // find last process step in this group
    int lastStep = thisStep;
    for ( int i = thisStep + 1 ; i < processSteps.length ; i++ ) {
      ProcessBean p = processSteps[i];
      if ( p.getType().equals( "EXPECT" ) || p.getType().equals( "FIRST_WORD" )
          || p.getType().equals( "CONTAIN_WORD" ) ) {
        lastStep = i;
      } else {
        break;
      }
    }

    // find previous step
    for ( int i = 0 ; i < processSteps.length ; i++ ) {
      ProcessBean p = processSteps[i];
      String menuStep = p.getStep();
      String nextStep = p.getNextStep();
      for ( int j = firstStep ; j <= lastStep ; j++ ) {
        ProcessBean p2 = processSteps[j];
        String step = p2.getStep();
        if ( nextStep.equals( step ) ) {
          return "MENU" + menuStep;
        }
      }
    }

    return "X";
  } // getExpectParamName()

  public String getVarParamName( ProcessBean _process ,
      ProcessBean processSteps[] ) {

    /*--------------------------
      get this step
    --------------------------*/
    String thisStep = _process.getStep();

    /*--------------------------
      find previous step
    --------------------------*/
    for ( int i = 0 ; i < processSteps.length ; i++ ) {
      ProcessBean p = processSteps[i];
      String menuStep = p.getStep();
      String nextStep = p.getNextStep();
      if ( nextStep.equals( thisStep ) )
        return "MENU" + menuStep;
    }

    return "X";

  } // getVarParamName()

  public EventBean select( String comment , String phone ) throws IOException {
    // new EventDAO().select( comment , phone );
    return null;
  }

  public Date getLastHitDate( EventBean _event ) throws IOException {

    Date lastHitDate = _event.getEndDate();
    String codes = _event.getCodes();
    int numCodes = _event.getNumCodes();
    StrTok st = new StrTok( codes , "," );
    for ( int i = 0 ; i < numCodes ; i++ ) {
      String code = st.nextTok();
      BeepcodeBean beepcode = new BeepcodeService().select( code );
      Date temp = beepcode.getLastHitDate();
      if ( Util.minuteDiff( lastHitDate , temp ) > 0 ) {
        Calendar c = new GregorianCalendar();
        c.setTime( temp );
        c.add( Calendar.MINUTE , 1 );
        lastHitDate = c.getTime();
      }
    }

    // if event expired
    if ( numCodes == 0 ) {
      Vector v = transLogService.select( _event.getStartDate() , new Date() ,
          "event_id=" + _event.getEventID() , true );
      if ( v.size() > 0 ) {
        TransactionLogBean transLog = (TransactionLogBean) v.elementAt( 0 );
        lastHitDate = transLog.getDateTm();
      }
    }

    return lastHitDate;

  } // getLastHitDate()

  private String getSurveyResults( EventBean eventBean ,
      ProcessBean processBean , String message , boolean showTotal ) {
    String response = "";

    DLog.debug( lctx ,
        "Getting survey results for event name = " + eventBean.getEventName() );

    // get menu items and letters
    String rfa = processBean.getRfa();
    String menuItems = "";
    String menuLetters = "";
    StringTokenizer st = new StringTokenizer( rfa , "\n" );
    int numMenuItems = st.countTokens();
    for ( int i = 0 ; i < numMenuItems ; i++ ) {
      String temp = st.nextToken();
      if ( temp.substring( 1 , 2 ).equals( ")" ) ) {
        menuItems += temp.substring( 3 ) + ",";
        menuLetters += temp.substring( 0 , 1 ).toUpperCase() + ",";
      }
    }
    if ( menuItems.endsWith( "," ) ) {
      menuItems = menuItems.substring( 0 , menuItems.length() - 1 );
      menuItems = StringUtils.remove( menuItems , "\r" );
      menuLetters = menuLetters.substring( 0 , menuLetters.length() - 1 );
    } else {
      return response;
    }

    // load dataset
    String dataset = "";
    try {
      Date fromDate = null; // eventBean.getStartDate();
      Date toDate = null; // getLastHitDate( eventBean );
      dataset = transLogService.getMenuSelections( fromDate , toDate ,
          eventBean , "1" , menuLetters );
      DLog.debug( lctx , "Get menu selections = " + dataset );
    } catch ( IOException e ) {
      DLog.warning( lctx , "Failed to get menu selections , " + e );
      return response;
    }

    // load arrays
    String datasetArray[] = StringUtils.split( dataset , "," );
    String menuLetterArray[] = StringUtils.split( menuLetters , "," );
    String menuItemArray[] = StringUtils.split( menuItems , "," );
    numMenuItems = datasetArray.length;

    // include this mobile users choice
    for ( int i = 0 ; i < numMenuItems ; i++ ) {
      if ( menuLetterArray[i].equalsIgnoreCase( message.substring( message
          .length() - 1 ) ) ) {
        datasetArray[i] = "" + ( Integer.parseInt( datasetArray[i] ) + 1 );
        dataset = StringUtils.join( datasetArray , "," );
        break;
      }
    }

    // sort dataset
    StringBuffer sb_dataset = new StringBuffer( dataset );
    StringBuffer sb_menuItems = new StringBuffer( menuItems );
    new ChartSupport().sortDataset( sb_dataset , sb_menuItems , true );
    dataset = sb_dataset.toString();
    menuItems = sb_menuItems.toString();
    datasetArray = StringUtils.split( dataset , "," );
    menuItemArray = StringUtils.split( menuItems , "," );

    // count total
    long total = 0L;
    for ( int i = 0 ; i < numMenuItems ; i++ ) {
      total += Long.parseLong( datasetArray[i] );
    }

    // build response participants
    response = "Results:\n";
    for ( int i = 0 ; i < numMenuItems ; i++ ) {
      long count = Long.parseLong( datasetArray[i] );
      double percent = ( total == 0L ) ? 0.0
          : ( (double) count / (double) total );
      response += menuItemArray[i] + " " + Util.strFormat( percent , "##%" )
          + "\n";
    }
    if ( showTotal ) {
      response += "Total=" + total;
    }

    return response;
  }

  public boolean checkOnePingOnly( TransactionInputMessage imsg ,
      LinkedList omsgs ) {
    boolean result = false;

    // get the eventBean object
    EventBean eventBean = (EventBean) imsg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( eventBean == null ) {
      log.warning( "Failed to check one ping  only "
          + ", found empty eventBean" );
      return result;
    }

    // check flag
    long flags = eventBean.getBitFlags();
    if ( ( flags & EventBean.ONE_PING_ONLY ) != EventBean.ONE_PING_ONLY ) {
      log.debug( "Found event bean is not ONE_PING_ONLY , bypass ." );
      return result;
    }

    // set one year timeframe
    Date toDate = new Date();
    Calendar c = new GregorianCalendar();
    c.setTime( toDate );
    c.add( Calendar.YEAR , -1 );
    Date fromDate = c.getTime();
    log.debug( "Validate is the event bean already ping before "
        + ", with duration time between " + fromDate + " and " + toDate );

    // get ping history for this phone and event
    String criteria = " phone = '" + imsg.getOriginalAddress()
        + "' AND event_id = " + imsg.getEventId() + " ";
    Vector records = transLogService.select( fromDate , toDate , criteria );

    // return reject message if previously pinged
    if ( records.size() > 0 ) {
      log.debug( "Found event is already ping before , stop the process "
          + "and create response from event's unsubscribe response" );
      omsgs.addFirst( trans.support().createReplyMessage( imsg ,
          eventBean.getUnsubscribeResponse() ) );
    }

    return result;
  } // checkOnePingOnly()

  public boolean updatePingCount( TransactionQueueBean transQueue ,
      EventBean eventBean ) {
    boolean result = false;

    // ensure process step 1
    if ( transQueue.getNextStep() != 1 ) {
      return result;
    }

    // update event ping count
    long pingCount = eventBean.getPingCount();
    pingCount = pingCount + 1;
    log.debug( "Increase event ping count became = " + pingCount );

    try {
      eventBean.setPingCount( pingCount );
      eventService.updatePingCount( eventBean );
      log.debug( "Successfully update event ping count" );
      result = true;
    } catch ( Exception e ) {
      log.warning( "Failed to update event ping count , " + e );
    }

    return result;
  } // updatePingCount()

  public boolean doDebitPayment( TransactionMessage msg , double debitUnit ) {
    boolean result = false;

    if ( msg == null ) {
      log.warning( "Failed to do client debit "
          + ", found null transaction message" );
      return result;
    }

    // get and validate clientBean and eventBean
    ClientBean clientBean = (ClientBean) msg
        .getMessageParam( TransactionMessageParam.HDR_CLIENT_BEAN );
    EventBean eventBean = (EventBean) msg
        .getMessageParam( TransactionMessageParam.HDR_EVENT_BEAN );
    if ( ( clientBean == null ) || ( eventBean == null ) ) {
      log.warning( "Failed to do client debit "
          + ", found empty client / event bean" );
      return result;
    }

    // get and validate clientId and eventId
    int clientId = (int) clientBean.getClientID();
    int eventId = (int) eventBean.getEventID();
    if ( ( clientId < 1 ) || ( eventId < 1 ) ) {
      log.warning( "Failed to do client debit "
          + ", found empty client / event id" );
      return result;
    }

    // get and validate clientLevel
    QClientLevel qClientLevel = ClientCommon.getClientLevel( clientId );
    if ( qClientLevel == null ) {
      log.warning( "Failed to do client debit , found null q client level" );
      return result;
    }
    TClientLevel clientLevel = ClientLevelCommon.getClientLevel( qClientLevel
        .getClientLevelId() );
    if ( clientLevel == null ) {
      log.warning( "Failed to do client debit , found null t client level" );
      return result;
    }
    log.debug( "Found client level name = " + clientLevel.getName() );

    // is traffic need to be daily track ?
    if ( clientLevel.getDailyTrafficQuota() > 0 ) {
      boolean dailyClientTrack = doDebitDailyClientTrack( clientId );
      if ( !dailyClientTrack ) {
        log.warning( "Failed to perform debit daily client track" );
        return result;
      }
    }

    // is traffic need to be monthly track ?
    if ( clientLevel.getMonthlyTrafficQuota() > 0 ) {
      boolean monthlyClientTrack = doDebitMonthlyClientTrack( clientId );
      if ( !monthlyClientTrack ) {
        log.warning( "Failed to perform debit monthly client track" );
        return result;
      }
    }

    // validate the payment type based on clientId
    int paymentType = clientBean.getPaymentType();
    if ( paymentType == PaymentType.UNKNOWN ) {
      log.warning( "Failed to do client debit "
          + ", found unknown payment type" );
      return result;
    }
    log.debug( "Found client payment type = "
        + PaymentType.paymentTypeToString( paymentType ) );

    // trying to diduct account based on client payment type
    String strLevel = "unknown";
    BillingResult billingResult = null;
    if ( paymentType == PaymentType.PREPAID ) {
      // when the traffic message use outgoing channel profile
      // it will perform debit in the event level
      if ( eventBean.getChannel() ) {
        strLevel = "event";
        billingResult = billingApp.doDebit( AccountProfile.EVENT_PREPAID ,
            new Integer( eventId ) , new Double( debitUnit ) );
        if ( ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_FAILED_NOACCOUNT )
            || ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_FAILED_NOTENOUGHBALANCE ) ) {
          // when found not enough balance in the event level
          // will perform re-debit in the client level
          log.debug( "Found no account and/or not enough balance in "
              + "event prepaid account , trying to re-debit in "
              + "client prepaid account" );
          strLevel = "client";
          billingResult = billingApp.doDebit( AccountProfile.CLIENT_PREPAID ,
              new Integer( clientId ) , new Double( debitUnit ) );
        }
      } else {
        strLevel = "client";
        billingResult = billingApp.doDebit( AccountProfile.CLIENT_PREPAID ,
            new Integer( clientId ) , new Double( debitUnit ) );
      }
    }
    if ( paymentType == PaymentType.POSTPAID ) {
      strLevel = "client";
      billingResult = billingApp.doDebit( AccountProfile.CLIENT_POSTPAID ,
          new Integer( clientId ) , new Double( debitUnit ) );
    }
    if ( billingResult == null ) {
      log.warning( "Failed to do the " + strLevel + " debit "
          + ", found null billing result" );
      return result;
    }
    if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_SUCCEED ) {

      // successfully perform client / event debit
      result = true;
      log.debug( "Succeed to perform " + strLevel
          + " debit , defined balance before = "
          + billingResult.getBalanceBefore() + " unit(s) , balance after = "
          + billingResult.getBalanceAfter() + " unit(s) " );

      if ( strLevel.equals( "client" ) ) {
        // get current balance
        Double clientBalanceBefore = billingResult.getBalanceBefore();
        Double clientBalanceAfter = billingResult.getBalanceAfter();
        if ( ( clientBalanceBefore != null ) && ( clientBalanceAfter != null ) ) {
          // verify balance threshold
          AlertService.alertClientLowBalance( clientId , clientBean.getEmail() ,
              clientBean.getManager() , clientBean.getCompanyName() ,
              clientBean.getBalanceThreshold() ,
              clientBalanceBefore.doubleValue() ,
              clientBalanceAfter.doubleValue() );
        } else {
          log.warning( "Failed to perform alert client low balance "
              + ", found null client balance after value" );
        }
      }

    } else {
      log.warning( "Failed to do the "
          + strLevel
          + " debit , found billing result = "
          + BillingStatus.paymentResultToString( billingResult
              .getPaymentResult() ) );
    }
    return result;
  }

  private boolean doDebitDailyClientTrack( int clientId ) {
    boolean result = false;
    log.debug( "Trying to perform debit daily client track" );
    BillingResult billingResult = billingApp.doDebit(
        AccountProfile.DAILY_CLIENT_TRACK , new Integer( clientId ) ,
        new Double( 1 ) );
    if ( billingResult == null ) {
      log.warning( "Failed to do debit daily client track "
          + ", found null billing result" );
      return result;
    }
    if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_FAILED_NOACCOUNT ) {
      log.debug( "Found no account "
          + ", trying to initialize daily client track account" );
      DailyClientTrackInit dailyClientTrackInit = new DailyClientTrackInit();
      boolean initialized = dailyClientTrackInit.execute( clientId );
      if ( initialized ) {
        log.debug( "Successfully initialized daily client track account "
            + ", trying to re-debit account" );
        billingResult = billingApp.doDebit( AccountProfile.DAILY_CLIENT_TRACK ,
            new Integer( clientId ) , new Double( 1 ) );
        if ( billingResult == null ) {
          log.warning( "Failed to do debit daily client track "
              + ", found null billing result" );
          return result;
        }
      } else {
        log.debug( "Failed to initialize daily client track account" );
      }
    }
    if ( billingResult.getPaymentResult() != BillingStatus.PAYMENT_RESULT_SUCCEED ) {
      log.warning( "Failed to do debit daily client track "
          + ", found billing result = "
          + BillingStatus.paymentResultToString( billingResult
              .getPaymentResult() ) );
      return result;
    } else {
      result = true;
    }
    log.debug( "Successfully perform debit daily client track "
        + ", balanceAfter = " + billingResult.getBalanceAfter() );
    return result;
  }

  private boolean doDebitMonthlyClientTrack( int clientId ) {
    boolean result = false;
    log.debug( "Trying to perform debit monthly client track" );
    BillingResult billingResult = billingApp.doDebit(
        AccountProfile.MONTHLY_CLIENT_TRACK , new Integer( clientId ) ,
        new Double( 1 ) );
    if ( billingResult == null ) {
      log.warning( "Failed to do debit monthly client track "
          + ", found null billing result" );
      return result;
    }
    if ( billingResult.getPaymentResult() == BillingStatus.PAYMENT_RESULT_FAILED_NOACCOUNT ) {
      log.debug( "Found no account "
          + ", trying to initialize monthly client track account" );
      MonthlyClientTrackInit monthlyClientTrackInit = new MonthlyClientTrackInit();
      boolean initialized = monthlyClientTrackInit.execute( clientId );
      if ( initialized ) {
        log.debug( "Successfully initialized monthly client track account "
            + ", trying to re-debit account" );
        billingResult = billingApp.doDebit(
            AccountProfile.MONTHLY_CLIENT_TRACK , new Integer( clientId ) ,
            new Double( 1 ) );
        if ( billingResult == null ) {
          log.warning( "Failed to do debit monthly client track "
              + ", found null billing result" );
          return result;
        }
      } else {
        log.debug( "Failed to initialize monthly client track account" );
      }
    }
    if ( billingResult.getPaymentResult() != BillingStatus.PAYMENT_RESULT_SUCCEED ) {
      log.warning( "Failed to do debit monthly client track "
          + ", found billing result = "
          + BillingStatus.paymentResultToString( billingResult
              .getPaymentResult() ) );
      return result;
    } else {
      result = true;
    }
    log.debug( "Successfully perform debit monthly client track "
        + ", balanceAfter = " + billingResult.getBalanceAfter() );
    return result;
  }

  public boolean doStopProcess( ProcessBean processBean ) {
    boolean result = false;

    if ( processBean == null ) {
      log.warning( "Failed to stop the process , found null process step" );
      return result;
    }

    // update the stop process
    processBean.setResponse( "" );
    processBean.setRfa( "" );
    processBean.setNextStep( "END" );
    result = true;

    return result;
  }

  public boolean hasReservedCodes( EventBean _event ) throws IOException {

    boolean reserved = false;

    /*------------------------
      get event codes
    ------------------------*/
    String codes = _event.getCodes();
    if ( codes == null || codes.equals( "" ) ) {
      return reserved;
    }

    /*------------------------
      check if any code is reserved
    ------------------------*/
    String codesArray[] = StringUtils.split( codes , "," );
    for ( int i = 0 ; i < codesArray.length ; i++ ) {
      BeepcodeBean beepcode = new BeepcodeService().select( codesArray[i] );
      if ( beepcode.getReserved() ) {
        reserved = true;
        break;
      }
    }

    // success
    return reserved;

  } // hasReservedCodes()

  public String[] getMenuNumbers( EventBean _event ) throws IOException {
    Hashtable hMenuNumbers = new Hashtable( 20 );
    ProcessBean processSteps[] = extractProcessClob( _event , false );
    for ( int i = 0 ; i < processSteps.length ; i++ ) {
      ProcessBean _process = processSteps[i];
      if ( _process.getType().equals( "EXPECT" ) ) {
        String paramName = getExpectParamName( _process , processSteps );
        if ( paramName.startsWith( "MENU" ) ) {
          String menuNumber = paramName.substring( 4 );
          if ( !hMenuNumbers.containsKey( menuNumber ) )
            hMenuNumbers.put( menuNumber , menuNumber );
        }
      }
      if ( _process.getType().equals( "VAR" ) ) {
        String paramName = getVarParamName( _process , processSteps );
        if ( paramName.startsWith( "MENU" ) ) {
          String menuNumber = paramName.substring( 4 );
          if ( !hMenuNumbers.containsKey( menuNumber ) )
            hMenuNumbers.put( menuNumber , menuNumber );
        }
      }
    }
    Vector v = new Vector( hMenuNumbers.values() );
    Util.sortVector( v );
    String menuNumbers[] = new String[v.size()];
    for ( int i = 0 ; i < menuNumbers.length ; i++ ) {
      menuNumbers[i] = (String) v.elementAt( i );
    }
    return menuNumbers;
  } // getMenuNumbers()

  public String[] getMenuChoices( EventBean _event ) throws IOException {
    HashSet hMenuChoices = new HashSet( 100 );

    ProcessBean processSteps[] = extractProcessClob( _event , false );
    for ( int i = 0 ; i < processSteps.length ; i++ ) {
      ProcessBean _process = processSteps[i];
      if ( _process.getType().equals( "EXPECT" ) ) {
        String values[] = _process.getNames();
        for ( int j = 0 ; j < values.length ; j++ )
          hMenuChoices.add( (String) values[j] );
      }
    }

    String menuChoices[] = new String[hMenuChoices.size()];
    try {
      Iterator iter = hMenuChoices.iterator();
      for ( int i = 0 ; i < menuChoices.length ; i++ )
        menuChoices[i] = (String) iter.next();
    } catch ( Exception e ) {
    }

    return menuChoices;
  }

} // eof
