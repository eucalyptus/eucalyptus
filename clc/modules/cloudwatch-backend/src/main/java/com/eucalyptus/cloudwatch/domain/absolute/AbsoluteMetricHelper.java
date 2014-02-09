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
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricCache;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue.AbsoluteMetricCacheKey;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class AbsoluteMetricHelper {
  private static final Logger LOG = Logger.getLogger(AbsoluteMetricHelper.class);
  private static final long MAX_DIFFERENCE_DURATION_MS = TimeUnit.MINUTES.toMillis(15L); // 15 minutes, also max reporting time
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
  public static MetricDifferenceInfo calculateDifferenceSinceLastEvent(AbsoluteMetricCache cache, String namespace, String metricName, String dimensionName, String dimensionValue, Date newTimestamp, Double newMetricValue) {
    LOG.trace("namespace="+namespace+",metricName="+metricName+",dimensionName="+dimensionName+",dimensionValue="+dimensionValue+",newTimestamp="+newTimestamp+",newMetricValue="+newMetricValue);
    MetricDifferenceInfo returnValue = null;
    AbsoluteMetricHistory lastEntity = cache.lookup(namespace, metricName, dimensionName, dimensionValue);
    if (lastEntity == null) {
      // first data point, add it and return null (nothing to diff against)
      LOG.trace("First entry");
      lastEntity = new AbsoluteMetricHistory();
      lastEntity.setNamespace(namespace);
      lastEntity.setMetricName(metricName);
      lastEntity.setDimensionName(dimensionName);
      lastEntity.setDimensionValue(dimensionValue);
      lastEntity.setTimestamp(newTimestamp);
      lastEntity.setLastMetricValue(newMetricValue);
      Entities.persist(lastEntity);
      cache.put(namespace, metricName, dimensionName, dimensionValue, lastEntity);
      returnValue =  null;
    } else {
      double TOLERANCE = 0.0000001; // arbitrary to check double "equality"
      long elapsedTimeInMillis = newTimestamp.getTime() - lastEntity.getTimestamp().getTime();
      LOG.trace("lastTimestamp="+lastEntity.getTimestamp());
      double valueDifference = newMetricValue - lastEntity.getLastMetricValue();
      if (elapsedTimeInMillis < 0) {
        LOG.trace("earlier point, kicking out");
        // a negative value of elapsedTimeInMillis means this data point is not useful
        return null;
      } else if (elapsedTimeInMillis == 0) {
        if (Math.abs(valueDifference) > TOLERANCE) {
          LOG.warn("Getting different values " + newMetricValue + " and " + lastEntity.getLastMetricValue() + " for absolute metric " + metricName + " at the same timestamp " + newTimestamp + ", keeping the second value.");
        }
        return null; // not a useful data point either
      } else if (elapsedTimeInMillis > MAX_DIFFERENCE_DURATION_MS) { 
        // Too much time has passed, a useful data point, but we will not report the 'difference'.  We will reset.
        LOG.trace("too much time has passed, (" + elapsedTimeInMillis + " ms), starting over");
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        returnValue = null;
      } else if (elapsedTimeInMillis > 0) { 
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        if (valueDifference < -TOLERANCE) { // value has gone "down" (or down more than the TOLERANCE)
          // if the value difference is negative (i.e. has gone down, the assumption is that the NC has restarted, and the new
          // value started from some time in the past.  Best thing to do here is either assume it is a first point again, or
          // assume the previous point had a 0 value.  Not sure which is the better choice, but for now, we will make it a "first"
          // point again
          returnValue = null;
        } else { // truncate differences within TOLERANCE to zero
          if (Math.abs(valueDifference) < TOLERANCE) {
            valueDifference = 0.0;
          } 
          returnValue = new MetricDifferenceInfo(valueDifference, elapsedTimeInMillis);
        }
      }
      if (returnValue != null) {
        LOG.trace("new values=valueDifference="+valueDifference+",elapsedTimeInMillis="+elapsedTimeInMillis);
      } else {
        LOG.trace("sending null value out");
      }
    }
    return returnValue;
  }
 
  public static MetricDifferenceInfo calculateDifferenceSinceLastEvent(String namespace, String metricName, String dimensionName, String dimensionValue, Date newTimestamp, Double newMetricValue) {
    LOG.trace("namespace="+namespace+",metricName="+metricName+",dimensionName="+dimensionName+",dimensionValue="+dimensionValue+",newTimestamp="+newTimestamp+",newMetricValue="+newMetricValue);
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
        LOG.trace("First entry");
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
        double TOLERANCE = 0.0000001; // arbitrary to check double "equality"
        long elapsedTimeInMillis = newTimestamp.getTime() - lastEntity.getTimestamp().getTime();
        LOG.trace("lastTimestamp="+lastEntity.getTimestamp());
        double valueDifference = newMetricValue - lastEntity.getLastMetricValue();
        if (elapsedTimeInMillis < 0) {
          LOG.trace("earlier point, kicking out");
          // a negative value of elapsedTimeInMillis means this data point is not useful
          returnValue = null;
        } else if (elapsedTimeInMillis == 0) {
          if (Math.abs(valueDifference) > TOLERANCE) {
            LOG.warn("Getting different values " + newMetricValue + " and " + lastEntity.getLastMetricValue() + " for absolute metric " + metricName + " at the same timestamp " + newTimestamp + ", keeping the second value.");
          }
          returnValue = null; // not a useful data point either
        } else if (elapsedTimeInMillis > MAX_DIFFERENCE_DURATION_MS) { 
          // Too much time has passed, a useful data point, but we will not report the 'difference'.  We will reset.
          LOG.trace("too much time has passed, (" + elapsedTimeInMillis + " ms), starting over");
          lastEntity.setTimestamp(newTimestamp);
          lastEntity.setLastMetricValue(newMetricValue);
          returnValue = null;
        } else if (elapsedTimeInMillis > 0) { 
          lastEntity.setTimestamp(newTimestamp);
          lastEntity.setLastMetricValue(newMetricValue);
          if (valueDifference < -TOLERANCE) { // value has gone "down" (or down more than the TOLERANCE)
            // if the value difference is negative (i.e. has gone down, the assumption is that the NC has restarted, and the new
            // value started from some time in the past.  Best thing to do here is either assume it is a first point again, or
            // assume the previous point had a 0 value.  Not sure which is the better choice, but for now, we will make it a "first"
            // point again
            returnValue = null;
          } else { // truncate differences within TOLERANCE to zero
            if (Math.abs(valueDifference) < TOLERANCE) {
              valueDifference = 0.0;
            } 
            returnValue = new MetricDifferenceInfo(valueDifference, elapsedTimeInMillis);
          }
        }
        if (returnValue != null) {
          LOG.trace("new values=valueDifference="+valueDifference+",elapsedTimeInMillis="+elapsedTimeInMillis);
        } else {
          LOG.trace("sending null value out");
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

  /**
   * Delete all absolute metric history before a certain date
   * @param before the date to delete before (inclusive)
   */
  public static void deleteAbsoluteMetricHistory(Date before) {
    EntityTransaction db = Entities.get(AbsoluteMetricHistory.class);
    try {
      Map<String, Date> criteria = new HashMap<String, Date>();
      criteria.put("before", before);
      Entities.deleteAllMatching(AbsoluteMetricHistory.class, "WHERE timestamp < :before", criteria);
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
  }
}  
