package com.beepcast.model.transaction;

import java.io.IOException;
import java.io.Serializable;

/*******************************************************************************
 * Receive Buffer Bean.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class ReceiveBufferBean implements Serializable {

  private static final long serialVersionUID = -5898410808935214134L;

  private long receiveID;
  private String phone;
  private String destinationNumber;
  private String messageID;
  private String message;
  private String senderID;
  private boolean simulation;
  private String provider;
  private boolean asBroadcastMessage;
  private String replyMessage;
  private int priority;
  private int channelSessionId;

  public ReceiveBufferBean() {
    receiveID = 0;
    phone = null;
    destinationNumber = null;
    messageID = null;
    message = null;
    senderID = null;
    simulation = false;
    provider = "MODEM";
    asBroadcastMessage = false;
    replyMessage = null;
    priority = 0;
    channelSessionId = 0;
  }

  public long getReceiveID() {
    return receiveID;
  }

  public void setReceiveID( long receiveID ) {
    this.receiveID = receiveID;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone( String phone ) {
    this.phone = phone;
  }

  public String getDestinationNumber() {
    return destinationNumber;
  }

  public void setDestinationNumber( String destinationNumber ) {
    this.destinationNumber = destinationNumber;
  }

  public String getMessageID() {
    return messageID;
  }

  public void setMessageID( String messageID ) {
    this.messageID = messageID;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getSenderID() {
    return senderID;
  }

  public void setSenderID( String senderID ) {
    this.senderID = senderID;
  }

  public boolean isSimulation() {
    return simulation;
  }

  public void setSimulation( boolean simulation ) {
    this.simulation = simulation;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider( String provider ) {
    this.provider = provider;
  }

  public boolean isAsBroadcastMessage() {
    return asBroadcastMessage;
  }

  public void setAsBroadcastMessage( boolean asBroadcastMessage ) {
    this.asBroadcastMessage = asBroadcastMessage;
  }

  public String getReplyMessage() {
    return replyMessage;
  }

  public void setReplyMessage( String replyMessage ) {
    this.replyMessage = replyMessage;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority( int priority ) {
    this.priority = priority;
  }

  public int getChannelSessionId() {
    return channelSessionId;
  }

  public void setChannelSessionId( int channelSessionId ) {
    this.channelSessionId = channelSessionId;
  }

  /*****************************************************************************
   * Insert new receive buffer record.
   * <p>
   * 
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void insert() throws IOException {
    new ReceiveBufferDAO().insert( this );
  }

  /*****************************************************************************
   * Select receive buffer record.
   * <p>
   * 
   * @param phone
   * @return ReceiveBufferBean
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public ReceiveBufferBean select( String phone ) throws IOException {
    return new ReceiveBufferDAO().select( phone );
  }

  /*****************************************************************************
   * Select all receive buffer records.
   * <p>
   * 
   * @return Array of ReceiveBufferBean, or null if none found
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public ReceiveBufferBean[] select() throws IOException {
    return new ReceiveBufferDAO().select();
  }

  /*****************************************************************************
   * Delete receive buffer record.
   * <p>
   * 
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void delete() throws IOException {
    new ReceiveBufferDAO().delete( this );
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "ReceiveBufferBean ( " + "receiveID = " + this.receiveID + TAB
        + "phone = " + this.phone + TAB + "destinationNumber = "
        + this.destinationNumber + TAB + "messageID = " + this.messageID + TAB
        + "message = " + this.message + TAB + "senderID = " + this.senderID
        + TAB + "simulation = " + this.simulation + TAB + "provider = "
        + this.provider + TAB + "asBroadcastMessage = "
        + this.asBroadcastMessage + TAB + "replyMessage = " + this.replyMessage
        + TAB + "priority = " + this.priority + TAB + "channelSessionId = "
        + this.channelSessionId + TAB + " )";
    return retValue;
  }

}
