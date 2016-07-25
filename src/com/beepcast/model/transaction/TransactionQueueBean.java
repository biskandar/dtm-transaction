package com.beepcast.model.transaction;

import java.util.Date;

public class TransactionQueueBean {

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // ////////////////////////////////////////////////////////////////////////////

  private long queueId;
  private long clientID;
  private long eventID;
  private long pendingEventID;
  private Date dateTm;
  private String phone;
  private String providerId;
  private int nextStep;
  private int messageCount;
  private String code;
  private String pendingCode;
  private String params;
  private boolean updateProfile;
  private boolean newUser;
  private int jumpCount;
  private long locationID;
  private long callingEventID;

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // ////////////////////////////////////////////////////////////////////////////

  public TransactionQueueBean() {
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Set / Get Function
  //
  // ////////////////////////////////////////////////////////////////////////////

  public long getQueueId() {
    return queueId;
  }

  public void setQueueId( long queueId ) {
    this.queueId = queueId;
  }

  public long getClientID() {
    return clientID;
  }

  public void setClientID( long clientID ) {
    this.clientID = clientID;
  }

  public long getEventID() {
    return eventID;
  }

  public void setEventID( long eventID ) {
    this.eventID = eventID;
  }

  public long getPendingEventID() {
    return pendingEventID;
  }

  public void setPendingEventID( long pendingEventID ) {
    this.pendingEventID = pendingEventID;
  }

  public Date getDateTm() {
    return dateTm;
  }

  public void setDateTm( Date dateTm ) {
    this.dateTm = dateTm;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone( String phone ) {
    this.phone = phone;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId( String providerId ) {
    this.providerId = providerId;
  }

  public int getNextStep() {
    return nextStep;
  }

  public void setNextStep( int nextStep ) {
    this.nextStep = nextStep;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public void setMessageCount( int messageCount ) {
    this.messageCount = messageCount;
  }

  public String getCode() {
    return code;
  }

  public void setCode( String code ) {
    this.code = code;
  }

  public String getPendingCode() {
    return pendingCode;
  }

  public void setPendingCode( String pendingCode ) {
    this.pendingCode = pendingCode;
  }

  public String getParams() {
    return params;
  }

  public void setParams( String params ) {
    this.params = params;
  }

  public boolean isUpdateProfile() {
    return updateProfile;
  }

  public void setUpdateProfile( boolean updateProfile ) {
    this.updateProfile = updateProfile;
  }

  public boolean isNewUser() {
    return newUser;
  }

  public void setNewUser( boolean newUser ) {
    this.newUser = newUser;
  }

  public int getJumpCount() {
    return jumpCount;
  }

  public void setJumpCount( int jumpCount ) {
    this.jumpCount = jumpCount;
  }

  public long getLocationID() {
    return locationID;
  }

  public void setLocationID( long locationID ) {
    this.locationID = locationID;
  }

  public long getCallingEventID() {
    return callingEventID;
  }

  public void setCallingEventID( long callingEventID ) {
    this.callingEventID = callingEventID;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Helper
  //
  // ////////////////////////////////////////////////////////////////////////////

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "TransactionQueueBean ( " + "queueId = " + this.queueId + TAB
        + "clientID = " + this.clientID + TAB + "eventID = " + this.eventID
        + TAB + "pendingEventID = " + this.pendingEventID + TAB + "dateTm = "
        + this.dateTm + TAB + "phone = " + this.phone + TAB + "providerId = "
        + this.providerId + TAB + "nextStep = " + this.nextStep + TAB
        + "messageCount = " + this.messageCount + TAB + "code = " + this.code
        + TAB + "pendingCode = " + this.pendingCode + TAB + "params = "
        + this.params + TAB + "updateProfile = " + this.updateProfile + TAB
        + "newUser = " + this.newUser + TAB + "jumpCount = " + this.jumpCount
        + TAB + "locationID = " + this.locationID + TAB + "callingEventID = "
        + this.callingEventID + TAB + " )";
    return retValue;
  }

}
