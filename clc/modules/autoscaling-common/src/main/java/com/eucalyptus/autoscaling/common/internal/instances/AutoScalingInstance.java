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
package com.eucalyptus.autoscaling.common.internal.instances;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingInstanceMetadata;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

import org.apache.log4j.Logger;
import com.eucalyptus.autoscaling.common.AutoScalingBackend;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_auto_scaling_instances" )
public class AutoScalingInstance extends AbstractOwnedPersistent implements AutoScalingInstanceMetadata {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_availability_zone", nullable = false, updatable = false )
  private String availabilityZone;

  @Column( name = "metadata_health_status", nullable = false )
  @Enumerated( EnumType.STRING )
  private HealthStatus healthStatus;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_auto_scaling_group_id" )
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

  @Column( name = "metadata_protected_from_scale_in" )
  private Boolean protectedFromScaleIn;

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

  public Boolean getProtectedFromScaleIn() {
    return protectedFromScaleIn;
  }

  public void setProtectedFromScaleIn( final Boolean protectedFromScaleIn ) {
    this.protectedFromScaleIn = protectedFromScaleIn;
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
                                            @Nonnull final Boolean protectedFromScaleIn,
                                            @Nonnull final AutoScalingGroup group ) {
    final AutoScalingInstance autoScalingInstance = new AutoScalingInstance( ownerFullName, instanceId );
    autoScalingInstance.setAvailabilityZone( availabilityZone );
    autoScalingInstance.setProtectedFromScaleIn( protectedFromScaleIn );
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

  @EntityUpgrade( entities = AutoScalingInstance.class, since = Upgrades.Version.v5_0_0, value = AutoScalingBackend.class)
  public enum AutoScalingInstanceUpgrade500 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger(AutoScalingInstanceUpgrade500.class);

    @Override
    public boolean apply(@Nullable Class aClass) {
      try ( final TransactionResource tran = Entities.transactionFor( AutoScalingInstance.class ) ) {
        final List<AutoScalingInstance> instances =
            Entities.criteriaQuery( Entities.restriction( AutoScalingInstance.class )
                .isNull( AutoScalingInstance_.protectedFromScaleIn )
            ).list( );
        for ( final AutoScalingInstance instance : instances ) {
          if ( instance.getProtectedFromScaleIn( ) == null ) {
            instance.setProtectedFromScaleIn( false );
            LOG.info( "Set default scale in protection for auto scaling instance : " + instance.getInstanceId( ) );
          }
        }
        tran.commit( );
      }
      catch (Exception ex) {
        LOG.error("Exception during upgrade while attempting to initialize scaling protection for instances");
        throw Exceptions.toUndeclared(ex);
      }
      return true;
    }
  }
}
