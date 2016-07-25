package com.beepcast.model.transaction;

import java.io.Serializable;

public class SMSBean implements Serializable {

  private static final long serialVersionUID = -3000406236257466459L;

  // private members
  private String phone = "";
  private String phoneList[];
  private String message = "";
  private String senderID = "";// Dev0
  private long elapsedTime;
  private int simIndex;
  private int messageType;

  /*****************************************************************************
   * No-args constructor.
   ****************************************************************************/
  public SMSBean() {
  }

  /*****************************************************************************
   * Set phone.
   ****************************************************************************/
  public void setPhone( String phone ) {
    this.phone = phone;
  }

  /**
   * Get phone.
   */
  public String getPhone() {
    return phone;
  }

  /*****************************************************************************
   * Set phone list.
   ****************************************************************************/
  public void setPhoneList( String phoneList[] ) {
    this.phoneList = phoneList;
  }

  /**
   * Get phone list.
   */
  public String[] getPhoneList() {
    return phoneList;
  }

  /*****************************************************************************
   * Set message.
   ****************************************************************************/
  public void setMessage( String message ) {
    this.message = message;
  }

  /**
   * Get message.
   */
  public String getMessage() {
    return message;
  }

  /*****************************************************************************
   * Set elapsed time.
   ****************************************************************************/
  public void setElapsedTime( long elapsedTime ) {
    this.elapsedTime = elapsedTime;
  }

  /**
   * Get elapsed time.
   */
  public long getElapsedTime() {
    return elapsedTime;
  }

  /*****************************************************************************
   * Set sim card index.
   ****************************************************************************/
  public void setSimIndex( int simIndex ) {
    this.simIndex = simIndex;
  }

  /**
   * Get elapsed time.
   */
  public int getSimIndex() {
    return simIndex;
  }

  /*****************************************************************************
   * Set message type.
   ****************************************************************************/
  public void setMessageType( int messageType ) {
    this.messageType = messageType;
  }

  /**
   * Get message type.
   */
  public int getMessageType() {
    return messageType;
  }

  public String getSenderID() {// Dev0
    return senderID;
  }

  public void setSenderID( String senderID ) {
    this.senderID = senderID;
  }

  // ////////////////////////////////////////////////////////////////////////////
  //
  // Helper
  //
  // ////////////////////////////////////////////////////////////////////////////

  public String toString() {
    final String TAB = " ";
    StringBuffer retValue = new StringBuffer();
    retValue.append( "SMSBean ( " ).append( super.toString() ).append( TAB )
        .append( "phone = " ).append( this.phone ).append( TAB )
        .append( "phoneList = " ).append( this.phoneList ).append( TAB )
        .append( "message = " ).append( this.message ).append( TAB )
        .append( "senderID = " ).append( this.senderID ).append( TAB )
        .append( "elapsedTime = " ).append( this.elapsedTime ).append( TAB )
        .append( "simIndex = " ).append( this.simIndex ).append( TAB )
        .append( "messageType = " ).append( this.messageType ).append( TAB )
        .append( " )" );
    return retValue.toString();
  }

} // eof
