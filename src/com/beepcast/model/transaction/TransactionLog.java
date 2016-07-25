package com.beepcast.model.transaction;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionLog {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "Transaction" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private String headerLog;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public TransactionLog() {
    headerLog = "";
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void generateHeaderLog( String logId ) {
    if ( ( logId != null ) && ( !logId.equals( "" ) ) ) {
      headerLog = "[" + logId + "] ";
    }
  }

  public String header() {
    return headerLog;
  }

  public void error( String strLog ) {
    DLog.error( lctx , headerLog + strLog );
  }

  public void warning( String strLog ) {
    DLog.warning( lctx , headerLog + strLog );
  }

  public void info( String strLog ) {
    DLog.info( lctx , headerLog + strLog );
  }

  public void debug( String strLog ) {
    DLog.debug( lctx , headerLog + strLog );
  }

}
