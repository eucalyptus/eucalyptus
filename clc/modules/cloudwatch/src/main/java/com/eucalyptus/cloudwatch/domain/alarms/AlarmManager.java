package com.eucalyptus.cloudwatch.domain.alarms;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.persistence.EntityTransaction;

import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory.HistoryItemType;
import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class AlarmManager {

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
      alarmEntity.setComparisionOperator(comparisonOperator);
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
        String historyData = "JSON DATA (TODO)"; // TODO:
        AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
            HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" created", now);
        Entities.persist(alarmEntity);
      } else {
        String historyData = "JSON DATA (TODO)"; // TODO:
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

  private static void addAlarmHistoryItem(String accountId, String alarmName,
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
      Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: alarmEntities) {
        final String alarmName = alarmEntity.getAlarmName(); 
        if (alarmNamesHashSet.contains(alarmEntity.getAlarmName())) {
          String historyData = "JSON DATA (TODO)"; // TODO:
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
      Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: alarmEntities) {
        final String alarmName = alarmEntity.getAlarmName(); 
        if (alarmNamesHashSet.contains(alarmEntity.getAlarmName()) && !Boolean.TRUE.equals(alarmEntity.getActionsEnabled())) {
          String historyData = "JSON DATA (TODO)"; // TODO:
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
      Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: alarmEntities) {
        final String alarmName = alarmEntity.getAlarmName(); 
        if (alarmNamesHashSet.contains(alarmEntity.getAlarmName()) && !Boolean.FALSE.equals(alarmEntity.getActionsEnabled())) {
          String historyData = "JSON DATA (TODO)"; // TODO:
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
      String stateReason, String stateReasonData, StateValue stateValue) {
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId" , accountId ) )
          .add( Restrictions.eq( "alarmName" , alarmName ) );
      AlarmEntity alarmEntity = (AlarmEntity) criteria.uniqueResult();
      if (alarmEntity == null) {
        throw new RuntimeException("No such alarm " + alarmName); /// TODO: properly errorify
      }
      StateValue oldStateValue = alarmEntity.getStateValue();
      if (stateValue != oldStateValue) {
        alarmEntity.setStateReason(stateReason);
        alarmEntity.setStateReasonData(stateReasonData);
        alarmEntity.setStateValue(stateValue);
        Date now = new Date();
        alarmEntity.setStateUpdatedTimestamp(now);
        alarmEntity.setAlarmConfigurationUpdatedTimestamp(now);
        String historyData = "JSON DATA (TODO)"; // TODO:
        AlarmManager.addAlarmHistoryItem(accountId, alarmName, historyData, 
            HistoryItemType.StateUpdate, "Alarm updated from " + oldStateValue + " to " + stateValue, now);
        // TODO: act on these items...(maybe make a generic function to do so)
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

  public static Collection<AlarmEntity> describeAlarms(String accountId,
      String actionPrefix, String alarmNamePrefix,
      Collection<String> alarmNames, Integer maxRecords, StateValue stateValue) {
    List<AlarmEntity> results = null;
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
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
      results = (List<AlarmEntity>) criteria.list();
      if (results != null && maxRecords != null && results.size() > maxRecords) {
        results = results.subList(0, maxRecords);
      } 
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

  public static Collection<AlarmHistory> describeAlarmHistory(String accountId,
      String alarmName, Date endDate, HistoryItemType historyItemType,
      Integer maxRecords, Date startDate) {
    // TODO Auto-generated method stub
    List<AlarmHistory> results = null;
    EntityTransaction db = Entities.get(AlarmHistory.class);
    try {
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
      
      results = (List<AlarmHistory>) criteria.list();
      if (results != null && maxRecords != null && results.size() > maxRecords) {
        results = results.subList(0, maxRecords);
      } 
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
}
