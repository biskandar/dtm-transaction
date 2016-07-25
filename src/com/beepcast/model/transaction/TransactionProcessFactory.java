package com.beepcast.model.transaction;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionProcessFactory {

  static final DLogContext lctx = new SimpleContext( "TransactionFactory" );

  public static TransactionProcess generateTransactionProcessStandard() {
    return generateTransactionProcess( TransactionProcessType.STANDARD , false );
  }

  public static TransactionProcess generateTransactionProcessStandard(
      boolean debug ) {
    return generateTransactionProcess( TransactionProcessType.STANDARD , debug );
  }

  public static TransactionProcess generateTransactionProcessInteract() {
    return generateTransactionProcess( TransactionProcessType.INTERACT , false );
  }

  public static TransactionProcess generateTransactionProcessInteract(
      boolean debug ) {
    return generateTransactionProcess( TransactionProcessType.INTERACT , debug );
  }

  public static TransactionProcess generateTransactionProcessBulk() {
    return generateTransactionProcess( TransactionProcessType.BULK , false );
  }

  public static TransactionProcess generateTransactionProcessBulk( boolean debug ) {
    return generateTransactionProcess( TransactionProcessType.BULK , debug );
  }

  public static TransactionProcess generateTransactionProcessSimulation() {
    return generateTransactionProcess( TransactionProcessType.SIMULATION ,
        false );
  }

  public static TransactionProcess generateTransactionProcessSimulation(
      boolean debug ) {
    return generateTransactionProcess( TransactionProcessType.SIMULATION ,
        debug );
  }

  public static TransactionProcess generateTransactionProcess( int type ,
      boolean debug ) {
    TransactionProcess transProcess = null;
    switch ( type ) {
    case TransactionProcessType.STANDARD :
      transProcess = new TransactionProcessStandard( debug );
      break;
    case TransactionProcessType.INTERACT :
      transProcess = new TransactionProcessInteract( debug );
      break;
    case TransactionProcessType.BULK :
      transProcess = new TransactionProcessBulk( debug );
      break;
    case TransactionProcessType.SIMULATION :
      transProcess = new TransactionProcessSimulation( debug );
      break;
    default :
      DLog.warning( lctx , "Failed to generate transaction process "
          + ", found anonymous type = " + type );
      break;
    }
    return transProcess;
  }

}
