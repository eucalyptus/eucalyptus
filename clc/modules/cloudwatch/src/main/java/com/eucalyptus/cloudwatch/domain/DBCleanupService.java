package com.eucalyptus.cloudwatch.domain;

import java.util.Date;

import org.apache.log4j.Logger;

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
    LOG.info("Done cleaning up cloudwatch db");
  }

}
