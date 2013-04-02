package com.eucalyptus.cloudwatch.domain.alarms;

import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;

public class AlarmState {
  StateValue stateValue;
  String stateReason;
  String stateReasonData;

  public AlarmState(StateValue stateValue, String stateReason,
      String stateReasonData) {
    super();
    this.stateValue = stateValue;
    this.stateReason = stateReason;
    this.stateReasonData = stateReasonData;
  }
  public StateValue getStateValue() {
    return stateValue;
  }
  public void setStateValue(StateValue stateValue) {
    this.stateValue = stateValue;
  }
  public String getStateReason() {
    return stateReason;
  }
  public void setStateReason(String stateReason) {
    this.stateReason = stateReason;
  }
  public String getStateReasonData() {
    return stateReasonData;
  }
  public void setStateReasonData(String stateReasonData) {
    this.stateReasonData = stateReasonData;
  }


}
