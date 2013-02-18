package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;

import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.hashing.HashUtils;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class MetricManager {
	public static final Logger LOG = Logger.getLogger(MetricManager.class);
  public static void addMetric(String accountId, String userId,
      String metricName, String namespace, Map<String, String> dimensionMap,
      MetricType metricType, Units units, Date timestamp, Double sampleSize,
      Double sampleMax, Double sampleMin, Double sampleSum) {
    if (dimensionMap == null) {
      dimensionMap = new HashMap<String, String>();
    } else if (dimensionMap.size() > ListMetric.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, "
          + dimensionMap.size());
    }
    timestamp = stripSeconds(timestamp);
    TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
    for (Map.Entry<String, String> entry : dimensionMap.entrySet()) {
      DimensionEntity d = new DimensionEntity();
      d.setName(entry.getKey());
      d.setValue(entry.getValue());
      dimensions.add(d);
    }
    Set<Set<DimensionEntity>> permutations = null;
    if (metricType == MetricType.System) {
      permutations = Sets.powerSet(dimensions);
    } else {
      permutations = Sets.newHashSet();
      permutations.add(dimensions);
    }
    Multimap<Class, MetricEntity> metricMap = ArrayListMultimap
        .<Class, MetricEntity> create();
    for (Set<DimensionEntity> dimensionsPermutation : permutations) {
      String dimensionHash = hash(dimensionsPermutation);
      MetricEntity metric = MetricEntityFactory.getNewMetricEntity(metricType,
          dimensionHash);
      metric.setAccountId(accountId);
      metric.setUserId(userId);
      metric.setMetricName(metricName);
      metric.setNamespace(namespace);
      metric.setDimensions(dimensions);// arguable, but has complete list
      metric.setDimensionHash(dimensionHash);
      metric.setMetricType(metricType);
      metric.setUnits(units);
      metric.setTimestamp(timestamp);
      metric.setSampleMax(sampleMax);
      metric.setSampleMin(sampleMin);
      metric.setSampleSum(sampleSum);
      metric.setSampleSize(sampleSize);
      metricMap
          .put(MetricEntityFactory.getClassForEntitiesGet(metricType,
              dimensionHash), metric);
    }
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

  private static String hash(Collection<DimensionEntity> dimensions) {
    StringBuilder sb = new StringBuilder();
    for (DimensionEntity dimension : dimensions) {
      sb.append(dimension.getName() + "|" + dimension.getValue() + "|");
    }
    return HashUtils.hash(sb.toString());
  }

  private static Date stripSeconds(Date timestamp) {
    if (timestamp == null)
      return timestamp;
    GregorianCalendar g = new GregorianCalendar();
    g.setTime(timestamp);
    g.set(Calendar.SECOND, 0);
    g.set(Calendar.MILLISECOND, 0);
    return g.getTime();
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

  public static Collection<MetricEntity> getAllMetrics() {
    ArrayList<MetricEntity> allResults = new ArrayList<MetricEntity>();
    for (Class c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      EntityTransaction db = Entities.get(c);
      try {
        Criteria criteria = Entities.createCriteria(c);
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
}
