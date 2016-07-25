package com.beepcast.model.transaction;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.beepcast.util.properties.GlobalEnvironment;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionInitializer implements ServletContextListener {

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // ////////////////////////////////////////////////////////////////////////////

  private static final String PROPERTY_FILE_TRANSACTION = "transaction.config.file";

  static final DLogContext lctx = new SimpleContext( "TransactionInitializer" );

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public void contextInitialized( ServletContextEvent sce ) {

    ServletContext context = sce.getServletContext();
    String logStr = "";

    GlobalEnvironment globalEnv = GlobalEnvironment.getInstance();

    TransactionConf transConf = TransactionConfFactory
        .generateTransactionConf( PROPERTY_FILE_TRANSACTION );
    logStr = this.getClass() + " : initialized " + transConf;
    context.log( logStr );
    System.out.println( logStr );
    DLog.debug( lctx , logStr );

    TransactionApp transApp = TransactionApp.getInstance();
    transApp.init( transConf );
    transApp.moduleStart();
    logStr = this.getClass() + " : initialized " + transApp;
    context.log( logStr );
    System.out.println( logStr );
    DLog.debug( lctx , logStr );

  }

  public void contextDestroyed( ServletContextEvent sce ) {

    ServletContext context = sce.getServletContext();
    String logStr = "";

    GlobalEnvironment globalEnv = GlobalEnvironment.getInstance();

    TransactionApp transApp = TransactionApp.getInstance();
    transApp.moduleStop();
    logStr = this.getClass() + " : destroyed ";
    context.log( logStr );
    System.out.println( logStr );
    DLog.debug( lctx , logStr );

  }

}
