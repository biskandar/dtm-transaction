package com.beepcast.model.transaction;

import java.io.Serializable;
import java.util.Date;

public class BogusRequestBean implements Serializable {

  private static final long serialVersionUID = 911529502120666222L;

  private long clientID;
  private long eventID;
  private String phone;
  private String shortCode;
  private Date dateTm;
  private String message;
  private String description;

  public BogusRequestBean() {
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

  public String getPhone() {
    return phone;
  }

  public void setPhone( String phone ) {
    this.phone = phone;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode( String shortCode ) {
    this.shortCode = shortCode;
  }

  public Date getDateTm() {
    return dateTm;
  }

  public void setDateTm( Date dateTm ) {
    this.dateTm = dateTm;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "BogusRequestBean ( " + "clientID = " + this.clientID + TAB
        + "eventID = " + this.eventID + TAB + "phone = " + this.phone + TAB
        + "shortCode = " + this.shortCode + TAB + "dateTm = " + this.dateTm
        + TAB + "message = " + this.message + TAB + "description = "
        + this.description + TAB + " )";
    return retValue;
  }

}
