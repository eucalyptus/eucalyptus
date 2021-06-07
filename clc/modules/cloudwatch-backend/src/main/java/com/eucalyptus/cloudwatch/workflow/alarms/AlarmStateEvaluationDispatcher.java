/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
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
        @SuppressWarnings( "unchecked" )
        List<AlarmEntity> results = (List<AlarmEntity>) criteria.list();
        resultsList = makeResultsList(results);
        before = System.currentTimeMillis();
        size = results.size();
      }
      try {
        if (resultsList != null) {
          ExecutorCompletionService<Object> executorCompletionService = new ExecutorCompletionService<>(executorService);
          Set<Future<?>> futures = Sets.newHashSet();
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
