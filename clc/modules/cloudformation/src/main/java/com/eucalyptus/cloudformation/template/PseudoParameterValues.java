/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudformation.template;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

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
