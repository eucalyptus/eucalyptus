/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudwatch.common.internal.domain.listmetrics;

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

import com.eucalyptus.cloudwatch.common.internal.domain.InvalidTokenException;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.common.internal.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.NextTokenUtils;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.SimpleMetricEntity;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ListMetricManager {

  public static volatile Integer LIST_METRIC_NUM_DB_OPERATIONS_PER_TRANSACTION = 10000;

  public static volatile Integer LIST_METRIC_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH = 50;

  private static final Logger LOG = Logger.getLogger(ListMetricManager.class);
  public static void addMetric(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
    try (final TransactionResource db = Entities.transactionFor(ListMetric.class)) {
      addMetric(db, accountId, metricName, namespace, dimensionMap, metricType);
      db.commit();
    }
  }
  private static ListMetric createMetric(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
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
    ListMetric metric = new ListMetric();
    metric.setAccountId(accountId);
    metric.setMetricName(metricName);
    metric.setNamespace(namespace);
    metric.setDimensions(dimensions);
    metric.setMetricType(metricType);
    return metric;
  }
  private static void addMetric(EntityTransaction db, String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType) {
    ListMetric metric = createMetric(accountId, metricName, namespace, dimensionMap, metricType);
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

  public static void deleteAllMetrics() {
    try (final TransactionResource db = Entities.transactionFor(ListMetric.class)) {
      Entities.deleteAll(ListMetric.class);
      db.commit();
    }
  }

  /**
   * Delete all metrics before a certain date
   * @param before the date to delete before (inclusive)
   */
  public static void deleteMetrics(Date before) {
    try (final TransactionResource db = Entities.transactionFor(ListMetric.class)) {
      Map<String, Date> criteria = new HashMap<String, Date>();
      criteria.put("before", before);
      Entities.deleteAllMatching(ListMetric.class, "WHERE lastUpdateTimestamp < :before", criteria);
      db.commit();
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
  public static List<ListMetric> listMetrics(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, Date after, Date before, Integer maxRecords, String nextToken) throws InvalidTokenException {
    if (dimensionMap != null && dimensionMap.size() > ListMetric.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions " + dimensionMap.size());
    }
    try (final TransactionResource db = Entities.transactionFor(ListMetric.class)) {
      Date nextTokenCreatedTime = NextTokenUtils.getNextTokenCreatedTime(nextToken, ListMetric.class);
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
      @SuppressWarnings( "unchecked" )
      List<ListMetric> dbResult = (List<ListMetric>) criteria.list();
      db.commit();
      return dbResult;
    }
  }

  private static class NonPrefetchFields {
    private MetricType metricType;
    private Map<String, String> dimensionMap;

    public NonPrefetchFields(MetricType metricType, Map<String, String> dimensionMap) {
      this.metricType = metricType;
      this.dimensionMap = dimensionMap;
    }

    public MetricType getMetricType() {
      return metricType;
    }

    public void setMetricType(MetricType metricType) {
      this.metricType = metricType;
    }

    public Map<String, String> getDimensionMap() {
      return dimensionMap;
    }

    public void setDimensionMap(Map<String, String> dimensionMap) {
      this.dimensionMap = dimensionMap;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      NonPrefetchFields that = (NonPrefetchFields) o;

      if (dimensionMap != null ? !dimensionMap.equals(that.dimensionMap) : that.dimensionMap != null) return false;
      if (metricType != that.metricType) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = metricType != null ? metricType.hashCode() : 0;
      result = 31 * result + (dimensionMap != null ? dimensionMap.hashCode() : 0);
      return result;
    }
  }
  private static class PrefetchFields {
    private String accountId;
    private String namespace;
    private String metricName;

    public String getAccountId() {
      return accountId;
    }

    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    public PrefetchFields(String accountId, String namespace, String metricName) {
      this.accountId = accountId;
      this.namespace = namespace;
      this.metricName = metricName;
    }

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public String getMetricName() {
      return metricName;
    }

    public void setMetricName(String metricName) {
      this.metricName = metricName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PrefetchFields that = (PrefetchFields) o;

      if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
      if (metricName != null ? !metricName.equals(that.metricName) : that.metricName != null) return false;
      if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = accountId != null ? accountId.hashCode() : 0;
      result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
      result = 31 * result + (metricName != null ? metricName.hashCode() : 0);
      return result;
    }
  }
  public static void addMetricBatch(List<ListMetric> dataBatch) {
    // sort the collection by common items to require fewer lookups
    Multimap<PrefetchFields, ListMetric> dataBatchPrefetchMap = LinkedListMultimap.create();
    for (final ListMetric item: dataBatch) {
      PrefetchFields prefetchFields = new PrefetchFields(item.getAccountId(), item.getNamespace(), item.getMetricName());
      dataBatchPrefetchMap.put(prefetchFields, item);
    }
    // do db stuff in a certain number of operations per connection
    for (List<PrefetchFields> prefetchFieldsListPartial : Iterables.partition(dataBatchPrefetchMap.keySet(), LIST_METRIC_NUM_DB_OPERATIONS_PER_TRANSACTION)) {
      try (final TransactionResource db = Entities.transactionFor(ListMetric.class)) {
        Entities.flushOnCommit(ListMetric.class);
        int numOperations = 0;
        for (PrefetchFields prefetchFields: prefetchFieldsListPartial) {
          // Prefetch all list metrics with same metric name/namespace/account id
          Map<NonPrefetchFields, ListMetric> dataCache = Maps.newHashMap();
          Criteria criteria = Entities.createCriteria(ListMetric.class)
            .add(Restrictions.eq("accountId", prefetchFields.getAccountId()))
            .add(Restrictions.eq("namespace", prefetchFields.getNamespace()))
            .add(Restrictions.eq("metricName", prefetchFields.getMetricName()));
          @SuppressWarnings( "unchecked" )
          List<ListMetric> results = (List<ListMetric>) criteria.list();
          for (ListMetric result : results) {
            dataCache.put(new NonPrefetchFields(result.getMetricType(), result.getDimensionMap()), result);
          }
          for (ListMetric listMetric : dataBatchPrefetchMap.get(prefetchFields)) {
            NonPrefetchFields cacheKey = new NonPrefetchFields(listMetric.getMetricType(), listMetric.getDimensionMap());
            if (dataCache.containsKey(cacheKey)) {
              dataCache.get(cacheKey).updateTimeStamps();
            } else {
              Entities.persist(listMetric);
              dataCache.put(cacheKey, listMetric);
            }
          }
          numOperations++;
          if (numOperations % LIST_METRIC_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH == 0) {
            Entities.flushSession(ListMetric.class);
            Entities.clearSession(ListMetric.class);
          }
        }
        db.commit();
      }
    }
  }

  public static ListMetric createListMetric(String accountId, String metricName, MetricEntity.MetricType metricType, String namespace, Map<String, String> dimensionMap) {
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

    ListMetric metric = new ListMetric();
    metric.setAccountId(accountId);
    metric.setMetricName(metricName);
    metric.setMetricType(metricType);
    metric.setNamespace(namespace);
    metric.setDimensions(dimensions);
    return metric;
  }

}
