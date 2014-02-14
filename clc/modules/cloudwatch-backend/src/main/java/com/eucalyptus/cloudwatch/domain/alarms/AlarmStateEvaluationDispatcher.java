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
package com.eucalyptus.cloudwatch.domain.alarms;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.backend.CloudWatchBackendService;
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
    if (!CloudWatchBackendService.DISABLE_CLOUDWATCH_SERVICE && Bootstrap.isOperational( ) && Topology.isEnabledLocally( CloudWatchBackend.class )) {
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
