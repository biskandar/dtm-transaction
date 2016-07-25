package com.beepcast.model.transaction.scheduleTask;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class BasicScheduleTask implements Job {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "BasicScheduleTask" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Inherited Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public void execute( JobExecutionContext context )
      throws JobExecutionException {
    DLog.debug( lctx , "test run" );
  }

}
