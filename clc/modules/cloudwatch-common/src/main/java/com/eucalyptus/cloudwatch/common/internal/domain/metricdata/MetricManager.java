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
package com.eucalyptus.cloudwatch.common.internal.domain.metricdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.eucalyptus.cloudwatch.common.internal.domain.AbstractPersistentWithDimensions;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.common.internal.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.internal.hashing.HashUtils;
import com.eucalyptus.entities.Entities;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class MetricManager {

  public static volatile Integer METRIC_DATA_NUM_DB_OPERATIONS_PER_TRANSACTION = 10000;
  public static volatile Integer METRIC_DATA_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH = 50;

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
    addManyMetrics(makeMetricMap(hash(simpleMetricEntity)));
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


  private static List<MetricEntity> hash(SimpleMetricEntity simpleMetricEntity) {
    if (simpleMetricEntity == null) return new ArrayList<MetricEntity>();
    TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
    for (Map.Entry<String, String> entry : simpleMetricEntity.getDimensionMap().entrySet()) {
      DimensionEntity d = new DimensionEntity();
      d.setName(entry.getKey());
      d.setValue(entry.getValue());
      dimensions.add(d);
    }
    ArrayList<MetricEntity> returnValue = new ArrayList<MetricEntity>();
    String dimensionHash = hash(dimensions);
    MetricEntity metric = MetricEntityFactory.getNewMetricEntity(simpleMetricEntity.getMetricType(),
          dimensionHash);
    metric.setAccountId(simpleMetricEntity.getAccountId());
    metric.setMetricName(simpleMetricEntity.getMetricName());
    metric.setNamespace(simpleMetricEntity.getNamespace());
    metric.setDimensionHash(dimensionHash);
    metric.setMetricType(simpleMetricEntity.getMetricType());
    metric.setUnits(simpleMetricEntity.getUnits());
    metric.setTimestamp(simpleMetricEntity.getTimestamp());
    metric.setSampleMax(simpleMetricEntity.getSampleMax());
    metric.setSampleMin(simpleMetricEntity.getSampleMin());
    metric.setSampleSum(simpleMetricEntity.getSampleSum());
    metric.setSampleSize(simpleMetricEntity.getSampleSize());
    returnValue.add(metric);
    return returnValue;
  }

  private static void addManyMetrics(Multimap<Class, MetricEntity> metricMap) {
    for (Class c : metricMap.keySet()) {
      for (List<MetricEntity> dataBatchPartial : Iterables.partition(metricMap.get(c), METRIC_DATA_NUM_DB_OPERATIONS_PER_TRANSACTION)) {
        try (final TransactionResource db = Entities.transactionFor(c)) {
          int numOperations = 0;
          for (MetricEntity me : dataBatchPartial) {
            numOperations++;
            if (numOperations % METRIC_DATA_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH == 0) {
              Entities.flushSession(c);
              Entities.clearSession(c);
            }
            Entities.persist(me);
          }
          db.commit();
        }
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
    return HashUtils.hash(sb);
  }

  public static String hash(Collection<DimensionEntity> dimensions) {
    StringBuilder sb = new StringBuilder();
    for (DimensionEntity dimension : dimensions) {
      sb.append(dimension.getName() + "|" + dimension.getValue() + "|");
    }
    return HashUtils.hash(sb);
  }

  public static void deleteAllMetrics() {
    for (Class<?> c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      try (final TransactionResource db = Entities.transactionFor(c)) {
        Entities.deleteAll(c);
        db.commit();
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
    for (Class<?> c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      try (final TransactionResource db = Entities.transactionFor(c)) {
        Map<String, Date> criteria = new HashMap<String, Date>();
        criteria.put("before", before);
        Entities.deleteAllMatching(c, "WHERE timestamp < :before", criteria);
        db.commit();
      }
    }
  }


  public static class GetMetricStatisticsParams {
    String accountId;
    String metricName;
    String namespace;
    Map<String, String> dimensionMap;
    MetricType metricType;
    Units units;
    Date startTime;
    Date endTime;
    Integer period;
    Collection<DimensionEntity> dimensions;
    String dimensionHash;
    public GetMetricStatisticsParams(String accountId, String metricName, String namespace, Map<String, String> dimensionMap, MetricType metricType, Units units, Date startTime, Date endTime, Integer period) {
      this.accountId = accountId;
      this.metricName = metricName;
      this.namespace = namespace;
      this.dimensionMap = dimensionMap;
      this.metricType = metricType;
      this.units = units;
      this.startTime = startTime;
      this.endTime = endTime;
      this.period = period;
      this.dimensions = getDimensionsFromMap(dimensionMap);
      this.dimensionHash = hash(dimensionMap);
    }

    private static Collection<DimensionEntity> getDimensionsFromMap(Map<String, String> dimensionMap) {
      TreeSet<DimensionEntity> dimensions = new TreeSet<DimensionEntity>();
      for (Map.Entry<String, String> entry : dimensionMap.entrySet()) {
        DimensionEntity d = new DimensionEntity();
        d.setName(entry.getKey());
        d.setValue(entry.getValue());
        dimensions.add(d);
      }
      return dimensions;
    }

    @Override
    public String toString() {
      return "GetMetricStatisticsParams{" +
        "accountId='" + accountId + '\'' +
        ", metricName='" + metricName + '\'' +
        ", namespace='" + namespace + '\'' +
        ", dimensionMap=" + dimensionMap +
        ", metricType=" + metricType +
        ", units=" + units +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", period=" + period +
        '}';
    }

    public String getAccountId() {
      return accountId;
    }

    public String getMetricName() {
      return metricName;
    }

    public String getNamespace() {
      return namespace;
    }

    public Map<String, String> getDimensionMap() {
      return dimensionMap;
    }

    public MetricType getMetricType() {
      return metricType;
    }

    public Units getUnits() {
      return units;
    }

    public Date getStartTime() {
      return startTime;
    }

    public Date getEndTime() {
      return endTime;
    }

    public Integer getPeriod() {
      return period;
    }

    public void validate(Date now) {
      if (dimensionMap == null) {
        dimensionMap = new HashMap<String, String>();
      } else if (dimensionMap.size() > AbstractPersistentWithDimensions.MAX_DIM_NUM) {
        throw new IllegalArgumentException("Too many dimensions for metric, "
          + dimensionMap.size());
      }
      if (endTime == null) endTime = now;
      if (startTime == null) startTime = new Date(now.getTime() - 60 * 60 * 1000L);
      startTime = MetricUtils.stripSeconds(startTime);
      endTime = MetricUtils.stripSeconds(endTime);
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
    }

    public Collection<DimensionEntity> getDimensions() {
      return dimensions;
    }

    public String getDimensionHash() {
      return dimensionHash;
    }

    public GetMetricStatisticsParams() {
    }
  }

  public static List<Collection<MetricStatistics>> getManyMetricStatistics(List<GetMetricStatisticsParams> getMetricStatisticsParamses) {
    if (getMetricStatisticsParamses == null) throw new IllegalArgumentException("getMetricStatisticsParamses can not be null");
    Date now = new Date();
    Map<GetMetricStatisticsParams, Collection<MetricStatistics>> resultMap = Maps.newHashMap();
    Multimap<Class, GetMetricStatisticsParams> hashGroupMap = LinkedListMultimap.create();
    for (GetMetricStatisticsParams getMetricStatisticsParams : getMetricStatisticsParamses) {
      if (getMetricStatisticsParams == null) throw new IllegalArgumentException("getMetricStatisticsParams can not be null");
      getMetricStatisticsParams.validate(now);
      Class metricEntityClass = MetricEntityFactory.getClassForEntitiesGet(getMetricStatisticsParams.getMetricType(), getMetricStatisticsParams.getDimensionHash());
      hashGroupMap.put(metricEntityClass, getMetricStatisticsParams);
    }
    for (Class metricEntityClass: hashGroupMap.keySet()) {
      try (final TransactionResource db = Entities.transactionFor(metricEntityClass)) {
        // set some global criteria to start (for narrowing?)
        Date minDate = null;
        Date maxDate = null;
        Junction disjunction = Restrictions.disjunction();
        Map<GetMetricStatisticsParams, TreeMap<GetMetricStatisticsAggregationKey, MetricStatistics>> multiAggregationMap = Maps.newHashMap();
        for (GetMetricStatisticsParams getMetricStatisticsParams : hashGroupMap.get(metricEntityClass)) {
          multiAggregationMap.put(getMetricStatisticsParams, new TreeMap<GetMetricStatisticsAggregationKey, MetricStatistics>(GetMetricStatisticsAggregationKey.COMPARATOR_WITH_NULLS.INSTANCE));
          Junction conjunction = Restrictions.conjunction();
          conjunction = conjunction.add(Restrictions.lt("timestamp", getMetricStatisticsParams.getEndTime()));
          conjunction = conjunction.add(Restrictions.ge("timestamp", getMetricStatisticsParams.getStartTime()));
          conjunction = conjunction.add(Restrictions.eq("accountId", getMetricStatisticsParams.getAccountId()));
          conjunction = conjunction.add(Restrictions.eq("metricName", getMetricStatisticsParams.getMetricName()));
          conjunction = conjunction.add(Restrictions.eq("namespace", getMetricStatisticsParams.getNamespace()));
          conjunction = conjunction.add(Restrictions.eq("dimensionHash", hash(getMetricStatisticsParams.getDimensionMap())));
          if (getMetricStatisticsParams.getUnits() != null) {
            conjunction = conjunction.add(Restrictions.eq("units", getMetricStatisticsParams.getUnits()));
          }
          disjunction = disjunction.add(conjunction);
          if (minDate == null || getMetricStatisticsParams.getStartTime().before(minDate)) {
            minDate = getMetricStatisticsParams.getStartTime();
          }
          if (maxDate == null || getMetricStatisticsParams.getEndTime().after(maxDate)) {
            maxDate = getMetricStatisticsParams.getEndTime();
          }
        }
        Criteria criteria = Entities.createCriteria(metricEntityClass);
        criteria = criteria.add(Restrictions.lt("timestamp", maxDate));
        criteria = criteria.add(Restrictions.ge("timestamp", minDate));
        criteria = criteria.add(disjunction);

        ProjectionList projectionList = Projections.projectionList();
        projectionList.add(Projections.max("sampleMax"));
        projectionList.add(Projections.min("sampleMin"));
        projectionList.add(Projections.sum("sampleSize"));
        projectionList.add(Projections.sum("sampleSum"));
        projectionList.add(Projections.groupProperty("units"));
        projectionList.add(Projections.groupProperty("timestamp"));
        projectionList.add(Projections.groupProperty("accountId"));
        projectionList.add(Projections.groupProperty("metricName"));
        projectionList.add(Projections.groupProperty("metricType"));
        projectionList.add(Projections.groupProperty("namespace"));
        projectionList.add(Projections.groupProperty("dimensionHash"));
        criteria.setProjection(projectionList);
        criteria.addOrder(Order.asc("timestamp"));

        final ScrollableResults results = criteria.setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
        try {
          while ( results.next( ) ) {
            MetricEntity me = getMetricEntity( results );
            for ( GetMetricStatisticsParams getMetricStatisticsParams : hashGroupMap.get( metricEntityClass ) ) {
              if ( metricDataMatches( getMetricStatisticsParams, me ) ) {
                Map<GetMetricStatisticsAggregationKey, MetricStatistics> aggregationMap = multiAggregationMap.get( getMetricStatisticsParams );
                GetMetricStatisticsAggregationKey key = new GetMetricStatisticsAggregationKey( me, getMetricStatisticsParams.getStartTime( ), getMetricStatisticsParams.getPeriod( ), getMetricStatisticsParams.getDimensionHash( ) );
                MetricStatistics item = new MetricStatistics( me, getMetricStatisticsParams.getStartTime( ), getMetricStatisticsParams.getPeriod( ), getMetricStatisticsParams.getDimensions( ) );
                if ( !aggregationMap.containsKey( key ) ) {
                  aggregationMap.put( key, item );
                } else {
                  MetricStatistics totalSoFar = aggregationMap.get( key );
                  totalSoFar.setSampleMax( Math.max( item.getSampleMax( ), totalSoFar.getSampleMax( ) ) );
                  totalSoFar.setSampleMin( Math.min( item.getSampleMin( ), totalSoFar.getSampleMin( ) ) );
                  totalSoFar.setSampleSize( totalSoFar.getSampleSize( ) + item.getSampleSize( ) );
                  totalSoFar.setSampleSum( totalSoFar.getSampleSum( ) + item.getSampleSum( ) );
                }
              }
            }
          }
        } finally {
          results.close( );
        }
        for (GetMetricStatisticsParams getMetricStatisticsParams : multiAggregationMap.keySet()) {
          resultMap.put(getMetricStatisticsParams, multiAggregationMap.get(getMetricStatisticsParams).values());
        }
      }
    }
    List<Collection<MetricStatistics>> resultList = Lists.newArrayList();
    for (GetMetricStatisticsParams getMetricStatisticsParams : getMetricStatisticsParamses) {
      if (resultMap.get(getMetricStatisticsParams) == null) {
        resultList.add(new ArrayList<MetricStatistics>());
      } else {
        resultList.add(resultMap.get(getMetricStatisticsParams));
      }
    }
    return resultList;
  }

  private static boolean metricDataMatches(GetMetricStatisticsParams getMetricStatisticsParams, MetricEntity metricEntity) {
    if (getMetricStatisticsParams == null || metricEntity == null) return false;
    if (getMetricStatisticsParams.getStartTime() == null || getMetricStatisticsParams.getStartTime().after(metricEntity.getTimestamp())) return false;
    if (metricEntity.getTimestamp() == null || !metricEntity.getTimestamp().before(getMetricStatisticsParams.getEndTime())) return false;
    if (getMetricStatisticsParams.getAccountId() == null || !getMetricStatisticsParams.getAccountId().equals(metricEntity.getAccountId())) return false;
    if (getMetricStatisticsParams.getMetricName() == null || !getMetricStatisticsParams.getMetricName().equals(metricEntity.getMetricName())) return false;
    if (getMetricStatisticsParams.getNamespace() == null || !getMetricStatisticsParams.getNamespace().equals(metricEntity.getNamespace())) return false;
    if (getMetricStatisticsParams.getMetricType() == null || !getMetricStatisticsParams.getMetricType().equals(metricEntity.getMetricType())) return false;
    if (getMetricStatisticsParams.getDimensionHash() == null || !getMetricStatisticsParams.getDimensionHash().equals(metricEntity.getDimensionHash())) return false;
    return true;
  }

  public static Collection<MetricStatistics> getMetricStatistics(GetMetricStatisticsParams getMetricStatisticsParams) {
    if (getMetricStatisticsParams == null) throw new IllegalArgumentException("getMetricStatisticsParams can not be null");
    Date now = new Date();
    getMetricStatisticsParams.validate(now);
    Class metricEntityClass = MetricEntityFactory.getClassForEntitiesGet(getMetricStatisticsParams.getMetricType(), getMetricStatisticsParams.getDimensionHash());
    Map<GetMetricStatisticsAggregationKey, MetricStatistics> aggregationMap = new TreeMap<GetMetricStatisticsAggregationKey, MetricStatistics>(GetMetricStatisticsAggregationKey.COMPARATOR_WITH_NULLS.INSTANCE);
    try (final TransactionResource db = Entities.transactionFor(metricEntityClass)) {
      Criteria criteria = Entities.createCriteria(metricEntityClass);
      criteria = criteria.add(Restrictions.eq("accountId", getMetricStatisticsParams.getAccountId()));
      criteria = criteria.add(Restrictions.eq("metricName", getMetricStatisticsParams.getMetricName()));
      criteria = criteria.add(Restrictions.eq("namespace", getMetricStatisticsParams.getNamespace()));
      criteria = criteria.add(Restrictions.lt("timestamp", getMetricStatisticsParams.getEndTime()));
      criteria = criteria.add(Restrictions.ge("timestamp", getMetricStatisticsParams.getStartTime()));
      criteria = criteria.add(Restrictions.eq("dimensionHash", getMetricStatisticsParams.getDimensionHash()));
      if (getMetricStatisticsParams.getUnits() != null) {
        criteria = criteria.add(Restrictions.eq("units", getMetricStatisticsParams.getUnits()));
      }

      ProjectionList projectionList = Projections.projectionList();
      projectionList.add(Projections.max("sampleMax"));
      projectionList.add(Projections.min("sampleMin"));
      projectionList.add(Projections.sum("sampleSize"));
      projectionList.add(Projections.sum("sampleSum"));
      projectionList.add(Projections.groupProperty("units"));
      projectionList.add(Projections.groupProperty("timestamp"));
      criteria.setProjection(projectionList);
      criteria.addOrder(Order.asc("timestamp"));
      final ScrollableResults results = criteria.setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
      try {
        while ( results.next( ) ) {
          MetricEntity me = getMetricEntity( getMetricStatisticsParams.getAccountId( ), getMetricStatisticsParams.getMetricName( ), getMetricStatisticsParams.getNamespace( ), getMetricStatisticsParams.getMetricType( ), getMetricStatisticsParams.getDimensionHash( ), results );
          GetMetricStatisticsAggregationKey key = new GetMetricStatisticsAggregationKey( me, getMetricStatisticsParams.getStartTime( ), getMetricStatisticsParams.getPeriod( ), getMetricStatisticsParams.getDimensionHash( ) );
          MetricStatistics item = new MetricStatistics( me, getMetricStatisticsParams.getStartTime( ), getMetricStatisticsParams.getPeriod( ), getMetricStatisticsParams.getDimensions( ) );
          if ( !aggregationMap.containsKey( key ) ) {
            aggregationMap.put( key, item );
          } else {
            MetricStatistics totalSoFar = aggregationMap.get( key );
            totalSoFar.setSampleMax( Math.max( item.getSampleMax( ), totalSoFar.getSampleMax( ) ) );
            totalSoFar.setSampleMin( Math.min( item.getSampleMin( ), totalSoFar.getSampleMin( ) ) );
            totalSoFar.setSampleSize( totalSoFar.getSampleSize( ) + item.getSampleSize( ) );
            totalSoFar.setSampleSum( totalSoFar.getSampleSum( ) + item.getSampleSum( ) );
          }
        }
      } finally {
        results.close( );
      }
    }
    return Lists.newArrayList(aggregationMap.values());
  }

  private static MetricEntity getMetricEntity(ScrollableResults results) {
    Double sampleMax = (Double) results.get(0);
    Double sampleMin = (Double) results.get(1);
    Double sampleSize = (Double) results.get(2);
    Double sampleSum = (Double) results.get(3);
    Units resultUnits = (Units) results.get(4);
    Date timestamp = (Date) results.get(5);
    String accountId = (String) results.get(6);
    String metricName = (String) results.get(7);
    MetricType metricType = (MetricType) results.get(8);
    String namespace = (String) results.get(9);
    String hash = (String) results.get(10);
    MetricEntity me = MetricEntityFactory.getNewMetricEntity(metricType, hash);
    me.setAccountId(accountId);
    me.setNamespace(namespace);
    me.setMetricName(metricName);
    me.setMetricType(metricType);
    me.setDimensionHash(hash);
    me.setSampleMax(sampleMax);
    me.setSampleMin(sampleMin);
    me.setSampleSize(sampleSize);
    me.setSampleSum(sampleSum);
    me.setTimestamp(timestamp);
    me.setUnits(resultUnits);
    return me;
  }

  private static MetricEntity getMetricEntity(String accountId, String metricName, String namespace, MetricType metricType, String hash, ScrollableResults results) {
    Double sampleMax = (Double) results.get(0);
    Double sampleMin = (Double) results.get(1);
    Double sampleSize = (Double) results.get(2);
    Double sampleSum = (Double) results.get(3);
    Units resultUnits = (Units) results.get(4);
    Date timestamp = (Date) results.get(5);
    MetricEntity me = MetricEntityFactory.getNewMetricEntity(metricType, hash);
    me.setAccountId(accountId);
    me.setNamespace(namespace);
    me.setMetricName(metricName);
    me.setMetricType(metricType);
    me.setDimensionHash(hash);
    me.setSampleMax(sampleMax);
    me.setSampleMin(sampleMin);
    me.setSampleSize(sampleSize);
    me.setSampleSum(sampleSum);
    me.setTimestamp(timestamp);
    me.setUnits(resultUnits);
    return me;
  }

  public static Collection<MetricEntity> getAllMetrics() {
    ArrayList<MetricEntity> allResults = new ArrayList<MetricEntity>();
    for (Class c : MetricEntityFactory.getAllClassesForEntitiesGet()) {
      try (final TransactionResource db = Entities.transactionFor(c)) {
        Criteria criteria = Entities.createCriteria(c);
        criteria = criteria.addOrder( Order.asc("timestamp") );
        criteria = criteria.addOrder( Order.asc("id") );
        Collection dbResults = criteria.list();
        for (Object result : dbResults) {
          allResults.add((MetricEntity) result);
        }
        db.commit();
      }
    }
    return allResults;
  }

  public static void addMetricBatch(List<SimpleMetricEntity> dataBatch) {
    ArrayList<MetricEntity> metricEntities = new ArrayList<MetricEntity>();
    for (SimpleMetricEntity simpleMetricEntity: dataBatch) {
      validateMetricQueueItem(simpleMetricEntity);
      metricEntities.addAll(hash(simpleMetricEntity));
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
    } else if (simpleMetricEntity.getDimensionMap().size() > AbstractPersistentWithDimensions.MAX_DIM_NUM) {
      throw new IllegalArgumentException("Too many dimensions for metric, "
        + simpleMetricEntity.getDimensionMap().size());
    }
    simpleMetricEntity.setTimestamp(MetricUtils.stripSeconds(simpleMetricEntity.getTimestamp()));
  }
}
