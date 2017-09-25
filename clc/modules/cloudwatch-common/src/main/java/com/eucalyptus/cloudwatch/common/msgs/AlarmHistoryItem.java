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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AlarmHistoryItem extends EucalyptusData {

  private String alarmName;
  private Date timestamp;
  private String historyItemType;
  private String historySummary;
  private String historyData;

  public String getAlarmName( ) {
    return alarmName;
  }

  public void setAlarmName( String alarmName ) {
    this.alarmName = alarmName;
  }

  public Date getTimestamp( ) {
    return timestamp;
  }

  public void setTimestamp( Date timestamp ) {
    this.timestamp = timestamp;
  }

  public String getHistoryItemType( ) {
    return historyItemType;
  }

  public void setHistoryItemType( String historyItemType ) {
    this.historyItemType = historyItemType;
  }

  public String getHistorySummary( ) {
    return historySummary;
  }

  public void setHistorySummary( String historySummary ) {
    this.historySummary = historySummary;
  }

  public String getHistoryData( ) {
    return historyData;
  }

  public void setHistoryData( String historyData ) {
    this.historyData = historyData;
  }
}
