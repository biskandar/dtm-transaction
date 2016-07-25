package com.beepcast.model.transaction;

import java.io.Serializable;

/*******************************************************************************
 * Http Request Bean.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class HttpRequestBean implements Serializable {

  private String host = "localhost";
  private String phone = "";
  private String message = "";
  private String simulation = "false";
  private String messageType = "";
  private String _provider = "";
  private String command = "";
  private String senderID = "";// dev0
  private String str = "";// dev0--1903

  /*****************************************************************************
   * No-args constructor.
   ****************************************************************************/
  public HttpRequestBean() {
  }

  /*****************************************************************************
   * Set host.
   ****************************************************************************/
  public void setHost( String host ) {
    this.host = host;
  }

  /**
   * Get host.
   */
  public String getHost() {
    return host;
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
   * Set simulation.
   ****************************************************************************/
  public void setSimulation( String simulation ) {
    this.simulation = simulation;
  }

  /**
   * Get simulation.
   */
  public String getSimulation() {
    return simulation;
  }

  /*****************************************************************************
   * Set message type.
   ****************************************************************************/
  public void setMessageType( String messageType ) {
    this.messageType = messageType;
  }

  /**
   * Get message type.
   */
  public String getMessageType() {
    return messageType;
  }

  /*****************************************************************************
   * Set provider.
   ****************************************************************************/
  public void setProvider( String _provider ) {
    this._provider = _provider;
  }

  /**
   * Get provider.
   */
  public String getProvider() {
    return _provider;
  }

  /*****************************************************************************
   * Set command.
   ****************************************************************************/
  public void setCommand( String command ) {
    this.command = command;
  }

  /**
   * Get command.
   */
  public String getCommand() {
    return command;
  }

  public String getSenderID() {// Dev0
    return senderID;
  }

  public void setSenderID( String senderID ) {
    this.senderID = senderID;
  }

  public String getStr() {
    return str;
  }

  public void setStr( String str ) {
    this.str = str;
  }

} // eof
