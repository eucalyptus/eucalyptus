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
package com.eucalyptus.autoscaling.policies;

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
public class PersistenceScalingPolicies extends ScalingPolicies {

  @Override
  public List<ScalingPolicy> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    try {
      return Transactions.findAll( ScalingPolicy.withOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find scaling policies for " + ownerFullName, e );
    }
  }

  @Override
  public List<ScalingPolicy> list( final OwnerFullName ownerFullName,
                                   final Predicate<? super ScalingPolicy> filter ) throws AutoScalingMetadataException {
    try {
      return Transactions.filter( ScalingPolicy.withOwner( ownerFullName ), filter );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Failed to find scaling policies for " + ownerFullName, e );
    }
  }

  @Override
  public ScalingPolicy lookup( final OwnerFullName ownerFullName,
                               final String autoScalingGroupName,
                               final String policyName ) throws AutoScalingMetadataException {
    try {
      return Transactions.find( ScalingPolicy.named( ownerFullName, autoScalingGroupName, policyName ) );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( "Scaling policy not found '"+policyName+"' for " + ownerFullName, e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error finding scaling policy '"+policyName+"' for " + ownerFullName, e );
    }
  }

  @Override
  public ScalingPolicy update( final OwnerFullName ownerFullName,
                               final String autoScalingGroupName,
                               final String policyName,
                               final Callback<ScalingPolicy> policyUpdateCallback ) throws AutoScalingMetadataException {
    try {
      return Transactions.one( ScalingPolicy.named( ownerFullName, autoScalingGroupName, policyName ), policyUpdateCallback );
    } catch ( NoSuchElementException e ) {
      throw new AutoScalingMetadataNotFoundException( "Scaling policy not found '"+policyName+"' for " + ownerFullName, e );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error finding scaling policy '"+policyName+"' for " + ownerFullName, e );
    }
  }

  @Override
  public boolean delete( final ScalingPolicy scalingPolicy ) throws AutoScalingMetadataException {
    try {
      return Transactions.delete( ScalingPolicy.withId( scalingPolicy ) );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error deleting scaling policy '"+scalingPolicy.getPolicyName()+"'", e );
    }
  }

  @Override
  public ScalingPolicy save( final ScalingPolicy scalingPolicy ) throws AutoScalingMetadataException {
    try {
      return Transactions.saveDirect( scalingPolicy );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataException( "Error creating scaling policy '"+scalingPolicy.getPolicyName()+"'", e );
    }
  }
}
