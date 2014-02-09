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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingPolicyMetadata;
import java.util.List;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistentsWithResourceNameSupport;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public class PersistenceScalingPolicies extends ScalingPolicies {

  private final PersistenceSupport persistenceSupport = new PersistenceSupport();
  
  @Override
  public <T> List<T> list( final OwnerFullName ownerFullName,
                           final Predicate<? super ScalingPolicy> filter,
                           final Function<? super ScalingPolicy,T> transform  ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter, transform );
  }

  @Override
  public <T> T lookup( final OwnerFullName ownerFullName,
                       final String autoScalingGroupName,
                       final String policyName,
                       final Function<? super ScalingPolicy,T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.lookup( ownerFullName, autoScalingGroupName, policyName, transform );
  }

  @Override
  public ScalingPolicy update( final OwnerFullName ownerFullName,
                               final String autoScalingGroupName,
                               final String policyName,
                               final Callback<ScalingPolicy> policyUpdateCallback ) throws AutoScalingMetadataException {
    return persistenceSupport.update( ownerFullName, autoScalingGroupName, policyName, policyUpdateCallback );
  }

  @Override
  public boolean delete( final ScalingPolicyMetadata scalingPolicy ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( scalingPolicy );
  }

  @Override
  public ScalingPolicy save( final ScalingPolicy scalingPolicy ) throws AutoScalingMetadataException {
    return persistenceSupport.save( scalingPolicy );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistentsWithResourceNameSupport<ScalingPolicy> {
    private PersistenceSupport() {
      super( AutoScalingResourceName.Type.scalingPolicy );
    }

    @Override
    protected ScalingPolicy exampleWithOwner( final OwnerFullName ownerFullName ) {
      return ScalingPolicy.withOwner( ownerFullName );
    }

    @Override
    protected ScalingPolicy exampleWithName( final OwnerFullName ownerFullName, 
                                             final String name ) {
      throw new IllegalStateException( "Unscoped name not supported." );
    }

    @Override
    protected ScalingPolicy exampleWithName( final OwnerFullName ownerFullName,
                                             final String scope,
                                             final String name ) {
      return ScalingPolicy.named( ownerFullName, scope, name );
    }

    @Override
    protected ScalingPolicy exampleWithUuid( final String uuid ) {
      return ScalingPolicy.withUuid( uuid );
    }
  }
}
