/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights
 *   Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.cloudwatch.workflow.alarms;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntityFactory;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;

public class AlarmStateEvaluationDispatcher implements Runnable {
  private static final Logger LOG = Logger.getLogger(AlarmStateEvaluationDispatcher.class);
  ExecutorService executorService;

  public AlarmStateEvaluationDispatcher(ExecutorService executorService) {
    super();
    this.executorService = executorService;
  }

  @Override
  public void run() {
    if (!CloudWatchConfigProperties.isDisabledCloudWatchService() && Bootstrap.isOperational( ) && Topology.isEnabledLocally( CloudWatchBackend.class )) {
      LOG.debug("Kicking off AlarmStateEvaluationDispatcher");

      int size = 0;
      long before = 0;
      List<List<AlarmEntity>> resultsList = null;
      try (final TransactionResource db = Entities.transactionFor(AlarmEntity.class)) {
        Criteria criteria = Entities.createCriteria(AlarmEntity.class);
        List<AlarmEntity> results = (List<AlarmEntity>) criteria.list();
        resultsList = makeResultsList(results);
        before = System.currentTimeMillis();
        size = results.size();
      }
      try {
        if (resultsList != null) {
          ExecutorCompletionService executorCompletionService = new ExecutorCompletionService(executorService);
          Set<Future> futures = Sets.newHashSet();
          for (List<AlarmEntity> alarmEntityList: resultsList) {
            futures.add(executorCompletionService.submit(new AlarmStateEvaluationWorker(alarmEntityList), new Object()));
          }
          Future completedFuture;
          while (futures.size() > 0) {
            completedFuture = executorCompletionService.take();
            futures.remove(completedFuture);
            try {
              completedFuture.get();
            } catch (ExecutionException e) {
              Throwable cause = e.getCause();
              LOG.error(cause);
            }
          }
          long after = System.currentTimeMillis();
          LOG.debug("Done evaluating " + size + " alarms, time = " + (after - before) + " ms");
        }
      } catch (InterruptedException e) {
        LOG.debug(e);
      }
    }
  }

  private List<List<AlarmEntity>> makeResultsList(List<AlarmEntity> results) {
    Multimap<Class, AlarmEntity> classMultiMap = LinkedListMultimap.create();
    for (AlarmEntity alarmEntity: results) {
      classMultiMap.put(MetricEntityFactory.getClassForEntitiesGet(alarmEntity.getMetricType(), MetricManager.hash(alarmEntity.getDimensionMap())), alarmEntity);
    }
    List<Iterator<List<AlarmEntity>>> iterators = Lists.newArrayList();
    for (Class clazz: classMultiMap.keySet()) {
      iterators.add(Iterables.partition(classMultiMap.get(clazz), 100).iterator());
    }
    List<List<AlarmEntity>> retVal = Lists.newArrayList();
    boolean atLeastOneMightHaveMore = true;
    while (atLeastOneMightHaveMore) {
      atLeastOneMightHaveMore = false;
      for (Iterator<List<AlarmEntity>> iterator : iterators) {
        if (iterator.hasNext()) {
          atLeastOneMightHaveMore = true;
          retVal.add(iterator.next());
        }
      }
    }
    return retVal;
  }
}
