package com.eucalyptus.cloudwatch.domain.alarms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.ExecutePolicyResponseType;
import com.eucalyptus.autoscaling.common.ExecutePolicyType;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory.HistoryItemType;
import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
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

  static void changeAlarmState(AlarmEntity alarmEntity, AlarmState newState, Date now) {
    LOG.info("Updating alarm " + alarmEntity.getAlarmName() + " from " + alarmEntity.getStateValue() + " to " + newState.getStateValue());
    alarmEntity.setStateUpdatedTimestamp(now);
    String historyData = "JSON DATA (TODO)"; // TODO:
    AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
        HistoryItemType.StateUpdate, " Alarm updated from " + alarmEntity.getStateValue() + " to " + newState.getStateValue(), now);
    alarmEntity.setStateReason(newState.getStateReason());
    alarmEntity.setStateReasonData(newState.getStateReasonData());
    alarmEntity.setStateValue(newState.getStateValue());
    alarmEntity.setStateUpdatedTimestamp(now);
  }


  static void executeActions(AlarmEntity alarmEntity, AlarmState state,
      boolean stateJustChanged, Date now) {
    if (alarmEntity.getActionsEnabled()) {
      Collection<String> actions = AlarmUtils.getActionsByState(alarmEntity, state);
      for (String action: actions) {
        Action actionToExecute = ActionManager.getAction(action, alarmEntity);
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

  private static String creatStateReasonData(StateValue stateValue,
      List<Double> dataPoints,
      ComparisonOperator comparisonOperator, Double threshold, String stateReason) {
    String stateReasonData = "JSON (TODO)";
    return stateReasonData;
  }
  
  private static String createStateReason(StateValue stateValue, List<Double> dataPoints,
      ComparisonOperator comparisonOperator, Double threshold) {
    String stateReason = null;
    if (stateValue == StateValue.INSUFFICIENT_DATA) {
      stateReason = "Insufficient Data: " + dataPoints.size() +
          AlarmUtils.matchSingularPlural(dataPoints.size(), " datapoint was ", " datapoints were ") +
          "unknown.";
    } else {
      stateReason = "Threshold Crossed: " + dataPoints.size() + 
          AlarmUtils.matchSingularPlural(dataPoints.size(), " datapoint ", " datapoints ") +
          AlarmUtils.makeDoubleList(dataPoints) + 
          AlarmUtils.matchSingularPlural(dataPoints.size(), " was ", " were ") +
          (stateValue == StateValue.OK ? " not " : "") + 
          AlarmUtils.comparisonOperatorString(comparisonOperator) + 
          " the threshold (" + threshold + ").";
    }
    return stateReason;
  }

  static AlarmState createAlarmState(StateValue stateValue,
      List<Double> dataPoints,
      ComparisonOperator comparisonOperator, Double threshold) {
    String stateReason = createStateReason(stateValue, dataPoints, comparisonOperator, threshold);
    return createAlarmState(stateValue, dataPoints, comparisonOperator, threshold, stateReason);
  }
  
  static AlarmState createAlarmState(StateValue stateValue,
      List<Double> dataPoints,
      ComparisonOperator comparisonOperator, Double threshold, String stateReason) {
    String stateReasonData = creatStateReasonData(stateValue, dataPoints, comparisonOperator, threshold, stateReason);
    return new AlarmState(stateValue, stateReason, stateReasonData);
  }

  private static AlarmState createAlarmState(StateValue stateValue,
      String stateReason, String stateReasonData) {
    return new AlarmState(stateValue, stateReason, stateReasonData);
  }
  
  private static abstract class Action {
    public abstract boolean filter(String actionURN, AlarmEntity entity);
    public abstract void executeAction(String actionARN, AlarmEntity alarmEntity, Date now);
    public abstract boolean alwaysExecute();
  }
  private static class ExecuteAutoScalingPolicyAction extends Action {
    @Override
    public boolean filter(String action, AlarmEntity entity) {
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
        String historyData = "JSON DATA (TODO)"; // TODO:
        AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
            HistoryItemType.Action, " Successfully executed action " + action, now);
      } catch (Exception ex) {
        String historyData = "JSON DATA (TODO)"; // TODO:
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
    public boolean filter(String action, AlarmEntity entity) {
      if (action == null) return false;
      // Example:
      // arn:aws:automate:us-east-1:ec2:terminate
      if (!action.startsWith("arn:aws:automate:")) return false;
      if (!action.endsWith(":ec2:terminate")) return false;
      if (entity == null) return false;
      if (entity.getDimensionMap() == null) return false;
      return (entity.getDimensionMap().containsKey("InstanceId"));
    }

    @Override
    public void executeAction(String action, AlarmEntity alarmEntity, Date now) {
      String historyData = "JSON DATA (TODO)"; // TODO:
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
    public boolean filter(String action, AlarmEntity entity) {
      if (action == null) return false;
      // Example:
      // arn:aws:automate:us-east-1:ec2:stop
      if (!action.startsWith("arn:aws:automate:")) return false;
      if (!action.endsWith(":ec2:stop")) return false;
      if (entity == null) return false;
      if (entity.getDimensionMap() == null) return false;
      return (entity.getDimensionMap().containsKey("InstanceId"));
    }

    @Override
    public void executeAction(String action, AlarmEntity alarmEntity, Date now) {
      String historyData = "JSON DATA (TODO)"; // TODO:
      AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
          HistoryItemType.Action, " Successfully executed action " + action, now);
    }

    @Override
    public boolean alwaysExecute() {
      return false;
    }
  }

  private static class ActionManager {
    private static List<Action> actions = new ArrayList<Action>();
    static {
      actions.add(new StopInstanceAction());
      actions.add(new TerminateInstanceAction());
      actions.add(new ExecuteAutoScalingPolicyAction());
      
    }
    public static Action getAction(String action, AlarmEntity entity) {
      for (Action actionFromList :actions) {
        if (actionFromList.filter(action, entity)) {
          return actionFromList;
        } 
      }
      return null;
    }
  }

}

