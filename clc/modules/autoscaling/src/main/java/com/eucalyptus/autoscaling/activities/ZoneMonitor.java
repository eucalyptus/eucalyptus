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
package com.eucalyptus.autoscaling.activities;

import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Monitors availability zones for failure detection.
 */
class ZoneMonitor {

  private static Logger logger = Logger.getLogger( ZoneMonitor.class );

  private static final Map<String,ZoneFailureInfo> failureMap = Maps.newHashMap();
  private static final Object failureMapSync = new Object();

  /**
   * Get the set of availability zones that have failed for the given duration.
   *
   * @param duration The duration in milliseconds
   * @return The zones that have been unavailable for the duration
   */
  public Set<String> getUnavailableZones( final long duration ) {
    synchronized ( failureMapSync ) {
      return Sets.newHashSet( Iterables.transform(
          Iterables.filter( failureMap.values(), unavailableSince( System.currentTimeMillis() - duration ) ),
          ZoneName.INSTANCE ) );
    }
  }

  public static void checkZones() {
    try {
      final DescribeServicesType describeServices = new DescribeServicesType();
      describeServices.setByServiceType( "cluster" );
      final DispatchingClient<EmpyreanMessage,Empyrean> client =
          new DispatchingClient<EmpyreanMessage,Empyrean>( Empyrean.class );
      client.init();
      client.dispatch( describeServices, ZoneCallback.INSTANCE );
    } catch ( DispatchingClient.DispatchingClientException e ) {
      logWarning( "Error describing zones", e );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
  }

  private static void checkAvailabilityForZones( final Set<String> zones ) {
    for ( final String zone : zones ) {
      final boolean available = ZoneAvailableFilter.INSTANCE.apply( zone );
      synchronized ( failureMapSync ) {
        if ( available ) {
          failureMap.remove( zone );
        } else {
          final ZoneFailureInfo info = failureMap.get( zone );
          if ( info == null ) {
            failureMap.put( zone, new ZoneFailureInfo( zone ) );
          }
        }
      }
    }
  }

  private static Predicate<ZoneFailureInfo> unavailableSince( final long since ) {
    return new Predicate<ZoneFailureInfo>() {
      @Override
      public boolean apply( final ZoneFailureInfo zoneFailureInfo ) {
        return zoneFailureInfo.since < since;
      }
    };
  }

  private static void logWarning( final String description, final Throwable t ) {
    final String message = description + ": " + t.getMessage();
    logger.warn( message );
    Logs.extreme().warn( message, t );
  }

  private enum ZoneAvailableFilter implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply( final String zone ) {
      boolean enabled = false;
      final Partition partition = Partitions.lookupByName( zone );
      if ( partition != null ) try {
        Topology.lookup( ClusterController.class, partition );
        enabled = true;
      } catch ( Exception e ) {
        Logs.exhaust().info( "Lookup failed for zone: " + zone, e );
      }
      return enabled;
    }
  }

  private enum ZoneName implements Function<ZoneFailureInfo,String> {
    INSTANCE;

    @Override
    public String apply( final ZoneFailureInfo info ) {
      return info.name;
    }
  }

  private enum ZoneCallback implements Callback.Checked<DescribeServicesResponseType> {
    INSTANCE;

    @Override
    public void fireException( final Throwable t ) {
      logWarning( "Error describing zones", t );
    }

    @Override
    public void fire( final DescribeServicesResponseType zonesResponse ) {
      final Set<String> zones = Sets.newHashSet();

      if ( zonesResponse.getServiceStatuses() != null ) {
        for ( final ServiceStatusType serviceStatus : zonesResponse.getServiceStatuses() ) {
          if ( serviceStatus != null && serviceStatus.getServiceId() != null && serviceStatus.getServiceId().getPartition() != null ) {
            zones.add( serviceStatus.getServiceId().getPartition() );
          }
        }
      }

      checkAvailabilityForZones( zones );
    }
  }

  private static final class ZoneFailureInfo {
    private final String name;
    private final long since;

    private ZoneFailureInfo( final String name ) {
      this.name = name;
      this.since = System.currentTimeMillis();
    }
  }

}
