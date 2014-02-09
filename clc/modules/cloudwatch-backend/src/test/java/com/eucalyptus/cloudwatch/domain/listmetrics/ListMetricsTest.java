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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.junit.Ignore;

import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.google.common.collect.Lists;


@Ignore("Manual development test")
public class ListMetricsTest {
  private static final Logger LOG = Logger.getLogger(ListMetricsTest.class);
  // The following situation is being set up
  // There are two accounts: THIS and THAT
  // So account ids are account_this and account_that
  // There are allowed values for metricName:
  // metric_name_this, metric_name_that, and metric_name_both
  // metric_name_both will be used by both accounts.  [This allows for
  // more than one value of metric name per account and also a metric
  // name that is used by more than one account.
  // Similarly there will be namespaces of
  // namespace_this, namespace_that, and namespace_both.
  //
  //
  // This means: for example, we may have the following metric combinations:
  // account_this, metric_name_this, namespace_this
  // account_this, metric_name_this, namespace_both
  // account_this, metric_name_both, namespace_this
  // account_this, metric_name_both, namespace_both
  // account_that, metric_name_that, namespace_that
  // account_that, metric_name_that, namespace_both
  // account_that, metric_name_both, namespace_that
  // account_that, metric_name_both, namespace_both
  //
  // This allows for 8 different metric combinations before dimensions
  // are taken into account.
  // Dimension names (if they are present) will always be:
  // dimension_1, dimension_2, dimension_3, ... dimension_N (where n is a
  // parameter.
  // Dimension values will be literally dimension_this, dimension_that, or
  // dimension_both.  The semantics will be the same as above.
  // Fixing a metric (say one of them under account_this), we have three
  // possibilities for a given dimension:  not included, dimension_this, or
  // dimension_both.  This means we have 3 possibilities for each of n dimensions,
  // Taking everything into account, this is 3^n possible dimension combinations.
  // That is 8 * 3^n total dimensions all together, including the differences
  // in accounts, namespaces, and metric names.  All of account_that metrics 
  // will be added after all of account_this metrics.  This allows for a 
  // way to predict for a given search parameter how many metrics will be
  // returned, and will exhaust all possible combinations.
  public static void testReadDelete() {
    testReadDelete(10);
  }
  public static void testReadDelete(int n) {
    try {
      ListMetricManager.deleteAllMetrics();
      // TODO make assertSize like a junit test
      LOG.fatal("Assertion Result: " + assertSize(0, ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
      Date start = new Date();

      String accountId = "account_this";
      List<List<DimensionEntity>> dimensionChoices = new ArrayList<List<DimensionEntity>>();
      for (int i=1;i<=n;i++) {
        // create choices for dimensions
        DimensionEntity d1 = new DimensionEntity("dimension_" + i, "dimension_this");
        DimensionEntity d2 = new DimensionEntity("dimension_" + i, "dimension_both");
        dimensionChoices.add(Lists.newArrayList(d1, d2));
      }

      List<String> metricNames = Lists.newArrayList("metric_name_this", "metric_name_both");
      List<String> namespaces = Lists.newArrayList("namespace_this", "namespace_both");
      for (String metricName : metricNames) {
        for (String namespace : namespaces) {
          for (Collection<DimensionEntity> dimensions: new PowerChoiceIterable<DimensionEntity>(dimensionChoices)) {
            ListMetricManager.addMetric(accountId, metricName, namespace, toDimensionMap(dimensions), MetricType.Custom);
          }
        }
      }

      Date middle = new Date();
      accountId = "account_that";
      dimensionChoices = new ArrayList<List<DimensionEntity>>();
      for (int i=1;i<=n;i++) {
        // create choices for dimensions
        DimensionEntity d1 = new DimensionEntity("dimension_" + i, "dimension_that");
        DimensionEntity d2 = new DimensionEntity("dimension_" + i, "dimension_both");
        dimensionChoices.add(Lists.newArrayList(d1, d2));
      }
      metricNames = Lists.newArrayList("metric_name_that", "metric_name_both");
      namespaces = Lists.newArrayList("namespace_that", "namespace_both");
      for (String metricName : metricNames) {
        for (String namespace : namespaces) {
          for (Collection<DimensionEntity> dimensions: new PowerChoiceIterable<DimensionEntity>(dimensionChoices)) {
            ListMetricManager.addMetric(accountId, metricName, namespace, toDimensionMap(dimensions), MetricType.Custom);
          }
        }
      }

      Date end = new Date();
      LOG.fatal("start=" + start);
      LOG.fatal("middle=" + middle);
      LOG.fatal("end=" + end);

      // now check all getters ... and predictive power
      List<String> accountNames = new ArrayList<String>();
      accountNames.add(null);
      accountNames.add("account_this");
      accountNames.add("account_that");
      metricNames = new ArrayList<String>();
      metricNames.add(null);
      metricNames.add("metric_name_this");
      metricNames.add("metric_name_that");
      metricNames.add("metric_name_both");
      namespaces = new ArrayList<String>();
      namespaces.add(null);
      namespaces.add("namespace_this");
      namespaces.add("namespace_that");
      namespaces.add("namespace_both");
      List<Date> afters = new ArrayList<Date>();
      afters.add(null);
      afters.add(start);
      afters.add(middle);
      afters.add(end);
      List<Date> befores = new ArrayList<Date>();
      befores.add(null);
      befores.add(start);
      befores.add(middle);
      befores.add(end);
      dimensionChoices = new ArrayList<List<DimensionEntity>>();
      for (int i=1;i<=n;i++) {
        // create choices for dimensions
        DimensionEntity d1 = new DimensionEntity("dimension_" + i, "dimension_that");
        DimensionEntity d2 = new DimensionEntity("dimension_" + i, "dimension_both");
        DimensionEntity d3 = new DimensionEntity("dimension_" + i, "dimension_this");
        dimensionChoices.add(Lists.newArrayList(d1, d2, d3));
      }
      int totalReads = 0;
      int totalExpectedReads = 3 * 4 * 4 * 4 * 4 * intPow(4,n);
      for (String accountName: accountNames) {
        for (String metricName: metricNames) {
          for (String namespace: namespaces) {
            for (Date after: afters) {
              for (Date before: befores) {
                for (Collection<DimensionEntity> dimensions: new PowerChoiceIterable<DimensionEntity>(dimensionChoices)) {
                  Collection<ListMetric> metrics = ListMetricManager.listMetrics(accountName, metricName, namespace, toDimensionMap(dimensions), after, before, null, null);
                  int numValues = predictedResults(accountName, metricName, 
                      namespace, toDimensionMap(dimensions), 
                      after, before, start, middle, end, n);
                  boolean checkAccuracy = assertSize(numValues, metrics);
                  if (!checkAccuracy) {
                    LOG.fatal("Expected: " + numValues + ", got " + (metrics == null ? 0 : metrics.size()));
                    LOG.fatal(paramsToString(accountName, metricName, 
                      namespace, toDimensionMap(dimensions), 
                      after, before, start, middle, end, n));
                  }
                  totalReads++;
                  if (totalReads % 1000 == 0) {
                    LOG.fatal("Read " + totalReads + " of " + totalExpectedReads);
                  }
                }
              }
            }
          }
        }
      }
      LOG.fatal("Assertion Result: " + assertSize(intPow(2,3) * intPow(3,n), ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
      ListMetricManager.deleteMetrics(start);
      LOG.fatal("Assertion Result: " + assertSize(intPow(2,3) * intPow(3,n), ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
      ListMetricManager.deleteMetrics(middle);
      LOG.fatal("Assertion Result: " + assertSize(intPow(2,2) * intPow(3,n), ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
      ListMetricManager.deleteMetrics(end);
      LOG.fatal("Assertion Result: " + assertSize(0, ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
    } catch (Throwable ex) {
      LOG.fatal(ex, ex);
      ex.printStackTrace();
    }
  }

  private static Object paramsToString(String accountName, String metricName,
      String namespace, Map<String, String> dimensionMap, Date after,
      Date before, Date start, Date middle, Date end, int n) {
    StringBuilder sb = new StringBuilder();
    sb.append("accountName = " + accountName);
    sb.append(", metricName = " + metricName);
    sb.append(", namespace = " + namespace);
    if (end.equals(after)) {
      sb.append(", after = end");
    }
    if (middle.equals(after)) {
      sb.append(", after = middle");
    }
    if (start.equals(after)) {
      sb.append(", after = start");
    }
    if (after == null) {
      sb.append(", after = null");
    }
    if (end.equals(before)) {
      sb.append(", before = end");
    }
    if (middle.equals(before)) {
      sb.append(", before = middle");
    }
    if (start.equals(before)) {
      sb.append(", before = start");
    }
    if (before == null) {
      sb.append(", before = null");
    }
    sb.append(", dimensions = " + dimensionMap);
    return sb.toString();
  }
  private static Map<String, String> toDimensionMap(
      Collection<DimensionEntity> dimensions) {
    Map<String, String> retVal = new HashMap<String, String>();
    for (DimensionEntity simpleDimension: dimensions) {
      retVal.put(simpleDimension.getName(), simpleDimension.getValue());
    }
    return retVal;
  }

  private static boolean assertSize(int size,Collection<ListMetric> metricCollection) {
    return ((metricCollection == null && size == 0) || 
        (metricCollection.size() == size));
  }
  private static int predictedResults(String accountId, String metricName, 
      String namespace, Map<String,String> dimensions, 
      Date after, Date before, Date start, Date middle, Date end, int n) {
    // Assumptions: about date values
    // start = a non-null value before we start
    // middle = a non-null value between inserts of account_this and account_that
    // end = a non-null value after we insert all values
    // after and before can be null but if not must be one of the above 3 dates.

    // How do we determine how many matches we can have?
    // It depends on how many degrees of freedom we have
    // If we have all degrees of freedom, the total return is
    // 2^3 * 3^n, where n = the number of dimensions we pass in.
    // The 2 choice degrees of freedom are: accountId, metricName, and namespace
    // The 3 choice degrees of freedom are: each dimension: not in, or 
    // one of two values
    // The accountId is tricky as there are many things that can restrict it:
    // a) it's own value
    // b) any of the other values containing a 'this' or 'that' 
    // c) date ranges.
    // In particular if the search query contains an account id that removes
    // that degree of freedom.  Any values that contain 'this' forces the
    // account id to 'account_this'.  Similarly any values that contain
    // 'that' forces the accout id to 'account_that'
    // This can result in a 'no results' situation.
    // For dates, start must mean 'before account_this' data is entered
    // middle must mean 'between account entries'
    // end must mean 'after account_that data is entered'
    // The following values for after and before will show all possibilities
    // after  | before | result                       |  addressed in clause
    //--------+--------+------------------------------+---------------------
    // null   | null   | no restriction on account_id | Clause D
    // null   | start  | no values                    | Clause A
    // null   | middle | account_id is account_this   | Clause E
    // null   | end    | no restriction on account_id | Clause E
    // start  | null   | no restriction on account_id | Clause E
    // start  | start  | no values                    | Clause A
    // start  | middle | account_id is account_this   | Clause E
    // start  | end    | no restriction on account_id | Clause D
    // middle | null   | account_id is account_that   | Clause C
    // middle | start  | no values                    | Clause A
    // middle | middle | no values                    | Clause B
    // middle | end    | account_id is account_that   | Clause C
    // end    | null   | no values                    | Clause A
    // end    | start  | no values                    | Clause A
    // end    | middle | no values                    | Clause A
    // end    | end    | no values                    | Clause A
    boolean referencesAccountThis = false;
    boolean referencesAccountThat = false;
    if (end.equals(after) || start.equals(before)) { // (Clause A)
      return 0;
    } if (middle.equals(after) && middle.equals(before)) { // (Clause B)
      return 0;
    } else if (middle.equals(after) && 
        ((null == before) || end.equals(before))) { // (Clause C)
      referencesAccountThat = true;
    } else if (middle.equals(before) && 
        ((null == after) || start.equals(after))) { // (Clause C)
      referencesAccountThis = true;
    } else { // (Clause E)
      ; // no restrictions 
    }
    // now we check degrees of freedom.  (account id is the last check)
    int twoChoiceDegreesOfFreedom = 0;
    Collection<String> values = new HashSet<String>();
    if (metricName != null) {
      values.add(metricName);
    } else {
      twoChoiceDegreesOfFreedom++;
    }
    if (namespace != null) {
      values.add(namespace);
    } else {
      twoChoiceDegreesOfFreedom++;
    }

    if (accountId != null) {
      values.add(accountId);
    } 
    
    // we do the degrees of freedom check for account id last.  It depends on
    // other values
    
    // now check dimension degrees of freedom
    int threeChoiceDegreesOfFreedom = n;
    if (dimensions != null) {
      values.addAll(dimensions.values());
      threeChoiceDegreesOfFreedom -= dimensions.size();
    }
    for (String value:values) {
      if (value.endsWith("this")) {
        referencesAccountThis = true;
      }
      if (value.endsWith("that")) {
        referencesAccountThat = true;
      }
    }
    // finally check accountId degrees of freedom (only if null & this/that not referenced)  
    if (!referencesAccountThis && 
        !referencesAccountThat && (accountId == null)) {
      twoChoiceDegreesOfFreedom++;
    }
    // if this and that are referenced, nothing
    if (referencesAccountThis && referencesAccountThat) {
      return 0;
    } else { 
      return intPow(2, twoChoiceDegreesOfFreedom) * intPow(3, threeChoiceDegreesOfFreedom);
    }
  }


  private static int intPow(int base, int power) {
    int result = 1;
    for (int i=0;i<power;i++) {
      result *= base;
    }
    return result;
  }

  private static class PowerChoiceIterable<T> implements Iterable<Collection<T>> {
    private int[] listSizes;
    private List<List<T>> internalChoiceList;
    public PowerChoiceIterable(List<? extends Collection<T>> choiceList) {
      listSizes = new int[choiceList.size()];
      // todo: deal with illegal argument exceptions later...
      int index = 0;
      internalChoiceList = new ArrayList<List<T>>();
      for (Collection<T> choice: choiceList) {
        List<T> copy = Lists.newArrayList();
        copy.addAll(choice);
        internalChoiceList.add(copy);
        listSizes[index++] = copy.size();
      }
    }
    @Override
    public Iterator<Collection<T>> iterator() {
      return new PowerChoiceIterator<T>(listSizes, internalChoiceList);
    }
  }

  private static class PowerChoiceIterator<T> implements Iterator<Collection<T>> {
    int[] listSizes;
    int[] currentState;
    List<List<T>> internalChoiceList;

    boolean done = false;
    public PowerChoiceIterator(int[] listSizes,
        List<List<T>> internalChoiceList) {
      this.internalChoiceList = internalChoiceList;
      this.listSizes = listSizes;
      this.currentState = new int[listSizes.length];
    }

    @Override
    public boolean hasNext() {
      return !done;
    }

    @Override
    public Collection<T> next() {
      if (done) throw new NoSuchElementException();
      int index = 0;
      ArrayList<T> result = new ArrayList<T>();
      for (List<T> choice: internalChoiceList) {
        if (currentState[index] != listSizes[index]) {
          result.add(choice.get(currentState[index]));
        }
        index++;
      }
      done = advance();
      return result;
    }

    private boolean advance() {
      int position = currentState.length - 1;
      while (position >= 0 && currentState[position] == listSizes[position]) {
        position--;
      }
      if (position < 0) return true;
      currentState[position]++;
      for (int index = position+1;index < currentState.length; index++) {
        currentState[index] = 0;
      }
      return false;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();

    }
  }

  private static void insertMetricsWithDimensions(int i) throws Exception {
    String accountId = "account1";
    String metricName = "metric"+i;
    String namespace = "namespace"+i;
    Map<String,String> dimensions = new HashMap<String,String>();
    for (int j=1;j<=i;j++) {
      String name = "name" + j;
      String value = "value" + j;
      dimensions.put(name,  value);
    }
    ListMetricManager.addMetric(accountId, metricName, namespace, dimensions, MetricType.Custom);
  }

  public static void testInsertUpdate() throws Exception {
    ListMetricManager.deleteAllMetrics();
    // TODO make assertSize like a junit test
    LOG.fatal("Assertion Result: " + assertSize(0, ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
    for (int i=2;i<=10;i++) {
      insertMetricsWithDimensions(i);
    }
    Thread.sleep(10000L);
    for (int i=10;i>=2;i--) {
      insertMetricsWithDimensions(i);
    }
    LOG.fatal("Assertion Result: " + assertSize(10, ListMetricManager.listMetrics(null, null, null, null, null, null, null, null)));
  }

}
