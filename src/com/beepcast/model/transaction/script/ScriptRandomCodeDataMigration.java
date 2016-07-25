package com.beepcast.model.transaction.script;

import com.beepcast.model.transaction.TransactionOutputMessage;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ScriptRandomCodeDataMigration {

  static final DLogContext lctx = new SimpleContext(
      "ScriptRandomCodeDataMigration" );

  public static boolean export( ScriptRandomCodeData data ,
      TransactionOutputMessage omsg ) {
    boolean result = false;

    if ( data == null ) {
      return result;
    }

    if ( omsg == null ) {
      return result;
    }

    // nothing to do yet ...

    result = true;
    return result;
  }

}
