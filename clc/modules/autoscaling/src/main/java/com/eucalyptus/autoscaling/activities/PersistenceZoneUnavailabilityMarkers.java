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

import java.util.List;
import java.util.Set;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 *
 */
public class PersistenceZoneUnavailabilityMarkers extends ZoneUnavailabilityMarkers {

  @Override
  public void updateUnavailableZones( final Set<String> unavailableZones,
                                      final ZoneCallback callback ) throws AutoScalingMetadataException {
    try {
      Entities.asTransaction( ZoneUnavailabilityMarker.class, UnavailableZoneUpdate.INSTANCE )
        .apply( new ZoneParams( unavailableZones, callback ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error marking zones as unavailable", e );
    }
  }

  private static class ZoneParams {
    private final Set<String> unavailableZones;
    private final ZoneCallback callback;

    private ZoneParams( final Set<String> unavailableZones,
                        final ZoneCallback callback ) {
      this.unavailableZones = unavailableZones;
      this.callback = callback;
    }

    public Set<String> getUnavailableZones() {
      return unavailableZones;
    }

    public ZoneCallback getCallback() {
      return callback;
    }
  }

  private enum UnavailableZoneUpdate implements Function<ZoneParams,Void> {
    INSTANCE;

    @Override
    public Void apply( final ZoneParams zoneParams ) {
      final List<ZoneUnavailabilityMarker> unavailableZoneList =
          Entities.query( new ZoneUnavailabilityMarker() );
      final Set<String> currentUnavailableZones = Sets.newHashSet(
          Iterables.transform( unavailableZoneList, ZoneName.INSTANCE )
      );

      final Set<String> changedZones = Sets.newHashSet();
      for ( final String zone : zoneParams.getUnavailableZones() ) {
        if ( !currentUnavailableZones.contains( zone ) ) {
          changedZones.add( zone );
          Entities.persist( new ZoneUnavailabilityMarker( zone ) );
        }
      }
      for ( final ZoneUnavailabilityMarker marker : unavailableZoneList ) {
        if ( !zoneParams.getUnavailableZones().contains( marker.getName() ) ) {
          changedZones.add( marker.getName() );
          Entities.delete( marker );
        }
      }

      try {
        zoneParams.getCallback().notifyChangedZones( changedZones );
      } catch ( AutoScalingMetadataException e ) {
        throw Exceptions.toUndeclared( e );
      }
      return null;
    }
  }

  private enum ZoneName implements Function<ZoneUnavailabilityMarker,String> {
    INSTANCE;

    @Override
    public String apply( final ZoneUnavailabilityMarker zoneUnavailabilityMarker ) {
      return zoneUnavailabilityMarker.getName();
    }
  }
}
