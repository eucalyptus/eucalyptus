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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.common.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.ProcessType;
import com.eucalyptus.autoscaling.common.SuspendedProcessType;
import com.eucalyptus.autoscaling.common.SuspendedProcesses;
import com.eucalyptus.autoscaling.common.TagType;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.AvailabilityZones;
import com.eucalyptus.autoscaling.common.LoadBalancerNames;
import com.eucalyptus.autoscaling.common.TerminationPolicies;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.tags.AutoScalingGroupTag;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 *
 */
public abstract class AutoScalingGroups {

  public abstract List<AutoScalingGroup> list( OwnerFullName ownerFullName ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingGroup> list( OwnerFullName ownerFullName,
                                               Predicate<? super AutoScalingGroup> filter ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingGroup> listRequiringScaling( ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingGroup> listRequiringInstanceReplacement( ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingGroup> listRequiringMonitoring( long interval ) throws AutoScalingMetadataException;
  
  public abstract AutoScalingGroup lookup( OwnerFullName ownerFullName,
                                           String autoScalingGroupName ) throws AutoScalingMetadataException;

  public abstract AutoScalingGroup update( OwnerFullName ownerFullName,
                                           String autoScalingGroupName,
                                           Callback<AutoScalingGroup> groupUpdateCallback ) throws AutoScalingMetadataException;

  public abstract void markScalingRequiredForZones( Set<String> availabilityZones ) throws AutoScalingMetadataException;

  public abstract boolean delete( AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException;

  public abstract AutoScalingGroup save( AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException;

  public final PersistingBuilder create( final OwnerFullName ownerFullName,
                                         final String autoScalingGroupName,
                                         final LaunchConfiguration launchConfiguration,
                                         final Integer minSize,
                                         final Integer maxSize ) {
    return new PersistingBuilder( this, ownerFullName, autoScalingGroupName, launchConfiguration, minSize, maxSize );
  }

  public static class PersistingBuilder extends AutoScalingGroup.BaseBuilder<PersistingBuilder> {
    private final AutoScalingGroups autoScalingGroups;

    PersistingBuilder( final AutoScalingGroups autoScalingGroups,
                       final OwnerFullName ownerFullName,
                       final String name,
                       final LaunchConfiguration launchConfiguration,
                       final Integer minSize,
                       final Integer maxSize ) {
      super( ownerFullName, name, launchConfiguration, minSize, maxSize );
      this.autoScalingGroups = autoScalingGroups;
    }

    @Override
    protected PersistingBuilder builder() {
      return this;
    }

    public AutoScalingGroup persist() throws AutoScalingMetadataException {
      return autoScalingGroups.save( build() );
    }
  }

  @TypeMapper
  public enum AutoScalingGroupTransform implements Function<AutoScalingGroup, AutoScalingGroupType> {
    INSTANCE;

    @Override
    public AutoScalingGroupType apply( final AutoScalingGroup group ) {
      final AutoScalingGroupType type = new AutoScalingGroupType();

      type.setAutoScalingGroupARN( group.getArn() );
      type.setAutoScalingGroupName( group.getAutoScalingGroupName() );
      type.setAvailabilityZones( new AvailabilityZones( group.getAvailabilityZones() ) );
      type.setCreatedTime( group.getCreationTimestamp() );
      type.setDefaultCooldown( group.getDefaultCooldown() );
      type.setDesiredCapacity( group.getDesiredCapacity() );
      //type.setEnabledMetrics(); //TODO:STEVE: auto scaling group mapping for enabled metrics
      type.setHealthCheckGracePeriod( group.getHealthCheckGracePeriod() );
      type.setHealthCheckType( Strings.toString( group.getHealthCheckType() ) );
      type.setLaunchConfigurationName( AutoScalingMetadatas.toDisplayName().apply( group.getLaunchConfiguration() ) );
      type.setLoadBalancerNames( new LoadBalancerNames( group.getLoadBalancerNames() ) );
      type.setMaxSize( group.getMaxSize() );
      type.setMinSize( group.getMinSize() );
      //type.setPlacementGroup(); //TODO:STEVE: auto scaling group mapping for placement groups?
      type.setStatus( group.getStatus() );
      final Collection<SuspendedProcess> suspendedProcesses = group.getSuspendedProcesses();
      if ( suspendedProcesses != null && !suspendedProcesses.isEmpty() ) {
        type.setSuspendedProcesses( new SuspendedProcesses() );
        Iterables.addAll(
            type.getSuspendedProcesses().getMember(),
            Iterables.transform(
                suspendedProcesses,
                TypeMappers.lookup(SuspendedProcess.class, SuspendedProcessType.class ) ) );
      }
      type.setTerminationPolicies( new TerminationPolicies( group.getTerminationPolicies() == null ? 
          null : 
          Collections2.transform( group.getTerminationPolicies(), Strings.toStringFunction() ) ) );
      //type.setVpcZoneIdentifier(); //TODO:STEVE: auto scaling group mapping for vpc zone identifiers?      

      return type;
    }
  }

  @TypeMapper
  public enum SuspendedProcessTransform implements Function<SuspendedProcess, SuspendedProcessType> {
    INSTANCE;

    @Override
    public SuspendedProcessType apply( final SuspendedProcess suspendedProcess ) {
      final SuspendedProcessType suspendedProcessType = new SuspendedProcessType();
      suspendedProcessType.setProcessName( suspendedProcess.getScalingProcessType().toString() );
      suspendedProcessType.setSuspensionReason( suspendedProcess.getReason() );
      return suspendedProcessType;
    }
  }

  @TypeMapper
  public enum ScalingProcessTypeTransform implements Function<ScalingProcessType, ProcessType> {
    INSTANCE;

    @Override
    public ProcessType apply( final ScalingProcessType scalingProcessType ) {
      final ProcessType processType = new ProcessType();
      processType.setProcessName( scalingProcessType.toString() );
      return processType;
    }
  }

  @TypeMapper
  public enum AutoScalingTagTransform implements Function<TagType, AutoScalingGroupTag> {
    INSTANCE;

    @Override
    public AutoScalingGroupTag apply( final TagType tagType ) {
      final AutoScalingGroupTag groupTag = AutoScalingGroupTag.createUnassigned();
      groupTag.setKey( tagType.getKey() );
      groupTag.setValue( Objects.firstNonNull( tagType.getValue(), "" ) );
      groupTag.setPropagateAtLaunch( Objects.firstNonNull( tagType.getPropagateAtLaunch(), Boolean.FALSE ) );
      return groupTag;
    }
  }

  @RestrictedTypes.QuantityMetricFunction( AutoScalingMetadata.AutoScalingGroupMetadata.class )
  public enum CountAutoScalingGroups implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( AutoScalingGroup.class );
      try {
        return Entities.count( AutoScalingGroup.withOwner( input ) );
      } finally {
        db.rollback( );
      }
    }
  }
}
