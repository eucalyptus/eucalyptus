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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.autoscaling.activities.ScalingActivity;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.policies.ScalingPolicy;
import com.eucalyptus.autoscaling.tags.AutoScalingGroupTag;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_auto_scaling_groups" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AutoScalingGroup extends AbstractOwnedPersistent implements AutoScalingGroupMetadata {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_max_size", nullable = false )
  private Integer maxSize;

  @Column( name = "metadata_min_size", nullable = false )
  private Integer minSize;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_launch_configuration_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private LaunchConfiguration launchConfiguration;

  @Column(name = "metadata_capacity_timestamp", nullable = false )
  @Temporal( TemporalType.TIMESTAMP)
  private Date capacityTimestamp;
  
  @Column( name = "metadata_default_cooldown", nullable = false )
  private Integer defaultCooldown;

  @Column( name = "metadata_desired_capacity", nullable = false )
  private Integer desiredCapacity;

  @Column( name = "metadata_capacity", nullable = false )
  private Integer capacity;
  
  @Column( name = "metadata_scaling_required", nullable = false )
  private Boolean scalingRequired;

  @Column( name = "metadata_health_check_grace_period" )
  private Integer healthCheckGracePeriod;

  @Column( name = "metadata_health_check_type", nullable = false )
  @Enumerated( EnumType.STRING )
  private HealthCheckType healthCheckType;

  @ElementCollection
  @CollectionTable( name = "metadata_auto_scaling_group_availability_zones" )
  @Column( name = "metadata_availability_zone" )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @OrderColumn( name = "metadata_availability_zone_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<String> availabilityZones = Lists.newArrayList();

  @ElementCollection
  @CollectionTable( name = "metadata_auto_scaling_group_termination_policies" )
  @Column( name = "metadata_termination_policy" )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @OrderColumn( name = "metadata_policy_index")
  @Enumerated( EnumType.STRING )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<TerminationPolicyType> terminationPolicies = Lists.newArrayList();
  
  @ElementCollection
  @CollectionTable( name = "metadata_auto_scaling_group_load_balancers" )
  @Column( name = "metadata_load_balancer_name" )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @OrderColumn( name = "metadata_load_balancer_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<String> loadBalancerNames = Lists.newArrayList();

  @ElementCollection
  @CollectionTable( name = "metadata_auto_scaling_group_suspended_processes" )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<SuspendedProcess> suspendedProcesses = Sets.newHashSet();

  @ElementCollection
  @CollectionTable( name = "metadata_auto_scaling_group_enabled_metrics" )
  @Column( name = "metadata_metric" )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @Enumerated( EnumType.STRING )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<MetricCollectionType> enabledMetrics = Sets.newHashSet();

  @ElementCollection
  @CollectionTable( name = "metadata_auto_scaling_group_scaling_causes" )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @OrderColumn( name = "metadata_scaling_causes_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<GroupScalingCause> scalingCauses = Lists.newArrayList();

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "group" )
  private Collection<ScalingActivity> scalingActivity;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "group" )
  private Collection<ScalingPolicy> scalingPolicies;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group" )
  private Collection<AutoScalingGroupTag> tags = Lists.newArrayList();

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, mappedBy = "autoScalingGroup" )
  private List<AutoScalingInstance> instances = Lists.newArrayList();

  protected AutoScalingGroup() {
  }

  protected AutoScalingGroup( @Nullable final OwnerFullName owner ) {
    super( owner );
  }

  protected AutoScalingGroup( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public String getAutoScalingGroupName() {
    return getDisplayName();
  }

  public Integer getMaxSize() {
    return maxSize;
  }

  public void setMaxSize( final Integer maxSize ) {
    this.maxSize = maxSize;
  }

  public Integer getMinSize() {
    return minSize;
  }

  public void setMinSize( final Integer minSize ) {
    this.minSize = minSize;
  }

  public LaunchConfiguration getLaunchConfiguration() {
    return launchConfiguration;
  }

  public void setLaunchConfiguration( final LaunchConfiguration launchConfiguration ) {
    this.launchConfiguration = launchConfiguration;
  }

  public Date getCapacityTimestamp() {
    return capacityTimestamp;
  }

  public void setCapacityTimestamp( final Date capacityTimestamp ) {
    this.capacityTimestamp = capacityTimestamp;
  }

  public Integer getDefaultCooldown() {
    return defaultCooldown;
  }

  public void setDefaultCooldown( final Integer defaultCooldown ) {
    this.defaultCooldown = defaultCooldown;
  }

  public Integer getDesiredCapacity() {
    return desiredCapacity;
  }

  public void setDesiredCapacity( final Integer desiredCapacity ) {
    this.desiredCapacity = desiredCapacity;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity( final Integer capacity ) {
    this.capacity = capacity;
  }

  public Boolean getScalingRequired() {
    return scalingRequired;
  }

  public void setScalingRequired( final Boolean scalingRequired ) {
    this.scalingRequired = scalingRequired;
  }

  public Integer getHealthCheckGracePeriod() {
    return healthCheckGracePeriod;
  }

  public void setHealthCheckGracePeriod( final Integer healthCheckGracePeriod ) {
    this.healthCheckGracePeriod = healthCheckGracePeriod;
  }

  public HealthCheckType getHealthCheckType() {
    return healthCheckType;
  }

  public void setHealthCheckType( final HealthCheckType healthCheckType ) {
    this.healthCheckType = healthCheckType;
  }

  public List<String> getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones( final List<String> availabilityZones ) {
    this.availabilityZones = availabilityZones;
  }

  public List<TerminationPolicyType> getTerminationPolicies() {
    return terminationPolicies;
  }

  public void setTerminationPolicies( final List<TerminationPolicyType> terminationPolicies ) {
    this.terminationPolicies = terminationPolicies;
  }

  public List<String> getLoadBalancerNames() {
    return loadBalancerNames;
  }

  public void setLoadBalancerNames( final List<String> loadBalancerNames ) {
    this.loadBalancerNames = loadBalancerNames;
  }

  public Set<SuspendedProcess> getSuspendedProcesses() {
    return suspendedProcesses;
  }

  public void setSuspendedProcesses( final Set<SuspendedProcess> suspendedProcesses ) {
    this.suspendedProcesses = suspendedProcesses;
  }

  public Set<MetricCollectionType> getEnabledMetrics() {
    return enabledMetrics;
  }

  public void setEnabledMetrics( final Set<MetricCollectionType> enabledMetrics ) {
    this.enabledMetrics = enabledMetrics;
  }

  public List<GroupScalingCause> getScalingCauses() {
    return scalingCauses;
  }

  public void setScalingCauses( final List<GroupScalingCause> scalingCauses ) {
    this.scalingCauses = scalingCauses;
  }

  public List<AutoScalingInstance> getAutoScalingInstances() {
    return instances;
  }

  /**
   * Update the capacity of the group, flag scaling required if necessary.
   *
   * @param capacity The new capacity.
   */
  public void updateCapacity( final int capacity ) {
    this.scalingRequired = scalingRequired || desiredCapacity == null || capacity != desiredCapacity;
    this.capacity = capacity;
  }

  public void updateDesiredCapacity( final int desiredCapacity,
                                     final String reason ) {
    if ( !this.desiredCapacity.equals( desiredCapacity ) ) {
      scalingCauses.add( new GroupScalingCause( reason ) );
      while ( scalingCauses.size() > 100 ) {
        scalingCauses.remove( 0 );
      }
    }
    this.scalingRequired = scalingRequired || capacity == null || !capacity.equals( desiredCapacity );
    this.desiredCapacity = desiredCapacity;
  }

  public void updateAvailabilityZones( final List<String> availabilityZones ) {
    final Set<String> currentZones = Sets.newHashSet( this.availabilityZones );
    final Set<String> newZones = Sets.newHashSet( availabilityZones );
    if ( !currentZones.equals( newZones ) ) {
      final String removedZones = Joiner.on(",").join( Sets.difference( currentZones, newZones ) );
      final String addedZones = Joiner.on(",").join( Sets.difference( newZones, currentZones ) );
      scalingCauses.add( new GroupScalingCause( String.format( "a user request removed the zones %1$s from this AutoScalingGroup making them invalid", removedZones ) ) );
      scalingCauses.add( new GroupScalingCause( String.format( "a user request added the zones %1$s to this AutoScalingGroup and the group may require rebalancing", addedZones ) ) );
    }
    this.scalingRequired = scalingRequired || !currentZones.equals( newZones );
    this.availabilityZones = availabilityZones;
  }

  @Override
  public String getArn() {
    return String.format(
        "arn:aws:autoscaling::%1s:autoScalingGroup:%2s:autoScalingGroupName/%3s",
        getOwnerAccountNumber(),
        getNaturalId(),
        getDisplayName() );
  }

  /**
   * Create an example AutoScalingGroup for the given owner. 
   *
   * @param ownerFullName The owner
   * @return The example
   */
  public static AutoScalingGroup withOwner( @Nullable final OwnerFullName ownerFullName ) {
    return new AutoScalingGroup( ownerFullName );
  }

  /**
   * Create an example AutoScalingGroup for the given owner and name. 
   *
   * @param ownerFullName The owner
   * @param name The name
   * @return The example
   */
  public static AutoScalingGroup named( final OwnerFullName ownerFullName,
                                        final String name ) {
    return new AutoScalingGroup( ownerFullName, name );
  }

  public static AutoScalingGroup withId( final String id ) {
    final AutoScalingGroup example = new AutoScalingGroup();
    example.setId( id);
    return example;
  }

  public static AutoScalingGroup withUuid( final String uuid ) {
    final AutoScalingGroup example = new AutoScalingGroup();
    example.setNaturalId( uuid );
    return example;
  }

  public static AutoScalingGroup requiringScaling( ) {
    final AutoScalingGroup example = new AutoScalingGroup();
    example.setScalingRequired( true );
    return example;
  }

  public static AutoScalingGroup create( final OwnerFullName ownerFullName,
                                         final String name,
                                         final LaunchConfiguration launchConfiguration,
                                         final Integer minSize,
                                         final Integer maxSize,
                                         final List<AutoScalingGroupTag> tags ) {
    final AutoScalingGroup autoScalingGroup = new AutoScalingGroup( ownerFullName, name );
    autoScalingGroup.setLaunchConfiguration( launchConfiguration );
    autoScalingGroup.setMinSize( minSize );
    autoScalingGroup.setMaxSize( maxSize );
    autoScalingGroup.setCapacity( 0 );
    for ( final AutoScalingGroupTag tag : tags ) {
      tag.setGroup( autoScalingGroup );
      tag.setOwner( autoScalingGroup.getOwner() );
      autoScalingGroup.tags.add( tag );
    }
    return autoScalingGroup;
  }

  @PrePersist
  @PreUpdate
  private void preUpdate() {
    if ( capacityTimestamp == null ) {
      capacityTimestamp = new Date();
    }
  }  
  
  protected static abstract class BaseBuilder<T extends BaseBuilder<T>> {
    private OwnerFullName ownerFullName;
    private String name;
    private Integer minSize;
    private Integer maxSize;
    private Integer defaultCooldown;
    private Integer desiredCapacity;
    private Integer healthCheckGracePeriod;
    private HealthCheckType healthCheckType;
    private LaunchConfiguration launchConfiguration;
    private Set<String> availabilityZones = Sets.newLinkedHashSet();
    private Set<TerminationPolicyType> terminationPolicies = Sets.newLinkedHashSet();
    private Set<String> loadBalancerNames = Sets.newLinkedHashSet();
    private List<AutoScalingGroupTag> tags = Lists.newArrayList();

    BaseBuilder( final OwnerFullName ownerFullName,
                 final String name,
                 final LaunchConfiguration launchConfiguration,
                 final Integer minSize,
                 final Integer maxSize ) {
      this.ownerFullName = ownerFullName;
      this.name = name;
      this.launchConfiguration = launchConfiguration;
      this.minSize = minSize;
      this.maxSize = maxSize;
    }

    protected abstract T builder();

    public T withDefaultCooldown( final Integer defaultCooldown ) {
      this.defaultCooldown  = defaultCooldown;
      return builder();
    }

    public T withDesiredCapacity( final Integer desiredCapacity ) {
      this.desiredCapacity  = desiredCapacity;
      return builder();
    }

    public T withHealthCheckGracePeriod( final Integer healthCheckGracePeriod ) {
      this.healthCheckGracePeriod  = healthCheckGracePeriod;
      return builder();
    }

    public T withHealthCheckType( final HealthCheckType healthCheckType ) {
      this.healthCheckType  = healthCheckType;
      return builder();
    }

    public T withAvailabilityZones( final Collection<String> availabilityZones ) {
      if ( availabilityZones != null ) {
        this.availabilityZones.addAll( availabilityZones );
      }
      return builder();
    }

    public T withTerminationPolicyTypes( final Collection<TerminationPolicyType> terminationPolicies ) {
      if ( terminationPolicies != null ) {
        this.terminationPolicies.addAll( terminationPolicies );
      }
      return builder();
    }

    public T withLoadBalancerNames( final Collection<String> loadBalancerNames ) {
      if ( loadBalancerNames != null ) {
        this.loadBalancerNames.addAll( loadBalancerNames );
      }
      return builder();
    }

    public T withTags( final Iterable<AutoScalingGroupTag> tags ) {
      if ( tags != null ) {
        Iterables.addAll( this.tags, tags );
      }
      return builder();
    }

    protected AutoScalingGroup build() {
      final AutoScalingGroup group =
          AutoScalingGroup.create( ownerFullName, name, launchConfiguration, minSize, maxSize, tags );
      group.setDefaultCooldown( Objects.firstNonNull( defaultCooldown, 300 ) ); 
      group.setDesiredCapacity( Objects.firstNonNull( desiredCapacity, minSize ) );
      group.setHealthCheckGracePeriod( Objects.firstNonNull( healthCheckGracePeriod, 0 ) );
      group.setHealthCheckType( Objects.firstNonNull( healthCheckType, HealthCheckType.EC2 ) );
      group.setAvailabilityZones( Lists.newArrayList( availabilityZones ) );
      group.setTerminationPolicies( terminationPolicies.isEmpty() ? 
          Collections.singletonList(TerminationPolicyType.Default) :
          Lists.newArrayList( terminationPolicies ) );
      group.setLoadBalancerNames( Lists.newArrayList( loadBalancerNames ) );
      group.setScalingRequired( group.getDesiredCapacity() > 0 );
      return group;
    }
  }  
}
