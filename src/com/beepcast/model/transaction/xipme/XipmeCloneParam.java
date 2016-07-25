package com.beepcast.model.transaction.xipme;

public class XipmeCloneParam {

  private String xipmeCode;
  private String xipmeLink;
  private String targetLink;
  private String targetMobileLink;

  public XipmeCloneParam() {

  }

  public String getXipmeCode() {
    return xipmeCode;
  }

  public void setXipmeCode( String xipmeCode ) {
    this.xipmeCode = xipmeCode;
  }

  public String getXipmeLink() {
    return xipmeLink;
  }

  public void setXipmeLink( String xipmeLink ) {
    this.xipmeLink = xipmeLink;
  }

  public String getTargetLink() {
    return targetLink;
  }

  public void setTargetLink( String targetLink ) {
    this.targetLink = targetLink;
  }

  public String getTargetMobileLink() {
    return targetMobileLink;
  }

  public void setTargetMobileLink( String targetMobileLink ) {
    this.targetMobileLink = targetMobileLink;
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "XipmeCloneParam ( " + "xipmeCode = " + this.xipmeCode + TAB
        + "xipmeLink = " + this.xipmeLink + TAB + "targetLink = "
        + this.targetLink + TAB + "targetMobileLink = " + this.targetMobileLink
        + TAB + " )";
    return retValue;
  }

}
