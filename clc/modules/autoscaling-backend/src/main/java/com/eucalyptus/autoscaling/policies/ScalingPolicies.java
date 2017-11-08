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
package com.eucalyptus.autoscaling.policies;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingPolicyMetadata;
import java.util.List;
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.backend.msgs.ScalingPolicyType;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public abstract class ScalingPolicies {

  public abstract <T> List<T> list( OwnerFullName ownerFullName,
                                    Predicate<? super ScalingPolicy> filter,
                                    Function<? super ScalingPolicy,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> T lookup( OwnerFullName ownerFullName,
                                String autoScalingGroupName,
                                String policyName,
                                Function<? super ScalingPolicy,T> transform ) throws AutoScalingMetadataException;

  public abstract ScalingPolicy update( OwnerFullName ownerFullName,
                                        String autoScalingGroupName,
                                        String policyName,                                        
                                        Callback<ScalingPolicy> policyUpdateCallback ) throws AutoScalingMetadataException;

  public abstract boolean delete( ScalingPolicyMetadata scalingPolicy ) throws AutoScalingMetadataException;

  public abstract ScalingPolicy save( ScalingPolicy scalingPolicy ) throws AutoScalingMetadataException;

  public final PersistingBuilder create( final OwnerFullName ownerFullName,
                                         final AutoScalingGroup autoScalingGroup,
                                         final String policyName,
                                         final AdjustmentType adjustmentType,
                                         final Integer scalingAdjustment ) {
    return new PersistingBuilder( this, ownerFullName, autoScalingGroup, policyName, adjustmentType, scalingAdjustment );
  }

  public static Function<ScalingPolicy,String> toGroupName() {
    return ScalingPolicyProperties.GROUP_NAME;
  }

  public static Function<ScalingPolicy,String> toGroupUuid() {
    return ScalingPolicyProperties.GROUP_UUID;
  }

  public static class PersistingBuilder extends ScalingPolicy.BaseBuilder<PersistingBuilder> {
    private final ScalingPolicies scalingPolicies;

    PersistingBuilder( final ScalingPolicies scalingPolicies,
                       final OwnerFullName ownerFullName,
                       final AutoScalingGroup autoScalingGroup,
                       final String name,
                       final AdjustmentType adjustmentType,
                       final Integer scalingAdjustment ) {
      super( ownerFullName, autoScalingGroup, name, adjustmentType, scalingAdjustment );
      this.scalingPolicies = scalingPolicies;
    }

    @Override
    protected PersistingBuilder builder() {
      return this;
    }

    public ScalingPolicy persist() throws AutoScalingMetadataException {
      return scalingPolicies.save( build() );
    }
  }

  @TypeMapper
  public enum ScalingPolicyViewTransform implements Function<ScalingPolicy, ScalingPolicyView> {
    INSTANCE;

    @Override
    public ScalingPolicyView apply( final ScalingPolicy scalingPolicy ) {
      return new ScalingPolicyView( scalingPolicy );
    }
  }

  @TypeMapper
  public enum ScalingPolicyTransform implements Function<ScalingPolicy, ScalingPolicyType> {
    INSTANCE;

    @Override
    public ScalingPolicyType apply( final ScalingPolicy policy ) {
      final ScalingPolicyType type = new ScalingPolicyType();

      type.setPolicyARN( policy.getArn() );
      type.setPolicyName( policy.getPolicyName() );
      if ( policy.getGroup() != null )
        type.setAutoScalingGroupName( policy.getGroup().getAutoScalingGroupName() );
      type.setAdjustmentType( Strings.toString( policy.getAdjustmentType() ) );
      type.setScalingAdjustment( policy.getScalingAdjustment() );
      type.setMinAdjustmentStep( policy.getMinAdjustmentStep() );
      type.setCooldown( policy.getCooldown() );

      return type;
    }
  }

  @RestrictedTypes.QuantityMetricFunction( ScalingPolicyMetadata.class )
  public enum CountScalingPolicies implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( ScalingPolicy.class );
      try {
        return Entities.count( ScalingPolicy.withOwner( input ) );
      } finally {
        db.rollback( );
      }
    }
  } 
  
  private enum ScalingPolicyProperties implements Function<ScalingPolicy,String> {
    GROUP_NAME {
      @Override
      public String apply( final ScalingPolicy scalingPolicy ) {
        return AutoScalingMetadatas.toDisplayName().apply( scalingPolicy.getGroup() );
      }
    },
    GROUP_UUID {
      @Override
      public String apply( final ScalingPolicy scalingPolicy ) {
        return scalingPolicy.getGroup() == null ? null : scalingPolicy.getGroup().getNaturalId();
      }
    }
  } 
}
