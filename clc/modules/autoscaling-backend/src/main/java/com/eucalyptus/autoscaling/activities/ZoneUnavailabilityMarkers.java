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

import java.util.Set;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;

/**
 *
 */
public abstract class ZoneUnavailabilityMarkers {

  /**
   * Update the set of unavailable zones.
   *
   * Note that the callback may be invoked multiple times but within a
   * transaction that will only commit (successfully) once.
   *
   * @param unavailableZones The currently unavailable zones
   * @param callback Callback for the set of zones with changed availability
   */
  public abstract void updateUnavailableZones( Set<String> unavailableZones,
                                               ZoneCallback callback ) throws AutoScalingMetadataException;

  public interface ZoneCallback {
    void notifyChangedZones( Set<String> zones ) throws AutoScalingMetadataException;
  }
}
