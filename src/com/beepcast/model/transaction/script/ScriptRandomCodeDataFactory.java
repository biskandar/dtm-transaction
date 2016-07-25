package com.beepcast.model.transaction.script;

public class ScriptRandomCodeDataFactory {

  public static ScriptRandomCodeData createScriptRandomCodeData( String format ,
      int length ) {
    ScriptRandomCodeData data = new ScriptRandomCodeData();
    data.setFormat( format );
    data.setLength( length );
    return data;
  }

}
