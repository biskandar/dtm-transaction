package com.beepcast.model.transaction.xipme;

public class XipmeCloneParamFactory {

  public static XipmeCloneParam createXipmeCloneParam( String xipmeCode ,
      String xipmeLink , String targetLink , String targetMobileLink ) {
    XipmeCloneParam xipmeCloneParam = new XipmeCloneParam();
    xipmeCloneParam.setXipmeCode( xipmeCode );
    xipmeCloneParam.setXipmeLink( xipmeLink );
    xipmeCloneParam.setTargetLink( targetLink );
    xipmeCloneParam.setTargetMobileLink( targetMobileLink );
    return xipmeCloneParam;
  }
}
