package com.beepcast.model.transaction;

import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class BogusRequestService {

  static final DLogContext lctx = new SimpleContext( "BogusRequestService" );

  private BogusRequestDAO bogusRequestDAO;

  public BogusRequestService() {
    bogusRequestDAO = new BogusRequestDAO();
  }

  public boolean insert( BogusRequestBean bean ) {
    return bogusRequestDAO.insert( bean );
  }

}
