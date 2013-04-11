package com.eucalyptus.cloudwatch.domain.listmetrics;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.CloudWatchException;
import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.NextTokenUtils;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class ListMetricManager {
  private static final Logger LOG = Logger.getLogger(ListMetricManager.class);
  public static void addMetric(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
    if (dimensionMap == null) {
      dimensionMap = new HashMap<String, String>();
    } else if (dimensionMap.size() > ListMetric.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, " + dimensionMap.size());
    }
    TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
    for (Map.Entry<String,String> entry: dimensionMap.entrySet()) {
      DimensionEntity d = new DimensionEntity();
      d.setName(entry.getKey());
      d.setValue(entry.getValue());
      dimensions.add(d);
    }
    Set<Set<DimensionEntity>> permutations = null;
    if (metricType == MetricType.System) {
      // do dimension folding (i.e. insert 2^n metrics.  
      // All with the same metric name and namespace, but one for each subset of the dimension set passed in, including all, and none)
      if (!metricName.equals("AWS/EC2")) {
        permutations = Sets.powerSet(dimensions);
      } else {
        // Hack: no values in AWS/EC2 have more than one dimension, so fold, but only choose dimension subsets of size at most 1.
        // See EUCA-do dimension folding (i.e. insert 2^n metrics.  
        // All with the same metric name and namespace, but one for each subset of the dimension set passed in, including all, and none)
        permutations = Sets.filter(Sets.powerSet(dimensions), new Predicate<Set<DimensionEntity>>(){
          public boolean apply(@Nullable Set<DimensionEntity> candidate) {
            return (candidate != null && candidate.size() < 2);
          } } );
      }
    } else { // no folding on custom metrics
      permutations = Sets.newHashSet();
      permutations.add(dimensions);
    }
    Multimap<Class, MetricEntity> metricMap = ArrayListMultimap
        .<Class, MetricEntity> create();
    EntityTransaction db = Entities.get(ListMetric.class);
    try {
      for (Set<DimensionEntity> dimensionsPermutation : permutations) {

        ListMetric metric = new ListMetric();
        metric.setAccountId(accountId);
        metric.setMetricName(metricName);
        metric.setNamespace(namespace);
        metric.setDimensions(dimensionsPermutation);
        metric.setMetricType(metricType);
        Criteria criteria = Entities.createCriteria(ListMetric.class)
            .add( Restrictions.eq( "accountId" , accountId ) )
            .add( Restrictions.eq( "metricName" , metricName ) )
            .add( Restrictions.eq( "namespace" , namespace ) );
      
        // add dimension restrictions
        int dimIndex = 1;
        for (DimensionEntity d: dimensionsPermutation) {
          criteria.add( Restrictions.eq( "dim" + dimIndex + "Name", d.getName() ) );
          criteria.add( Restrictions.eq( "dim" + dimIndex + "Value", d.getValue() ) );
          dimIndex++;
        }
        while (dimIndex <= ListMetric.MAX_DIM_NUM) {
          criteria.add( Restrictions.isNull( "dim" + dimIndex + "Name") );
          criteria.add( Restrictions.isNull( "dim" + dimIndex + "Value") );
          dimIndex++;
        }
        ListMetric inDbMetric = (ListMetric) criteria.uniqueResult();
        if (inDbMetric != null) {
          inDbMetric.setVersion(1 + inDbMetric.getVersion());
        } else {
          Entities.persist(metric);
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
  
  public static void deleteAllMetrics() {
    EntityTransaction db = Entities.get(ListMetric.class);
    try {
      Entities.deleteAll(ListMetric.class);
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
    EntityTransaction db = Entities.get(ListMetric.class);
    try {
      Map<String, Date> criteria = new HashMap<String, Date>();
      criteria.put("before", before);
      Entities.deleteAllMatching(ListMetric.class, "WHERE lastUpdateTimestamp < :before", criteria);
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
   * Returns the metrics that are associated with the applied parameters
   * @param accountId the account Id.  If null, this filter will not be used.
   * @param metricName the metric name.  If null, this filter will not be used.
   * @param namespace the namespace.  If null, this filter will not be used.
   * @param dimensionMap the dimensions (name/value) to filter against.  Only metrics containing all these dimensions will be returned (it is only a subset match, not exact).  If null, this filter will not be used.
   * @param after the time after which all metrics must have been updated (last seen).  If null, this filter will not be used.
   * @param before the time before which all metrics must have been updated (last seen). If null, this filter will not be used.
   * @param maxRecords TODO
   * @param nextToken TODO
   * @return the collection of metrics, filtered by the input
   */
  public static List<ListMetric> listMetrics(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, Date after, Date before, Integer maxRecords, String nextToken) throws CloudWatchException {
    if (dimensionMap != null && dimensionMap.size() > ListMetric.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions " + dimensionMap.size());
    }
    EntityTransaction db = Entities.get(ListMetric.class);
    try {
      Date nextTokenCreatedTime = NextTokenUtils.getNextTokenCreatedTime(nextToken, ListMetric.class, false);
      Map<String, String> sortedDimensionMap = new TreeMap<String, String>();
      Criteria criteria = Entities.createCriteria(ListMetric.class);
      if (accountId != null) {
        criteria = criteria.add(Restrictions.eq("accountId", accountId));
      }
      if (metricName != null) {
        criteria = criteria.add(Restrictions.eq("metricName", metricName));
      }
      if (namespace != null) {
        criteria = criteria.add(Restrictions.eq("namespace", namespace));
      }
      if (before != null) {
        criteria = criteria.add(Restrictions.le("lastUpdateTimestamp", before));
      }
      if (after != null) {
        criteria = criteria.add(Restrictions.ge("lastUpdateTimestamp", after));
      }
      if (dimensionMap != null && !dimensionMap.isEmpty()) {
        // sort the map 
        sortedDimensionMap.putAll(dimensionMap);
        // now we are going to add a bunch of restrictions to the criteria...
        // note though there are certain dimensions we don't need to check.
        // For example if we have two dimensions, we don't need to check dimension 10 for
        // the first item or dimension 1 for the last item.
        int numDimensions = sortedDimensionMap.size();
        int lowDimNum = 1;
        int highDimNum = ListMetric.MAX_DIM_NUM + 1 - numDimensions;
        for (Map.Entry<String, String> dimEntry : sortedDimensionMap.entrySet()) {
          Disjunction or = Restrictions.disjunction();
          for (int i = lowDimNum; i <= highDimNum; i++) {
            or.add(Restrictions.conjunction()
                .add(Restrictions.eq("dim" + i + "Name", dimEntry.getKey()))
                .add(Restrictions.eq("dim" + i + "Value", dimEntry.getValue())));
          }
          lowDimNum++;
          highDimNum++;
          criteria = criteria.add(or);
        }
      }
      criteria = NextTokenUtils.addNextTokenConstraints(maxRecords, nextToken, nextTokenCreatedTime, criteria);
      List<ListMetric> dbResult = (List<ListMetric>) criteria.list();
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