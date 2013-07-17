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
package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.hashing.HashUtils;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class MetricManager {
	public static final Logger LOG = Logger.getLogger(MetricManager.class);
  public static void addMetric(String accountId, 
      String metricName, String namespace, Map<String, String> dimensionMap,
      MetricType metricType, Units units, Date timestamp, Double sampleSize,
      Double sampleMax, Double sampleMin, Double sampleSum) {
    SimpleMetricEntity simpleMetricEntity = new SimpleMetricEntity();
    simpleMetricEntity.setAccountId(accountId);
    simpleMetricEntity.setDimensionMap(dimensionMap);
    simpleMetricEntity.setMetricName(metricName);
    simpleMetricEntity.setMetricType(metricType);
    simpleMetricEntity.setNamespace(namespace);
    simpleMetricEntity.setSampleMax(sampleMax);
    simpleMetricEntity.setSampleMin(sampleMin);
    simpleMetricEntity.setSampleSize(sampleSize);
    simpleMetricEntity.setSampleSum(sampleSum);
    simpleMetricEntity.setTimestamp(timestamp);
    simpleMetricEntity.setUnits(units);
    validateMetricQueueItem(simpleMetricEntity);
    addManyMetrics(makeMetricMap(foldAndHash(simpleMetricEntity)));
  }
  
  private static Multimap<Class, MetricEntity> makeMetricMap(Collection<MetricEntity> entities) {
    Multimap<Class, MetricEntity> metricMap = ArrayListMultimap
        .<Class, MetricEntity> create();
    for (MetricEntity entity:entities) {
      metricMap
          .put(MetricEntityFactory.getClassForEntitiesGet(entity.getMetricType(),
              entity.getDimensionHash()), entity);
    }
    return metricMap;
  }
  

  private static List<MetricEntity> foldAndHash(SimpleMetricEntity simpleMetricEntity) {
    if (simpleMetricEntity == null) return new ArrayList<MetricEntity>();
    TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
    for (Map.Entry<String, String> entry : simpleMetricEntity.getDimensionMap().entrySet()) {
      DimensionEntity d = new DimensionEntity();
      d.setName(entry.getKey());
      d.setValue(entry.getValue());
      dimensions.add(d);
    }
    Set<Set<DimensionEntity>> permutations = null;
    if (simpleMetricEntity.getMetricType() == MetricType.System) {
      permutations = Sets.powerSet(dimensions);
    } else {
      permutations = Sets.newHashSet();
      permutations.add(dimensions);
    }
    ArrayList<MetricEntity> returnValue = new ArrayList<MetricEntity>();
    for (Set<DimensionEntity> dimensionsPermutation : permutations) {
      String dimensionHash = hash(dimensionsPermutation);
      MetricEntity metric = MetricEntityFactory.getNewMetricEntity(simpleMetricEntity.getMetricType(),
          dimensionHash);
      metric.setAccountId(simpleMetricEntity.getAccountId());
      metric.setMetricName(simpleMetricEntity.getMetricName());
      metric.setNamespace(simpleMetricEntity.getNamespace());
      metric.setDimensions(dimensions);// arguable, but has complete list
      metric.setDimensionHash(dimensionHash);
      metric.setMetricType(simpleMetricEntity.getMetricType());
      metric.setUnits(simpleMetricEntity.getUnits());
      metric.setTimestamp(simpleMetricEntity.getTimestamp());
      metric.setSampleMax(simpleMetricEntity.getSampleMax());
      metric.setSampleMin(simpleMetricEntity.getSampleMin());
      metric.setSampleSum(simpleMetricEntity.getSampleSum());
      metric.setSampleSize(simpleMetricEntity.getSampleSize());
      returnValue.add(metric);
    }
    return returnValue;
  }

  private static void addManyMetrics(Multimap<Class, MetricEntity> metricMap) {
    for (Class c : metricMap.keySet()) {
      EntityTransaction db = Entities.get(c);
      try {
        for (MetricEntity me : metricMap.get(c)) {
          Entities.persist(me);
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
  }

  public static String hash(Map<String, String> dimensionMap) {
    TreeMap<String, String> sortedDimensionMap = Maps.newTreeMap();
    if (dimensionMap != null) {
      sortedDimensionMap.putAll(dimensionMap);
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : sortedDimensionMap.entrySet()) {
      sb.append(entry.getKey() + "|" + entry.getValue() + "|");
    }
    return HashUtils.hash(sb.toString());
  }

  public static String hash(Collection<DimensionEntity> dimensions) {
    StringBuilder sb = new StringBuilder();
    for (DimensionEntity dimension : dimensions) {
      sb.append(dimension.getName() + "|" + dimension.getValue() + "|");
    }
    return HashUtils.hash(sb.toString());
  }

  public static Date stripSeconds(Date timestamp) {
    if (timestamp == null)
      return timestamp;
    long time = timestamp.getTime();
    time = time - time % 60000L;
    return new Date(time);
  }

  public static void deleteAllMetrics() {
    for (Class c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      EntityTransaction db = Entities.get(c);
      try {
        Entities.deleteAll(c);
        db.commit();
      } catch (RuntimeException ex) {
        Logs.extreme().error(ex, ex);
        throw ex;
      } finally {
        if (db.isActive())
          db.rollback();
      }
    }
  }

  /**
   * Delete all metrics before a certain date
   * 
   * @param before
   *          the date to delete before (inclusive)
   */
  public static void deleteMetrics(Date before) {
    for (Class c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      EntityTransaction db = Entities.get(c);
      try {
        Map<String, Date> criteria = new HashMap<String, Date>();
        criteria.put("before", before);
        Entities.deleteAllMatching(c, "WHERE timestamp < :before", criteria);
        db.commit();
      } catch (RuntimeException ex) {
        Logs.extreme().error(ex, ex);
        throw ex;
      } finally {
        if (db.isActive())
          db.rollback();
      }
    }
  }


  public static Collection<MetricStatistics> getMetricStatistics(String accountId, 
      String metricName, String namespace, Map<String, String> dimensionMap,
      MetricType metricType, Units units, Date startTime, Date endTime, Integer period) {
    if (dimensionMap == null) {
      dimensionMap = new HashMap<String, String>();
    } else if (dimensionMap.size() > MetricEntity.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, "
          + dimensionMap.size());
    }
    TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
    for (Map.Entry<String, String> entry : dimensionMap.entrySet()) {
      DimensionEntity d = new DimensionEntity();
      d.setName(entry.getKey());
      d.setValue(entry.getValue());
      dimensions.add(d);
    }
    Date now = new Date();
    if (endTime == null) endTime = now;
    if (startTime == null) startTime = new Date(now.getTime() - 60 * 60 * 1000L);
    startTime = stripSeconds(startTime);
    endTime = stripSeconds(endTime);
    if (startTime.after(endTime)) {
      throw new IllegalArgumentException("Start time must be after end time");
    }
    if (period == null) {
      period = 60;
    }
    if (period % 60 != 0) {
      throw new IllegalArgumentException("Period must be a multiple of 60");
    }
    if (period < 0) {
      throw new IllegalArgumentException("Period must be greater than 0");
    }
    if (period == 0) {
      throw new IllegalArgumentException("Period must not equal 0");
    }
    if (metricType == null) {
      throw new IllegalArgumentException("metricType must not be null");
    }
    if (accountId == null) {
      throw new IllegalArgumentException("accountId must not be null");
    }
    if (metricName == null) {
      throw new IllegalArgumentException("metricName must not be null");
    }
    if (namespace == null) {
      throw new IllegalArgumentException("namespace must not be null");
    }
    String hash = hash(dimensions);
    Class metricEntityClass = MetricEntityFactory.getClassForEntitiesGet(metricType, hash);
    Map<GetMetricStatisticsAggregationKey, MetricStatistics> aggregationMap = new TreeMap<GetMetricStatisticsAggregationKey, MetricStatistics>(GetMetricStatisticsAggregationKey.COMPARATOR_WITH_NULLS.INSTANCE);
    EntityTransaction db = Entities.get(metricEntityClass);
    try {
      Criteria criteria = Entities.createCriteria(metricEntityClass);
      criteria = criteria.add(Restrictions.eq("accountId", accountId));
      criteria = criteria.add(Restrictions.eq("metricName", metricName));
      criteria = criteria.add(Restrictions.eq("namespace", namespace));
      criteria = criteria.add(Restrictions.lt("timestamp", endTime));
      criteria = criteria.add(Restrictions.ge("timestamp", startTime));
      criteria = criteria.add(Restrictions.eq("dimensionHash", hash));
      if (units != null) {
        criteria = criteria.add(Restrictions.eq("units", units));
      }
      criteria = criteria.addOrder( Order.asc("creationTimestamp") );
      criteria = criteria.addOrder( Order.asc("naturalId") );
      Collection results = criteria.list();
      for (Object o: results) {
        MetricEntity me = (MetricEntity) o;
        // Note: dimensions from metric entity are the actual dimensions for the point.  dimensions passed in are from the
        // hash (used for aggregation).  The hash dimensions are what we want.
        GetMetricStatisticsAggregationKey key = new GetMetricStatisticsAggregationKey(me, startTime, period, hash);
        MetricStatistics item = new MetricStatistics(me, startTime, period, dimensions);
        if (!aggregationMap.containsKey(key)) {
          aggregationMap.put(key, item);
        } else {
          MetricStatistics totalSoFar = aggregationMap.get(key);
          totalSoFar.setSampleMax(Math.max(item.getSampleMax(), totalSoFar.getSampleMax()));
          totalSoFar.setSampleMin(Math.min(item.getSampleMin(), totalSoFar.getSampleMin()));
          totalSoFar.setSampleSize(totalSoFar.getSampleSize() + item.getSampleSize());
          totalSoFar.setSampleSum(totalSoFar.getSampleSum() + item.getSampleSum());
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
    return Lists.newArrayList(aggregationMap.values());
  }    

  public static Date getPeriodStart(Date originalTimestamp, Date startTime, Integer period) {
    long difference = originalTimestamp.getTime() - startTime.getTime();
    long remainderInOnePeriod = difference % (1000L * period);
    return new Date(originalTimestamp.getTime() - remainderInOnePeriod);
  }

  public static Collection<MetricEntity> getAllMetrics() {
    ArrayList<MetricEntity> allResults = new ArrayList<MetricEntity>();
    for (Class c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      EntityTransaction db = Entities.get(c);
      try {
        Criteria criteria = Entities.createCriteria(c);
        criteria = criteria.addOrder( Order.asc("creationTimestamp") );
        criteria = criteria.addOrder( Order.asc("naturalId") );
        Collection dbResults = criteria.list();
        for (Object result : dbResults) {
          allResults.add((MetricEntity) result);
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
    return allResults;
  }

  public static void addMetricBatch(List<SimpleMetricEntity> dataBatch) {
    ArrayList<MetricEntity> metricEntities = new ArrayList<MetricEntity>();
    for (SimpleMetricEntity simpleMetricEntity: dataBatch) {
      validateMetricQueueItem(simpleMetricEntity);
      metricEntities.addAll(foldAndHash(simpleMetricEntity));
    }
    addManyMetrics(makeMetricMap(metricEntities));
  }

  private static void validateMetricQueueItem(SimpleMetricEntity simpleMetricEntity) {
    LOG.trace("metricName="+simpleMetricEntity.getMetricName());
    LOG.trace("namespace="+simpleMetricEntity.getNamespace());
    LOG.trace("dimensionMap="+simpleMetricEntity.getDimensionMap());
    LOG.trace("metricType="+simpleMetricEntity.getMetricType());
    LOG.trace("units="+simpleMetricEntity.getUnits());
    LOG.trace("timestamp="+simpleMetricEntity.getTimestamp());
    LOG.trace("sampleSize="+simpleMetricEntity.getSampleSize());
    LOG.trace("sampleMax="+simpleMetricEntity.getSampleMax());
    LOG.trace("sampleMin="+simpleMetricEntity.getSampleMin());
    LOG.trace("sampleSum="+simpleMetricEntity.getSampleSum());
  
    if (simpleMetricEntity.getDimensionMap() == null) {
      simpleMetricEntity.setDimensionMap(new HashMap<String, String>());
    } else if (simpleMetricEntity.getDimensionMap().size() > MetricEntity.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, "
        + simpleMetricEntity.getDimensionMap().size());
    }
    simpleMetricEntity.setTimestamp(stripSeconds(simpleMetricEntity.getTimestamp()));
  }
}
