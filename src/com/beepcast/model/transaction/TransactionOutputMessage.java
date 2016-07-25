package com.beepcast.model.transaction;

public class TransactionOutputMessage extends TransactionMessage {

  private String correlationId;

  private String messageProfile;

  private int processCode;

  private String messageStatusCode;
  private String messageStatusDescription;

  public TransactionOutputMessage() {
    super();
    processCode = ProcessCode.PROCESS_SUCCEED;
    messageProfile = MessageProfile.PROFILE_NORMAL_MSG;
    messageStatusCode = MessageStatusCode.SC_OK;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId( String correlationId ) {
    this.correlationId = correlationId;
  }

  public String getMessageProfile() {
    return messageProfile;
  }

  public void setMessageProfile( String messageProfile ) {
    this.messageProfile = messageProfile;
  }

  public int getProcessCode() {
    return processCode;
  }

  public void setProcessCode( int processCode ) {
    this.processCode = processCode;
  }

  public String getMessageStatusCode() {
    return messageStatusCode;
  }

  public void setMessageStatusCode( String messageStatusCode ) {
    this.messageStatusCode = messageStatusCode;
  }

  public String getMessageStatusDescription() {
    return messageStatusDescription;
  }

  public void setMessageStatusDescription( String messageStatusDescription ) {
    this.messageStatusDescription = messageStatusDescription;
  }

  public boolean isHasExpectInput() {
    boolean result = false;
    String value = (String) getMessageParam( TransactionMessageParam.HDR_HAS_EXPECT_INPUT );
    if ( value == null ) {
      return result;
    }
    result = value.equalsIgnoreCase( "true" );
    return result;
  }

  public boolean isHasEventMenuType() {
    boolean result = false;
    String value = (String) getMessageParam( TransactionMessageParam.HDR_HAS_EVENT_MENU_TYPE );
    if ( value == null ) {
      return result;
    }
    result = value.equalsIgnoreCase( "true" );
    return result;
  }

}
