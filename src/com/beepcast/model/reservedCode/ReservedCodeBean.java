package com.beepcast.model.reservedCode;

public class ReservedCodeBean {

  private int clientId;
  private String code;
  private String phone;
  private String params[];

  public ReservedCodeBean() {
    clientId = 1; // default to used beepcast company
  }

  public int getClientId() {
    return clientId;
  }

  public void setClientId( int clientId ) {
    this.clientId = clientId;
  }

  public void setCode( String code ) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public void setPhone( String phone ) {
    this.phone = phone;
  }

  public String getPhone() {
    return phone;
  }

  public void setParams( String[] params ) {
    this.params = params;
  }

  public String[] getParams() {
    return params;
  }

}
