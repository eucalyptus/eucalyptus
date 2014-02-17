/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
