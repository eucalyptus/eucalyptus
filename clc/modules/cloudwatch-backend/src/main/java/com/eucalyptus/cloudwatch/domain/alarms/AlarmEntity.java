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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.cloudwatch.common.CloudWatchMetadata;
import com.eucalyptus.cloudwatch.domain.AbstractPersistentWithDimensions;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.util.OwnerFullName;

@Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="alarms")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AlarmEntity extends AbstractPersistentWithDimensions implements CloudWatchMetadata.AlarmMetadata {

  private static final Logger LOG = Logger.getLogger(AlarmEntity.class);
  @Column(name = "account_id", nullable = false)
  private String accountId;

  @Column(name="actions_enabled", nullable = false)
  private Boolean actionsEnabled;

  @Column(name = "alarm_configuration_updated_timestamp", nullable = false)
  private Date alarmConfigurationUpdatedTimestamp;

  @Column(name="alarm_description")
  private String alarmDescription;

  public String getAccountId() {
    return accountId;
  }
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
  public Boolean getActionsEnabled() {
    return actionsEnabled;
  }
  public void setActionsEnabled(Boolean actionsEnabled) {
    this.actionsEnabled = actionsEnabled;
  }
  public Date getAlarmConfigurationUpdatedTimestamp() {
    return alarmConfigurationUpdatedTimestamp;
  }
  public void setAlarmConfigurationUpdatedTimestamp(
      Date alarmConfigurationUpdatedTimestamp) {
    this.alarmConfigurationUpdatedTimestamp = alarmConfigurationUpdatedTimestamp;
  }
  public String getAlarmDescription() {
    return alarmDescription;
  }
  public void setAlarmDescription(String alarmDescription) {
    this.alarmDescription = alarmDescription;
  }
  public String getAlarmName() {
    return alarmName;
  }
  public void setAlarmName(String alarmName) {
    this.alarmName = alarmName;
  }
  public ComparisonOperator getComparisonOperator() {
    return comparisonOperator;
  }
  public void setComparisonOperator(ComparisonOperator comparisonOperator) {
    this.comparisonOperator = comparisonOperator;
  }
  public Integer getEvaluationPeriods() {
    return evaluationPeriods;
  }
  public void setEvaluationPeriods(Integer evaluationPeriods) {
    this.evaluationPeriods = evaluationPeriods;
  }
  public String getMetricName() {
    return metricName;
  }
  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }
  public MetricType getMetricType() {
    return metricType;
  }
  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }
  public String getNamespace() {
    return namespace;
  }
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
  public Integer getPeriod() {
    return period;
  }
  public void setPeriod(Integer period) {
    this.period = period;
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
  public Date getStateUpdatedTimestamp() {
    return stateUpdatedTimestamp;
  }
  public void setStateUpdatedTimestamp(Date stateUpdatedTimestamp) {
    this.stateUpdatedTimestamp = stateUpdatedTimestamp;
  }
  public Date getLastActionsUpdatedTimestamp() {
    return lastActionsUpdatedTimestamp;
  }
  public void setLastActionsUpdatedTimestamp(Date lastActionsUpdatedTimestamp) {
    this.lastActionsUpdatedTimestamp = lastActionsUpdatedTimestamp;
  }
  public StateValue getStateValue() {
    return stateValue;
  }
  public void setStateValue(StateValue stateValue) {
    this.stateValue = stateValue;
  }
  public Statistic getStatistic() {
    return statistic;
  }
  public void setStatistic(Statistic statistic) {
    this.statistic = statistic;
  }
  public Units getUnit() {
    return unit;
  }
  public void setUnit(Units unit) {
    this.unit = unit;
  }
  public Double getThreshold() {
    return threshold;
  }
  public void setThreshold(Double threshold) {
    this.threshold = threshold;
  }
  public String getResourceName() {
    return String.format(
        "arn:aws:cloudwatch::%1s:alarm:%2s",
        getAccountId(),
        getAlarmName() );
  }

  public String getDisplayName() {
    return alarmName;
  }

  public OwnerFullName getOwner() {
    return AccountFullName.getInstance( accountId );
  }

  @Column(name="alarm_name", nullable = false)
  private String alarmName;

  @Column(name = "comparison_operator", nullable = false)
  @Enumerated(EnumType.STRING)
  private ComparisonOperator comparisonOperator;

  @Column(name = "evaluation_periods", nullable = false)
  private Integer evaluationPeriods;

  @Column(name = "metric_name", nullable = false)
  private String metricName;

  @Column(name = "metric_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MetricType metricType;

  @Column(name = "namespace", nullable = false)
  private String namespace;

  @Column(name = "period", nullable = false)
  private Integer period;

  @Column( name = "state_reason", length = 1023)
  private String stateReason;

  @Column( name = "state_reason_data", length =  4000)
  private String stateReasonData;
  
  @Column(name = "state_updated_timestamp", nullable = false)
  private Date stateUpdatedTimestamp;

  @Column(name = "last_actions_executed_timestamp", nullable = false)
  private Date lastActionsUpdatedTimestamp;

  @Column(name = "state_value", nullable = false)
  @Enumerated(EnumType.STRING)
  private StateValue stateValue;

  @Column(name = "statistic", nullable = false)
  @Enumerated(EnumType.STRING)
  private Statistic statistic;

  @Column(name = "unit", nullable = false)
  @Enumerated(EnumType.STRING)
  private Units unit;

  @Column(name = "threshold", nullable = false)
  private Double threshold;

  public enum ComparisonOperator {
    GreaterThanOrEqualToThreshold,
    GreaterThanThreshold,
    LessThanThreshold,
    LessThanOrEqualToThreshold
  }
  
  public enum Statistic {
    SampleCount,
    Average,
    Sum,
    Minimum,
    Maximum
  }

  public enum StateValue {
    OK,
    ALARM,
    INSUFFICIENT_DATA
  }

  // OK ACTIONS
  
  public static final int MAX_OK_ACTIONS_NUM = 5;

  public Collection<String> getOkActions() {
    ArrayList<String> okActions = new ArrayList<String>();
    for (int actionNum = 1; actionNum <= MAX_OK_ACTIONS_NUM; actionNum++) {
      String okAction = getOkAction(actionNum);
      if (okAction != null) {
        okActions.add(okAction);
      }
    }
    return okActions;
  }
  /**
   * Sets all ok actions.  This method copies the input parameter, do not lazily load it.
   * @param okActions
   */
  public void setOkActions(Collection<String> okActions) {
    if (okActions != null && okActions.size() > MAX_OK_ACTIONS_NUM) {
      throw new IllegalArgumentException("Too many actions, " + okActions.size());
    }
    for (int actionNum = 1; actionNum <= MAX_OK_ACTIONS_NUM; actionNum++) {
      setOkAction(actionNum, null);
    }
    if (okActions == null) {
      return;
    }
    Iterator<String> iter = okActions.iterator();
    String action = null;
    for (int actionNum = 1; actionNum <= MAX_OK_ACTIONS_NUM; actionNum++) {
      if (!iter.hasNext()) {
        return;
      }
      action = iter.next();
      setOkAction(actionNum, action);
    }
  }

  @Column( name = "ok_action_1" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String okAction1;

  @Column( name = "ok_action_2" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String okAction2;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "ok_action_3" )
  private String okAction3;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "ok_action_4" )
  private String okAction4;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "ok_action_5" )
  private String okAction5;

  public String getOkAction1() {
    return okAction1;
  }
  public void setOkAction1(String okAction1) {
    this.okAction1 = okAction1;
  }
  public String getOkAction2() {
    return okAction2;
  }
  public void setOkAction2(String okAction2) {
    this.okAction2 = okAction2;
  }
  public String getOkAction3() {
    return okAction3;
  }
  public void setOkAction3(String okAction3) {
    this.okAction3 = okAction3;
  }
  public String getOkAction4() {
    return okAction4;
  }
  public void setOkAction4(String okAction4) {
    this.okAction4 = okAction4;
  }
  public String getOkAction5() {
    return okAction5;
  }
  public void setOkAction5(String okAction5) {
    this.okAction5 = okAction5;
  }
  private void setOkAction(int actionNum, String value) {
    try {
      if ((actionNum < 1) || (actionNum > MAX_OK_ACTIONS_NUM)) {
        throw new IllegalArgumentException("No such method");
      }
      Method m = this.getClass().getMethod("setOkAction"+actionNum, String.class);
      m.invoke(this, value);
    } catch (Exception ex) {
      LOG.error("Unable to invoke setOkAction"+actionNum+", method may not exist");
      LOG.error(ex,ex);
    }
  }

  private String getOkAction(int actionNum) {
    try {
      if ((actionNum < 1) || (actionNum > MAX_OK_ACTIONS_NUM)) {
        throw new IllegalArgumentException("No such method");
      }
      Method m = this.getClass().getMethod("getOkAction"+actionNum);
      return (String) m.invoke(this);
    } catch (Exception ex) {
      LOG.error("Unable to invoke getOkAction" + actionNum + ", method may not exist");
      LOG.error(ex,ex);
      return null;
    }
  }
  
  // ALARM ACTIONS
  public static final int MAX_ALARM_ACTIONS_NUM = 5;

  public Collection<String> getAlarmActions() {
    ArrayList<String> alarmActions = new ArrayList<String>();
    for (int actionNum = 1; actionNum <= MAX_ALARM_ACTIONS_NUM; actionNum++) {
      String alarmAction = getAlarmAction(actionNum);
      if (alarmAction != null) {
        alarmActions.add(alarmAction);
      }
    }
    return alarmActions;
  }
  /**
   * Sets all alarm actions.  This method copies the input parameter, do not lazily load it.
   * @param alarmActions
   */
  public void setAlarmActions(Collection<String> alarmActions) {
    if (alarmActions != null && alarmActions.size() > MAX_ALARM_ACTIONS_NUM) {
      throw new IllegalArgumentException("Too many actions, " + alarmActions.size());
    }
    for (int actionNum = 1; actionNum <= MAX_ALARM_ACTIONS_NUM; actionNum++) {
      setAlarmAction(actionNum, null);
    }
    if (alarmActions == null) {
      return;
    }
    Iterator<String> iter = alarmActions.iterator();
    String action = null;
    for (int actionNum = 1; actionNum <= MAX_ALARM_ACTIONS_NUM; actionNum++) {
      if (!iter.hasNext()) {
        return;
      }
      action = iter.next();
      setAlarmAction(actionNum, action);
    }
  }

  @Column( name = "alarm_action_1" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String alarmAction1;

  @Column( name = "alarm_action_2" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String alarmAction2;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "alarm_action_3" )
  private String alarmAction3;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "alarm_action_4" )
  private String alarmAction4;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "alarm_action_5" )
  private String alarmAction5;

  public String getAlarmAction1() {
    return alarmAction1;
  }
  public void setAlarmAction1(String alarmAction1) {
    this.alarmAction1 = alarmAction1;
  }
  public String getAlarmAction2() {
    return alarmAction2;
  }
  public void setAlarmAction2(String alarmAction2) {
    this.alarmAction2 = alarmAction2;
  }
  public String getAlarmAction3() {
    return alarmAction3;
  }
  public void setAlarmAction3(String alarmAction3) {
    this.alarmAction3 = alarmAction3;
  }
  public String getAlarmAction4() {
    return alarmAction4;
  }
  public void setAlarmAction4(String alarmAction4) {
    this.alarmAction4 = alarmAction4;
  }
  public String getAlarmAction5() {
    return alarmAction5;
  }
  public void setAlarmAction5(String alarmAction5) {
    this.alarmAction5 = alarmAction5;
  }
  private void setAlarmAction(int actionNum, String value) {
    try {
      if ((actionNum < 1) || (actionNum > MAX_ALARM_ACTIONS_NUM)) {
        throw new IllegalArgumentException("No such method");
      }
      Method m = this.getClass().getMethod("setAlarmAction"+actionNum, String.class);
      m.invoke(this, value);
    } catch (Exception ex) {
      LOG.error("Unable to invoke setAlarmAction"+actionNum+", method may not exist");
      LOG.error(ex,ex);
    }
  }

  private String getAlarmAction(int actionNum) {
    try {
      if ((actionNum < 1) || (actionNum > MAX_ALARM_ACTIONS_NUM)) {
        throw new IllegalArgumentException("No such method");
      }
      Method m = this.getClass().getMethod("getAlarmAction"+actionNum);
      return (String) m.invoke(this);
    } catch (Exception ex) {
      LOG.error("Unable to invoke getAlarmAction" + actionNum + ", method may not exist");
      LOG.error(ex,ex);
      return null;
    }
  }

  // INSUFFICIENT_DATA ACTIONS
  public static final int MAX_INSUFFICIENT_DATA_ACTIONS_NUM = 5;

  public Collection<String> getInsufficientDataActions() {
    ArrayList<String> insufficientDataActions = new ArrayList<String>();
    for (int actionNum = 1; actionNum <= MAX_INSUFFICIENT_DATA_ACTIONS_NUM; actionNum++) {
      String insufficientDataAction = getInsufficientDataAction(actionNum);
      if (insufficientDataAction != null) {
        insufficientDataActions.add(insufficientDataAction);
      }
    }
    return insufficientDataActions;
  }
  /**
   * Sets all insufficientData actions.  This method copies the input parameter, do not lazily load it.
   * @param insufficientDataActions
   */
  public void setInsufficientDataActions(Collection<String> insufficientDataActions) {
    if (insufficientDataActions != null && insufficientDataActions.size() > MAX_INSUFFICIENT_DATA_ACTIONS_NUM) {
      throw new IllegalArgumentException("Too many actions, " + insufficientDataActions.size());
    }
    for (int actionNum = 1; actionNum <= MAX_INSUFFICIENT_DATA_ACTIONS_NUM; actionNum++) {
      setInsufficientDataAction(actionNum, null);
    }
    if (insufficientDataActions == null) {
      return;
    }
    Iterator<String> iter = insufficientDataActions.iterator();
    String action = null;
    for (int actionNum = 1; actionNum <= MAX_INSUFFICIENT_DATA_ACTIONS_NUM; actionNum++) {
      if (!iter.hasNext()) {
        return;
      }
      action = iter.next();
      setInsufficientDataAction(actionNum, action);
    }
  }

  @Column( name = "insufficient_data_action_1" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String insufficientDataAction1;

  @Column( name = "insufficient_data_action_2" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String insufficientDataAction2;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "insufficient_data_action_3" )
  private String insufficientDataAction3;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "insufficient_data_action_4" )
  private String insufficientDataAction4;

  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  @Column( name = "insufficient_data_action_5" )
  private String insufficientDataAction5;

  public String getInsufficientDataAction1() {
    return insufficientDataAction1;
  }
  public void setInsufficientDataAction1(String insufficientDataAction1) {
    this.insufficientDataAction1 = insufficientDataAction1;
  }
  public String getInsufficientDataAction2() {
    return insufficientDataAction2;
  }
  public void setInsufficientDataAction2(String insufficientDataAction2) {
    this.insufficientDataAction2 = insufficientDataAction2;
  }
  public String getInsufficientDataAction3() {
    return insufficientDataAction3;
  }
  public void setInsufficientDataAction3(String insufficientDataAction3) {
    this.insufficientDataAction3 = insufficientDataAction3;
  }
  public String getInsufficientDataAction4() {
    return insufficientDataAction4;
  }
  public void setInsufficientDataAction4(String insufficientDataAction4) {
    this.insufficientDataAction4 = insufficientDataAction4;
  }
  public String getInsufficientDataAction5() {
    return insufficientDataAction5;
  }
  public void setInsufficientDataAction5(String insufficientDataAction5) {
    this.insufficientDataAction5 = insufficientDataAction5;
  }
  private void setInsufficientDataAction(int actionNum, String value) {
    try {
      if ((actionNum < 1) || (actionNum > MAX_INSUFFICIENT_DATA_ACTIONS_NUM)) {
        throw new IllegalArgumentException("No such method");
      }
      Method m = this.getClass().getMethod("setInsufficientDataAction"+actionNum, String.class);
      m.invoke(this, value);
    } catch (Exception ex) {
      LOG.error("Unable to invoke setInsufficientDataAction"+actionNum+", method may not exist");
      LOG.error(ex,ex);
    }
  }

  private String getInsufficientDataAction(int actionNum) {
    try {
      if ((actionNum < 1) || (actionNum > MAX_INSUFFICIENT_DATA_ACTIONS_NUM)) {
        throw new IllegalArgumentException("No such method");
      }
      Method m = this.getClass().getMethod("getInsufficientDataAction"+actionNum);
      return (String) m.invoke(this);
    } catch (Exception ex) {
      LOG.error("Unable to invoke getInsufficientDataAction" + actionNum + ", method may not exist");
      LOG.error(ex,ex);
      return null;
    }
  }

}
