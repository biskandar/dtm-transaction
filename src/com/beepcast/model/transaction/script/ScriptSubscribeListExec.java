package com.beepcast.model.transaction.script;

import org.apache.commons.lang.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ScriptSubscribeListExec {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "ScriptSubscribeListExec" );

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

  public ScriptSubscribeListExec( String headerLog , String scriptSource ) {
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

    initialized = true;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public boolean execute( ScriptSubscribeListData data ) {
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

      // setup old data
      ScriptSubscribeListData dataCur = (ScriptSubscribeListData) data.clone();

      // prepare the script data
      scriptScope.put( "list" , scriptScope , data );

      // execute the script
      int lineNo = 1;
      Object securityDomain = null;
      scriptContext.evaluateString( scriptScope , scriptSource ,
          "ScriptSubscribeListExec" , lineNo , securityDomain );

      // log for the data difference
      logDataDiff( dataCur , data );

      // return as true
      result = true;

    } catch ( Exception e ) {
      DLog.warning( lctx , headerLog + "Failed to execute , " + e );
    } finally {
      Context.exit();
    }

    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private void logDataDiff( ScriptSubscribeListData dataCur ,
      ScriptSubscribeListData dataNew ) {

    if ( ( dataCur == null ) || ( dataNew == null ) ) {
      return;
    }

    StringBuffer sbLog = new StringBuffer();

    sbLog.append( "insert = " + dataCur.isInsert() + " -> "
        + dataNew.isInsert() + " , " );

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom0() ,
        dataNew.getCustom0() ) ) {
      sbLog.append( "Custom0 = " + dataCur.getCustom0() + " -> "
          + dataNew.getCustom0() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom1() ,
        dataNew.getCustom1() ) ) {
      sbLog.append( "Custom1 = " + dataCur.getCustom1() + " -> "
          + dataNew.getCustom1() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom2() ,
        dataNew.getCustom2() ) ) {
      sbLog.append( "Custom2 = " + dataCur.getCustom2() + " -> "
          + dataNew.getCustom2() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom3() ,
        dataNew.getCustom3() ) ) {
      sbLog.append( "Custom3 = " + dataCur.getCustom3() + " -> "
          + dataNew.getCustom3() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom4() ,
        dataNew.getCustom4() ) ) {
      sbLog.append( "Custom4 = " + dataCur.getCustom4() + " -> "
          + dataNew.getCustom4() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom5() ,
        dataNew.getCustom5() ) ) {
      sbLog.append( "Custom5 = " + dataCur.getCustom5() + " -> "
          + dataNew.getCustom5() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom6() ,
        dataNew.getCustom6() ) ) {
      sbLog.append( "Custom6 = " + dataCur.getCustom6() + " -> "
          + dataNew.getCustom6() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom7() ,
        dataNew.getCustom7() ) ) {
      sbLog.append( "Custom7 = " + dataCur.getCustom7() + " -> "
          + dataNew.getCustom7() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom8() ,
        dataNew.getCustom8() ) ) {
      sbLog.append( "Custom8 = " + dataCur.getCustom8() + " -> "
          + dataNew.getCustom8() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom9() ,
        dataNew.getCustom9() ) ) {
      sbLog.append( "Custom9 = " + dataCur.getCustom9() + " -> "
          + dataNew.getCustom9() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom10() ,
        dataNew.getCustom10() ) ) {
      sbLog.append( "Custom10 = " + dataCur.getCustom10() + " -> "
          + dataNew.getCustom10() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom11() ,
        dataNew.getCustom11() ) ) {
      sbLog.append( "Custom11 = " + dataCur.getCustom11() + " -> "
          + dataNew.getCustom11() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom12() ,
        dataNew.getCustom12() ) ) {
      sbLog.append( "Custom12 = " + dataCur.getCustom12() + " -> "
          + dataNew.getCustom12() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom13() ,
        dataNew.getCustom13() ) ) {
      sbLog.append( "Custom13 = " + dataCur.getCustom13() + " -> "
          + dataNew.getCustom13() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom14() ,
        dataNew.getCustom14() ) ) {
      sbLog.append( "Custom14 = " + dataCur.getCustom14() + " -> "
          + dataNew.getCustom14() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom15() ,
        dataNew.getCustom15() ) ) {
      sbLog.append( "Custom15 = " + dataCur.getCustom15() + " -> "
          + dataNew.getCustom15() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom16() ,
        dataNew.getCustom16() ) ) {
      sbLog.append( "Custom16 = " + dataCur.getCustom16() + " -> "
          + dataNew.getCustom16() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom17() ,
        dataNew.getCustom17() ) ) {
      sbLog.append( "Custom17 = " + dataCur.getCustom17() + " -> "
          + dataNew.getCustom17() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom18() ,
        dataNew.getCustom18() ) ) {
      sbLog.append( "Custom18 = " + dataCur.getCustom18() + " -> "
          + dataNew.getCustom18() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom19() ,
        dataNew.getCustom19() ) ) {
      sbLog.append( "Custom19 = " + dataCur.getCustom19() + " -> "
          + dataNew.getCustom19() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom20() ,
        dataNew.getCustom20() ) ) {
      sbLog.append( "Custom20 = " + dataCur.getCustom20() + " -> "
          + dataNew.getCustom20() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom21() ,
        dataNew.getCustom21() ) ) {
      sbLog.append( "Custom21 = " + dataCur.getCustom21() + " -> "
          + dataNew.getCustom21() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom22() ,
        dataNew.getCustom22() ) ) {
      sbLog.append( "Custom22 = " + dataCur.getCustom22() + " -> "
          + dataNew.getCustom22() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom23() ,
        dataNew.getCustom23() ) ) {
      sbLog.append( "Custom23 = " + dataCur.getCustom23() + " -> "
          + dataNew.getCustom23() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom24() ,
        dataNew.getCustom24() ) ) {
      sbLog.append( "Custom24 = " + dataCur.getCustom24() + " -> "
          + dataNew.getCustom24() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom25() ,
        dataNew.getCustom25() ) ) {
      sbLog.append( "Custom25 = " + dataCur.getCustom25() + " -> "
          + dataNew.getCustom25() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom26() ,
        dataNew.getCustom26() ) ) {
      sbLog.append( "Custom26 = " + dataCur.getCustom26() + " -> "
          + dataNew.getCustom26() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom27() ,
        dataNew.getCustom27() ) ) {
      sbLog.append( "Custom27 = " + dataCur.getCustom27() + " -> "
          + dataNew.getCustom27() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom28() ,
        dataNew.getCustom28() ) ) {
      sbLog.append( "Custom28 = " + dataCur.getCustom8() + " -> "
          + dataNew.getCustom28() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom29() ,
        dataNew.getCustom29() ) ) {
      sbLog.append( "Custom29 = " + dataCur.getCustom29() + " -> "
          + dataNew.getCustom29() + " , " );
    }

    if ( !StringUtils.equalsIgnoreCase( dataCur.getCustom30() ,
        dataNew.getCustom30() ) ) {
      sbLog.append( "Custom30 = " + dataCur.getCustom30() + " -> "
          + dataNew.getCustom30() + " , " );
    }

    DLog.debug( lctx , headerLog + "Executed script subscriber list data : "
        + sbLog.toString() );

  }

}
