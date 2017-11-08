/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudwatch

import com.eucalyptus.auth.Accounts
import com.eucalyptus.cloudwatch.common.internal.domain.DimensionEntity
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntityFactory
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units
import com.eucalyptus.cloudwatch.service.queue.listmetrics.ListMetricQueueItem
import com.eucalyptus.entities.Entities
import com.eucalyptus.records.Logs
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import org.apache.log4j.Logger
import org.hibernate.id.UUIDHexGenerator
import org.hibernate.jdbc.Work
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

import javax.persistence.EntityTransaction
import java.sql.Connection
import java.sql.SQLException
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat

/**
 * Created by ethomas on 9/2/15.
 */
class CloudWatchBulkDataImportTool {

  /*
   README: To use this tool, modify the endTime final date, DURATION_MINS, and NUM_INSTANCES values as appropriate.

  */

  // TODO: the below class is abusing groovy's private access (getTransaction() is private in Entities.java).
  // We either need to add this method to Entities.java or do something else.  This whole method abuses
  // the knowledge of postgres as the db for faster access
  static class EntitiesExtended extends Entities {
    public static <T> void doWork(final T object, Work work) {
      getTransaction(object).txState.getSession().doWork(work);
    }

    public static void insertData(Object txObj, String header, InputStream body) throws Exception {
      // Sorry.  Groovy doesn't like autoclosable unfortunately
      EntityTransaction db = Entities.get(txObj);
      try {
        doWork(txObj, new Work() {
          @Override
          public void execute(Connection connection) throws SQLException {
            CopyManager copyManager = null;
            copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
            if (copyManager == null) {
              LOG.fatal("Database is incompatible with the PostgreSQL COPY command");
              return;
            }
            try {
              copyManager.copyIn(header, body);
            } catch (IOException e) {
              LOG.error(e, e);
            }
          }
        });
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

  private static final Logger LOG = Logger.getLogger(CloudWatchBulkDataImportTool.class);

  private String escape(String s) {
    if (s == null) return "\\n";
    s = s.replace("\\", "\\\\");
    s = s.replace("\t", "\\t");
    s = s.replace("\r", "\\r");
    s = s.replace("\n", "\\n");
    return s;
  }

  // 2015-05-24 11:58:06.812
  private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  private static UUIDHexGenerator uuidHexGenerator = new UUIDHexGenerator();

  private Integer notNull(Integer m) {
    if (m == null) return 0;
    return m;
  }

  String dateFormat(java.util.Date date) {
    if (date == null) return "\\N";
    return sdf.format(date);
  }


  static abstract class ListMetricField {
    abstract String name();

    abstract value(ListMetric listMetric);
  }

  static abstract class MetricDataField {
    abstract String name();

    abstract value(MetricEntity metricEntity);
  }


  List<ListMetricField> listMetricFields = Lists.newArrayList(
    new ListMetricField() {
      @Override
      String name() { return "id"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getId()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "creation_timestamp"; }

      @Override
      String value(ListMetric listMetric) { return dateFormat(listMetric.getCreationTimestamp()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "last_update_timestamp"; }

      @Override
      String value(ListMetric listMetric) { return dateFormat(listMetric.getLastUpdateTimestamp()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "metadata_perm_uuid"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getNaturalId()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "version"; }

      @Override
      String value(ListMetric listMetric) { return notNull(listMetric.getVersion()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_10_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim10Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_10_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim10Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_1_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim1Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_1_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim1Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_2_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim2Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_2_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim2Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_3_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim3Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_3_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim3Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_4_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim4Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_4_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim4Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_5_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim5Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_5_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim5Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_6_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim6Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_6_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim6Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_7_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim7Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_7_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim7Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_8_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim8Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_8_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim8Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_9_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim9Name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "dim_9_value"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim9Value()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "account_id"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getAccountId()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "metric_name"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getMetricName()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "metric_type"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getMetricType().name()); }
    },
    new ListMetricField() {
      @Override
      String name() { return "namespace"; }

      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getNamespace()); }
    }
  );

  List<MetricDataField> putMetricDataFields = Lists.newArrayList(
    new MetricDataField() {
      @Override
      String name() { return "id"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getId()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "account_id"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getAccountId()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "dimension_hash"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getDimensionHash()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "metric_name"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getMetricName()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "metric_type"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getMetricType().name()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "namespace"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getNamespace()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "sample_max"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleMax())); }
    },
    new MetricDataField() {
      @Override
      String name() { return "sample_min"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleMin())); }
    },
    new MetricDataField() {
      @Override
      String name() { return "sample_size"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleSize())); }
    },
    new MetricDataField() {
      @Override
      String name() { return "sample_sum"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleSum())); }
    },
    new MetricDataField() {
      @Override
      String name() { return "timestamp"; }

      @Override
      String value(MetricEntity metricEntity) { return dateFormat(metricEntity.getTimestamp()); }
    },
    new MetricDataField() {
      @Override
      String name() { return "units"; }

      @Override
      String value(MetricEntity metricEntity) { return escape(metricEntity.getUnits().name()); }
    }
  );


  List<String> addMetrics(Collection<MetricEntity> meList) throws Exception {
    List<String> lines = Lists.newArrayList();
    for (MetricEntity me : meList) {
      List<String> fieldValues = Lists.newArrayList();
      for (MetricDataField metricDataField : putMetricDataFields) {
        fieldValues.add(metricDataField.value(me));
      }
      lines.add(Joiner.on("\t").join(fieldValues));
    }
    lines.add("\\.");
    return lines;
  }

  List<String> addListMetrics(Collection<ListMetric> listMetrics) throws Exception {
    List<String> lines = Lists.newArrayList();
    Map<ListMetricQueueItem, ListMetric> removeDups = Maps.newLinkedHashMap();
    for (ListMetric listMetric : listMetrics) {
      removeDups.put(convertToListMetricQueueItem(listMetric), listMetric);
    }
    for (ListMetric listMetric : removeDups.values()) {
      List<String> fieldValues = Lists.newArrayList();
      for (ListMetricField listMetricField : listMetricFields) {
        fieldValues.add(listMetricField.value(listMetric));
      }
      lines.add(Joiner.on("\t").join(fieldValues));
    }
    lines.add("\\.");
    return lines;
  }

  ListMetricQueueItem convertToListMetricQueueItem(ListMetric item) {
    ListMetricQueueItem metricMetadata = new ListMetricQueueItem();
    metricMetadata.setAccountId(item.getAccountId());
    metricMetadata.setNamespace(item.getNamespace());
    metricMetadata.setMetricName(item.getMetricName());
    metricMetadata.setDimensionMap(item.getDimensionMap());
    metricMetadata.setMetricType(item.getMetricType());
    return metricMetadata;
  }

  String getTableName(String s) {
    return "eucalyptus_cloudwatch_backend.custom_metric_data_" + s;
  }

  String getTableNameListMetrics(String s) {
    return "eucalyptus_cloudwatch.list_metrics";
  }


  void putInstanceData(Multimap<String, MetricEntity> metricMap, List<ListMetric> listMetrics, String userId, Date date, String metricName, String imageId, String imageType, String instanceId, String unit, Double value) {
    List<List<DimensionEntity>> dimensionEntitiesList = Lists.newArrayList(
      Lists.newArrayList(), // no dimensions
      Lists.newArrayList(new DimensionEntity(name: "ImageId", value: imageId)),
      Lists.newArrayList(new DimensionEntity(name: "ImageType", value: imageType)),
      Lists.newArrayList(new DimensionEntity(name: "InstanceId", value: instanceId))
    );
    for (List<DimensionEntity> dimensionEntities : dimensionEntitiesList) {
      String dimensionHash = MetricManager.hash(dimensionEntities);
      MetricEntity metricEntity = MetricEntityFactory.getNewMetricEntity(MetricEntity.MetricType.Custom, dimensionHash);
      metricEntity.setId("XXX");
      metricEntity.setAccountId(userId);
      metricEntity.setMetricName(metricName);
      metricEntity.setNamespace("AWZ/EC2");
      metricEntity.setDimensionHash(dimensionHash);
      metricEntity.setMetricType(MetricEntity.MetricType.Custom);
      metricEntity.setUnits(Units.fromValue(unit));
      metricEntity.setTimestamp(date);
      metricEntity.setSampleMax(value);
      metricEntity.setSampleMin(value);
      metricEntity.setSampleSum(value);
      metricEntity.setSampleSize(1.0);
      metricMap.put(dimensionHash.substring(0, 1), metricEntity);

      ListMetric listMetric = new ListMetric();
      listMetric.setId("XXX");
      listMetric.setCreationTimestamp(date);
      listMetric.setLastUpdateTimestamp(date);
      listMetric.setNaturalId("YYY");
      listMetric.setAccountId(metricEntity.getAccountId());
      listMetric.setMetricName(metricEntity.getMetricName());
      listMetric.setMetricType(metricEntity.getMetricType());
      listMetric.setNamespace(metricEntity.getNamespace());
      listMetric.setDimensions(dimensionEntities);
      listMetrics.add(listMetric);

    }
  }

  void putVolumeData(Multimap<String, MetricEntity> metricMap, List<ListMetric> listMetrics, String userId, Date date, String metricName, String volumeId, String unit, Double value) {
    List<DimensionEntity> dimensionEntities = Lists.newArrayList(
      new DimensionEntity(name: "VolumeId", value: volumeId)
    );
    // one of each type
    String dimensionHash = MetricManager.hash(dimensionEntities);
    MetricEntity metricEntity = MetricEntityFactory.getNewMetricEntity(MetricEntity.MetricType.Custom, dimensionHash);
    metricEntity.setId("XXX");
    metricEntity.setAccountId(userId);
    metricEntity.setMetricName(metricName);
    metricEntity.setNamespace("AWZ/EBS");
    metricEntity.setDimensionHash(dimensionHash);
    metricEntity.setMetricType(MetricEntity.MetricType.Custom);
    metricEntity.setUnits(Units.fromValue(unit));
    metricEntity.setTimestamp(date);
    metricEntity.setSampleMax(value);
    metricEntity.setSampleMin(value);
    metricEntity.setSampleSum(value);
    metricEntity.setSampleSize(1.0);
    metricMap.put(dimensionHash.substring(0, 1), metricEntity);

    ListMetric listMetric = new ListMetric();
    listMetric.setId("XXX");
    listMetric.setCreationTimestamp(date);
    listMetric.setLastUpdateTimestamp(date);
    listMetric.setNaturalId("YYY");
    listMetric.setAccountId(metricEntity.getAccountId());
    listMetric.setMetricName(metricEntity.getMetricName());
    listMetric.setMetricType(metricEntity.getMetricType());
    listMetric.setNamespace(metricEntity.getNamespace());
    listMetric.setDimensions(dimensionEntities);
    listMetrics.add(listMetric);

  }

  public synchronized void runMyTest() throws Exception {

    final ImmutableMap<String, Integer> HASHES_AND_CLASSES =
      new ImmutableMap.Builder<String, Class<MetricEntity>>()
        .put("0", MetricEntityFactory.CustomMetricEntity0.class)
        .put("1", MetricEntityFactory.CustomMetricEntity1.class)
        .put("2", MetricEntityFactory.CustomMetricEntity2.class)
        .put("3", MetricEntityFactory.CustomMetricEntity3.class)
        .put("4", MetricEntityFactory.CustomMetricEntity4.class)
        .put("5", MetricEntityFactory.CustomMetricEntity5.class)
        .put("6", MetricEntityFactory.CustomMetricEntity6.class)
        .put("7", MetricEntityFactory.CustomMetricEntity7.class)
        .put("8", MetricEntityFactory.CustomMetricEntity8.class)
        .put("9", MetricEntityFactory.CustomMetricEntity9.class)
        .put("a", MetricEntityFactory.CustomMetricEntityA.class)
        .put("b", MetricEntityFactory.CustomMetricEntityB.class)
        .put("c", MetricEntityFactory.CustomMetricEntityC.class)
        .put("d", MetricEntityFactory.CustomMetricEntityD.class)
        .put("e", MetricEntityFactory.CustomMetricEntityE.class)
        .put("f", MetricEntityFactory.CustomMetricEntityF.class)
        .build();

    final int NUM_EBS_VOLUMES_PER_INSTANCE = 2;
    final long NUM_MILLISECONDS_BETWEEN_PUTS = 60000L;
    final int NUM_INSTANCES = 10000;
    final long DURATION_MINS =  14 * 24 * 60L;
    final Date endTime = new Date(sdf.parse("2015-09-16 00:00:00.000").getTime());
    final Date startTime = new Date(endTime.getTime() - DURATION_MINS * 60 * 1000L);

    final double MB_PER_SEC = 1024.0 * 1024.0 / 10000.0;
    DecimalFormat df = new DecimalFormat("00000");

    String admin = Accounts.lookupSystemAdmin().getAccountNumber();

    String vmType = "m1.fake";
    Date zero = new Date(0L);
    Multimap<String, MetricEntity> metricMap = LinkedListMultimap.create();
    List<ListMetric> listMetrics = Lists.newArrayList();
    for (int i = 0; i < NUM_INSTANCES; i++) {
      LOG.fatal(i);
      String instanceId = "i-" + df.format(i);
      String imageId = "emi-" + df.format(i);
      double baseFraction = ((double) i) / NUM_INSTANCES;
      putInstanceData(metricMap, listMetrics, admin, zero, "CPUUtilization", imageId, vmType, instanceId, "Percent", baseFraction * 100.0);
      putInstanceData(metricMap, listMetrics, admin, zero, "DiskReadBytes", imageId, vmType, instanceId, "Bytes", baseFraction * 25 * MB_PER_SEC);
      putInstanceData(metricMap, listMetrics, admin, zero, "DiskReadOps", imageId, vmType, instanceId, "Count", baseFraction * 10000);
      putInstanceData(metricMap, listMetrics, admin, zero, "DiskWriteBytes", imageId, vmType, instanceId, "Bytes", baseFraction * 35 * MB_PER_SEC);
      putInstanceData(metricMap, listMetrics, admin, zero, "DiskWriteOps", imageId, vmType, instanceId, "Count", baseFraction * 20000);
      putInstanceData(metricMap, listMetrics, admin, zero, "NetworkIn", imageId, vmType, instanceId, "Bytes", baseFraction * 50 * MB_PER_SEC);
      putInstanceData(metricMap, listMetrics, admin, zero, "NetworkOut", imageId, vmType, instanceId, "Bytes", baseFraction * 70 * MB_PER_SEC);
      putInstanceData(metricMap, listMetrics, admin, zero, "StatusCheckFailed", imageId, vmType, instanceId, "Count", 0);
      putInstanceData(metricMap, listMetrics, admin, zero, "StatusCheckFailed_Instance", imageId, vmType, instanceId, "Count", 0);
      putInstanceData(metricMap, listMetrics, admin, zero, "StatusCheckFailed_System", imageId, vmType, instanceId, "Count", 0);

      // Add some EBS metrics
      for (int j = 0; j < NUM_EBS_VOLUMES_PER_INSTANCE; j++) {
        String volumeId = "vol-" + df.format(i) + j;
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeIdleTime", volumeId, "Seconds", 0);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeQueueLength", volumeId, "Count", 0);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeReadBytes", volumeId, "Bytes", baseFraction * 25 * MB_PER_SEC);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeReadOps", volumeId, "Count", baseFraction * 10000);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeThroughputPercentage", volumeId, "Percent", 100);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeTotalReadTime", volumeId, "Seconds", baseFraction * 2000);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeTotalWriteTime", volumeId, "Seconds", baseFraction * 1000);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeWriteBytes", volumeId, "Bytes", baseFraction * 35 * MB_PER_SEC);
        putVolumeData(metricMap, listMetrics, admin, zero, "VolumeWriteOps", volumeId, "Count", baseFraction * 20000);
      }
    }
    Map<String, List<String>> putMetricDataLinesMap = Maps.newHashMap();
    for (String hash : metricMap.keySet()) {
      putMetricDataLinesMap.put(hash, addMetrics(aggregate(metricMap.get(hash))));
    }
    List<String> listMetricsLines = addListMetrics(listMetrics);

    String zeroDate = sdf.format(zero);

    String putMetricDataFieldNamesDelimiter = "";
    String putMetricDataFieldNames = "(";
    for (MetricDataField metricDataField : putMetricDataFields) {
      putMetricDataFieldNames += putMetricDataFieldNamesDelimiter + metricDataField.name();
      putMetricDataFieldNamesDelimiter = ",";
    }
    putMetricDataFieldNames += ")";

    String listMetricFieldNamesDelimiter = "";
    String listMetricFieldNames = "(";
    for (ListMetricField listMetricField : listMetricFields) {
      listMetricFieldNames += listMetricFieldNamesDelimiter + listMetricField.name();
      listMetricFieldNamesDelimiter = ",";
    }
    listMetricFieldNames += ")";

    for (long l = startTime.getTime(); l < endTime.getTime(); l += NUM_MILLISECONDS_BETWEEN_PUTS) {
      Date currentTime = new Date(l);
      String currentDate = sdf.format(currentTime);
      LOG.fatal("Current time: " + currentTime);
      for (String hash : HASHES_AND_CLASSES.keySet()) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(bOut));
        for (String line : putMetricDataLinesMap.get(hash)) {
          writer.println(line.replace("XXX", uuidHexGenerator.generate(null, null)).replace("YYY", UUID.randomUUID().toString()).replace(zeroDate, currentDate));
        }
        writer.close();
        String header = "COPY " + getTableName(hash) + " " + putMetricDataFieldNames + " FROM STDIN";
        final InputStream body = new ByteArrayInputStream(bOut.toByteArray());
        EntitiesExtended.insertData(HASHES_AND_CLASSES.get(hash), header, body);
      }
    }
    String endDate = sdf.format(endTime);
    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(bOut));
    for (String line : listMetricsLines) {
      writer.println(line.replace("XXX", uuidHexGenerator.generate(null, null)).replace("YYY", UUID.randomUUID().toString()).replace(zeroDate, endDate));
    }
    writer.close();
    String header = "COPY " + getTableNameListMetrics() + " " + listMetricFieldNames + " FROM STDIN";
    final InputStream body = new ByteArrayInputStream(bOut.toByteArray());
    EntitiesExtended.insertData(ListMetric.class, header, body);
    LOG.fatal("done");
  }

  public static List<MetricEntity> aggregate(List<MetricEntity> dataBatch) {
    HashMap<AggregationKey, MetricEntity> aggregationMap = Maps.newHashMap();
    for (MetricEntity item : dataBatch) {
      AggregationKey key = new AggregationKey(item);
      if (!aggregationMap.containsKey(key)) {
        aggregationMap.put(key, item);
      } else {
        MetricEntity totalSoFar = aggregationMap.get(key);
        totalSoFar.setSampleMax(Math.max(item.getSampleMax(), totalSoFar.getSampleMax()));
        totalSoFar.setSampleMin(Math.min(item.getSampleMin(), totalSoFar.getSampleMin()));
        totalSoFar.setSampleSize(totalSoFar.getSampleSize() + item.getSampleSize());
        totalSoFar.setSampleSum(totalSoFar.getSampleSum() + item.getSampleSum());
      }
    }
    return Lists.newArrayList(aggregationMap.values());
  }


  static class AggregationKey {

    private String accountId;
    private String dimensionHash;
    private String metricName;
    private MetricType metricType;
    private String namespace;
    private Date timestamp;
    private Units units;

    public AggregationKey(MetricEntity item) {
      this.accountId = item.getAccountId();
      this.dimensionHash = item.getDimensionHash();
      this.metricName = item.getMetricName();
      this.metricType = item.getMetricType();
      this.namespace = item.getNamespace();
      this.timestamp = item.getTimestamp();
      this.units = item.getUnits();
    }

    boolean equals(o) {
      if (this.is(o)) return true
      if (getClass() != o.class) return false

      AggregationKey that = (AggregationKey) o

      if (accountId != that.accountId) return false
      if (dimensionHash != that.dimensionHash) return false
      if (metricName != that.metricName) return false
      if (metricType != that.metricType) return false
      if (namespace != that.namespace) return false
      if (timestamp != that.timestamp) return false
      if (units != that.units) return false

      return true
    }

    int hashCode() {
      int result
      result = (accountId != null ? accountId.hashCode() : 0)
      result = 31 * result + (dimensionHash != null ? dimensionHash.hashCode() : 0)
      result = 31 * result + (metricName != null ? metricName.hashCode() : 0)
      result = 31 * result + (metricType != null ? metricType.hashCode() : 0)
      result = 31 * result + (namespace != null ? namespace.hashCode() : 0)
      result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0)
      result = 31 * result + (units != null ? units.hashCode() : 0)
      return result
    }
  }
}

new Thread() {
  public void run() {
    try {
      new CloudWatchBulkDataImportTool().runMyTest()
    } catch (Exception e) {
      CloudWatchBulkDataImportTool.LOG.error(e, e);
    }
  }
}.start();



