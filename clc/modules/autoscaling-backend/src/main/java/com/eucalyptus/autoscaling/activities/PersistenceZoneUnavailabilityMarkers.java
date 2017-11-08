/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.autoscaling.activities;

import java.util.List;
import java.util.Set;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
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
