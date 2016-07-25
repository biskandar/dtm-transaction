package com.beepcast.model.transaction;

import java.util.Date;

public class TransactionLogBean {

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // ////////////////////////////////////////////////////////////////////////////

  private int logId;
  private long clientID;
  private long eventID;
  private int nextStep;
  private long catagoryID;
  private Date dateTm;
  private String phone;
  private String providerId;
  private int messageCount;
  private String code;
  private String params;
  private int jumpCount;
  private long locationID;
  private int closedReasonId;

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // ////////////////////////////////////////////////////////////////////////////

  public TransactionLogBean() {
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Set / Get Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public int getLogId() {
    return logId;
  }

  public void setLogId( int logId ) {
    this.logId = logId;
  }

  public void setClientID( long clientID ) {
    this.clientID = clientID;
  }

  public long getClientID() {
    return clientID;
  }

  public void setEventID( long eventID ) {
    this.eventID = eventID;
  }

  public long getEventID() {
    return eventID;
  }

  public int getNextStep() {
    return nextStep;
  }

  public void setNextStep( int nextStep ) {
    this.nextStep = nextStep;
  }

  public void setCatagoryID( long catagoryID ) {
    this.catagoryID = catagoryID;
  }

  public long getCatagoryID() {
    return catagoryID;
  }

  public void setDateTm( Date dateTm ) {
    this.dateTm = dateTm;
  }

  public Date getDateTm() {
    return dateTm;
  }

  public void setPhone( String phone ) {
    this.phone = phone;
  }

  public String getPhone() {
    return phone;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId( String providerId ) {
    this.providerId = providerId;
  }

  public void setMessageCount( int messageCount ) {
    this.messageCount = messageCount;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public void setCode( String code ) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public void setParams( String params ) {
    this.params = params;
  }

  public String getParams() {
    return params;
  }

  public void setJumpCount( int jumpCount ) {
    this.jumpCount = jumpCount;
  }

  public int getJumpCount() {
    return jumpCount;
  }

  public void setLocationID( long locationID ) {
    this.locationID = locationID;
  }

  public long getLocationID() {
    return locationID;
  }

  public int getClosedReasonId() {
    return closedReasonId;
  }

  public void setClosedReasonId( int closedReasonId ) {
    this.closedReasonId = closedReasonId;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Helper
  //
  // ////////////////////////////////////////////////////////////////////////////

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "TransactionLogBean ( " + "logId = " + this.logId + TAB
        + "clientID = " + this.clientID + TAB + "eventID = " + this.eventID
        + TAB + "nextStep = " + this.nextStep + TAB + "catagoryID = "
        + this.catagoryID + TAB + "dateTm = " + this.dateTm + TAB + "phone = "
        + this.phone + TAB + "messageCount = " + this.messageCount + TAB
        + "code = " + this.code + TAB + "params = " + this.params + TAB
        + "jumpCount = " + this.jumpCount + TAB + "locationID = "
        + this.locationID + TAB + "closedReasonId = " + this.closedReasonId
        + TAB + " )";
    return retValue;
  }

}
