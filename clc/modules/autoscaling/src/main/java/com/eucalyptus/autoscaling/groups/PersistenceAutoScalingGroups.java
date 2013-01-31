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
import java.util.NoSuchElementException;
import com.eucalyptus.autoscaling.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.AutoScalingMetadataNotFoundException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
public class PersistenceAutoScalingGroups extends AutoScalingGroups {

  @Override
  public List<AutoScalingGroup> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    try {
      return Transactions.findAll( AutoScalingGroup.withOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find auto scaling groups for " + ownerFullName, e );
    }
  }

  @Override
  public List<AutoScalingGroup> list( final OwnerFullName ownerFullName, 
                                      final Predicate<? super AutoScalingGroup> filter ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( AutoScalingGroup.withOwner( ownerFullName ), filter );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find auto scaling groups for " + ownerFullName, e );
    }
  }

  @Override
  public AutoScalingGroup lookup( final OwnerFullName ownerFullName, 
                                  final String autoScalingGroupName ) throws AutoScalingMetadataException {
    try {
      return Transactions.find( AutoScalingGroup.named( ownerFullName, autoScalingGroupName ) );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( "Auto scaling group not found '"+autoScalingGroupName+"' for " + ownerFullName, e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error finding auto scaling group '"+autoScalingGroupName+"' for " + ownerFullName, e );
    }
  }

  @Override
  public AutoScalingGroup update( final OwnerFullName ownerFullName,
                                  final String autoScalingGroupName,
                                  final Callback<AutoScalingGroup> groupUpdateCallback ) throws AutoScalingMetadataException {
    try {
      return Transactions.one( AutoScalingGroup.named( ownerFullName, autoScalingGroupName ), groupUpdateCallback );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( "Auto scaling group not found '"+autoScalingGroupName+"' for " + ownerFullName, e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error finding auto scaling group '"+autoScalingGroupName+"' for " + ownerFullName, e );
    }
  }

  @Override
  public boolean delete( final AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException {
    try {
      return Transactions.delete( AutoScalingGroup.withId( autoScalingGroup ) );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting auto scaling group '"+autoScalingGroup.getAutoScalingGroupName()+"'", e );
    }
  }

  @Override
  public AutoScalingGroup save( final AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException {
    try {
      return Transactions.saveDirect( autoScalingGroup );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error creating auto scaling group '"+autoScalingGroup.getAutoScalingGroupName()+"'", e );
    }
  }
}
