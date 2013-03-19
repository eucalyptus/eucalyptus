package com.eucalyptus.cloudwatch.domain.alarms;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;

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
    LOG.info("Kicking off AlarmStateEvaluationDispatcher");
    EntityTransaction db = Entities.get(AlarmEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(AlarmEntity.class);
      List<AlarmEntity> results = (List<AlarmEntity>) criteria.list();
      for (AlarmEntity alarmEntity: results) {
        LOG.info("Submitting job for " + alarmEntity.getAlarmName());
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
