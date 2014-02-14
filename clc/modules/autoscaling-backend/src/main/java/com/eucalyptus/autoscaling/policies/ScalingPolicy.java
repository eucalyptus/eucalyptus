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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_scaling_policies" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ScalingPolicy extends AbstractOwnedPersistent implements ScalingPolicyMetadata {
  private static final long serialVersionUID = 1L;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_group_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private AutoScalingGroup group;

  @Column( name = "metadata_auto_scaling_group_name", nullable = false  )
  private String autoScalingGroupName;

  @Column( name = "metadata_scaling_adjustment", nullable = false  )
  private Integer scalingAdjustment;

  @Column( name = "metadata_min_adjustment_step" )
  private Integer minAdjustmentStep;

  @Column( name = "metadata_cooldown" )
  private Integer cooldown;

  @Column( name = "metadata_adjustment_type", nullable = false  )
  @Enumerated( EnumType.STRING )  
  private AdjustmentType adjustmentType;
  
  protected ScalingPolicy() {
  }

  protected ScalingPolicy( final OwnerFullName owner ) {
    super( owner );
  }

  protected ScalingPolicy( final OwnerFullName owner,
                           final AutoScalingGroup group,
                           final String displayName ) {
    super( owner, displayName );
    setGroup( group );
  }

  public String getPolicyName() {
    return getDisplayName();
  }

  public AutoScalingGroup getGroup() {
    return group;
  }

  public void setGroup( final AutoScalingGroup group ) {
    this.group = group;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( final String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public Integer getScalingAdjustment() {
    return scalingAdjustment;
  }

  public void setScalingAdjustment( final Integer scalingAdjustment ) {
    this.scalingAdjustment = scalingAdjustment;
  }

  public Integer getMinAdjustmentStep() {
    return minAdjustmentStep;
  }

  public void setMinAdjustmentStep( final Integer minAdjustmentStep ) {
    this.minAdjustmentStep = minAdjustmentStep;
  }

  public Integer getCooldown() {
    return cooldown;
  }

  public void setCooldown( final Integer cooldown ) {
    this.cooldown = cooldown;
  }

  public AdjustmentType getAdjustmentType() {
    return adjustmentType;
  }

  public void setAdjustmentType( final AdjustmentType adjustmentType ) {
    this.adjustmentType = adjustmentType;
  }

  @Override
  public String getArn() {
    return String.format(
        "arn:aws:autoscaling::%1s:scalingPolicy:%2s:autoScalingGroupName/%3s:policyName/%4s",
        getOwnerAccountNumber(),
        getNaturalId(),
        group == null ? "" : group.getAutoScalingGroupName(),
        getPolicyName() );
  }

  /**
   * Create an example ScalingPolicy for the given owner. 
   *
   * @param ownerFullName The owner
   * @return The example
   */
  public static ScalingPolicy withOwner( final OwnerFullName ownerFullName ) {
    return new ScalingPolicy( ownerFullName );
  }

  /**
   * Create an example ScalingPolicy for the given owner, group name and name. 
   *
   * @param ownerFullName The owner
   * @param autoScalingGroupName The group name
   * @param policyName The policy name
   * @return The example
   */
  public static ScalingPolicy named( final OwnerFullName ownerFullName,
                                     final String autoScalingGroupName,
                                     final String policyName ) {
    final ScalingPolicy example = withOwner( ownerFullName );
    example.setAutoScalingGroupName( autoScalingGroupName );
    example.setDisplayName( policyName );
    return example;
  }
  
  public static ScalingPolicy withId( final String id ) {
    final ScalingPolicy example = new ScalingPolicy();
    example.setId( id );
    return example;
  }

  public static ScalingPolicy withUuid( final String uuid ) {
    final ScalingPolicy example = new ScalingPolicy();
    example.setNaturalId( uuid );
    return example;
  }

  public static ScalingPolicy create( final OwnerFullName ownerFullName,
                                      final AutoScalingGroup autoScalingGroup,
                                      final String name,
                                      final AdjustmentType adjustmentType,
                                      final Integer scalingAdjustment ) {
    final ScalingPolicy scalingPolicy = new ScalingPolicy( ownerFullName, autoScalingGroup, name );
    scalingPolicy.setAdjustmentType( adjustmentType );
    scalingPolicy.setScalingAdjustment( scalingAdjustment );
    return scalingPolicy;
  }

  @Override
  protected String createUniqueName() {
    return ( this.getOwnerAccountNumber() != null && this.getGroup() != null && this.getDisplayName( ) != null )
        ? this.getOwnerAccountNumber() + ":" + getGroup().getDisplayName() + ":" + this.getDisplayName( )
        : null;
  }

  protected static abstract class BaseBuilder<T extends BaseBuilder<T>> {
    private final OwnerFullName ownerFullName;
    private final AutoScalingGroup autoScalingGroup;
    private final String name;
    private final AdjustmentType adjustmentType;
    private final Integer scalingAdjustment;
    private Integer cooldown;
    private Integer minAdjustmentStep;

    BaseBuilder( final OwnerFullName ownerFullName,
                 final AutoScalingGroup autoScalingGroup,
                 final String name,
                 final AdjustmentType adjustmentType,
                 final Integer scalingAdjustment ) {
      this.ownerFullName = ownerFullName;
      this.autoScalingGroup = autoScalingGroup;
      this.name = name;
      this.adjustmentType = adjustmentType;
      this.scalingAdjustment = scalingAdjustment;
    }

    protected abstract T builder();

    public T withCooldown( final Integer cooldown ) {
      this.cooldown  = cooldown;
      return builder();
    }

    public T withMinAdjustmentStep( final Integer minAdjustmentStep ) {
      this.minAdjustmentStep  = minAdjustmentStep;
      return builder();
    }   

    protected ScalingPolicy build() {
      final ScalingPolicy policy =
          ScalingPolicy.create( ownerFullName, autoScalingGroup, name, adjustmentType, scalingAdjustment );
      policy.setCooldown( cooldown );
      policy.setMinAdjustmentStep( minAdjustmentStep );      
      return policy;
    }
  }

  @PrePersist
  @PreUpdate 
  private void preUpdate() {
    autoScalingGroupName = AutoScalingMetadatas.toDisplayName().apply( group );    
  }
}
