package com.beepcast.model.transaction.script;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ScriptWebhookExec {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "ScriptWebhookExec" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private boolean initialized;
  private String headerLog;
  private String scriptSource;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public ScriptWebhookExec( String headerLog , String scriptSource ) {
    initialized = false;

    // setup header log
    headerLog = ( headerLog == null ) ? "" : headerLog;
    this.headerLog = headerLog;

    // setup script source
    if ( StringUtils.isBlank( scriptSource ) ) {
      DLog.warning( lctx , headerLog + "Failed to initialized "
          + ", found blank script source" );
      return;
    }
    this.scriptSource = scriptSource;

    // log it
    DLog.debug( lctx , headerLog + "Created script webhook execution "
        + ": script = " + StringEscapeUtils.escapeJava( scriptSource ) );

    initialized = true;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean execute( ScriptWebhookData data ) {
    boolean result = false;

    if ( !initialized ) {
      DLog.warning( lctx , headerLog + "Failed to execute "
          + ", found not yet initialized" );
      return result;
    }

    Context scriptContext = Context.enter();
    try {

      // prepare the script scope
      ScriptableObject scriptScope = scriptContext.initStandardObjects();

      // prepare the script data
      scriptScope.put( "request" , scriptScope , data );

      // execute the script
      int lineNo = 1;
      Object securityDomain = null;
      scriptContext.evaluateString( scriptScope , scriptSource ,
          "ScriptWebhookExec" , lineNo , securityDomain );

      // return as true
      result = true;

    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to execute , " + e );
    } finally {
      Context.exit();
    }

    return result;
  }

}
