package com.beepcast.model.transaction;

import java.util.Date;
import java.util.HashMap;

public class TransactionMessage {

  protected Date dateCreated;
  protected Date dateUpdated;

  protected String messageId;
  protected int messageType;
  protected int messageCount;
  protected String messageContent;

  protected String originalNode;
  protected String originalAddress;
  protected String originalMaskingAddress;
  protected String originalProvider;

  protected String destinationNode;
  protected String destinationAddress;
  protected String destinationMaskingAddress;
  protected String destinationProvider;

  protected int clientId;
  protected int eventId;
  protected int channelSessionId;

  protected double debitAmount;

  protected int priority;

  protected HashMap messageParams;

  public TransactionMessage() {
    super();
    dateCreated = new Date();
    dateUpdated = new Date();

    // default message type as sms plain text
    messageType = MessageType.TEXT_TYPE;

    // default no eventId , clientId and channelSessionId
    clientId = eventId = channelSessionId = 0;

    messageParams = new HashMap();
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public Date getDateUpdated() {
    return dateUpdated;
  }

  public void setDateUpdated( Date dateUpdated ) {
    this.dateUpdated = dateUpdated;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId( String messageId ) {
    this.messageId = messageId;
  }

  public int getMessageType() {
    return messageType;
  }

  public void setMessageType( int messageType ) {
    this.messageType = messageType;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public void setMessageCount( int messageCount ) {
    this.messageCount = messageCount;
  }

  public String getMessageContent() {
    return messageContent;
  }

  public void setMessageContent( String messageContent ) {
    this.messageContent = messageContent;
  }

  public String getOriginalNode() {
    return originalNode;
  }

  public void setOriginalNode( String originalNode ) {
    this.originalNode = originalNode;
  }

  public String getOriginalAddress() {
    return originalAddress;
  }

  public void setOriginalAddress( String originalAddress ) {
    this.originalAddress = originalAddress;
  }

  public String getOriginalMaskingAddress() {
    return originalMaskingAddress;
  }

  public void setOriginalMaskingAddress( String originalMaskingAddress ) {
    this.originalMaskingAddress = originalMaskingAddress;
  }

  public String getOriginalProvider() {
    return originalProvider;
  }

  public void setOriginalProvider( String originalProvider ) {
    this.originalProvider = originalProvider;
  }

  public String getDestinationNode() {
    return destinationNode;
  }

  public void setDestinationNode( String destinationNode ) {
    this.destinationNode = destinationNode;
  }

  public String getDestinationAddress() {
    return destinationAddress;
  }

  public void setDestinationAddress( String destinationAddress ) {
    this.destinationAddress = destinationAddress;
  }

  public String getDestinationMaskingAddress() {
    return destinationMaskingAddress;
  }

  public void setDestinationMaskingAddress( String destinationMaskingAddress ) {
    this.destinationMaskingAddress = destinationMaskingAddress;
  }

  public String getDestinationProvider() {
    return destinationProvider;
  }

  public void setDestinationProvider( String destinationProvider ) {
    this.destinationProvider = destinationProvider;
  }

  public int getClientId() {
    return clientId;
  }

  public void setClientId( int clientId ) {
    this.clientId = clientId;
  }

  public int getEventId() {
    return eventId;
  }

  public void setEventId( int eventId ) {
    this.eventId = eventId;
  }

  public int getChannelSessionId() {
    return channelSessionId;
  }

  public void setChannelSessionId( int channelSessionId ) {
    this.channelSessionId = channelSessionId;
  }

  public double getDebitAmount() {
    return debitAmount;
  }

  public void setDebitAmount( double debitAmount ) {
    this.debitAmount = debitAmount;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority( int priority ) {
    this.priority = priority;
  }

  public Object addMessageParam( String key , Object value ) {
    return messageParams.put( key , value );
  }

  public Object delMessageParam( String key ) {
    return messageParams.remove( key );
  }

  public Object getMessageParam( String key ) {
    return messageParams.get( key );
  }

  public HashMap getMessageParams() {
    return messageParams;
  }

  public String toString() {
    final String TAB = " ";
    String retValue = "";
    retValue = "TransactionMessage ( " + "dateCreated = " + this.dateCreated
        + TAB + "dateUpdated = " + this.dateUpdated + TAB + "messageId = "
        + this.messageId + TAB + "messageType = " + this.messageType + TAB
        + "messageContent = " + this.messageContent + TAB + "originalNode = "
        + this.originalNode + TAB + "originalAddress = " + this.originalAddress
        + TAB + "originalMaskingAddress = " + this.originalMaskingAddress + TAB
        + "originalProvider = " + this.originalProvider + TAB
        + "destinationNode = " + this.destinationNode + TAB
        + "destinationAddress = " + this.destinationAddress + TAB
        + "destinationMaskingAddress = " + this.destinationMaskingAddress + TAB
        + "destinationProvider = " + this.destinationProvider + TAB
        + "clientId = " + this.clientId + TAB + "eventId = " + this.eventId
        + TAB + "channelSessionId = " + this.channelSessionId + TAB
        + "debitAmount = " + this.debitAmount + TAB + "priority = "
        + this.priority + TAB + "messageParams = " + this.messageParams + TAB
        + " )";
    return retValue;
  }

}
