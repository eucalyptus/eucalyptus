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

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.persistence.EntityTransaction;

import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

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
      EntityTransaction db = Entities.get(AlarmEntity.class);
      try {
        Criteria criteria = Entities.createCriteria(AlarmEntity.class);
        List<AlarmEntity> results = (List<AlarmEntity>) criteria.list();
        for (AlarmEntity alarmEntity: results) {
          LOG.debug("Submitting job for " + alarmEntity.getAlarmName());
          executorService.submit(new AlarmStateEvaluationWorker(alarmEntity.getAccountId(), alarmEntity.getAlarmName()));
        }
        db.commit();
      } catch (RuntimeException ex) {
        Logs.extreme().error(ex, ex); // TODO the exception will be swallowed...
        throw ex;
      } finally {
        if (db.isActive())
          db.rollback();
      }
    }
  }
}
