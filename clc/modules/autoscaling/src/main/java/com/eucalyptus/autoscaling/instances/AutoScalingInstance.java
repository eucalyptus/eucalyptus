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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingInstanceMetadata;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Objects;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_auto_scaling_instances" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AutoScalingInstance extends AbstractOwnedPersistent implements AutoScalingInstanceMetadata {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_availability_zone", nullable = false, updatable = false )
  private String availabilityZone;

  @Column( name = "metadata_health_status", nullable = false )
  @Enumerated( EnumType.STRING )
  private HealthStatus healthStatus;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private AutoScalingGroup autoScalingGroup;

  @Column( name = "metadata_auto_scaling_group_name", nullable = false, updatable = false )
  private String autoScalingGroupName;

  @Column( name = "metadata_launch_configuration_name", nullable = false, updatable = false )
  private String launchConfigurationName;

  @Column( name = "metadata_lifecycle_state", nullable = false )
  @Enumerated( EnumType.STRING )
  private LifecycleState lifecycleState;

  @Temporal( TemporalType.TIMESTAMP)
  @Column( name = "metadata_in_service_timestamp" )
  private Date inServiceTimestamp;

  @Column( name = "metadata_configuration_state", nullable = false )
  @Enumerated( EnumType.STRING )
  private ConfigurationState configurationState;

  @Column( name = "metadata_registration_attempts", nullable = false )
  private Integer registrationAttempts;

  protected AutoScalingInstance() {
  }

  protected AutoScalingInstance( final OwnerFullName owner ) {
    super( owner );
  }

  protected AutoScalingInstance( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }
  
  public String getInstanceId() {
    return getDisplayName();
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone( final String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public HealthStatus getHealthStatus() {
    return healthStatus;
  }

  public void setHealthStatus( final HealthStatus healthStatus ) {
    this.healthStatus = healthStatus;
  }

  public AutoScalingGroup getAutoScalingGroup() {
    return autoScalingGroup;
  }

  public void setAutoScalingGroup( final AutoScalingGroup autoScalingGroup ) {
    this.autoScalingGroup = autoScalingGroup;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( final String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getLaunchConfigurationName() {
    return launchConfigurationName;
  }

  public void setLaunchConfigurationName( final String launchConfigurationName ) {
    this.launchConfigurationName = launchConfigurationName;
  }

  public LifecycleState getLifecycleState() {
    return lifecycleState;
  }

  public void setLifecycleState( final LifecycleState lifecycleState ) {
    this.lifecycleState = lifecycleState;
  }

  public Date getInServiceTimestamp() {
    return inServiceTimestamp;
  }

  public void setInServiceTimestamp( final Date inServiceTimestamp ) {
    this.inServiceTimestamp = inServiceTimestamp;
  }

  public ConfigurationState getConfigurationState() {
    return configurationState;
  }

  public void setConfigurationState( final ConfigurationState configurationState ) {
    this.configurationState = configurationState;
  }

  public Integer getRegistrationAttempts() {
    return registrationAttempts;
  }

  public void setRegistrationAttempts( final Integer registrationAttempts ) {
    this.registrationAttempts = registrationAttempts;
  }

  public boolean healthStatusGracePeriodExpired() {
    final long gracePeriodMillis = TimeUnit.SECONDS.toMillis( 
        Objects.firstNonNull( getAutoScalingGroup( ).getHealthCheckGracePeriod(), 0 ) );
    // last update timestamp will be when InService state entered
    return getInServiceTimestamp()!=null && System.currentTimeMillis() - getInServiceTimestamp().getTime() > gracePeriodMillis;
  }

  public int incrementRegistrationAttempts() {
    return ++this.registrationAttempts;
  }

  /**
   * Create an example AutoScalingInstance for the given owner. 
   *
   * @param ownerFullName The owner
   * @return The example
   */
  public static AutoScalingInstance withOwner( final OwnerFullName ownerFullName ) {
    return new AutoScalingInstance( ownerFullName );
  }

  /**
   * Create an example AutoScalingInstance for the given owner.
   *
   * @param accountNumber The owning account number
   * @return The example
   */
  public static AutoScalingInstance withOwner( final String accountNumber ) {
    final AutoScalingInstance example = new AutoScalingInstance( );
    example.setOwnerAccountNumber( accountNumber );
    return example;
  }

  /**
   * Create an example AutoScalingInstance for the given owner and name. 
   *
   * @param ownerFullName The owner
   * @param name The name
   * @return The example
   */
  public static AutoScalingInstance named( final OwnerFullName ownerFullName,
                                           final String name ) {
    return new AutoScalingInstance( ownerFullName, name );
  }

  public static AutoScalingInstance withUuid( final String uuid ) {
    final AutoScalingInstance example = new AutoScalingInstance();
    example.setNaturalId( uuid );
    return example;
  }
  
  public static AutoScalingInstance withHealthStatus( final HealthStatus healthStatus ) {
    final AutoScalingInstance example = new AutoScalingInstance();
    example.setHealthStatus( healthStatus );
    return example;
  }

  public static AutoScalingInstance withStates( final LifecycleState lifecycleState,
                                                final ConfigurationState configurationState ) {
    final AutoScalingInstance example = new AutoScalingInstance();
    example.setLifecycleState( lifecycleState );
    example.setConfigurationState( configurationState );
    return example;
  }

  public static AutoScalingInstance create( @Nonnull final OwnerFullName ownerFullName,
                                            @Nonnull final String instanceId,
                                            @Nonnull final String availabilityZone,
                                            @Nonnull final AutoScalingGroup group ) {
    final AutoScalingInstance autoScalingInstance = new AutoScalingInstance( ownerFullName, instanceId );
    autoScalingInstance.setAvailabilityZone( availabilityZone );
    autoScalingInstance.setAutoScalingGroup( group );
    autoScalingInstance.setLaunchConfigurationName(
        AutoScalingMetadatas.toDisplayName().apply( group.getLaunchConfiguration() ) );
    autoScalingInstance.setHealthStatus( HealthStatus.Healthy );
    autoScalingInstance.setLifecycleState( LifecycleState.Pending );
    autoScalingInstance.setConfigurationState( ConfigurationState.Instantiated );
    autoScalingInstance.setRegistrationAttempts( 0 );
    return autoScalingInstance;
  }

  @PrePersist
  @PreUpdate
  private void preUpdate() {
    autoScalingGroupName = AutoScalingMetadatas.toDisplayName().apply( autoScalingGroup );
    if ( lifecycleState == LifecycleState.InService && getInServiceTimestamp() == null ) {
      setInServiceTimestamp( new Date() );
    }
  }
}
