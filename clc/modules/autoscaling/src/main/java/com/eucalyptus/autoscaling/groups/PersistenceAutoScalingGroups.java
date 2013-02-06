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
package com.eucalyptus.autoscaling.groups;

import java.util.List;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
public class PersistenceAutoScalingGroups extends AutoScalingGroups {
  
  private PersistenceSupport persistenceSupport = new PersistenceSupport();
  
  @Override
  public List<AutoScalingGroup> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName );
  }

  @Override
  public List<AutoScalingGroup> list( final OwnerFullName ownerFullName, 
                                      final Predicate<? super AutoScalingGroup> filter ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter );
  }

  @Override
  public AutoScalingGroup lookup( final OwnerFullName ownerFullName, final String autoScalingGroupName ) throws AutoScalingMetadataException {
    return persistenceSupport.lookup( ownerFullName, autoScalingGroupName );
  }

  @Override
  public AutoScalingGroup update( final OwnerFullName ownerFullName,
                                  final String autoScalingGroupName,
                                  final Callback<AutoScalingGroup> groupUpdateCallback ) throws AutoScalingMetadataException {
    return persistenceSupport.update( ownerFullName, autoScalingGroupName, groupUpdateCallback );
  }

  @Override
  public boolean delete( final AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( autoScalingGroup );
  }

  @Override
  public AutoScalingGroup save( final AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException {
    return persistenceSupport.save( autoScalingGroup );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistents<AutoScalingGroup> {
    private PersistenceSupport() {
      super( AutoScalingResourceName.Type.autoScalingGroup );
    }

    @Override
    protected AutoScalingGroup exampleWithOwner( final OwnerFullName ownerFullName ) {
      return AutoScalingGroup.withOwner( ownerFullName );
    }

    @Override
    protected AutoScalingGroup exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return AutoScalingGroup.named( ownerFullName, name );
    }

    @Override
    protected AutoScalingGroup exampleWithUuid( final String uuid ) {
      return AutoScalingGroup.withUuid( uuid );
    }
  }  
}
