package com.beepcast.model.transaction;

import com.beepcast.oproperties.OnlinePropertiesApp;

public class PropertyProcessStep {

  private static OnlinePropertiesApp opropsApp = OnlinePropertiesApp
      .getInstance();

  public static String getValue( TransactionLog log , String headerLog ,
      TransactionConf conf , String processStepName , String processStepField ) {
    String value = null;

    if ( conf == null ) {
      return value;
    }

    if ( ( processStepName == null ) || ( processStepField == null ) ) {
      return value;
    }

    try {

      String valueFromConf = conf.getProcessStepValue( processStepName ,
          processStepField );

      String opropsFieldName = "Transaction.ProcessStep."
          .concat( processStepName ).concat( "." ).concat( processStepField );

      value = opropsApp.getString( opropsFieldName , valueFromConf );

      log.debug( headerLog + "Resolved property process step : "
          + opropsFieldName + " = " + valueFromConf + " -> " + value );

    } catch ( Exception e ) {

      log.warning( headerLog + "Failed to resolve property process step , " + e );

    }

    return value;
  }

}
