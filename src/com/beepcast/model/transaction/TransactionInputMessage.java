package com.beepcast.model.transaction;

public class TransactionInputMessage extends TransactionMessage {

  private boolean noProcessResponse;
  private String replyMessageContent;

  public TransactionInputMessage() {
    super();
  }

  public boolean isAsBroadcastMessage() {
    return ( super.channelSessionId > 0 );
  }

  public boolean isNoProcessResponse() {
    return noProcessResponse;
  }

  public void setNoProcessResponse( boolean noProcessResponse ) {
    this.noProcessResponse = noProcessResponse;
  }

  public String getReplyMessageContent() {
    return replyMessageContent;
  }

  public void setReplyMessageContent( String replyMessageContent ) {
    this.replyMessageContent = replyMessageContent;
  }

}
