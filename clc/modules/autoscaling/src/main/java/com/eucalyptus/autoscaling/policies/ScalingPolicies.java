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
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.ScalingPolicyType;
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

  public abstract List<ScalingPolicy> list( OwnerFullName ownerFullName ) throws AutoScalingMetadataException;

  public abstract List<ScalingPolicy> list( OwnerFullName ownerFullName,
                                            Predicate<? super ScalingPolicy> filter ) throws AutoScalingMetadataException;

  public abstract ScalingPolicy lookup( OwnerFullName ownerFullName,
                                        String autoScalingGroupName,
                                        String policyName ) throws AutoScalingMetadataException;

  public abstract ScalingPolicy update( OwnerFullName ownerFullName,
                                        String autoScalingGroupName,
                                        String policyName,                                        
                                        Callback<ScalingPolicy> policyUpdateCallback ) throws AutoScalingMetadataException;

  public abstract boolean delete( ScalingPolicy scalingPolicy ) throws AutoScalingMetadataException;

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
  public enum ScalingPolicyTransform implements Function<ScalingPolicy, ScalingPolicyType> {
    INSTANCE;

    @Override
    public ScalingPolicyType apply( final ScalingPolicy policy ) {
      final ScalingPolicyType type = new ScalingPolicyType();

      type.setPolicyARN( policy.getPolicyARN() );
      type.setPolicyName( policy.getPolicyName() );
      if ( policy.getGroup() != null )
        type.setAutoScalingGroupName( policy.getGroup().getAutoScalingGroupName() );
      type.setAdjustmentType( Strings.toString( policy.getAdjustmentType() ) );
      type.setScalingAdjustment( policy.getScalingAdjustment() );
      type.setMinAdjustmentStep( policy.getMinAdjustmentStep() );
      type.setCooldown( policy.getCooldown() );
      // type.setAlarms(); //TODO:STEVE: Alarms for scaling policies
      
      return type;
    }
  }

  @RestrictedTypes.QuantityMetricFunction( AutoScalingMetadata.ScalingPolicyMetadata.class )
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
    }
  } 
}
