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
import java.util.List;
import java.util.Set;
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.backend.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.backend.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.backend.msgs.EnabledMetrics;
import com.eucalyptus.autoscaling.common.backend.msgs.Instance;
import com.eucalyptus.autoscaling.common.backend.msgs.Instances;
import com.eucalyptus.autoscaling.common.backend.msgs.LoadBalancerNames;
import com.eucalyptus.autoscaling.common.backend.msgs.ProcessType;
import com.eucalyptus.autoscaling.common.backend.msgs.SuspendedProcessType;
import com.eucalyptus.autoscaling.common.backend.msgs.SuspendedProcesses;
import com.eucalyptus.autoscaling.common.backend.msgs.TagType;
import com.eucalyptus.autoscaling.common.backend.msgs.TerminationPolicies;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
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
import com.google.common.collect.Ordering;

/**
 *
 */
public abstract class AutoScalingGroups {

  public abstract <T> List<T> list( OwnerFullName ownerFullName,
                                    Predicate<? super AutoScalingGroup> filter,
                                    Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T> listRequiringScaling( Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T> listRequiringInstanceReplacement( Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T> listRequiringMonitoring( long interval,
                                                       Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException;
  
  public abstract <T> T lookup( OwnerFullName ownerFullName,
                                String autoScalingGroupName,
                                Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException;

  public abstract void update( OwnerFullName ownerFullName,
                               String autoScalingGroupName,
                              Callback<AutoScalingGroup> groupUpdateCallback ) throws AutoScalingMetadataException;

  public abstract void markScalingRequiredForZones( Set<String> availabilityZones ) throws AutoScalingMetadataException;

  public abstract boolean delete( AutoScalingGroupMetadata autoScalingGroup ) throws AutoScalingMetadataException;

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
      type.setEnabledMetrics( new EnabledMetrics( group.getEnabledMetrics() == null ?
          null :
          Ordering.natural().sortedCopy( Iterables.transform( group.getEnabledMetrics(), Strings.toStringFunction() ) ) ) );
      type.setHealthCheckGracePeriod( group.getHealthCheckGracePeriod() );
      type.setHealthCheckType( Strings.toString( group.getHealthCheckType() ) );
      type.setLaunchConfigurationName( AutoScalingMetadatas.toDisplayName().apply( group.getLaunchConfiguration() ) );
      type.setLoadBalancerNames( new LoadBalancerNames( group.getLoadBalancerNames() ) );
      type.setMaxSize( group.getMaxSize() );
      type.setMinSize( group.getMinSize() );
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
      if ( group.getAutoScalingInstances() != null && !group.getAutoScalingInstances().isEmpty() ) {
        type.setInstances( new Instances() );
        Iterables.addAll( type.getInstances().getMember(),
            Iterables.transform(
                group.getAutoScalingInstances(),
                TypeMappers.lookup( AutoScalingInstance.class, Instance.class ) ) );
      }

      return type;
    }
  }

  @TypeMapper
  public enum AutoScalingGroupMinimumViewTransform implements Function<AutoScalingGroup, AutoScalingGroupMinimumView> {
    INSTANCE;

    @Override
    public AutoScalingGroupMinimumView apply( final AutoScalingGroup group ) {
      return new AutoScalingGroupMinimumView( group );
    }
  }

  @TypeMapper
  public enum AutoScalingGroupCoreViewTransform implements Function<AutoScalingGroup, AutoScalingGroupCoreView> {
    INSTANCE;

    @Override
    public AutoScalingGroupCoreView apply( final AutoScalingGroup group ) {
      return new AutoScalingGroupCoreView( group );
    }
  }

  @TypeMapper
  public enum AutoScalingGroupScalingViewTransform implements Function<AutoScalingGroup, AutoScalingGroupScalingView> {
    INSTANCE;

    @Override
    public AutoScalingGroupScalingView apply( final AutoScalingGroup group ) {
      return new AutoScalingGroupScalingView( group );
    }
  }

  @TypeMapper
  public enum AutoScalingGroupMetricsViewTransform implements Function<AutoScalingGroup, AutoScalingGroupMetricsView> {
    INSTANCE;

    @Override
    public AutoScalingGroupMetricsView apply( final AutoScalingGroup group ) {
      return new AutoScalingGroupMetricsView( group );
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

  @RestrictedTypes.QuantityMetricFunction( AutoScalingGroupMetadata.class )
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
