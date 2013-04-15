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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.persistence.EntityTransaction;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.ExecutePolicyResponseType;
import com.eucalyptus.autoscaling.common.ExecutePolicyType;
import com.eucalyptus.cloudwatch.CloudWatchException;
import com.eucalyptus.cloudwatch.ResourceNotFoundException;
import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.NextTokenUtils;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory.HistoryItemType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.AsyncRequests;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AlarmManager {
  private static final Logger LOG = Logger.getLogger(AlarmManager.class);
  public static Long countMetricAlarms(String accountId) {
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class);
      criteria = criteria.setProjection(Projections.rowCount());
      if (accountId != null) {
        criteria = criteria.add( Restrictions.eq( "accountId" , accountId ) );
      }
      return (Long) criteria.uniqueResult();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      db.rollback();
    }
  }
  public static void putMetricAlarm(String accountId, Boolean actionsEnabled,
      Collection<String> alarmActions, String alarmDescription,
      String alarmName, ComparisonOperator comparisonOperator,
      Map<String, String> dimensionMap, Integer evaluationPeriods,
      Collection<String> insufficientDataActions, String metricName,
      MetricType metricType, String namespace, Collection<String> okActions,
      Integer period, Statistic statistic, Double threshold, Units unit) {

    if (dimensionMap == null) {
      dimensionMap = new HashMap<String, String>();
    } else if (dimensionMap.size() > AlarmEntity.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, " + dimensionMap.size());
    }

    AlarmEntity alarmEntity = new AlarmEntity();
    alarmEntity.setAccountId(accountId);
    alarmEntity.setAlarmName(alarmName);
    EntityTransaction db = Entities.get(AlarmEntity.class);
    boolean inDb = false;
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId" , accountId ) )
          .add( Restrictions.eq( "alarmName" , alarmName ) );
      AlarmEntity inDbAlarm = (AlarmEntity) criteria.uniqueResult();
      if (inDbAlarm != null) {
        inDb = true;
        alarmEntity = inDbAlarm;
      } 
      alarmEntity.setActionsEnabled(actionsEnabled);
      alarmEntity.setAlarmActions(alarmActions);
      alarmEntity.setAlarmDescription(alarmDescription);
      alarmEntity.setComparisonOperator(comparisonOperator);
      TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
      for (Map.Entry<String,String> entry: dimensionMap.entrySet()) {
        DimensionEntity d = new DimensionEntity();
        d.setName(entry.getKey());
        d.setValue(entry.getValue());
        dimensions.add(d);
      }
      alarmEntity.setDimensions(dimensions);
      alarmEntity.setEvaluationPeriods(evaluationPeriods);
      alarmEntity.setInsufficientDataActions(insufficientDataActions);
      alarmEntity.setMetricName(metricName);
      alarmEntity.setMetricType(metricType);
      alarmEntity.setNamespace(namespace);
      alarmEntity.setOkActions(okActions);
      alarmEntity.setPeriod(period);
      alarmEntity.setStatistic(statistic);
      alarmEntity.setThreshold(threshold);
      alarmEntity.setUnit(unit);
      Date now = new Date();
      alarmEntity.setAlarmConfigurationUpdatedTimestamp(now);
      if (!inDb) {
        alarmEntity.setStateValue(StateValue.INSUFFICIENT_DATA);
        alarmEntity.setStateReason("Unchecked: Initial alarm creation");
        alarmEntity.setStateUpdatedTimestamp(now);
        // TODO: revisit (we are not firing actions on alarm creation, but may after one period)
        alarmEntity.setLastActionsUpdatedTimestamp(now); 
        JSONObject historyDataJSON = new JSONObject();
        historyDataJSON.element("version", "1.0");
        historyDataJSON.element("type", "Create");
        JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity(alarmEntity);
        historyDataJSON.element("createdAlarm", historyDataDeletedAlarmJSON);
        String historyData = historyDataJSON.toString();
        AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
            HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" created", now);
        Entities.persist(alarmEntity);
      } else {
        JSONObject historyDataJSON = new JSONObject();
        historyDataJSON.element("version", "1.0");
        historyDataJSON.element("type", "Update");
        JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity(alarmEntity);
        historyDataJSON.element("updatedAlarm", historyDataDeletedAlarmJSON);
        String historyData = historyDataJSON.toString();
        AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
            HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" updated", now);
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  static void addAlarmHistoryItem(String accountId, String alarmName,
      String historyData, HistoryItemType historyItemType, String historySummary,
      Date now) {
    if (now == null) now = new Date();
    EntityTransaction db = Entities.get(AlarmHistory.class);
    try {
      AlarmHistory alarmHistory = new AlarmHistory();
      alarmHistory.setAccountId(accountId);
      alarmHistory.setAlarmName(alarmName);
      alarmHistory.setHistoryData(historyData);
      alarmHistory.setHistoryItemType(historyItemType);
      alarmHistory.setHistorySummary(historySummary);
      alarmHistory.setTimestamp(now);
      Entities.persist(alarmHistory);
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }
  private static SimpleDateFormat utcSimpleDateFormat;
  static {
    utcSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    utcSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
  
  public static void deleteAlarms(String accountId,
      Collection<String> alarmNames) {
    HashSet<String> alarmNamesHashSet = new HashSet<String>();
    Date now = new Date();
    if (alarmNames != null) {
      alarmNamesHashSet.addAll(alarmNames);
    }
    
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId" , accountId ) );
      criteria = criteria.addOrder( Order.asc("creationTimestamp") );
      criteria = criteria.addOrder( Order.asc("naturalId") );
      Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: alarmEntities) {
        final String alarmName = alarmEntity.getAlarmName(); 
        if (alarmNamesHashSet.contains(alarmEntity.getAlarmName())) {
          JSONObject historyDataJSON = new JSONObject();
          historyDataJSON.element("version", "1.0");
          historyDataJSON.element("type", "Delete");
          JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity(alarmEntity);
          historyDataJSON.element("deletedAlarm", historyDataDeletedAlarmJSON);
          String historyData = historyDataJSON.toString();
          AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
              HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" deleted", now);
          Entities.delete(alarmEntity);
        }
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  private static JSONObject getJSONObjectFromAlarmEntity(AlarmEntity alarmEntity) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.element("threshold", alarmEntity.getThreshold());
    jsonObject.element("namespace", alarmEntity.getNamespace());
    jsonObject.element("stateValue", alarmEntity.getStateValue().toString());
    ArrayList<JSONObject> dimensions = new ArrayList<JSONObject>();
    if (alarmEntity.getDimensions() != null) {
      for (DimensionEntity dimensionEntity: alarmEntity.getDimensions()) {
        JSONObject dimension = new JSONObject();
        dimension.element("name", dimensionEntity.getName());
        dimension.element("value", dimensionEntity.getValue());
        dimensions.add(dimension);
      }
    }
    jsonObject.element("dimensions", dimensions);
    jsonObject.element("okactions", alarmEntity.getOkActions() != null ? alarmEntity.getOkActions() : new ArrayList<String>());
    jsonObject.element("alarmActions", alarmEntity.getAlarmActions() != null ? alarmEntity.getAlarmActions() : new ArrayList<String>());
    jsonObject.element("evaluationPeriods", alarmEntity.getEvaluationPeriods());
    jsonObject.element("comparisonOperator", alarmEntity.getComparisonOperator().toString());
    jsonObject.element("metricName", alarmEntity.getMetricName());
    jsonObject.element("period", alarmEntity.getPeriod());
    jsonObject.element("alarmName", alarmEntity.getAlarmName());
    jsonObject.element("insufficientDataActions", alarmEntity.getInsufficientDataActions() != null ? alarmEntity.getInsufficientDataActions() : new ArrayList<String>());
    jsonObject.element("actionsEnabled", alarmEntity.getActionsEnabled());
    jsonObject.element("alarmDescription", alarmEntity.getAlarmDescription());
    jsonObject.element("statistic", alarmEntity.getStatistic());
    jsonObject.element("alarmArn", alarmEntity.getResourceName());
    jsonObject.element("alarmConfigurationUpdatedTimestamp", utcSimpleDateFormat.format(alarmEntity.getAlarmConfigurationUpdatedTimestamp()));
    jsonObject.element("stateUpdatedTimestamp", utcSimpleDateFormat.format(alarmEntity.getStateUpdatedTimestamp()));
    return jsonObject;
  }

  public static void enableAlarmActions(String accountId,
      Collection<String> alarmNames) {
    HashSet<String> alarmNamesHashSet = new HashSet<String>();
    Date now = new Date();
    if (alarmNames != null) {
      alarmNamesHashSet.addAll(alarmNames);
    }
    
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId" , accountId ) );
      criteria = criteria.addOrder( Order.asc("creationTimestamp") );
      criteria = criteria.addOrder( Order.asc("naturalId") );
      Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: alarmEntities) {
        final String alarmName = alarmEntity.getAlarmName(); 
        if (alarmNamesHashSet.contains(alarmEntity.getAlarmName()) && !Boolean.TRUE.equals(alarmEntity.getActionsEnabled())) {
          JSONObject historyDataJSON = new JSONObject();
          historyDataJSON.element("version", "1.0");
          historyDataJSON.element("type", "Update");
          JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity(alarmEntity);
          historyDataJSON.element("updatedAlarm", historyDataDeletedAlarmJSON);
          String historyData = historyDataJSON.toString();
          AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
              HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" updated", now);
          alarmEntity.setActionsEnabled(Boolean.TRUE);
        }
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  public static void disableAlarmActions(String accountId,
      Collection<String> alarmNames) {
    HashSet<String> alarmNamesHashSet = new HashSet<String>();
    Date now = new Date();
    if (alarmNames != null) {
      alarmNamesHashSet.addAll(alarmNames);
    }
    
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId" , accountId ) );
      criteria = criteria.addOrder( Order.asc("creationTimestamp") );
      criteria = criteria.addOrder( Order.asc("naturalId") );
      Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: alarmEntities) {
        final String alarmName = alarmEntity.getAlarmName(); 
        if (alarmNamesHashSet.contains(alarmEntity.getAlarmName()) && !Boolean.FALSE.equals(alarmEntity.getActionsEnabled())) {
          JSONObject historyDataJSON = new JSONObject();
          historyDataJSON.element("version", "1.0");
          historyDataJSON.element("type", "Update");
          JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity(alarmEntity);
          historyDataJSON.element("updatedAlarm", historyDataDeletedAlarmJSON);
          String historyData = historyDataJSON.toString();
          AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
              HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" updated", now);
          alarmEntity.setActionsEnabled(Boolean.FALSE);
        }
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  public static void setAlarmState(String accountId, String alarmName,
      String stateReason, String stateReasonData, StateValue stateValue) throws CloudWatchException {
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId" , accountId ) )
          .add( Restrictions.eq( "alarmName" , alarmName ) );
      AlarmEntity alarmEntity = (AlarmEntity) criteria.uniqueResult();
      if (alarmEntity == null) {
        throw new ResourceNotFoundException("Could not find alarm with name '" + alarmName + "'"); 
      }
      StateValue oldStateValue = alarmEntity.getStateValue();
      if (stateValue != oldStateValue) {
        Date evaluationDate = new Date();
        AlarmState newState = createAlarmState(stateValue, stateReason, stateReasonData);
        AlarmManager.changeAlarmState(alarmEntity, newState, evaluationDate);
        AlarmManager.executeActions(alarmEntity, newState, true, evaluationDate);
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  public static List<AlarmEntity> describeAlarms(String accountId,
      String actionPrefix, String alarmNamePrefix,
      Collection<String> alarmNames, Integer maxRecords, StateValue stateValue, String nextToken) throws CloudWatchException {
    List<AlarmEntity> results = null;
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Date nextTokenCreatedTime = NextTokenUtils.getNextTokenCreatedTime(nextToken, AlarmEntity.class, true);
      Criteria criteria = Entities.createCriteria(AlarmEntity.class);
      if (accountId != null) {
        criteria = criteria.add( Restrictions.eq( "accountId" , accountId ) );
      }
      if (actionPrefix != null) {
        final Junction actionsOf = Restrictions.disjunction();
        for (int i=1; i<= AlarmEntity.MAX_OK_ACTIONS_NUM; i++) {
          actionsOf.add( Restrictions.like( "okAction" + i, actionPrefix + "%" ) ); // May need Restrictions.ilike for case insensitive
        }
        for (int i=1; i<= AlarmEntity.MAX_ALARM_ACTIONS_NUM; i++) {
          actionsOf.add( Restrictions.like( "alarmAction" + i, actionPrefix + "%" ) ); // May need Restrictions.ilike for case insensitive
        }
        for (int i=1; i<= AlarmEntity.MAX_INSUFFICIENT_DATA_ACTIONS_NUM; i++) {
          actionsOf.add( Restrictions.like( "insufficientDataAction" + i, actionPrefix + "%" ) ); // May need Restrictions.ilike for case insensitive
        }
        criteria = criteria.add( actionsOf );
      }
      if (alarmNamePrefix != null) {
        criteria = criteria.add( Restrictions.like( "alarmName" , alarmNamePrefix + "%" ) );
      }
      if (alarmNames != null && !alarmNames.isEmpty()) {
        final Junction alarmNamesOf = Restrictions.disjunction();
        for (String alarmName: alarmNames) {
          alarmNamesOf.add( Restrictions.eq( "alarmName" , alarmName) );
        }
        criteria = criteria.add(alarmNamesOf);
      }
      if (stateValue != null) {
        criteria = criteria.add( Restrictions.eq( "stateValue" , stateValue ) );
      }
      criteria = NextTokenUtils.addNextTokenConstraints(maxRecords, nextToken, nextTokenCreatedTime, criteria);
      results = (List<AlarmEntity>) criteria.list();
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
    return results;
  }


  public static Collection<AlarmEntity> describeAlarmsForMetric(
      String accountId, Map<String, String> dimensionMap, String metricName,
      String namespace, Integer period, Statistic statistic, Units unit) {
    List<AlarmEntity> results = null;
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class);
      if (accountId != null) {
        criteria = criteria.add( Restrictions.eq( "accountId" , accountId ) );
      }
      TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
      for (Map.Entry<String,String> entry: dimensionMap.entrySet()) {
        DimensionEntity d = new DimensionEntity();
        d.setName(entry.getKey());
        d.setValue(entry.getValue());
        dimensions.add(d);
      }
      int dimIndex = 1;
      for (DimensionEntity d: dimensions) {
        criteria.add( Restrictions.eq( "dim" + dimIndex + "Name", d.getName() ) );
        criteria.add( Restrictions.eq( "dim" + dimIndex + "Value", d.getValue() ) );
        dimIndex++;
      }
      while (dimIndex <= AlarmEntity.MAX_DIM_NUM) {
        criteria.add( Restrictions.isNull( "dim" + dimIndex + "Name") );
        criteria.add( Restrictions.isNull( "dim" + dimIndex + "Value") );
        dimIndex++;
      }

      if (metricName != null) {
        criteria = criteria.add( Restrictions.eq( "metricName" , metricName ) );
      }
      if (namespace != null) {
        criteria = criteria.add( Restrictions.eq( "namespace" , namespace ) );
      }
      if (period != null) {
        criteria = criteria.add( Restrictions.eq( "period" , period ) );
      }
      if (statistic != null) {
        criteria = criteria.add( Restrictions.eq( "statistic" , statistic ) );
      }
      if (unit != null) {
        criteria = criteria.add( Restrictions.eq( "unit" , unit ) );
      }
      
      results = (List<AlarmEntity>) criteria.list();
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
    return results;
  }

  public static List<AlarmHistory> describeAlarmHistory(String accountId,
      String alarmName, Date endDate, HistoryItemType historyItemType,
      Integer maxRecords, Date startDate, String nextToken) throws CloudWatchException {
    List<AlarmHistory> results = null;
    EntityTransaction db = Entities.get(AlarmHistory.class);
    try {
      Date nextTokenCreatedTime = NextTokenUtils.getNextTokenCreatedTime(nextToken, AlarmEntity.class, true);
      Criteria criteria = Entities.createCriteria(AlarmHistory.class);
      if (accountId != null) {
        criteria = criteria.add( Restrictions.eq( "accountId" , accountId ) );
      }
      if (alarmName != null) {
        criteria = criteria.add( Restrictions.eq( "alarmName" , alarmName ) );
      }
      if (historyItemType != null) {
        criteria = criteria.add( Restrictions.eq( "historyItemType" , historyItemType ) );
      }
      if (startDate != null) {
        criteria = criteria.add( Restrictions.ge( "timestamp" , startDate ) );
      }
      if (endDate != null) {
        criteria = criteria.add( Restrictions.le( "timestamp" , endDate ) );
      }
      
      criteria = NextTokenUtils.addNextTokenConstraints(maxRecords, nextToken, nextTokenCreatedTime, criteria);
      results = (List<AlarmHistory>) criteria.list();
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
    return results;
  }

  /**
   * Delete all alarm history before a certain date
   * @param before the date to delete before (inclusive)
   */
  public static void deleteAlarmHistory(Date before) {
    EntityTransaction db = Entities.get(AlarmHistory.class);
    try {
      Map<String, Date> criteria = new HashMap<String, Date>();
      criteria.put("before", before);
      Entities.deleteAllMatching(AlarmHistory.class, "WHERE timestamp < :before", criteria);
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  
  static void changeAlarmState(AlarmEntity alarmEntity, AlarmState newState, Date now) {
    LOG.info("Updating alarm " + alarmEntity.getAlarmName() + " from " + alarmEntity.getStateValue() + " to " + newState.getStateValue());
    alarmEntity.setStateUpdatedTimestamp(now);
    JSONObject historyDataJSON = new JSONObject();
    historyDataJSON.element("version", "1.0");
    historyDataJSON.element("oldState", getJSONObjectFromState(alarmEntity.getStateValue(), alarmEntity.getStateReason(), alarmEntity.getStateReasonData()));
    historyDataJSON.element("newState", getJSONObjectFromState(newState.getStateValue(), newState.getStateReason(), newState.getStateReasonData()));
    String historyData = historyDataJSON.toString();
    AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
        HistoryItemType.StateUpdate, " Alarm updated from " + alarmEntity.getStateValue() + " to " + newState.getStateValue(), now);
    alarmEntity.setStateReason(newState.getStateReason());
    alarmEntity.setStateReasonData(newState.getStateReasonData());
    alarmEntity.setStateValue(newState.getStateValue());
    alarmEntity.setStateUpdatedTimestamp(now);
  }


  private static JSONObject getJSONObjectFromState(StateValue stateValue,
      String stateReason, String stateReasonData) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.element("stateValue", stateValue.toString());
    jsonObject.element("stateReason", stateReason);
    if (stateReasonData != null) {
      jsonObject.element("stateReasonData", stateReasonData);
    }
    return jsonObject;
  }

  static void executeActions(AlarmEntity alarmEntity, AlarmState state,
      boolean stateJustChanged, Date now) {
    if (alarmEntity.getActionsEnabled()) {
      Collection<String> actions = AlarmUtils.getActionsByState(alarmEntity, state);
      for (String action: actions) {
        Action actionToExecute = ActionManager.getAction(action, alarmEntity.getDimensionMap());
        if (actionToExecute == null) {
          LOG.warn("Unsupported action " + action); // TODO: do not let it in to start with...
        } 
        // always execute autoscaling actions, but others only on state change...
        else if (actionToExecute.alwaysExecute() || stateJustChanged) {
          LOG.info("Executing alarm " + alarmEntity.getAlarmName() + " action " + action);
          actionToExecute.executeAction(action, alarmEntity, now);
        }
      }
    }
    alarmEntity.setLastActionsUpdatedTimestamp(now);
  }

  private static String createStateReasonData(StateValue stateValue,
      List<Double> relevantDataPoints, List<Double> recentDataPoints,
      ComparisonOperator comparisonOperator, Double threshold, String stateReason, Integer period, Date queryDate, Statistic statistic) {
    JSONObject stateReasonDataJSON = new JSONObject();
    stateReasonDataJSON.element("version", "1.0");
    stateReasonDataJSON.element("queryDate", utcSimpleDateFormat.format(queryDate));
    stateReasonDataJSON.element("statistic", statistic.toString());
    stateReasonDataJSON.element("recentDatapoints", pruneNullsAtBeginning(recentDataPoints));
    stateReasonDataJSON.element("period", period);
    stateReasonDataJSON.element("threshold", threshold);
    String stateReasonData = stateReasonDataJSON.toString();
    return stateReasonData;
  }
  
  private static List<Double> pruneNullsAtBeginning(List<Double> recentDataPoints) {
    ArrayList<Double> returnValue = new ArrayList<Double>();
    boolean foundNotNull = false;
    for (Double recentDataPoint: recentDataPoints) {
      if (recentDataPoint != null) {
        foundNotNull = true;
      }
      if (foundNotNull) {
        returnValue.add(recentDataPoint);
      }
    }
    return returnValue;
  }

  private static String createStateReason(StateValue stateValue, List<Double> relevantDataPoints,
      ComparisonOperator comparisonOperator, Double threshold) {
    String stateReason = null;
    if (stateValue == StateValue.INSUFFICIENT_DATA) {
      stateReason = "Insufficient Data: " + relevantDataPoints.size() +
          AlarmUtils.matchSingularPlural(relevantDataPoints.size(), " datapoint was ", " datapoints were ") +
          "unknown.";
    } else {
      stateReason = "Threshold Crossed: " + relevantDataPoints.size() + 
          AlarmUtils.matchSingularPlural(relevantDataPoints.size(), " datapoint ", " datapoints ") +
          AlarmUtils.makeDoubleList(relevantDataPoints) + 
          AlarmUtils.matchSingularPlural(relevantDataPoints.size(), " was ", " were ") +
          (stateValue == StateValue.OK ? " not " : "") + 
          AlarmUtils.comparisonOperatorString(comparisonOperator) + 
          " the threshold (" + threshold + ").";
    }
    return stateReason;
  }

  static AlarmState createAlarmState(StateValue stateValue,
      List<Double> relevantDataPoints, List<Double> recentDataPoints,
      ComparisonOperator comparisonOperator, Double threshold, Integer period, Date queryDate, Statistic statistic) {
    String stateReason = createStateReason(stateValue, relevantDataPoints, comparisonOperator, threshold);
    return createAlarmState(stateValue, relevantDataPoints, recentDataPoints, comparisonOperator, threshold, stateReason, period, queryDate, statistic);
  }
  
  static AlarmState createAlarmState(StateValue stateValue,
      List<Double> relevantDataPoints, List<Double> recentDataPoints,
      ComparisonOperator comparisonOperator, Double threshold, String stateReason, Integer period, Date queryDate, Statistic statistic) {
    String stateReasonData = createStateReasonData(stateValue, relevantDataPoints, recentDataPoints, comparisonOperator, threshold, stateReason, period, queryDate, statistic);
    return new AlarmState(stateValue, stateReason, stateReasonData);
  }

  private static AlarmState createAlarmState(StateValue stateValue,
      String stateReason, String stateReasonData) {
    return new AlarmState(stateValue, stateReason, stateReasonData);
  }
  
  private static abstract class Action {
    public abstract boolean filter(String actionURN, Map<String, String> dimensionMap);
    public abstract void executeAction(String actionARN, AlarmEntity alarmEntity, Date now);
    public abstract boolean alwaysExecute();
  }
  private static class ExecuteAutoScalingPolicyAction extends Action {
    @Override
    public boolean filter(String action, Map<String, String> dimensionMap) {
      return (action != null && action.startsWith("arn:aws:autoscaling:"));
    }

    @Override
    public void executeAction(String action, AlarmEntity alarmEntity, Date now) {
      try {
        ServiceConfiguration serviceConfiguration = ServiceConfigurations.createEphemeral(ComponentIds.lookup(AutoScaling.class));
        ExecutePolicyType executePolicyType = new ExecutePolicyType();
        executePolicyType.setPolicyName(action);
        executePolicyType.setHonorCooldown(true);
        Account account = Accounts.getAccountProvider().lookupAccountById(
              alarmEntity.getAccountId());
        User user = account.lookupUserByName(User.ACCOUNT_ADMIN);
        executePolicyType.setEffectiveUserId(user.getUserId());
        BaseMessage reply = AsyncRequests.dispatch(serviceConfiguration, executePolicyType).get();
        if (!(reply instanceof ExecutePolicyResponseType)) {
          throw new Exception(reply.getStatusMessage()); // TODO: create an exception
        }
        JSONObject historyDataJSON = new JSONObject();
        historyDataJSON.element("actionState", "Succeeded");
        historyDataJSON.element("notificationResource", action);
        historyDataJSON.element("stateUpdateTimestamp", utcSimpleDateFormat.format(alarmEntity.getStateUpdatedTimestamp()));
        String historyData = historyDataJSON.toString();
        AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
            HistoryItemType.Action, " Successfully executed action " + action, now);
      } catch (Exception ex) {
        JSONObject historyDataJSON = new JSONObject();
        historyDataJSON.element("actionState", "Failed");
        historyDataJSON.element("notificationResource", action);
        historyDataJSON.element("stateUpdateTimestamp", utcSimpleDateFormat.format(alarmEntity.getStateUpdatedTimestamp()));
        historyDataJSON.element("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
        String historyData = historyDataJSON.toString();
        AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
            HistoryItemType.Action, " Failed to execute action " + action, now);
      }
    }

    @Override
    public boolean alwaysExecute() {
      return true;
    }

  }

  private static class TerminateInstanceAction extends Action {

    @Override
    public boolean filter(String action, Map<String, String> dimensionMap) {
      if (action == null) return false;
      // Example:
      // arn:aws:automate:us-east-1:ec2:terminate
      if (!action.startsWith("arn:aws:automate:")) return false;
      if (!action.endsWith(":ec2:terminate")) return false;
      if (dimensionMap == null) return false;
      return (dimensionMap.containsKey("InstanceId"));
    }

    @Override
    public void executeAction(String action, AlarmEntity alarmEntity, Date now) {
      JSONObject historyDataJSON = new JSONObject();
      historyDataJSON.element("actionState", "Succeeded");
      historyDataJSON.element("notificationResource", action);
      historyDataJSON.element("stateUpdateTimestamp", utcSimpleDateFormat.format(alarmEntity.getStateUpdatedTimestamp()));
      String historyData = historyDataJSON.toString();
      AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
          HistoryItemType.Action, " Successfully executed action " + action, now);
    }

    @Override
    public boolean alwaysExecute() {
      return false;
    }
  }
  private static class StopInstanceAction extends Action {

    @Override
    public boolean filter(String action, Map<String, String> dimensionMap) {
      if (action == null) return false;
      // Example:
      // arn:aws:automate:us-east-1:ec2:stop
      if (!action.startsWith("arn:aws:automate:")) return false;
      if (!action.endsWith(":ec2:stop")) return false;
      if (dimensionMap == null) return false;
      return (dimensionMap.containsKey("InstanceId"));
    }

    @Override
    public void executeAction(String action, AlarmEntity alarmEntity, Date now) {
      JSONObject historyDataJSON = new JSONObject();
      historyDataJSON.element("actionState", "Succeeded");
      historyDataJSON.element("notificationResource", action);
      historyDataJSON.element("stateUpdateTimestamp", utcSimpleDateFormat.format(alarmEntity.getStateUpdatedTimestamp()));
      String historyData = historyDataJSON.toString();
      AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
          HistoryItemType.Action, " Successfully executed action " + action, now);
    }

    @Override
    public boolean alwaysExecute() {
      return false;
    }
  }

  public static class ActionManager {
    private static List<Action> actions = new ArrayList<Action>();
    static {
      actions.add(new StopInstanceAction());
      actions.add(new TerminateInstanceAction());
      actions.add(new ExecuteAutoScalingPolicyAction());
      
    }
    public static Action getAction(String action, Map<String, String> dimensionMap) {
      for (Action actionFromList :actions) {
        if (actionFromList.filter(action, dimensionMap)) {
          return actionFromList;
        } 
      }
      return null;
    }
  }

}

