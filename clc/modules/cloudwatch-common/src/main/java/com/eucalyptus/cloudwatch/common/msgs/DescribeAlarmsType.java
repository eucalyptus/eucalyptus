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

import java.util.List;
import com.google.common.collect.Lists;

public class DescribeAlarmsType extends CloudWatchMessage {

  private AlarmNames alarmNames;
  private String alarmNamePrefix;
  private String stateValue;
  private String actionPrefix;
  private Integer maxRecords;
  private String nextToken;

  public List<String> getAlarms( ) {
    List<String> names = Lists.newArrayList( );
    if ( alarmNames != null ) {
      names = alarmNames.getMember( );
    }

    return names;
  }

  public AlarmNames getAlarmNames( ) {
    return alarmNames;
  }

  public void setAlarmNames( AlarmNames alarmNames ) {
    this.alarmNames = alarmNames;
  }

  public String getAlarmNamePrefix( ) {
    return alarmNamePrefix;
  }

  public void setAlarmNamePrefix( String alarmNamePrefix ) {
    this.alarmNamePrefix = alarmNamePrefix;
  }

  public String getStateValue( ) {
    return stateValue;
  }

  public void setStateValue( String stateValue ) {
    this.stateValue = stateValue;
  }

  public String getActionPrefix( ) {
    return actionPrefix;
  }

  public void setActionPrefix( String actionPrefix ) {
    this.actionPrefix = actionPrefix;
  }

  public Integer getMaxRecords( ) {
    return maxRecords;
  }

  public void setMaxRecords( Integer maxRecords ) {
    this.maxRecords = maxRecords;
  }

  public String getNextToken( ) {
    return nextToken;
  }

  public void setNextToken( String nextToken ) {
    this.nextToken = nextToken;
  }
}
