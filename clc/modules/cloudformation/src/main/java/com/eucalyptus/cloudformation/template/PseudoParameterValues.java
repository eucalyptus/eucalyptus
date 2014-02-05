package com.eucalyptus.cloudformation.template;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
* Created by ethomas on 2/4/14.
*/
public class PseudoParameterValues {
  String accountId;
  List<String> notificationArns;
  String region;
  String stackName;
  String stackId;
  Map<String, List<String>> availabilityZones = Maps.newHashMap();

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public List<String> getNotificationArns() {
    return notificationArns;
  }

  public void setNotificationArns(List<String> notificationArns) {
    this.notificationArns = notificationArns;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public Map<String, List<String>> getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(Map<String, List<String>> availabilityZones) {
    this.availabilityZones = availabilityZones;
  }
}
