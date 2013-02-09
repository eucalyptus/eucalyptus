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
package com.eucalyptus.autoscaling.instances;

import java.util.List;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 *
 */
public class PersistenceAutoScalingInstances extends AutoScalingInstances {

  private PersistenceSupport persistenceSupport = new PersistenceSupport();

  @Override
  public List<AutoScalingInstance> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName );
  }

  @Override
  public List<AutoScalingInstance> list( final OwnerFullName ownerFullName, 
                                         final Predicate<? super AutoScalingInstance> filter ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter );
  }

  @Override
  public List<AutoScalingInstance> listByGroup( final OwnerFullName ownerFullName,
                                                final String groupName ) throws AutoScalingMetadataException {
    final AutoScalingInstance example = AutoScalingInstance.withOwner( ownerFullName );
    example.setAutoScalingGroupName( groupName );
    return persistenceSupport.listByExample( example, Predicates.alwaysTrue( ) );
  }

  @Override
  public List<AutoScalingInstance> listByGroup( final AutoScalingGroup group ) throws AutoScalingMetadataException {
    final AutoScalingInstance example = AutoScalingInstance.withOwner( group.getOwner() );
    example.clearUserIdentity();
    example.setAutoScalingGroupName( group.getAutoScalingGroupName() );
    return persistenceSupport.listByExample( example, Predicates.alwaysTrue() );
  }

  @Override
  public AutoScalingInstance lookup( final OwnerFullName ownerFullName, 
                                     final String instanceId ) throws AutoScalingMetadataException {
    return persistenceSupport.lookupByExample(
        persistenceSupport.exampleWithName( ownerFullName, instanceId ),
        ownerFullName,
        instanceId );
  }

  @Override
  public AutoScalingInstance update( final OwnerFullName ownerFullName, 
                                     final String instanceId, 
                                     final Callback<AutoScalingInstance> instanceUpdateCallback ) throws AutoScalingMetadataException {
    return persistenceSupport.updateByExample(
        persistenceSupport.exampleWithName( ownerFullName, instanceId ),
        ownerFullName,
        instanceId,
        instanceUpdateCallback );
  }

  @Override
  public boolean delete( final AutoScalingInstance autoScalingInstance ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( autoScalingInstance );
  }

  @Override
  public boolean deleteByGroup( final AutoScalingGroup group ) throws AutoScalingMetadataException {
    final AutoScalingInstance example = AutoScalingInstance.withOwner( group.getOwner() );
    example.clearUserIdentity();
    example.setAutoScalingGroupName( group.getAutoScalingGroupName() );
    return !persistenceSupport.deleteByExample( example ).isEmpty();
  }

  @Override
  public AutoScalingInstance save( final AutoScalingInstance autoScalingInstance ) throws AutoScalingMetadataException {
    return persistenceSupport.save( autoScalingInstance );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistents<AutoScalingInstance> {
    private PersistenceSupport() {
      super( "auto scaling instance" );
    }

    @Override
    protected AutoScalingInstance exampleWithUuid( final String uuid ) {
      return AutoScalingInstance.withUuid( uuid );
    }

    @Override
    protected AutoScalingInstance exampleWithOwner( final OwnerFullName ownerFullName ) {
      return AutoScalingInstance.withOwner( ownerFullName );
    }

    @Override
    protected AutoScalingInstance exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return AutoScalingInstance.named( ownerFullName, name );
    }
  }
}
