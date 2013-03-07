package com.eucalyptus.cloudwatch.domain.alarms;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.Type;

import com.eucalyptus.cloudwatch.domain.dimension.AbstractPersistentWithDimensions;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.entities.AbstractPersistent;

@Entity @javax.persistence.Entity
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
  @Column( name = "history_data" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
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