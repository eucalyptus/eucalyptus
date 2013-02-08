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
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
public class PersistenceScalingActivities extends ScalingActivities {

  private PersistenceSupport persistenceSupport = new PersistenceSupport();

  @Override
  public List<ScalingActivity> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName );
  }

  @Override
  public List<ScalingActivity> list( final OwnerFullName ownerFullName, 
                                     final Predicate<? super ScalingActivity> filter ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter );
  }

  @Override
  public ScalingActivity lookup( final OwnerFullName ownerFullName, 
                                 final String activityId ) throws AutoScalingMetadataException {
    return persistenceSupport.lookupByExample( 
        persistenceSupport.exampleWithName( ownerFullName, activityId ), 
        ownerFullName, 
        activityId );
  }

  @Override
  public ScalingActivity update( final OwnerFullName ownerFullName, 
                                 final String activityId, 
                                 final Callback<ScalingActivity> activityUpdateCallback ) throws AutoScalingMetadataException {
    return persistenceSupport.updateByExample(
        persistenceSupport.exampleWithName( ownerFullName, activityId ),
        ownerFullName,
        activityId,
        activityUpdateCallback
    );
  }

  @Override
  public boolean delete( final ScalingActivity scalingActivity ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( scalingActivity );
  }

  @Override
  public ScalingActivity save( final ScalingActivity scalingActivity ) throws AutoScalingMetadataException {
    return persistenceSupport.save( scalingActivity );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistents<ScalingActivity> {
    private PersistenceSupport() {
      super( "scaling activity" );
    }

    @Override
    protected ScalingActivity exampleWithUuid( final String uuid ) {
      return ScalingActivity.withUuid( uuid );
    }

    @Override
    protected ScalingActivity exampleWithOwner( final OwnerFullName ownerFullName ) {
      return ScalingActivity.withOwner( ownerFullName );
    }

    @Override
    protected ScalingActivity exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return ScalingActivity.named( ownerFullName, name );

    }
  }
}
