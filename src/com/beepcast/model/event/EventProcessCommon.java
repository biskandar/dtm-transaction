package com.beepcast.model.event;

import java.util.ArrayList;
import java.util.List;

import com.beepcast.model.transaction.TransactionProcessBeanUtils;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class EventProcessCommon {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "EventProcessCommon" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public static String[] getFirstAndNextExecutedProcessBeanResponses(
      int eventId ) {
    String[] arrResponses = null;

    List listProcessBeans = getFirstAndNextExecutedProcessBeans( eventId );
    if ( listProcessBeans == null ) {
      return arrResponses;
    }

    arrResponses = new String[listProcessBeans.size()];

    ProcessBean processBean = null;
    for ( int idx = 0 ; idx < arrResponses.length ; idx++ ) {
      processBean = (ProcessBean) listProcessBeans.get( idx );
      if ( processBean == null ) {
        continue;
      }
      arrResponses[idx] = processBean.getResponse();
    }

    return arrResponses;
  }

  public static List getFirstAndNextExecutedProcessBeans( int eventId ) {
    List listProcessBeans = new ArrayList();

    if ( eventId < 1 ) {
      return listProcessBeans;
    }

    ProcessBean[] arrProcessBeans = ProcessCommon.getProcessBeans( eventId );
    if ( ( arrProcessBeans == null ) || ( arrProcessBeans.length < 1 ) ) {
      return listProcessBeans;
    }

    ProcessBean processBeanFirst = ProcessCommon.getFirstProcessBean( eventId );
    if ( processBeanFirst == null ) {
      return listProcessBeans;
    }

    listProcessBeans.add( processBeanFirst );

    ProcessBean processBeanNext = ProcessCommon.nextProcessBean(
        processBeanFirst , arrProcessBeans );
    while ( processBeanNext != null ) {

      // stop for expected and/or code process type
      String processType = processBeanNext.getType();
      if ( TransactionProcessBeanUtils.validProcessTypeAll( processType ) ) {
        break;
      }

      listProcessBeans.add( processBeanNext );

      processBeanNext = ProcessCommon.nextProcessBean( processBeanNext ,
          arrProcessBeans );
    }

    return listProcessBeans;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  // ...

}
