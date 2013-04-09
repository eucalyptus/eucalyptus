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

package com.eucalyptus.cloudwatch.domain.absolute;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EntityTransaction;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class AbsoluteMetricHelper {
  public static class MetricDifferenceInfo {
    private Double valueDifference;
    private Long elapsedTimeInMillis;
    public Double getValueDifference() {
      return valueDifference;
    }
    public Long getElapsedTimeInMillis() {
      return elapsedTimeInMillis;
    }
    public MetricDifferenceInfo(Double valueDifference, Long elapsedTimeInMillis) {
      this.valueDifference = valueDifference;
      this.elapsedTimeInMillis = elapsedTimeInMillis;
    }
    
  }
  public static MetricDifferenceInfo calculateDifferenceSinceLastEvent(String namespace, String metricName, String dimensionName, String dimensionValue, Date newTimestamp, Double newMetricValue) {
    MetricDifferenceInfo returnValue = null;
    EntityTransaction db = Entities.get(AbsoluteMetricHistory.class);
    try {
      Criteria criteria = Entities.createCriteria(AbsoluteMetricHistory.class)
          .add( Restrictions.eq( "namespace", namespace ) )
          .add( Restrictions.eq( "metricName", metricName ) )
          .add( Restrictions.eq( "dimensionName", dimensionName ) )
          .add( Restrictions.eq( "dimensionValue", dimensionValue ) );
      AbsoluteMetricHistory lastEntity = (AbsoluteMetricHistory) criteria.uniqueResult();
      if (lastEntity == null) {
        // first data point, add it and return null (nothing to diff against)
        lastEntity = new AbsoluteMetricHistory();
        lastEntity.setNamespace(namespace);
        lastEntity.setMetricName(metricName);
        lastEntity.setDimensionName(dimensionName);
        lastEntity.setDimensionValue(dimensionValue);
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        Entities.persist(lastEntity);
        returnValue =  null;
      } else {
        // TODO: should we assume all metrics are increasing?
        long elapsedTimeInMillis = Math.abs(newTimestamp.getTime() - lastEntity.getTimestamp().getTime());
        double valueDifference = Math.abs(newMetricValue - lastEntity.getLastMetricValue());
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        returnValue = new MetricDifferenceInfo(valueDifference, elapsedTimeInMillis);
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
