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

import java.util.Date;

public class DescribeAlarmHistoryType extends CloudWatchMessage {

  private String alarmName;
  private String historyItemType;
  private Date startDate;
  private Date endDate;
  private Integer maxRecords;
  private String nextToken;

  public String getAlarmName( ) {
    return alarmName;
  }

  public void setAlarmName( String alarmName ) {
    this.alarmName = alarmName;
  }

  public String getHistoryItemType( ) {
    return historyItemType;
  }

  public void setHistoryItemType( String historyItemType ) {
    this.historyItemType = historyItemType;
  }

  public Date getStartDate( ) {
    return startDate;
  }

  public void setStartDate( Date startDate ) {
    this.startDate = startDate;
  }

  public Date getEndDate( ) {
    return endDate;
  }

  public void setEndDate( Date endDate ) {
    this.endDate = endDate;
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
