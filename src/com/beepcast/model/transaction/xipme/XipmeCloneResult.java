package com.beepcast.model.transaction.xipme;

public class XipmeCloneResult {

  private String masterMapId;
  private String mapId;
  private String mapIdEncrypted;
  private String gatewayXipmeId;

  public XipmeCloneResult() {
  }

  public String getMasterMapId() {
    return masterMapId;
  }

  public void setMasterMapId( String masterMapId ) {
    this.masterMapId = masterMapId;
  }

  public String getMapId() {
    return mapId;
  }

  public void setMapId( String mapId ) {
    this.mapId = mapId;
  }

  public String getMapIdEncrypted() {
    return mapIdEncrypted;
  }

  public void setMapIdEncrypted( String mapIdEncrypted ) {
    this.mapIdEncrypted = mapIdEncrypted;
  }

  public String getGatewayXipmeId() {
    return gatewayXipmeId;
  }

  public void setGatewayXipmeId( String gatewayXipmeId ) {
    this.gatewayXipmeId = gatewayXipmeId;
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "XipmeCloneResult ( " + "masterMapId = " + this.masterMapId
        + TAB + "mapId = " + this.mapId + TAB + "mapIdEncrypted = "
        + this.mapIdEncrypted + TAB + "gatewayXipmeId = " + this.gatewayXipmeId
        + TAB + " )";
    return retValue;
  }

}
