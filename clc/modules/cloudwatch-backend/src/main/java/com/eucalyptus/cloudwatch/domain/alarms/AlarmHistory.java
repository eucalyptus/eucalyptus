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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="alarm_history")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AlarmHistory extends AbstractPersistent {

  @Override
  public String toString() {
    return "AlarmHistory [accountId=" + accountId + ", alarmName=" + alarmName
        + ", historyData=" + historyData + ", historyItemType="
        + historyItemType + ", historySummary=" + historySummary
        + ", timestamp=" + timestamp + "]";
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAlarmName() {
    return alarmName;
  }

  public void setAlarmName(String alarmName) {
    this.alarmName = alarmName;
  }

  public String getHistoryData() {
    return historyData;
  }

  public void setHistoryData(String historyData) {
    this.historyData = historyData;
  }

  public HistoryItemType getHistoryItemType() {
    return historyItemType;
  }

  public void setHistoryItemType(HistoryItemType historyItemType) {
    this.historyItemType = historyItemType;
  }

  public String getHistorySummary() {
    return historySummary;
  }

  public void setHistorySummary(String historySummary) {
    this.historySummary = historySummary;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public static Logger getLog() {
    return LOG;
  }

  private static final Logger LOG = Logger.getLogger(AlarmHistory.class);
  public AlarmHistory() {
    super();
  }

  @Column( name = "account_id" , nullable = false)
  private String accountId;
  @Column( name = "alarm_name" , nullable = false)
  private String alarmName; 
  @Column( name = "history_data", length = 4095 )
  private String historyData;

  @Column(name = "history_item_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private HistoryItemType historyItemType;

  @Column( name = "history_summary" , nullable = false)
  private String historySummary;
  @Column( name = "timestamp" , nullable = false)
  private Date timestamp;
  
  public enum HistoryItemType {
    ConfigurationUpdate, StateUpdate, Action
  }
}
