package com.eucalyptus.imaging;

import net.sf.json.JSONObject;

public interface IConversionTask {
  String getTaskExpirationTime();
  JSONObject toJSON();
  void setTaskState(String state);
  void setTaskStatusMessage(String msg);
  String getTaskState();
}
