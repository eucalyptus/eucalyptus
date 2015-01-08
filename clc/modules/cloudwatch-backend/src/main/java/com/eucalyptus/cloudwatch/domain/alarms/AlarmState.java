/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights
 *   Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/

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
