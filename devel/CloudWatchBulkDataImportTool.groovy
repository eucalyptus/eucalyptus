/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch

import com.eucalyptus.auth.Accounts
import com.eucalyptus.cloudwatch.common.internal.domain.DimensionEntity
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric
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
   Much better performance can be gained by removing indexes before doing the inserts.
   The following psql commands will drop the indexes.  (The index names may be different.
   Check "\d custom_metric_data_0" for example to see the indexes on custom_metric_data_0.  The tables used here
   are custom_metric_data_0, custom_metric_data_1, ..., custom_metric_data_9, custom_metric_data_a, ...,custom_metric_data_f

     set search_path to eucalyptus_cloudwatch_backend;

     -- primary keys
     alter table custom_metric_data_0 drop constraint custom_metric_data_0_pkey;
     alter table custom_metric_data_1 drop constraint custom_metric_data_1_pkey;
     alter table custom_metric_data_2 drop constraint custom_metric_data_2_pkey;
     alter table custom_metric_data_3 drop constraint custom_metric_data_3_pkey;
     alter table custom_metric_data_4 drop constraint custom_metric_data_4_pkey;
     alter table custom_metric_data_5 drop constraint custom_metric_data_5_pkey;
     alter table custom_metric_data_6 drop constraint custom_metric_data_6_pkey;
     alter table custom_metric_data_7 drop constraint custom_metric_data_7_pkey;
     alter table custom_metric_data_8 drop constraint custom_metric_data_8_pkey;
     alter table custom_metric_data_9 drop constraint custom_metric_data_9_pkey;
     alter table custom_metric_data_a drop constraint custom_metric_data_a_pkey;
     alter table custom_metric_data_b drop constraint custom_metric_data_b_pkey;
     alter table custom_metric_data_c drop constraint custom_metric_data_c_pkey;
     alter table custom_metric_data_d drop constraint custom_metric_data_d_pkey;
     alter table custom_metric_data_e drop constraint custom_metric_data_e_pkey;
     alter table custom_metric_data_f drop constraint custom_metric_data_f_pkey;

     -- uniqueness constraint on metadata_perm_uuid (double check index name)

     alter table custom_metric_data_0 drop constraint uk_2qawd63vl9mrg10l8ilcungna;
     alter table custom_metric_data_1 drop constraint uk_cfh9o6nfpioyu4jdrffccsxlp;
     alter table custom_metric_data_2 drop constraint uk_1xb1x4490uknw90s4op43wd98;
     alter table custom_metric_data_3 drop constraint uk_qme9bb2nxy6ucduiafrv5l4gq;
     alter table custom_metric_data_4 drop constraint uk_i4u2jwqio3t2ty3f8sojr7r6f;
     alter table custom_metric_data_5 drop constraint uk_bb6fhrcubw1h7kbcbcrfpid7h;
     alter table custom_metric_data_6 drop constraint uk_9prctmyh3ugfv6abnadfm7dag;
     alter table custom_metric_data_7 drop constraint uk_94s2ur2hoajlergs5xt5lb36i;
     alter table custom_metric_data_8 drop constraint uk_911u5pk4b34bkgphgnhmxjhhx;
     alter table custom_metric_data_9 drop constraint uk_l6j3lvyt50qo0y3hgvc5qd23s;
     alter table custom_metric_data_a drop constraint uk_ggalgvbwo6pueiie1n4pccvwl;
     alter table custom_metric_data_b drop constraint uk_o3lcsvc1m8o8qljc826qkalds;
     alter table custom_metric_data_c drop constraint uk_nuls7hrnefxiiuy11istw4br;
     alter table custom_metric_data_d drop constraint uk_idpea8hk5kfcx3c27c1pjs3hx;
     alter table custom_metric_data_e drop constraint uk_pve0e7sni3ol6bqt0hpoykkxb;
     alter table custom_metric_data_f drop constraint uk_8krjy59uuqsbb6jmamj2xi89k;

   For the list_metrics case, the following will remove those indexes  (this step is less
   important as list metrics is only used once)

     -- list_metrics
     set search_path to eucalyptus_cloudwatch;

     alter table list_metrics drop constraint list_metrics_pkey;
     alter table list_metrics drop constraint uk_32ixhisfm7i1bur5lf83mu75r;


   To restore the above indexes (after load), the following code can be used within psql

     set search_path to eucalyptus_cloudwatch_backend;

     -- primary keys
     alter table custom_metric_data_0 add constraint custom_metric_data_0_pkey primary key (id);
     alter table custom_metric_data_1 add constraint custom_metric_data_1_pkey primary key (id);
     alter table custom_metric_data_2 add constraint custom_metric_data_2_pkey primary key (id);
     alter table custom_metric_data_3 add constraint custom_metric_data_3_pkey primary key (id);
     alter table custom_metric_data_4 add constraint custom_metric_data_4_pkey primary key (id);
     alter table custom_metric_data_5 add constraint custom_metric_data_5_pkey primary key (id);
     alter table custom_metric_data_6 add constraint custom_metric_data_6_pkey primary key (id);
     alter table custom_metric_data_7 add constraint custom_metric_data_7_pkey primary key (id);
     alter table custom_metric_data_8 add constraint custom_metric_data_8_pkey primary key (id);
     alter table custom_metric_data_9 add constraint custom_metric_data_9_pkey primary key (id);
     alter table custom_metric_data_a add constraint custom_metric_data_a_pkey primary key (id);
     alter table custom_metric_data_b add constraint custom_metric_data_b_pkey primary key (id);
     alter table custom_metric_data_c add constraint custom_metric_data_c_pkey primary key (id);
     alter table custom_metric_data_d add constraint custom_metric_data_d_pkey primary key (id);
     alter table custom_metric_data_e add constraint custom_metric_data_e_pkey primary key (id);
     alter table custom_metric_data_f add constraint custom_metric_data_f_pkey primary key (id);

     -- uniqueness constraint on metadata_perm_uuid
     alter table custom_metric_data_0 add constraint uk_2qawd63vl9mrg10l8ilcungna unique (metadata_perm_uuid);
     alter table custom_metric_data_1 add constraint uk_cfh9o6nfpioyu4jdrffccsxlp unique (metadata_perm_uuid);
     alter table custom_metric_data_2 add constraint uk_1xb1x4490uknw90s4op43wd98 unique (metadata_perm_uuid);
     alter table custom_metric_data_3 add constraint uk_qme9bb2nxy6ucduiafrv5l4gq unique (metadata_perm_uuid);
     alter table custom_metric_data_4 add constraint uk_i4u2jwqio3t2ty3f8sojr7r6f unique (metadata_perm_uuid);
     alter table custom_metric_data_5 add constraint uk_bb6fhrcubw1h7kbcbcrfpid7h unique (metadata_perm_uuid);
     alter table custom_metric_data_6 add constraint uk_9prctmyh3ugfv6abnadfm7dag unique (metadata_perm_uuid);
     alter table custom_metric_data_7 add constraint uk_94s2ur2hoajlergs5xt5lb36i unique (metadata_perm_uuid);
     alter table custom_metric_data_8 add constraint uk_911u5pk4b34bkgphgnhmxjhhx unique (metadata_perm_uuid);
     alter table custom_metric_data_9 add constraint uk_l6j3lvyt50qo0y3hgvc5qd23s unique (metadata_perm_uuid);
     alter table custom_metric_data_a add constraint uk_ggalgvbwo6pueiie1n4pccvwl unique (metadata_perm_uuid);
     alter table custom_metric_data_b add constraint uk_o3lcsvc1m8o8qljc826qkalds unique (metadata_perm_uuid);
     alter table custom_metric_data_c add constraint uk_nuls7hrnefxiiuy11istw4br unique (metadata_perm_uuid);
     alter table custom_metric_data_d add constraint uk_idpea8hk5kfcx3c27c1pjs3hx unique (metadata_perm_uuid);
     alter table custom_metric_data_e add constraint uk_pve0e7sni3ol6bqt0hpoykkxb unique (metadata_perm_uuid);
     alter table custom_metric_data_f add constraint uk_8krjy59uuqsbb6jmamj2xi89k unique (metadata_perm_uuid);

     -- list_metrics
     set search_path to eucalyptus_cloudwatch;

     alter table list_metrics add constraint list_metrics_pkey primary key (id);
     alter table list_metrics add constraint uk_32ixhisfm7i1bur5lf83mu75r unique (metadata_perm_uuid);

  */

  // TODO: the below class is abusing groovy's private access (getTransaction() is private in Entities.java).
  // We either need to add this method to Entities.java or do something else.  This whole method abuses
  // the knowledge of postgres as the db for faster access
  static class EntitiesExtended extends Entities {
    public static <T> void doWork( final T object, Work work ) {
      getTransaction( object ).txState.getSession( ).doWork(work);
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
    new ListMetricField ( ) {
      @Override
      String name() { return "creation_timestamp"; }
      @Override
      String value(ListMetric listMetric) { return dateFormat(listMetric.getCreationTimestamp()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "last_update_timestamp"; }
      @Override
      String value(ListMetric listMetric) { return dateFormat(listMetric.getLastUpdateTimestamp()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "metadata_perm_uuid"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getNaturalId()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "version"; }
      @Override
      String value(ListMetric listMetric) { return notNull(listMetric.getVersion()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_10_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim10Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_10_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim10Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_1_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim1Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_1_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim1Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_2_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim2Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_2_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim2Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_3_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim3Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_3_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim3Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_4_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim4Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_4_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim4Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_5_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim5Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_5_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim5Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_6_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim6Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_6_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim6Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_7_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim7Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_7_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim7Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_8_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim8Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_8_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim8Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_9_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim9Name()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "dim_9_value"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getDim9Value()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "account_id"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getAccountId()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "metric_name"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getMetricName()); }
    },
    new ListMetricField ( ) {
      @Override
      String name() { return "metric_type"; }
      @Override
      String value(ListMetric listMetric) { return escape(listMetric.getMetricType().name()); }
    },
    new ListMetricField ( ) {
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
      new MetricDataField ( ) {
        @Override
        String name() { return "creation_timestamp"; }
        @Override
        String value(MetricEntity metricEntity) { return dateFormat(metricEntity.getCreationTimestamp()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "last_update_timestamp"; }
        @Override
        String value(MetricEntity metricEntity) { return dateFormat(metricEntity.getLastUpdateTimestamp()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "metadata_perm_uuid"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getNaturalId()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "version"; }
        @Override
        String value(MetricEntity metricEntity) { return notNull(metricEntity.getVersion()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_10_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim10Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_10_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim10Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_1_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim1Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_1_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim1Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_2_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim2Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_2_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim2Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_3_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim3Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_3_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim3Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_4_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim4Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_4_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim4Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_5_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim5Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_5_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim5Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_6_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim6Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_6_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim6Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_7_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim7Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_7_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim7Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_8_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim8Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_8_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim8Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_9_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim9Name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dim_9_value"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDim9Value()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "account_id"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getAccountId()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "dimension_hash"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getDimensionHash()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "metric_name"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getMetricName()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "metric_type"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getMetricType().name()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "namespace"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getNamespace()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "sample_max"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleMax())); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "sample_min"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleMin())); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "sample_size"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleSize())); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "sample_sum"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(String.valueOf(metricEntity.getSampleSum())); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "timestamp"; }
        @Override
        String value(MetricEntity metricEntity) { return dateFormat(metricEntity.getTimestamp()); }
      },
      new MetricDataField ( ) {
        @Override
        String name() { return "units"; }
        @Override
        String value(MetricEntity metricEntity) { return escape(metricEntity.getUnits().name()); }
      }
  );


  List<String> addMetrics(Collection<MetricEntity> meList) throws Exception {
    List<String> lines = Lists.newArrayList();
    for (MetricEntity me: meList) {
      List<String> fieldValues = Lists.newArrayList();
      for (MetricDataField metricDataField : putMetricDataFields) {
        fieldValues.add(metricDataField.value(me));
      }
      lines.add(Joiner.on("\t").join(fieldValues));
    }
    lines.add("\\.");
    return lines;
  }

  List<String> addListMetrics(Collection<MetricEntity> meList, Date date) throws Exception {
    List<String> lines = Lists.newArrayList();
    Map<ListMetricQueueItem, ListMetric> removeDups = Maps.newLinkedHashMap();
    for (MetricEntity me: meList) {
      ListMetric listMetric = convertToListMetric(me, date);
      removeDups.put(convertToListMetricQueueItem(listMetric), listMetric);
    }
    for (ListMetric listMetric: removeDups.values()) {
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


  ListMetric convertToListMetric(MetricEntity metricEntity, Date date) {
    if (metricEntity == null) return null;
    ListMetric listMetric = new ListMetric();
    listMetric.setId(uuidHexGenerator.generate(null, null));
    listMetric.setCreationTimestamp(date);
    listMetric.setLastUpdateTimestamp(date);
    listMetric.setNaturalId(UUID.randomUUID( ).toString( ));
    listMetric.setVersion(metricEntity.getVersion());
    listMetric.setDimensions(metricEntity.getDimensions());
    listMetric.setAccountId(metricEntity.getAccountId());
    listMetric.setMetricName(metricEntity.getMetricName());
    listMetric.setMetricType(metricEntity.getMetricType());
    listMetric.setNamespace(metricEntity.getNamespace());
    return listMetric;
  }

  String getTableName(String s) {
    return "eucalyptus_cloudwatch_backend.custom_metric_data_" + s;
  }

  String getTableNameListMetrics(String s) {
    return "eucalyptus_cloudwatch.list_metrics";
  }


  void putInstanceData(Multimap<String, MetricEntity> map, String userId, Date date, String metricName, String imageId, String imageType, String instanceId, String unit,  Double value) {
    List<List<DimensionEntity>> dimensionEntitiesList = Lists.newArrayList(
        Lists.newArrayList(), // no dimensions
        Lists.newArrayList(new DimensionEntity(name: "ImageId", value: imageId)),
        Lists.newArrayList(new DimensionEntity(name: "ImageType", value: imageType)),
        Lists.newArrayList(new DimensionEntity(name: "InstanceId", value: instanceId))
    );
    for (List<DimensionEntity> dimensionEntities: dimensionEntitiesList) {
      String dimensionHash = MetricManager.hash(dimensionEntities);
      MetricEntity metricEntity = MetricEntityFactory.getNewMetricEntity(MetricEntity.MetricType.Custom, dimensionHash);
      metricEntity.setId("XXX");
      metricEntity.setAccountId(userId);
      metricEntity.setMetricName(metricName);
      metricEntity.setNamespace("AWZ/EC2");
      metricEntity.setDimensions(dimensionEntities);
      metricEntity.setDimensionHash(dimensionHash);
      metricEntity.setMetricType(MetricEntity.MetricType.Custom);
      metricEntity.setUnits(Units.fromValue(unit));
      metricEntity.setTimestamp(date);
      metricEntity.setSampleMax(value);
      metricEntity.setSampleMin(value);
      metricEntity.setSampleSum(value);
      metricEntity.setSampleSize(1.0);
      metricEntity.setCreationTimestamp(date);
      metricEntity.setLastUpdateTimestamp(date);
      metricEntity.setNaturalId("YYY");
      map.put(dimensionHash.substring(0, 1), metricEntity);
    }
  }

  void putVolumeData(Multimap<String, MetricEntity> map, String userId, Date date, String metricName, String volumeId, String unit,  Double value) {
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
    metricEntity.setDimensions(dimensionEntities);
    metricEntity.setDimensionHash(dimensionHash);
    metricEntity.setMetricType(MetricEntity.MetricType.Custom);
    metricEntity.setUnits(Units.fromValue(unit));
    metricEntity.setTimestamp(date);
    metricEntity.setSampleMax(value);
    metricEntity.setSampleMin(value);
    metricEntity.setSampleSum(value);
    metricEntity.setSampleSize(1.0);
    metricEntity.setCreationTimestamp(date);
    metricEntity.setLastUpdateTimestamp(date);
    metricEntity.setNaturalId("YYY");
    map.put(dimensionHash.substring(0, 1), metricEntity);
  }

  public synchronized void runMyTest() throws Exception {

    final ImmutableMap<String, Integer> HASHES_AND_CLASSES  =
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
    final Date endTime = new Date(sdf.parse("2015-09-06 00:00:00.000").getTime());
    final Date startTime = new Date(endTime.getTime() - DURATION_MINS * 60 * 1000L);

    final double MB_PER_SEC = 1024.0 * 1024.0 / 10000.0;
    DecimalFormat df = new DecimalFormat("00000");

    String admin = Accounts.lookupSystemAdmin().getAccountNumber();

    String vmType = "m1.fake";
    Date zero = new Date(0L);
    Multimap<String, MetricEntity> map = LinkedListMultimap.create();
    for (int i = 0; i < NUM_INSTANCES; i++) {
      LOG.fatal(i);
      String instanceId = "i-" + df.format(i);
      String imageId = "emi-" + df.format(i);
      double baseFraction = ((double) i) / NUM_INSTANCES;
      putInstanceData(map, admin, zero, "CPUUtilization", imageId, vmType, instanceId, "Percent", baseFraction * 100.0);
      putInstanceData(map, admin, zero, "DiskReadBytes", imageId, vmType, instanceId, "Bytes", baseFraction * 25 * MB_PER_SEC);
      putInstanceData(map, admin, zero, "DiskReadOps", imageId, vmType, instanceId, "Count", baseFraction * 10000);
      putInstanceData(map, admin, zero, "DiskWriteBytes", imageId, vmType, instanceId, "Bytes", baseFraction * 35 * MB_PER_SEC);
      putInstanceData(map, admin, zero, "DiskWriteOps", imageId, vmType, instanceId, "Count", baseFraction * 20000);
      putInstanceData(map, admin, zero, "NetworkIn", imageId, vmType, instanceId, "Bytes", baseFraction * 50 * MB_PER_SEC);
      putInstanceData(map, admin, zero, "NetworkOut", imageId, vmType, instanceId, "Bytes", baseFraction * 70 * MB_PER_SEC);
      putInstanceData(map, admin, zero, "StatusCheckFailed", imageId, vmType, instanceId, "Count", 0);
      putInstanceData(map, admin, zero, "StatusCheckFailed_Instance", imageId, vmType, instanceId, "Count", 0);
      putInstanceData(map, admin, zero, "StatusCheckFailed_System", imageId, vmType, instanceId, "Count", 0);

      // Add some EBS metrics
      for (int j = 0; j < NUM_EBS_VOLUMES_PER_INSTANCE; j++) {
        String volumeId = "vol-" + df.format(i) + j;
        putVolumeData(map, admin, zero, "VolumeIdleTime", volumeId, "Seconds", 0);
        putVolumeData(map, admin, zero, "VolumeQueueLength", volumeId, "Count", 0);
        putVolumeData(map, admin, zero, "VolumeReadBytes", volumeId, "Bytes", baseFraction * 25 * MB_PER_SEC);
        putVolumeData(map, admin, zero, "VolumeReadOps", volumeId, "Count", baseFraction * 10000);
        putVolumeData(map, admin, zero, "VolumeThroughputPercentage", volumeId, "Percent", 100);
        putVolumeData(map, admin, zero, "VolumeTotalReadTime", volumeId, "Seconds", baseFraction * 2000);
        putVolumeData(map, admin, zero, "VolumeTotalWriteTime", volumeId, "Seconds", baseFraction * 1000);
        putVolumeData(map, admin, zero, "VolumeWriteBytes", volumeId, "Bytes", baseFraction * 35 * MB_PER_SEC);
        putVolumeData(map, admin, zero, "VolumeWriteOps", volumeId, "Count", baseFraction * 20000);
      }
    }
    Map<String,List<String>> putMetricDataLinesMap = Maps.newHashMap();
    for (String hash : map.keySet()) {
      putMetricDataLinesMap.put(hash, addMetrics(map.get(hash)));
    }
    List<String> listMetricsLines = addListMetrics(map.values(), endTime);

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
    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(bOut));
    for (String line : listMetricsLines) {
      writer.println(line);
    }
    writer.close();
    String header = "COPY " + getTableNameListMetrics() + " " + listMetricFieldNames + " FROM STDIN";
    final InputStream body = new ByteArrayInputStream(bOut.toByteArray());
    EntitiesExtended.insertData(ListMetric.class, header, body);
    LOG.fatal("done");
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



