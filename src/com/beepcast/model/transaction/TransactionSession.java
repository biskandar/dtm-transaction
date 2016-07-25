package com.beepcast.model.transaction;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.beepcast.api.client.ClientApp;
import com.beepcast.api.provider.ProviderApp;
import com.beepcast.billing.BillingApp;
import com.beepcast.database.DatabaseLibrary;
import com.beepcast.dbmanager.DBManagerApp;
import com.beepcast.idgen.IdGenApp;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionSession {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "TransactionSession" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private IdGenApp idGenApp;
  private DatabaseLibrary dbLib;
  private DBManagerApp dbMan;
  private BillingApp billingApp;
  private ClientApp clientApp;
  private ProviderApp providerApp;

  private TransactionQueueService transQueueService;
  private TransactionLogService transLogService;

  private TransactionProcessBasic trans;
  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionSession( TransactionProcessBasic trans ) {

    idGenApp = IdGenApp.getInstance();
    dbLib = DatabaseLibrary.getInstance();
    dbMan = DBManagerApp.getInstance();
    billingApp = BillingApp.getInstance();
    clientApp = ClientApp.getInstance();
    providerApp = ProviderApp.getInstance();

    transQueueService = new TransactionQueueService();
    transLogService = new TransactionLogService();

    // transfer parameter(s)
    this.trans = trans;
    this.log = trans.log();

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionQueueBean getLastSession( String phoneNumber ) {
    return getLastSession( phoneNumber , -1 );
  }

  public TransactionQueueBean getLastSession( String phoneNumber , int eventId ) {
    TransactionQueueBean transQueue = null;
    log.debug( "Trying to get last session , based on : phoneNumber = "
        + phoneNumber + " , eventId = " + eventId );
    if ( ( phoneNumber == null ) || ( phoneNumber.equals( "" ) ) ) {
      log.warning( "Failed to get last session , found null phoneNumber" );
      return transQueue;
    }
    transQueue = transQueueService.select( phoneNumber , eventId );
    return transQueue;
  }

  public boolean deleteSession( TransactionQueueBean transQueue ) {
    boolean result = false;

    if ( transQueue == null ) {
      log.warning( "Failed to delete session , found null trans queue" );
      return result;
    }

    result = transQueueService.delete( transQueue );
    if ( !result ) {
      log.warning( "Failed to delete session" );
      return result;
    }

    return result;
  }

  public boolean closeSession( TransactionQueueBean transQueue ,
      int closedReasonId ) {
    boolean result = false;

    if ( transQueue == null ) {
      log.warning( "Failed to close session , found null trans queue" );
      return result;
    }

    // disponse current session based on queue id and/or phone
    if ( !transQueueService.delete( transQueue ) ) {
      log.warning( "Failed to delete trans queue : queueId = "
          + transQueue.getQueueId() + " , phone = " + transQueue.getPhone() );
      return result;
    }

    // as long as the current session is disposed it will return as true
    result = true;

    // log into history session ( save it )
    if ( !transLogService.logTransaction( transQueue , closedReasonId ) ) {
      log.warning( "Failed to insert trans log : queueId = "
          + transQueue.getQueueId() + " , phone = " + transQueue.getPhone()
          + " , closedReasonId = " + closedReasonId );
    }

    return result;
  }

  public TransactionQueueBean createSession( boolean persist , long eventId ,
      long clientId , String phoneNumber , String providerId , String code ,
      long locationId ) {
    TransactionQueueBean tqBean = null;

    // must be params

    if ( eventId < 1 ) {
      log.warning( "Failed to create session , found zero eventId" );
      return tqBean;
    }

    if ( clientId < 1 ) {
      log.warning( "Failed to create session , found zero clientId" );
      return tqBean;
    }

    if ( StringUtils.isBlank( phoneNumber ) ) {
      log.warning( "Failed to create session , found empty phoneNumber" );
      return tqBean;
    }

    if ( StringUtils.isBlank( code ) ) {
      log.warning( "Failed to create session , found empty code" );
      return tqBean;
    }

    // clean params

    phoneNumber = ( phoneNumber == null ) ? "" : phoneNumber.trim();
    providerId = ( providerId == null ) ? "" : providerId.trim();
    code = ( code == null ) ? "" : code.trim();

    // prepare other params

    long pendingEventID = 0;
    Date dateTm = new Date();
    int nextStep = 1; // 1 indicates new transaction
    int messageCount = 0;
    String pendingCode = null;
    String params = "";
    boolean updateProfile = false;
    boolean newUser = false;
    int jumpCount = 0;
    long callingEventID = 0;

    // build trans queue bean from factory

    tqBean = TransactionQueueBeanFactory.createTransactionQueueBean( eventId ,
        clientId , pendingEventID , dateTm , phoneNumber , providerId ,
        nextStep , messageCount , code , pendingCode , params , updateProfile ,
        newUser , jumpCount , locationId , callingEventID );
    if ( tqBean == null ) {
      log.warning( "Failed to create session , found failed to create "
          + "transaction queue bean from factory" );
      return tqBean;
    }
    log.debug( "Created transaction queue bean from factory : eventId = "
        + tqBean.getEventID() + " , clientId = " + tqBean.getClientID()
        + " , pendingEventId = " + tqBean.getPendingEventID() + " , dateTm = "
        + tqBean.getDateTm() + " , phoneNumber = " + tqBean.getPhone()
        + " , providerId = " + tqBean.getProviderId() + " , nextStep = "
        + tqBean.getNextStep() + " , messageCount = "
        + tqBean.getMessageCount() + " , code = " + tqBean.getCode()
        + " , params = " + tqBean.getParams() + " , updateProfile = "
        + tqBean.isUpdateProfile() + " , newUser = " + tqBean.isNewUser()
        + " , jumpCount = " + tqBean.getJumpCount() );

    // need to persist ?

    if ( !persist ) {
      log.debug( "Created session without persist into table, bypass insert." );
      return tqBean;
    }

    // insert into table

    if ( !transQueueService.insert( tqBean ) ) {
      log.warning( "Failed to create session "
          + ", found failed insert into trans queue table" );
      tqBean = null;
      return tqBean;
    }

    // log it

    log.debug( "Persisted transaction queue bean into the table" );

    return tqBean;
  }

  public TransactionQueueBean createSession( TransactionLogBean transLog ) {
    TransactionQueueBean transQueue = null;

    // must be params

    if ( transLog == null ) {
      log.warning( "Failed to create session , found null trans log" );
      return transQueue;
    }

    // read params

    Date dateTm = transLog.getDateTm();
    String phone = transLog.getPhone();
    String providerId = transLog.getProviderId();
    String code = transLog.getCode();

    // validate params

    if ( StringUtils.isBlank( phone ) ) {
      log.warning( "Failed to create session , found blank phone" );
      return transQueue;
    }
    if ( StringUtils.isBlank( code ) ) {
      log.warning( "Failed to create session , found blank code" );
      return transQueue;
    }

    // clean params

    providerId = ( providerId == null ) ? "" : providerId.trim();
    phone = ( phone == null ) ? "" : phone.trim();
    dateTm = ( dateTm == null ) ? new Date() : dateTm;
    code = ( code == null ) ? "" : code.trim();

    // prepare other params

    long pendingEventID = 0;
    String pendingCode = null;
    boolean updateProfile = false;
    boolean newUser = false;
    long callingEventID = 0;

    // build trans queue bean from factory

    transQueue = TransactionQueueBeanFactory.createTransactionQueueBean(
        transLog.getEventID() , transLog.getClientID() , pendingEventID ,
        dateTm , phone , providerId , transLog.getNextStep() ,
        transLog.getMessageCount() , code , pendingCode , transLog.getParams() ,
        updateProfile , newUser , transLog.getJumpCount() ,
        transLog.getLocationID() , callingEventID );

    // insert into table

    if ( !transQueueService.insert( transQueue ) ) {
      log.warning( "Failed to create session "
          + ", found failed insert into trans queue table" );
      transQueue = null;
      return transQueue;
    }

    return transQueue;
  }

}
