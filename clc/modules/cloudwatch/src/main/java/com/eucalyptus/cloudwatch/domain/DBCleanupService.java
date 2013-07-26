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
package com.eucalyptus.cloudwatch.domain;

import java.util.Date;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmManager;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricManager;

public class DBCleanupService implements Runnable {
  Logger LOG = Logger.getLogger(DBCleanupService.class);
  public DBCleanupService() {
  }

  @Override
  public void run() {
    LOG.info("Calling cloudwatch db cleanup service");
    Date twoWeeksAgo = new Date(System.currentTimeMillis() - 2 * 7 * 24 * 60 * 60 * 1000L);
    try {
      MetricManager.deleteMetrics(twoWeeksAgo);
    } catch (Exception ex) {
      LOG.error(ex);
      LOG.error(ex, ex);
    }
    try {
      ListMetricManager.deleteMetrics(twoWeeksAgo);
    } catch (Exception ex) {
      LOG.error(ex);
      LOG.error(ex, ex);
    }
    try {
      AlarmManager.deleteAlarmHistory(twoWeeksAgo);
    } catch (Exception ex) {
      LOG.error(ex);
      LOG.error(ex, ex);
    }
    try {
      AbsoluteMetricHelper.deleteAbsoluteMetricHistory(twoWeeksAgo);
    } catch (Exception ex) {
      LOG.error(ex);
      LOG.error(ex, ex);
    }
    LOG.info("Done cleaning up cloudwatch db");
  }

}
