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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.msgs.ExecutePolicyType;
import com.eucalyptus.cloudwatch.backend.CloudWatchException;
import com.eucalyptus.cloudwatch.common.CloudWatchMetadata;
import com.eucalyptus.cloudwatch.common.CloudWatchResourceName;
import com.eucalyptus.cloudwatch.backend.ResourceNotFoundException;
import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.NextTokenUtils;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory.HistoryItemType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.StopInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

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
      dimensionMap = Maps.newHashMap();
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
      TreeSet<DimensionEntity> dimensions = Sets.newTreeSet();
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
  
  public static boolean deleteAlarms(
      final String accountId,
      final Collection<String> alarmNames,
      final Predicate<CloudWatchMetadata.AlarmMetadata> filter
  ) {
    return modifySelectedAlarms( accountId, alarmNames, filter, new Predicate<AlarmEntity>() {
      private final Date now = new Date();

      @Override
      public boolean apply( final AlarmEntity alarmEntity ) {
        final String alarmName = alarmEntity.getAlarmName();
        JSONObject historyDataJSON = new JSONObject();
        historyDataJSON.element( "version", "1.0" );
        historyDataJSON.element( "type", "Delete" );
        JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity( alarmEntity );
        historyDataJSON.element( "deletedAlarm", historyDataDeletedAlarmJSON );
        String historyData = historyDataJSON.toString();
        AlarmManager.addAlarmHistoryItem( alarmEntity.getAccountId(), alarmName, historyData,
            HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" deleted", now );
        Entities.delete( alarmEntity );
        return true;
      }
    } );
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
    jsonObject.element("alarmConfigurationUpdatedTimestamp", Timestamps.formatIso8601UTCLongDateMillisTimezone(alarmEntity.getAlarmConfigurationUpdatedTimestamp()));
    jsonObject.element("stateUpdatedTimestamp", Timestamps.formatIso8601UTCLongDateMillisTimezone(alarmEntity.getStateUpdatedTimestamp()));
    return jsonObject;
  }

  /**
   * @return False if enable rejected due to filter
   */
  public static boolean enableAlarmActions(
      final String accountId,
      final Collection<String> alarmNames,
      final Predicate<CloudWatchMetadata.AlarmMetadata> filter
  ) {
    return modifySelectedAlarms( accountId, alarmNames, filter, new Predicate<AlarmEntity>() {
      private final Date now = new Date();

      @Override
      public boolean apply( final AlarmEntity alarmEntity ) {
        final String alarmName = alarmEntity.getAlarmName();
        if ( !Boolean.TRUE.equals( alarmEntity.getActionsEnabled() ) ) {
          JSONObject historyDataJSON = new JSONObject();
          historyDataJSON.element( "version", "1.0" );
          historyDataJSON.element( "type", "Update" );
          JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity( alarmEntity );
          historyDataJSON.element( "updatedAlarm", historyDataDeletedAlarmJSON );
          String historyData = historyDataJSON.toString();
          AlarmManager.addAlarmHistoryItem( alarmEntity.getAccountId(), alarmName, historyData,
              HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" updated", now );
          alarmEntity.setActionsEnabled( Boolean.TRUE );
        }
        return true;
      }
    } );
  }

  /**
   * @return False if disable rejected due to filter
   */
  public static boolean disableAlarmActions(
      final String accountId,
      final Collection<String> alarmNames,
      final Predicate<CloudWatchMetadata.AlarmMetadata> filter
  ) {
    return modifySelectedAlarms( accountId, alarmNames, filter, new Predicate<AlarmEntity>() {
      private final Date now = new Date();

      @Override
      public boolean apply( final AlarmEntity alarmEntity ) {
        final String alarmName = alarmEntity.getAlarmName();
        if ( !Boolean.FALSE.equals( alarmEntity.getActionsEnabled() ) ) {
          JSONObject historyDataJSON = new JSONObject();
          historyDataJSON.element( "version", "1.0" );
          historyDataJSON.element( "type", "Update" );
          JSONObject historyDataDeletedAlarmJSON = getJSONObjectFromAlarmEntity( alarmEntity );
          historyDataJSON.element( "updatedAlarm", historyDataDeletedAlarmJSON );
          String historyData = historyDataJSON.toString();
          AlarmManager.addAlarmHistoryItem( alarmEntity.getAccountId(), alarmName, historyData,
              HistoryItemType.ConfigurationUpdate, "Alarm \"" + alarmName + "\" updated", now );
          alarmEntity.setActionsEnabled( Boolean.FALSE );
        }
        return true;
      }
    } );
  }

  private static boolean modifySelectedAlarms(
      final String accountId,
      final Collection<String> alarmNames,
      final Predicate<CloudWatchMetadata.AlarmMetadata> filter,
      final Predicate<AlarmEntity> update
  ) {
    final Map<String,Collection<String>> accountToNamesMap =
        buildAccountIdToAlarmNamesMap( accountId, alarmNames );
    final EntityTransaction db = Entities.get( AlarmEntity.class );
    try {
      final Criteria criteria = Entities.createCriteria(AlarmEntity.class);
      final Junction disjunction = Restrictions.disjunction();
      for ( final Map.Entry<String,Collection<String>> entry : accountToNamesMap.entrySet( ) ) {
        final Junction conjunction = Restrictions.conjunction();
        conjunction.add( Restrictions.eq( "accountId", entry.getKey() ) );
        conjunction.add( Restrictions.in( "alarmName", entry.getValue() ) );
        disjunction.add( conjunction );
      }
      criteria.add( disjunction );
      criteria.addOrder( Order.asc( "creationTimestamp" ) );
      criteria.addOrder( Order.asc( "naturalId" ) );
      final Collection<AlarmEntity> alarmEntities = (Collection<AlarmEntity>) criteria.list();
      if ( !Iterables.all( alarmEntities, filter ) ) {
        return false;
      }
      CollectionUtils.each( alarmEntities, update );
      db.commit();
      return true;
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  private static Map<String,Collection<String>> buildAccountIdToAlarmNamesMap(
      @Nullable final String accountId,
      @Nullable final Collection<String> alarmNames
  ) {
    final Multimap<String,String> alarmNamesMultimap = HashMultimap.create( );
    if ( alarmNames != null ) {
      if ( accountId != null ) {
        alarmNamesMultimap.putAll( accountId, alarmNames ); // An ARN is also a valid name
      }
      CollectionUtils.putAll(
          Optional.presentInstances( Iterables.transform(
              alarmNames,
              CloudWatchResourceName.asArnOfType( CloudWatchResourceName.Type.alarm ) ) ),
          alarmNamesMultimap,
          CloudWatchResourceName.toNamespace( ),
          CloudWatchResourceName.toName( )
      );
    }
    return alarmNamesMultimap.asMap();
  }

  public static void setAlarmState(
      final String accountId,
      final String alarmName,
      final String stateReason,
      final String stateReasonData,
      final StateValue stateValue,
      final Predicate<CloudWatchMetadata.AlarmMetadata> filter
  ) throws CloudWatchException {
    final EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      AlarmEntity alarmEntity = (AlarmEntity) Entities.createCriteria( AlarmEntity.class )
          .add( Restrictions.eq( "accountId" , accountId ) )
          .add( Restrictions.eq( "alarmName" , alarmName ) )
          .uniqueResult();
      if ( alarmEntity == null && CloudWatchResourceName.isResourceName().apply( alarmName ) ) try {
        final CloudWatchResourceName arn =
            CloudWatchResourceName.parse( alarmName, CloudWatchResourceName.Type.alarm );
        alarmEntity = (AlarmEntity) Entities.createCriteria(AlarmEntity.class)
          .add( Restrictions.eq( "accountId", arn.getNamespace() ) )
          .add( Restrictions.eq( "alarmName", arn.getName() ) )
          .uniqueResult();
      } catch ( CloudWatchResourceName.InvalidResourceNameException e ) {
      }
      if ( alarmEntity == null || !filter.apply( alarmEntity ) ) {
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

  public static List<AlarmEntity> describeAlarms(
      @Nullable final String accountId,
      @Nullable final String actionPrefix,
      @Nullable final String alarmNamePrefix,
      @Nullable final Collection<String> alarmNames,
      @Nullable final Integer maxRecords,
      @Nullable final StateValue stateValue,
      @Nullable final String nextToken,
                final Predicate<CloudWatchMetadata.AlarmMetadata> filter
  ) throws CloudWatchException {
    final List<AlarmEntity> results = Lists.newArrayList();
    final EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      boolean first = true;
      String token = nextToken;
      while ( token != null || first ) {
        first = false;
        final Date nextTokenCreatedTime = NextTokenUtils.getNextTokenCreatedTime(token, AlarmEntity.class, true);
        final Criteria criteria = Entities.createCriteria(AlarmEntity.class);
        if (accountId != null) {
          criteria.add( Restrictions.eq( "accountId" , accountId ) );
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
          criteria.add( actionsOf );
        }
        if (alarmNamePrefix != null) {
          criteria.add( Restrictions.like( "alarmName" , alarmNamePrefix + "%" ) );
        }
        if (alarmNames != null && !alarmNames.isEmpty()) {
          criteria.add( Restrictions.in( "alarmName", alarmNames ) );
        }
        if (stateValue != null) {
          criteria.add( Restrictions.eq( "stateValue" , stateValue ) );
        }
        NextTokenUtils.addNextTokenConstraints(
            maxRecords == null ? null : maxRecords - results.size( ), token, nextTokenCreatedTime, criteria);
        final List<AlarmEntity> alarmEntities = (List<AlarmEntity>) criteria.list();
        Iterables.addAll( results, Iterables.filter( alarmEntities, filter ) );
        token = maxRecords==null || ( maxRecords!=null && ( results.size() >= maxRecords || alarmEntities.size() < maxRecords ) )  ?
            null :
            alarmEntities.get(alarmEntities.size() - 1).getNaturalId();
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
    @Nullable final String accountId,
    @Nonnull  final Map<String, String> dimensionMap,
    @Nullable final String metricName,
    @Nullable final String namespace,
    @Nullable final Integer period,
    @Nullable final Statistic statistic,
    @Nullable final Units unit,
    @Nonnull  final Predicate<CloudWatchMetadata.AlarmMetadata> filter
  ) {
    final List<AlarmEntity> results = Lists.newArrayList();
    final EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      final Criteria criteria = Entities.createCriteria(AlarmEntity.class);
      if (accountId != null) {
        criteria.add( Restrictions.eq( "accountId" , accountId ) );
      }
      final Set<DimensionEntity> dimensions = Sets.newTreeSet( );
      for ( final Map.Entry<String,String> entry: dimensionMap.entrySet( ) ) {
        final DimensionEntity d = new DimensionEntity();
        d.setName(entry.getKey());
        d.setValue(entry.getValue());
        dimensions.add(d);
      }
      int dimIndex = 1;
      for (final DimensionEntity d: dimensions) {
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
        criteria.add( Restrictions.eq( "metricName" , metricName ) );
      }
      if (namespace != null) {
        criteria.add( Restrictions.eq( "namespace" , namespace ) );
      }
      if (period != null) {
        criteria.add( Restrictions.eq( "period" , period ) );
      }
      if (statistic != null) {
        criteria.add( Restrictions.eq( "statistic" , statistic ) );
      }
      if (unit != null) {
        criteria.add( Restrictions.eq( "unit" , unit ) );
      }
      final List<AlarmEntity> alarmEntities = (List<AlarmEntity>) criteria.list();
      Iterables.addAll( results, Iterables.filter( alarmEntities, filter ) );
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

  public static List<AlarmHistory> describeAlarmHistory(
      @Nullable final String accountId,
      @Nullable final String alarmName,
      @Nullable final Date endDate,
      @Nullable final HistoryItemType historyItemType,
      @Nullable final Integer maxRecords,
      @Nullable final Date startDate,
      @Nullable final String nextToken,
      final Predicate<AlarmHistory> filter ) throws CloudWatchException {
    final List<AlarmHistory> results = Lists.newArrayList();
    final EntityTransaction db = Entities.get(AlarmHistory.class);
    try {
      final Map<String,Collection<String>> accountToNamesMap = alarmName == null ?
          Collections.<String,Collection<String>>emptyMap( ) :
          buildAccountIdToAlarmNamesMap( accountId, Collections.singleton( alarmName ) );
      boolean first = true;
      String token = nextToken;
      while ( token != null || first ) {
        first = false;
        final Date nextTokenCreatedTime = NextTokenUtils.getNextTokenCreatedTime(token, AlarmHistory.class, true);
        final Criteria criteria = Entities.createCriteria(AlarmHistory.class);
        final Junction disjunction = Restrictions.disjunction();
        for ( final Map.Entry<String,Collection<String>> entry : accountToNamesMap.entrySet( ) ) {
          final Junction conjunction = Restrictions.conjunction();
          conjunction.add( Restrictions.eq( "accountId", entry.getKey() ) );
          conjunction.add( Restrictions.in( "alarmName", entry.getValue() ) );
          disjunction.add( conjunction );
        }
        criteria.add( disjunction );
        if (historyItemType != null) {
          criteria.add( Restrictions.eq( "historyItemType" , historyItemType ) );
        }
        if (startDate != null) {
          criteria.add( Restrictions.ge( "timestamp" , startDate ) );
        }
        if (endDate != null) {
          criteria.add( Restrictions.le( "timestamp" , endDate ) );
        }
        NextTokenUtils.addNextTokenConstraints(
            maxRecords == null ? null : maxRecords - results.size( ), token, nextTokenCreatedTime, criteria);
        final List<AlarmHistory> alarmHistoryEntities = (List<AlarmHistory>) criteria.list();
        Iterables.addAll( results, Iterables.filter( alarmHistoryEntities, filter ) );
        token = maxRecords==null || ( maxRecords!=null && ( results.size() >= maxRecords || alarmHistoryEntities.size() < maxRecords ) ) ?
            null :
            alarmHistoryEntities.get(alarmHistoryEntities.size() - 1).getNaturalId();
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

  /**
   * Delete all alarm history before a certain date
   * @param before the date to delete before (inclusive)
   */
  public static void deleteAlarmHistory(Date before) {
    EntityTransaction db = Entities.get(AlarmHistory.class);
    try {
      Map<String, Date> criteria = Maps.newHashMap();
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
          actionToExecute.executeAction(action, alarmEntity.getDimensionMap(), alarmEntity, now);
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
    stateReasonDataJSON.element("queryDate", Timestamps.formatIso8601UTCLongDateMillisTimezone(queryDate));
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
  
  private static class AutoScalingClient extends DispatchingClient<AutoScalingMessage,AutoScaling> {
    public AutoScalingClient( final String userId ) {
      super( userId, AutoScaling.class );
    }
  }

  private static class EucalyptusClient extends DispatchingClient<EucalyptusMessage,Eucalyptus> {
    public EucalyptusClient( final String userId ) {
      super( userId, Eucalyptus.class );
    }
  }
  
  private static abstract class Action {
    public abstract boolean filter(final String actionURN, final Map<String, String> dimensionMap);
    public abstract void executeAction(final String actionARN, final Map<String, String> dimensionMap, final AlarmEntity alarmEntity, final Date now);
    public abstract boolean alwaysExecute();
    public void success(final String actionARN, final AlarmEntity alarmEntity, final Date now) {
      JSONObject historyDataJSON = new JSONObject();
      historyDataJSON.element("actionState", "Succeeded");
      historyDataJSON.element("notificationResource", actionARN);
      historyDataJSON.element("stateUpdateTimestamp", Timestamps.formatIso8601UTCLongDateMillisTimezone(alarmEntity.getStateUpdatedTimestamp()));
      String historyData = historyDataJSON.toString();
      AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
        HistoryItemType.Action, " Successfully executed action " + actionARN, now);
    }
    public void failure(final String actionARN, final AlarmEntity alarmEntity, final Date now, Throwable cause) {
      JSONObject historyDataJSON = new JSONObject();
      historyDataJSON.element("actionState", "Failed");
      historyDataJSON.element("notificationResource", actionARN);
      historyDataJSON.element("stateUpdateTimestamp", Timestamps.formatIso8601UTCLongDateMillisTimezone(alarmEntity.getStateUpdatedTimestamp()));
      historyDataJSON.element("error", cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName());
      String historyData = historyDataJSON.toString();
      AlarmManager.addAlarmHistoryItem(alarmEntity.getAccountId(), alarmEntity.getAlarmName(), historyData, 
        HistoryItemType.Action, " Failed to execute action " + actionARN, now);
    }
   }
  private static class ExecuteAutoScalingPolicyAction extends Action {
    @Override
    public boolean filter(String action, Map<String, String> dimensionMap) {
      return (action != null && action.startsWith("arn:aws:autoscaling:"));
    }

    @Override
    public void executeAction(final String action, final Map<String, String> dimensionMap, final AlarmEntity alarmEntity, final Date now) {
      ExecutePolicyType executePolicyType = new ExecutePolicyType();
      executePolicyType.setPolicyName(action);
      executePolicyType.setHonorCooldown(true);
      Callback.Checked<AutoScalingMessage> callback = new Callback.Checked<AutoScalingMessage>() {
        @Override
        public void fire(AutoScalingMessage input) {
          success(action, alarmEntity, now);
        }

        @Override
        public void fireException(Throwable t) {
          failure(action, alarmEntity, now, t);
        }
      };
      try {
        Account account = Accounts.getAccountProvider().lookupAccountById(
          alarmEntity.getAccountId());
        User user = account.lookupUserByName(User.ACCOUNT_ADMIN);
        AutoScalingClient client = new AutoScalingClient(user.getUserId());
        client.init();
        client.dispatch(executePolicyType, callback);
      } catch (Exception ex) {
        failure(action, alarmEntity, now, ex);
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
    public void executeAction(final String action, final Map<String, String> dimensionMap, final AlarmEntity alarmEntity, final Date now) {
      TerminateInstancesType terminateInstances = new TerminateInstancesType();
      terminateInstances.getInstancesSet().add( dimensionMap.get("InstanceId"));
      Callback.Checked<EucalyptusMessage> callback = new Callback.Checked<EucalyptusMessage>() {
        @Override
        public void fire(EucalyptusMessage input) {
          success(action, alarmEntity, now);
        }

        @Override
        public void fireException(Throwable t) {
          failure(action, alarmEntity, now, t);
        }
      };
      try {
        Account account = Accounts.getAccountProvider().lookupAccountById(
          alarmEntity.getAccountId());
        User user = account.lookupUserByName(User.ACCOUNT_ADMIN);
        EucalyptusClient client = new EucalyptusClient(user.getUserId());
        client.init();
        client.dispatch(terminateInstances, callback);
      } catch (Exception ex) {
        failure(action, alarmEntity, now, ex);
      }
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
    public void executeAction(final String action, final Map<String, String> dimensionMap, final AlarmEntity alarmEntity, final Date now) {
      StopInstancesType stopInstances = new StopInstancesType();
      stopInstances.getInstancesSet().add( dimensionMap.get("InstanceId"));
      Callback.Checked<EucalyptusMessage> callback = new Callback.Checked<EucalyptusMessage>() {
        @Override
        public void fire(EucalyptusMessage input) {
          success(action, alarmEntity, now);
        }

        @Override
        public void fireException(Throwable t) {
          failure(action, alarmEntity, now, t);
        }
      };
      try {
        Account account = Accounts.getAccountProvider().lookupAccountById(
          alarmEntity.getAccountId());
        User user = account.lookupUserByName(User.ACCOUNT_ADMIN);
        EucalyptusClient client = new EucalyptusClient(user.getUserId());
        client.init();
        client.dispatch(stopInstances, callback);
      } catch (Exception ex) {
        failure(action, alarmEntity, now, ex);
      }
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

