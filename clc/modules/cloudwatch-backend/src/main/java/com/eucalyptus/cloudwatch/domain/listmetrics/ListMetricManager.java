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
package com.eucalyptus.cloudwatch.domain.listmetrics;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

import com.eucalyptus.cloudwatch.backend.CloudWatchException;
import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.NextTokenUtils;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.SimpleMetricEntity;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ListMetricManager {
  private static final Logger LOG = Logger.getLogger(ListMetricManager.class);
  public static void addMetric(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
    EntityTransaction db = Entities.get(ListMetric.class);
    try {
      addMetric(db, accountId, metricName, namespace, dimensionMap, metricType);
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }
  private static List<ListMetric> foldMetric(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
    List<ListMetric> metrics = Lists.newArrayList();
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
      if (!namespace.equals("AWS/EC2")) {
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
    for (Set<DimensionEntity> dimensionsPermutation : permutations) {
      ListMetric metric = new ListMetric();
      metric.setAccountId(accountId);
      metric.setMetricName(metricName);
      metric.setNamespace(namespace);
      metric.setDimensions(dimensionsPermutation);
      metric.setMetricType(metricType);
      metrics.add(metric);
    }
    return metrics;
  }
  private static void addMetric(EntityTransaction db, String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
    List<ListMetric> foldedMetrics = foldMetric(accountId, metricName, namespace, dimensionMap, metricType);
    for (ListMetric metric: foldedMetrics) {
      Criteria criteria = Entities.createCriteria(ListMetric.class)
          .add( Restrictions.eq( "accountId" , metric.getAccountId() ) )
          .add( Restrictions.eq( "metricName" , metric.getMetricName() ) )
          .add( Restrictions.eq( "namespace" , metric.getNamespace() ) );
    
      // add dimension restrictions
      int dimIndex = 1;
      for (DimensionEntity d: metric.getDimensions()) {
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

  public static void addMetricBatch(List<SimpleMetricEntity> dataBatch) {
    EntityTransaction db = Entities.get(ListMetric.class);
    try {
      HashSet<ListMetricCacheLoadKey> loadedKeys = Sets.newHashSet();
      HashMap<ListMetricCacheKey, ListMetric> cache = Maps.newHashMap();
      Collection<ListMetricCacheKey> cacheKeys = prune(dataBatch);
      List<ListMetric> foldedMetrics = Lists.newArrayList();
      for (ListMetricCacheKey cacheKey:cacheKeys) {
        foldedMetrics.addAll(foldMetric(cacheKey.getLoadKey().getAccountId(), 
            cacheKey.getMetricName(), cacheKey.getLoadKey().getNamespace(), 
            cacheKey.getDimensionMap(), cacheKey.getMetricType()));
      }
      for (ListMetric metric: foldedMetrics) {
        ListMetricCacheLoadKey loadKey = new ListMetricCacheLoadKey();
        loadKey.setAccountId(metric.getAccountId());
        loadKey.setNamespace(metric.getNamespace());
        if (!loadedKeys.contains(loadKey)) {
          Criteria criteria = Entities.createCriteria(ListMetric.class)
              .add( Restrictions.eq( "accountId" , metric.getAccountId() ) )
              .add( Restrictions.eq( "namespace" , metric.getNamespace() ) );
          List<ListMetric> results = (List<ListMetric>) criteria.list();
          for (ListMetric result: results) {
            ListMetricCacheKey key = new ListMetricCacheKey();
            key.setLoadKey(loadKey);
            key.setDimensionMap(result.getDimensionMap());
            key.setMetricName(result.getMetricName());
            key.setMetricType(result.getMetricType());
            cache.put(key, result);
          }
          loadedKeys.add(loadKey);
        }

        ListMetricCacheKey key = new ListMetricCacheKey();
        key.setDimensionMap(metric.getDimensionMap());
        key.setLoadKey(loadKey);
        key.setMetricName(metric.getMetricName());
        key.setMetricType(metric.getMetricType());
        ListMetric inDbMetric = cache.get(key);
        if (inDbMetric != null) {
          inDbMetric.setVersion(1 + inDbMetric.getVersion());
        } else {
          cache.put(key, metric);
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


  private static Collection<ListMetricCacheKey> prune(
      List<SimpleMetricEntity> dataBatch) {
    Collection<ListMetricCacheKey> returnValue = new LinkedHashSet<ListMetricCacheKey>();
    for (SimpleMetricEntity item: dataBatch) {
      ListMetricCacheLoadKey loadKey = new ListMetricCacheLoadKey();
      loadKey.setAccountId(item.getAccountId());
      loadKey.setNamespace(item.getNamespace());
      ListMetricCacheKey key = new ListMetricCacheKey();
      key.setDimensionMap(item.getDimensionMap());
      key.setLoadKey(loadKey);
      key.setMetricName(item.getMetricName());
      key.setMetricType(item.getMetricType());
      returnValue.add(key);
    }
    return returnValue;
  }

  private static class ListMetricCacheLoadKey {
    String accountId;
    String namespace;
    public String getAccountId() {
      return accountId;
    }
    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }
    public String getNamespace() {
      return namespace;
    }
    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result
          + ((namespace == null) ? 0 : namespace.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ListMetricCacheLoadKey other = (ListMetricCacheLoadKey) obj;
      if (accountId == null) {
        if (other.accountId != null)
          return false;
      } else if (!accountId.equals(other.accountId))
        return false;
      if (namespace == null) {
        if (other.namespace != null)
          return false;
      } else if (!namespace.equals(other.namespace))
        return false;
      return true;
    }
  }

  private static class ListMetricCacheKey {
    String metricName;
    MetricType metricType;
    ListMetricCacheLoadKey loadKey;
    Map<String, String> dimensionMap;
    public ListMetricCacheLoadKey getLoadKey() {
      return loadKey;
    }
    public void setLoadKey(ListMetricCacheLoadKey loadKey) {
      this.loadKey = loadKey;
    }
    public Map<String, String> getDimensionMap() {
      return dimensionMap;
    }
    public void setDimensionMap(Map<String, String> dimensionMap) {
      this.dimensionMap = dimensionMap;
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
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((dimensionMap == null) ? 0 : dimensionMap.hashCode());
      result = prime * result + ((loadKey == null) ? 0 : loadKey.hashCode());
      result = prime * result
          + ((metricName == null) ? 0 : metricName.hashCode());
      result = prime * result
          + ((metricType == null) ? 0 : metricType.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ListMetricCacheKey other = (ListMetricCacheKey) obj;
      if (dimensionMap == null) {
        if (other.dimensionMap != null)
          return false;
      } else if (!dimensionMap.equals(other.dimensionMap))
        return false;
      if (loadKey == null) {
        if (other.loadKey != null)
          return false;
      } else if (!loadKey.equals(other.loadKey))
        return false;
      if (metricName == null) {
        if (other.metricName != null)
          return false;
      } else if (!metricName.equals(other.metricName))
        return false;
      if (metricType != other.metricType)
        return false;
      return true;
    }
  }
}
