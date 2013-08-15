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
package com.eucalyptus.cloudwatch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.SimpleMetricEntity;

public class TestAggregationQueue {

  private static final double TOLERANCE = 0.000000001; 
  @Test
  public void testDistinctAccounts() {
    final Date now = MetricManager.stripSeconds(new Date());
    // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account"+(i % 2)); 
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap); // just a random dimension order
      mqi.setMetricName("metric1");
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits(Units.None);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have accountId account1 
    // and one should have accountId0
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getAccountId().equals("account0")) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account0");
    assertEquals(even.getDimensionMap(), hashMap); // really either should be ok
    assertEquals(even.getMetricName(), "metric1");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace1");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.Custom);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), now);
    assertEquals(odd.getUnits(), Units.None);
  }

  @Test
  public void testDistinctMetricNames() {
    final Date now = MetricManager.stripSeconds(new Date());
    // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap); // just a random dimension order
      mqi.setMetricName("metric"+(i % 2));
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits(Units.None);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have metricName metric1 
    // and one should have metric0
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getMetricName().equals("metric0")) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account1");
    assertEquals(even.getDimensionMap(), hashMap); // really either should be ok
    assertEquals(even.getMetricName(), "metric0");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace1");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.Custom);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), now);
    assertEquals(odd.getUnits(), Units.None);
  }


  @Test
  public void testDistinctMetricTypes() {
    final Date now = MetricManager.stripSeconds(new Date());
    // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap); // just a random dimension order
      mqi.setMetricName("metric1");
      mqi.setMetricType((i % 2) == 0 ? MetricType.Custom: MetricType.System);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits(Units.None);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have metricType Custom 
    // and one should have System
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getMetricType().equals(MetricType.Custom)) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account1");
    assertEquals(even.getDimensionMap(), hashMap); // really either should be ok
    assertEquals(even.getMetricName(), "metric1");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace1");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.System);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), now);
    assertEquals(odd.getUnits(), Units.None);
  }
  
  @Test
  public void testDistinctNamespaces() {
    final Date now = MetricManager.stripSeconds(new Date());
    // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap); // just a random dimension order
      mqi.setMetricName("metric1");
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace"+(i%2));
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits(Units.None);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have namespace namespace0
    // and one should have namespace0
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getNamespace().equals("namespace0")) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account1");
    assertEquals(even.getDimensionMap(), hashMap); // really either should be ok
    assertEquals(even.getMetricName(), "metric1");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace0");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.Custom);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), now);
    assertEquals(odd.getUnits(), Units.None);
  }

  @Test
  public void testDistinctTimestamps() {
    final Date now = MetricManager.stripSeconds(new Date());
    final Date later = MetricManager.stripSeconds(new Date(now.getTime() + 120000L)); // two minutes
      // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap); // just a random dimension order
      mqi.setMetricName("metric1");
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp((i % 2 == 0) ? now : later);
      mqi.setUnits(Units.None);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have timestamp now
    // and one should have later
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getTimestamp().equals(now)) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account1");
    assertEquals(even.getDimensionMap(), hashMap); // really either should be ok
    assertEquals(even.getMetricName(), "metric1");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace1");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.Custom);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), later);
    assertEquals(odd.getUnits(), Units.None);
  }

  @Test
  public void testDistinctUnits() {
    final Date now = MetricManager.stripSeconds(new Date());
      // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap); // just a random dimension order
      mqi.setMetricName("metric1");
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits((i % 2 == 0) ? Units.None : Units.Count);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have units none
    // and one should have count
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getUnits().equals(Units.None)) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account1");
    assertEquals(even.getDimensionMap(), hashMap); // really either should be ok
    assertEquals(even.getMetricName(), "metric1");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace1");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.Custom);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), now);
    assertEquals(odd.getUnits(), Units.Count);
  }

  @Test
  public void testDistinctDimensionMaps() {
    final Date now = MetricManager.stripSeconds(new Date());
      // throw some different dimension order in there...
    final Map<String,String> hashMap = new HashMap<String, String>();
    hashMap.put("dim1", "val1");
    hashMap.put("dim2", "val2");
    final Map<String,String> treeMap = new TreeMap<String, String>();
    treeMap.put("dim2", "val2");
    treeMap.put("dim1", "val1");
  
    ArrayList<SimpleMetricEntity> list = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 2 == 0) ? null: hashMap); 
      mqi.setMetricName("metric1");
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits(Units.None);
      list.add(mqi);
    }

    List<SimpleMetricEntity> aggregateList = MetricDataQueue.aggregate(list);
    // should be two items...
    assertEquals(2, aggregateList.size());
    // since we are not sure which order, one should have dimensionMap null
    // and one should have hashMap
    SimpleMetricEntity odd, even;
    if (aggregateList.get(0).getDimensionMap() == null) {
      even = aggregateList.get(0);
      odd = aggregateList.get(1);
    } else {
      even = aggregateList.get(1);
      odd = aggregateList.get(0);
    }
    // even totals are 0,2,4,6,8 so total = 20, max = 8, min = 0, count = 5
    assertEquals(even.getSampleMax().doubleValue(), 8.0, TOLERANCE);
    assertEquals(even.getSampleMin().doubleValue(), 0.0, TOLERANCE);
    assertEquals(even.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(even.getSampleSum().doubleValue(), 20.0, TOLERANCE);
    assertEquals(even.getAccountId(), "account1");
    assertEquals(even.getDimensionMap(), null); 
    assertEquals(even.getMetricName(), "metric1");
    assertEquals(even.getMetricType(), MetricType.Custom);
    assertEquals(even.getNamespace(), "namespace1");
    assertEquals(even.getTimestamp(), now);
    assertEquals(even.getUnits(), Units.None);

    // odd totals are 1,3,5,7,9 so total = 25, max = 9, min = 1, count = 5
    assertEquals(odd.getSampleMax().doubleValue(), 9.0, TOLERANCE);
    assertEquals(odd.getSampleMin().doubleValue(), 1.0, TOLERANCE);
    assertEquals(odd.getSampleSize().doubleValue(), 5.0, TOLERANCE);
    assertEquals(odd.getSampleSum().doubleValue(), 25.0, TOLERANCE);
    assertEquals(odd.getAccountId(), "account1");
    assertEquals(odd.getDimensionMap(), treeMap); // really either should be ok
    assertEquals(odd.getMetricName(), "metric1");
    assertEquals(odd.getMetricType(), MetricType.Custom);
    assertEquals(odd.getNamespace(), "namespace1");
    assertEquals(odd.getTimestamp(), now);
    assertEquals(odd.getUnits(), Units.None);
  }

  /*
  public static void main(String[] args) {
    
    ArrayList<SimpleMetricEntity> list6 = new ArrayList<SimpleMetricEntity>();
    for (int i=0;i<10;i++) {
      SimpleMetricEntity mqi = new SimpleMetricEntity();
      mqi.setAccountId("account1");
      mqi.setDimensionMap((i % 3 == 0) ? hashMap: treeMap);
      mqi.setMetricName("metric1");
      mqi.setMetricType(MetricType.Custom);
      mqi.setNamespace("namespace1");
      mqi.setSampleMax((double) i);
      mqi.setSampleMin((double) i);
      mqi.setSampleSize((double) 1);
      mqi.setSampleSum((double) i);
      mqi.setTimestamp(now);
      mqi.setUnits((i % 2 == 0) ? Units.None : Units.Count);
      mqi.setUserId("user1");
      list6.add(mqi);
    }
*/  

}
