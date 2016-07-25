package com.beepcast.model.transaction.script;

import java.util.Map;

public class ScriptWebhookDataFactory {

  public static ScriptWebhookData createScriptWebhookData( String method ,
      String uri , Map parameters ) {
    ScriptWebhookData data = new ScriptWebhookData();

    data.setMethod( method );

    data.setUri( uri );

    if ( parameters != null ) {
      data.getParameters().putAll( parameters );
    }

    return data;
  }

}
