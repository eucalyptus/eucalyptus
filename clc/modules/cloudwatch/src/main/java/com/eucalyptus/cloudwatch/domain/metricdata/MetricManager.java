package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.EntityTransaction;

import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.hashing.HashUtils;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.google.common.collect.Sets;

public class MetricManager {
  public static void addMetric(String accountId, String userId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType, Units units, Date timestamp, Double sampleSize, Double sampleMax, Double sampleMin, Double sampleSum) {
  	if (dimensionMap == null) {
      dimensionMap = new HashMap<String, String>();
    } else if (dimensionMap.size() > ListMetric.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, " + dimensionMap.size());
    }
  	timestamp = stripSeconds(timestamp);
    TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
    for (Map.Entry<String,String> entry: dimensionMap.entrySet()) {
      DimensionEntity d = new DimensionEntity();
      d.setName(entry.getKey());
      d.setValue(entry.getValue());
      dimensions.add(d);
    }
    EntityTransaction db = Entities.get(MetricEntity.class);
    Set<Set<DimensionEntity>> permutations = null;
    if (metricType == MetricType.System) {
    	permutations = Sets.powerSet(dimensions);
    } else {
    	permutations = Sets.newHashSet();
    	permutations.add(dimensions);
    }
  	try {
  		for (Set<DimensionEntity> dimensionsPermutation: permutations) {
  			MetricEntity metric = new MetricEntity();
  			metric.setAccountId(accountId);
  			metric.setUserId(userId);
  			metric.setMetricName(metricName);
  			metric.setNamespace(namespace);
  			metric.setDimensions(dimensions);// arguable, but has complete list
  			metric.setDimensionHash(hash(dimensionsPermutation));
  			metric.setMetricType(metricType);
  			metric.setUnits(units);
  			metric.setTimestamp(timestamp);
  			metric.setSampleMax(sampleMax);
  			metric.setSampleMin(sampleMin);
  			metric.setSampleSum(sampleSum);
  			metric.setSampleSize(sampleSize);
  			Entities.persist(metric);
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
  private static String hash(Collection<DimensionEntity> dimensions) {
  	StringBuilder sb = new StringBuilder();
  	for (DimensionEntity dimension: dimensions) {
  		sb.append(dimension.getName() + "|" + dimension.getValue() + "|");
  	}
  	return HashUtils.hash(sb.toString());
  }
  private static Date stripSeconds(Date timestamp) {
  	if (timestamp == null) return timestamp;
  	GregorianCalendar g = new GregorianCalendar();
  	g.setTime(timestamp);
  	g.set(Calendar.SECOND, 0);
  	g.set(Calendar.MILLISECOND, 0);
  	return g.getTime();
  }
	public static void deleteAllMetrics() {
    EntityTransaction db = Entities.get(MetricEntity.class);
    try {
      Entities.deleteAll(MetricEntity.class);
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  /**
   * Delete all metrics before a certain date
   * @param before the date to delete before (inclusive)
   */
  public static void deleteMetrics(Date before) {
    EntityTransaction db = Entities.get(MetricEntity.class);
    try {
      Map<String, Date> criteria = new HashMap<String, Date>();
      criteria.put("before", before);
      Entities.deleteAllMatching(ListMetric.class, "WHERE timestamp < :before", criteria);
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

  public static Collection<MetricEntity> getAllMetrics() {
    EntityTransaction db = Entities.get(MetricEntity.class);
    try {
    	Criteria criteria = Entities.createCriteria(MetricEntity.class);
    	Collection<MetricEntity> dbResult = (Collection<MetricEntity>) criteria.list();
      db.commit();
      return dbResult;
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }

}
