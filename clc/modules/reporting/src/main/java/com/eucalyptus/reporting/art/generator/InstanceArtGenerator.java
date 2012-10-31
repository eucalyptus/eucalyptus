/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.reporting.art.generator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.reporting.art.entity.*;
import com.eucalyptus.reporting.domain.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

public class InstanceArtGenerator extends AbstractArtGenerator {
  private static Logger log = Logger.getLogger( InstanceArtGenerator.class );

  /* Metric names */
  public static final String METRIC_NET_IN_BYTES   = "NetworkIn";
  public static final String METRIC_NET_OUT_BYTES  = "NetworkOut";
  public static final String METRIC_DISK_IN_BYTES  = "DiskReadBytes";
  public static final String METRIC_DISK_OUT_BYTES = "DiskWriteBytes";
  public static final String METRIC_DISK_READ_OPS  = "DiskReadOps";
  public static final String METRIC_DISK_WRITE_OPS = "DiskWriteOps";
  public static final String METRIC_CPU_USAGE_MS   = "CPUUtilization";
  public static final String METRIC_VOLUME_READ    = "VolumeTotalReadTime";
  public static final String METRIC_VOLUME_WRITE   = "VolumeTotalWriteTime";

  public static final String DIM_TOTAL    = "total";
  public static final String DIM_DEFAULT  = "default";
  public static final String DIM_EXTERNAL = "external";

  private static final long USAGE_SEARCH_PERIOD = TimeUnit.DAYS.toMillis( 12 );

  @Override
  public ReportArtEntity generateReportArt( final ReportArtEntity report ) {
    log.debug( "Generating report ART" );

    /* Find all instance create events. These will be used to populate the ART tree with instances etc,
     * if there is usage for them within the report boundaries (determined below).
     */
    final Map<String, ReportingInstanceCreateEvent> createEvents = Maps.newHashMap();
    foreachInstanceCreateEvent( report.getEndMs(), new Predicate<ReportingInstanceCreateEvent>() {
      @Override
      public boolean apply( final ReportingInstanceCreateEvent createEvent ) {
        final ReportingInstanceCreateEvent prevEvent = createEvents.put( createEvent.getUuid(), createEvent );
        if ( prevEvent != null && prevEvent.getTimestampMs() < createEvent.getTimestampMs() ) {
          createEvents.put( prevEvent.getUuid(), prevEvent ); // use first creation
        }
        return true;
      }
    } );

    /* Scan through usage events in order, and populate the ART tree with nodes and usage.
     */
    final Map<InstanceMetricDimensionKey, MetricPrevData> prevDataMap = Maps.newHashMap();
    final Map<String, InstanceUsageArtEntity> usageEntities = Maps.newHashMap();
    foreachInstanceUsageEvent(
        report.getBeginMs() - USAGE_SEARCH_PERIOD,
        report.getEndMs() + USAGE_SEARCH_PERIOD,
        new Predicate<ReportingInstanceUsageEvent>() {
          @Override
          public boolean apply( final ReportingInstanceUsageEvent event ) {
            final InstanceMetricDimensionKey key =
                new InstanceMetricDimensionKey( event.getUuid(), event.getMetric(),
                    event.getDimension() );
            final long eventMs = event.getTimestampMs();
            if ( event.getValue() == null ) return true;

            if ( !usageEntities.containsKey( event.getUuid() ) ) {
              usageEntities.put( event.getUuid(), new InstanceUsageArtEntity() );
            }
            final InstanceUsageArtEntity usageEntity = usageEntities.get( event.getUuid() );
            final ReportingInstanceCreateEvent createEvent = createEvents.get( event.getUuid() );
            if ( createEvent == null ) {
              log.error( "Usage event without create event:" + event.getUuid() );
              return true;
            }

            /* Populate the nodes in the tree for this usage, if the usage falls within report boundaries */
            if ( eventMs >= report.getBeginMs() || eventMs <= report.getEndMs() ) {
              if (!addParentNodes( report, createEvent, usageEntity )) {
                return true;
              }
            }

            /* Update the total usage in the usage art entity, for this uuid/metric/dimension combo.
             * Metric values are cumulative, so we must subtract each from the last. For this reason,
             * we must retain previous values of each uuid/metric/dim combo, the earliest and latest times
             * for each combo (to update the duration), and the sequence numbers (to detect sensor resets)
             */
            if ( !prevDataMap.containsKey( key ) ) {
              /* No prior value. Use usage from instance creation to present
               * Equivalent to inserting a zero-usage event at instance creation time
               *
               * Find the fraction of this period which falls within report boundaries. This is
               * needed because period boundaries do not align with report boundaries.
               */
              usageEntity.setDurationMs( Math.max( usageEntity.getDurationMs(),
                  overlap( report.getBeginMs(), report.getEndMs(), createEvent.getTimestampMs(), eventMs ) ) );
              Double fractionalVal = fractionalUsage( report.getBeginMs(), report.getEndMs(),
                  createEvent.getTimestampMs(), eventMs, event.getValue() );
              addMetricValueToUsageEntity( usageEntity, event.getMetric(), event.getDimension(),
                  fractionalVal );
              log.debug( String.format( "new metric time:%d-%d report:%d-%d uuid:%s metric:%s dim:%s val:%f fraction:%f",
                  createEvent.getTimestampMs(), eventMs, report.getBeginMs(), report.getEndMs(),
                  event.getUuid(), event.getMetric(), event.getDimension(), event.getValue(), fractionalVal ) );
              prevDataMap.put( key, new MetricPrevData( eventMs, eventMs, event.getValue(), event.getSequenceNum() ) );
            } else {
              /* Previous value exists */
              final MetricPrevData prevData = prevDataMap.get( key );

              /* We have a period (firstMs to now); update the instance duration if necessary */
              usageEntity.setDurationMs( Math.max( usageEntity.getDurationMs(),
                  overlap( report.getBeginMs(), report.getEndMs(), prevData.firstMs, eventMs ) ) );

              if ( event.getSequenceNum() < prevData.lastSeq || event.getSequenceNum()==0 ) {
                /* SENSOR RESET; we lost data; just take whatever amount greater than 0 */

                /* Find the fraction of this period which falls within report boundaries. */
                Double fractionalVal = fractionalUsage( report.getBeginMs(), report.getEndMs(),
                    prevData.lastMs, eventMs, event.getValue() );
                addMetricValueToUsageEntity( usageEntity, event.getMetric(), event.getDimension(),
                    fractionalVal );
                log.debug( String.format( "reset time:%d-%d report:%d-%d uuid:%s metric:%s dim:%s val:%f fraction:%f",
                    prevData.lastMs, eventMs, report.getBeginMs(), report.getEndMs(), event.getUuid(), event.getMetric(),
                    event.getDimension(), event.getValue(), fractionalVal ) );
              } else {
                /* Increase total by val minus lastVal */

                /* Find the fraction of this period which falls within report boundaries. */
                Double fractionalVal = fractionalUsage( report.getBeginMs(), report.getEndMs(),
                    prevData.lastMs, eventMs, event.getValue() - prevData.lastVal );
                addMetricValueToUsageEntity( usageEntity, event.getMetric(), event.getDimension(),
                    fractionalVal );
                log.debug( String.format( "event time:%d-%d report:%d-%d uuid:%s metric:%s dim:%s val:%f lastVal:%f fraction:%f",
                    prevData.lastMs, eventMs, report.getBeginMs(), report.getEndMs(), event.getUuid(), event.getMetric(),
                    event.getDimension(), event.getValue(), prevData.lastVal, fractionalVal ) );
              }
              prevDataMap.put( key, new MetricPrevData( prevData.firstMs, eventMs, event.getValue(), event.getSequenceNum() ) );
            }
            return true;
          }
        } );


    /* Perform totals and summations
       */
    for ( final AvailabilityZoneArtEntity zone : report.getZones().values() ) {
      final UsageTotalsArtEntity zoneUsage = zone.getUsageTotals();
      for ( final AccountArtEntity account : zone.getAccounts().values() ) {
        final UsageTotalsArtEntity accountUsage = account.getUsageTotals();
        for ( final UserArtEntity user : account.getUsers().values() ) {
          final UsageTotalsArtEntity userUsage = user.getUsageTotals();
          for ( final InstanceArtEntity instance : user.getInstances().values() ) {
            updateUsageTotals( userUsage, instance );
            updateUsageTotals( accountUsage, instance );
            updateUsageTotals( zoneUsage, instance );
          }
        }
      }
    }

    return report;
  }

  private boolean addParentNodes( final ReportArtEntity report,
                                  final ReportingInstanceCreateEvent createEvent,
                                  final InstanceUsageArtEntity usageEntity ) {
    final Map<String, ReportingUser> users = Maps.newHashMap();
    final Map<String, String> accounts = Maps.newHashMap();

    final ReportingUser reportingUser = getUserById( users, createEvent.getUserId() );
    if ( reportingUser == null ) {
      log.error( "No user corresponding to event:" + createEvent.getUserId() );
      return false;
    }
    final String accountName = getAccountNameById( accounts, reportingUser.getAccountId() );
    if ( accountName == null ) {
      log.error( "No account corresponding to user:" + reportingUser.getAccountId() );
      return false;
    }
    if ( !report.getZones().containsKey( createEvent.getAvailabilityZone() ) ) {
      report.getZones().put( createEvent.getAvailabilityZone(), new AvailabilityZoneArtEntity() );
    }
    final AvailabilityZoneArtEntity zone = report.getZones().get( createEvent.getAvailabilityZone() );
    if ( !zone.getAccounts().containsKey( accountName ) ) {
      zone.getAccounts().put( accountName, new AccountArtEntity() );
    }
    final AccountArtEntity account = zone.getAccounts().get( accountName );
    if ( !account.getUsers().containsKey( reportingUser.getName() ) ) {
      account.getUsers().put( reportingUser.getName(), new UserArtEntity() );
    }
    final UserArtEntity user = account.getUsers().get( reportingUser.getName() );
    if ( !user.getInstances().containsKey( createEvent.getUuid() ) ) {
      final InstanceArtEntity instance = new InstanceArtEntity( createEvent.getInstanceType(),
          createEvent.getInstanceId(), usageEntity );
      user.getInstances().put( createEvent.getUuid(), instance );
      instance.getUsage().addInstanceCnt( 1 );
    }
    return true;
  }

  private static void updateUsageTotals( UsageTotalsArtEntity totals, InstanceArtEntity instance ) {
    InstanceUsageArtEntity totalEntity = totals.getInstanceTotals();
    InstanceUsageArtEntity usage = instance.getUsage();

    /* Update metrics */
    totalEntity.addUsage( usage );

    /* Update total running time and type count for this instance type */
    Map<String, InstanceUsageArtEntity> typeTotals = totals.getTypeTotals();
    if ( !typeTotals.containsKey( instance.getInstanceType().toLowerCase() ) ) {
      typeTotals.put( instance.getInstanceType().toLowerCase(),
          new InstanceUsageArtEntity() );
    }
    InstanceUsageArtEntity typeTotal =
        typeTotals.get( instance.getInstanceType().toLowerCase() );

    typeTotal.addUsage( usage );
  }

  private static void addMetricValueToUsageEntity( InstanceUsageArtEntity usage, String metric, String dim, double val ) {
    final long value = new Double( val ).longValue();
    final long valueMB = value >> 20;
    if ( metric.equals( METRIC_NET_IN_BYTES ) && dim.equals( DIM_TOTAL ) ) {
      usage.addNetTotalInMegs( valueMB );
    } else if ( metric.equals( METRIC_NET_OUT_BYTES ) && dim.equals( DIM_TOTAL ) ) {
      usage.addNetTotalOutMegs( valueMB );
    } else if ( metric.equals( METRIC_NET_IN_BYTES ) && dim.equals( DIM_EXTERNAL ) ) {
      usage.addNetExternalInMegs( valueMB );
    } else if ( metric.equals( METRIC_NET_OUT_BYTES ) && dim.equals( DIM_EXTERNAL ) ) {
      usage.addNetExternalOutMegs( valueMB );
    } else if ( metric.equals( METRIC_DISK_IN_BYTES ) ) {
      usage.addDiskReadMegs( valueMB );
    } else if ( metric.equals( METRIC_DISK_OUT_BYTES ) ) {
      usage.addDiskWriteMegs( valueMB );
    } else if ( metric.equals( METRIC_DISK_READ_OPS ) ) {
      usage.addDiskReadOps( value );
    } else if ( metric.equals( METRIC_DISK_WRITE_OPS ) ) {
      usage.addDiskWriteOps( value );
    } else if ( metric.equals( METRIC_VOLUME_READ ) ) {
      usage.addDiskReadTime( value );
    } else if ( metric.equals( METRIC_VOLUME_WRITE ) ) {
      usage.addDiskWriteTime( value );
    } else if ( metric.equals( METRIC_CPU_USAGE_MS ) && (dim.equals( DIM_DEFAULT )) ) {
      usage.addCpuUtilizationMs( value );
    } else {
      log.debug( "Unrecognized metric for report:" + metric + "/" + dim );
    }
  }

  private static class InstanceMetricDimensionKey {
    private final String instanceUuid;
    private final String metric;
    private final String dimension;

    public InstanceMetricDimensionKey( String instanceUuid, String metric, String dimension ) {
      this.instanceUuid = instanceUuid;
      this.metric = metric;
      this.dimension = dimension;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((dimension == null) ? 0 : dimension.hashCode());
      result = prime * result + ((instanceUuid == null) ? 0 : instanceUuid.hashCode());
      result = prime * result + ((metric == null) ? 0 : metric.hashCode());
      return result;
    }

    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass() != obj.getClass() ) return false;
      InstanceMetricDimensionKey other = (InstanceMetricDimensionKey) obj;
      if ( dimension == null ) {
        if ( other.dimension != null ) return false;
      } else if ( !dimension.equals( other.dimension ) ) return false;
      if ( instanceUuid == null ) {
        if ( other.instanceUuid != null ) return false;
      } else if ( !instanceUuid.equals( other.instanceUuid ) ) return false;
      if ( metric == null ) {
        if ( other.metric != null ) return false;
      } else if ( !metric.equals( other.metric ) ) return false;
      return true;
    }

  }

  /**
   * Immutable record of prior data for a uuid/metric/dim combo. This is replaced rather than updated.
   */
  private static class MetricPrevData {
    private final long firstMs;
    private final double lastVal;
    private final long lastMs;
    private final long lastSeq;

    private MetricPrevData( long firstMs, long lastMs, double lastVal, long lastSeq ) {
      this.firstMs = firstMs;
      this.lastMs = lastMs;
      this.lastVal = lastVal;
      this.lastSeq = lastSeq;
    }
  }

  /**
   * Find the overlapping portion of two time periods
   */
  private static long overlap( long repBegin, long repEnd, long perBegin, long perEnd ) {
    if ( perEnd <= repBegin || perBegin >= repEnd ) {
      return 0l;
    } else {
      return Math.min( repEnd, perEnd ) - Math.max( repBegin, perBegin );
    }
  }

  /**
   * Return the fraction of usage which occurs within both the report boundaries and the period boundaries.
   */
  private static Double fractionalUsage( long repBegin, long repEnd, long perBegin, long perEnd, Double usage ) {
    if ( usage == null ) return null;
    double duration = (double) (perEnd - perBegin);
    double overlapping = (double) overlap( repBegin, repEnd, perBegin, perEnd );
    return usage * (overlapping / duration);
  }

  protected void foreachInstanceUsageEvent( long startInclusive, long endExclusive,
                                            Predicate<? super ReportingInstanceUsageEvent> callback ) {
    foreach( ReportingInstanceUsageEvent.class, between( startInclusive, endExclusive ), true, callback );
  }

  protected void foreachInstanceCreateEvent( long endExclusive,
                                             Predicate<? super ReportingInstanceCreateEvent> callback ) {
    foreach( ReportingInstanceCreateEvent.class, before( endExclusive ), true, callback );
  }

}
