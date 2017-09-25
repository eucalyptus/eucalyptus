/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudwatch.common.msgs;

public class SetAlarmStateType extends CloudWatchMessage {

  private String alarmName;
  private String stateValue;
  private String stateReason;
  private String stateReasonData;

  public String getAlarmName( ) {
    return alarmName;
  }

  public void setAlarmName( String alarmName ) {
    this.alarmName = alarmName;
  }

  public String getStateValue( ) {
    return stateValue;
  }

  public void setStateValue( String stateValue ) {
    this.stateValue = stateValue;
  }

  public String getStateReason( ) {
    return stateReason;
  }

  public void setStateReason( String stateReason ) {
    this.stateReason = stateReason;
  }

  public String getStateReasonData( ) {
    return stateReasonData;
  }

  public void setStateReasonData( String stateReasonData ) {
    this.stateReasonData = stateReasonData;
  }
}
