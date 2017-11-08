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
package com.eucalyptus.autoscaling.common.internal.policies;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingPolicyMetadata;
import java.util.List;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.common.internal.metadata.AbstractOwnedPersistentsWithResourceNameSupport;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
@ComponentNamed
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
