package com.beepcast.model.transaction.script;

import java.util.HashMap;
import java.util.Map;

public class ScriptWebhookData implements Cloneable {

  private String method;
  private String uri;
  private Map parameters;

  public ScriptWebhookData() {
    parameters = new HashMap();
  }

  public String getMethod() {
    return method;
  }

  public void setMethod( String method ) {
    this.method = method;
  }

  public String getUri() {
    return uri;
  }

  public void setUri( String uri ) {
    this.uri = uri;
  }

  public Map getParameters() {
    return parameters;
  }

  public void addParameter( String field , String value ) {
    parameters.put( field , value );
  }

  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
