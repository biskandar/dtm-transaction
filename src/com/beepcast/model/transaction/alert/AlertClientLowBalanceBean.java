package com.beepcast.model.transaction.alert;

public class AlertClientLowBalanceBean {

  private String alertId;
  private double thresholdUnit;

  public AlertClientLowBalanceBean() {

  }

  public String getAlertId() {
    return alertId;
  }

  public void setAlertId( String alertId ) {
    this.alertId = alertId;
  }

  public double getThresholdUnit() {
    return thresholdUnit;
  }

  public void setThresholdUnit( double thresholdUnit ) {
    this.thresholdUnit = thresholdUnit;
  }

}
