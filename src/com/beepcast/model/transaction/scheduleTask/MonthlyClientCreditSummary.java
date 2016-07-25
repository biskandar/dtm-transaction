package com.beepcast.model.transaction.scheduleTask;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.beepcast.billing.BillingApp;
import com.beepcast.billing.BillingResult;
import com.beepcast.billing.BillingStatus;
import com.beepcast.billing.profile.PaymentType;
import com.beepcast.model.client.ClientBean;
import com.beepcast.model.client.ClientCreditUnitBean;
import com.beepcast.model.client.ClientCreditUnitFactory;
import com.beepcast.model.client.ClientCreditUnitService;
import com.beepcast.model.client.ClientsBean;
import com.beepcast.model.client.ClientsService;
import com.beepcast.model.transaction.billing.AccountProfile;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class MonthlyClientCreditSummary implements Job {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext(
      "MonthlyClientCreditSummary" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private ClientsService clientsService;
  private BillingApp billingApp;
  private ClientCreditUnitService clientCreditUnitService;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public MonthlyClientCreditSummary() {
    super();
    clientsService = new ClientsService();
    billingApp = BillingApp.getInstance();
    clientCreditUnitService = new ClientCreditUnitService();
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void execute( JobExecutionContext context )
      throws JobExecutionException {
    try {

      // trap latency
      long deltaTime = System.currentTimeMillis();

      // get the list of postpaid clients
      Iterator iterClients = iterPostpaidClients();
      if ( iterClients == null ) {
        DLog.debug( lctx , "Failed to execute task "
            + ", found empty postpaid clients" );
        return;
      }

      // iterate each postpaid client
      int totalClients = 0;
      while ( iterClients.hasNext() ) {

        ClientBean clientBean = (ClientBean) iterClients.next();
        if ( clientBean == null ) {
          continue;
        }

        // verify existing client id
        int clientId = (int) clientBean.getClientID();
        if ( clientId < 1 ) {
          continue;
        }

        // verify payment type must be postpaid
        int paymentType = clientBean.getPaymentType();
        if ( paymentType != PaymentType.POSTPAID ) {
          continue;
        }

        BillingResult billingResult = billingApp.getBalance(
            AccountProfile.CLIENT_POSTPAID , new Integer( clientId ) );
        if ( billingResult == null ) {
          continue;
        }

        int paymentResult = billingResult.getPaymentResult();
        if ( paymentResult != BillingStatus.PAYMENT_RESULT_SUCCEED ) {
          continue;
        }

        Double balanceAfter = billingResult.getBalanceAfter();

        if ( !logTransaction( clientId , balanceAfter ) ) {
          continue;
        }

        DLog.debug( lctx , "Stored a payment summary into "
            + "client billing history : clientId = " + clientId
            + " , clientName = " + clientBean.getCompanyName()
            + " , debitAmount = " + balanceAfter + " unit(s)" );

        // summary
        totalClients = totalClients + 1;
      }

      // calculate latency
      deltaTime = System.currentTimeMillis() - deltaTime;
      DLog.debug( lctx , "Successfully executed task , found total "
          + totalClients + " client(s) effected , take " + deltaTime + " ms" );

    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to execute task , " + e );
    }
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Functions
  //
  // //////////////////////////////////////////////////////////////////////////

  private String datePrevMonth() {
    Calendar cal = Calendar.getInstance();
    cal.add( Calendar.MONTH , -1 );
    Date date = new Date( cal.getTimeInMillis() );
    SimpleDateFormat sdf = new SimpleDateFormat( "MMMM yyyy" );
    return sdf.format( date );
  }

  public static String displayUnit( double unit ) {
    NumberFormat formatter = new DecimalFormat( "#0.00" );
    return formatter.format( unit );
  }

  private Iterator iterPostpaidClients() {
    Iterator iterClientBeans = null;

    ClientsBean bean = clientsService
        .generateClientsBeanByPaymentType( PaymentType.POSTPAID );
    if ( bean == null ) {
      return iterClientBeans;
    }

    List listClientBeans = bean
        .listClientBean( ClientsBean.SORT_BY_CLIENTID_ASC );
    if ( listClientBeans == null ) {
      return iterClientBeans;
    }

    iterClientBeans = listClientBeans.iterator();

    return iterClientBeans;
  }

  private boolean logTransaction( int clientId , Double debitAmount ) {
    boolean result = false;

    if ( clientId < 1 ) {
      return result;
    }

    if ( debitAmount == null ) {
      return result;
    }

    double unit = debitAmount.doubleValue();

    String description = "Total units used up till end " + datePrevMonth()
        + " is " + displayUnit( unit );

    ClientCreditUnitBean bean = ClientCreditUnitFactory
        .createClientCreditUnitBean( clientId , unit , description );
    if ( bean == null ) {
      return result;
    }

    if ( !clientCreditUnitService.insert( bean ) ) {
      return result;
    }

    result = true;
    return result;
  }

}
