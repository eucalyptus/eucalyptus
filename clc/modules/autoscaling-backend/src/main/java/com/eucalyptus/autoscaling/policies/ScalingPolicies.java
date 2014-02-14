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
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.backend.msgs.ScalingPolicyType;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
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
