package com.beepcast.model.transaction.script;

public class ScriptRandomCodeData implements Cloneable {

  public static final String FORMAT_NUMERIC = "Numeric";
  public static final String FORMAT_ALPHABETIC = "Alphabetic";
  public static final String FORMAT_ALPHANUMERIC = "Alphanumeric";

  private String format;
  private int length;

  public ScriptRandomCodeData() {

  }

  public String getFormat() {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public int getLength() {
    return length;
  }

  public void setLength( int length ) {
    this.length = length;
  }

}
