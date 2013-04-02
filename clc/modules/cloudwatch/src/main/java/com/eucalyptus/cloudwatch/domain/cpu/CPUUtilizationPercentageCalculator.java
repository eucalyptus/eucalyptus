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

package com.eucalyptus.cloudwatch.domain.cpu;

import java.util.Date;

import javax.persistence.EntityTransaction;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class CPUUtilizationPercentageCalculator {
  public static Double calculateCPUUtilizationSinceLastEvent(String instanceId, Date newTimestamp, Double newUsageTimeMillisDouble) {
    Long newUsageTimeMillis = (newUsageTimeMillisDouble == null) ? null : Long.valueOf((long) newUsageTimeMillisDouble.longValue());
    Double returnValue = null;
    EntityTransaction db = Entities.get(CPUUtilizationEntity.class);
    try {
      Criteria criteria = Entities.createCriteria(CPUUtilizationEntity.class)
          .add( Restrictions.eq( "instanceId" , instanceId ) );
      CPUUtilizationEntity cpuUtilizationEntity = (CPUUtilizationEntity) criteria.uniqueResult();
      if (cpuUtilizationEntity == null) {
        // first data point, add it and return null (nothing to diff against)
        cpuUtilizationEntity = new CPUUtilizationEntity();
        cpuUtilizationEntity.setInstanceId(instanceId);
        cpuUtilizationEntity.setTimestamp(newTimestamp);
        cpuUtilizationEntity.setMachineUsageMilliseconds(newUsageTimeMillis);
        Entities.persist(cpuUtilizationEntity);
        returnValue =  null;
      } else {
        long elapsedTime = Math.abs(newTimestamp.getTime() - cpuUtilizationEntity.getTimestamp().getTime());
        long elapsedMachineTime = Math.abs(newUsageTimeMillis - cpuUtilizationEntity.getMachineUsageMilliseconds());
        cpuUtilizationEntity.setTimestamp(newTimestamp);
        cpuUtilizationEntity.setMachineUsageMilliseconds(newUsageTimeMillis);
        // TODO: should we reject this new data point if it is in the past or shows a smaller usage of time?
        if (elapsedTime == 0L) {
          returnValue =  0.0; // avoid division by zero
        } else {
          returnValue = 100.0 * ((double) elapsedMachineTime / (double) elapsedTime); 
        }
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
    return returnValue;
  }
}