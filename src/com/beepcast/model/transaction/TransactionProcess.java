package com.beepcast.model.transaction;

import java.util.Iterator;
import java.util.LinkedList;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public abstract class TransactionProcess implements Process {

  static final DLogContext lctx = new SimpleContext( "TransactionProcess" );

  // prepare output object

  protected LinkedList omsgs;

  // for debug purpose

  protected boolean debugMode;

  // constructor

  public TransactionProcess( boolean debugMode ) {
    omsgs = new LinkedList();

    if ( debugMode ) {
      this.debugMode = debugMode;
      DLog.debug( lctx , "Created main process" );
    }

  }

  // main method

  public int main( TransactionInputMessage imsg ) {
    int processCode = ProcessCode.PROCESS_SUCCEED;
    if ( debugMode ) {
      DLog.debug( lctx , "Begin main process" );
    }

    // initialized
    if ( processCode == ProcessCode.PROCESS_SUCCEED ) {
      if ( debugMode ) {
        DLog.debug( lctx , "Initialize main process" );
      }
      processCode = begin( imsg , omsgs );
    }

    // run the process
    if ( processCode == ProcessCode.PROCESS_SUCCEED ) {
      if ( debugMode ) {
        DLog.debug( lctx , "Run main process" );
      }
      processCode = run( imsg , omsgs );
    }

    // finalized
    if ( processCode == ProcessCode.PROCESS_SUCCEED ) {
      if ( debugMode ) {
        DLog.debug( lctx , "Finalize main process" );
      }
      processCode = end( imsg , omsgs );
    }

    if ( debugMode ) {
      DLog.debug( lctx , "End main process" );
    }
    return processCode;
  }

  // publish output object

  public int sizeOmsgs() {
    return omsgs.size();
  }

  public Iterator iterOmsgs() {
    return omsgs.iterator();
  }

  public TransactionOutputMessage readOmsg( int index ) {
    TransactionOutputMessage omsg = null;
    if ( index < 0 ) {
      DLog.warning( lctx , "Failed to read output message "
          + ", found negatif index" );
      return omsg;
    }
    try {
      if ( omsgs == null ) {
        DLog.warning( lctx , "Failed to read output message "
            + ", found null list outbound messages" );
        return omsg;
      }
      if ( omsgs.size() < 1 ) {
        DLog.warning( lctx , "Failed to read output message "
            + ", found empty list outbound messages" );
        return omsg;
      }
      if ( index >= omsgs.size() ) {
        DLog.warning( lctx , "Failed to read output message "
            + ", found index is out of bounds : " + index );
        return omsg;
      }
      omsg = (TransactionOutputMessage) omsgs.get( index );
    } catch ( Exception e ) {
      DLog.warning( lctx , "Failed to read output message , " + e );
    }
    return omsg;
  }

  // info method

  abstract public String info();

}
