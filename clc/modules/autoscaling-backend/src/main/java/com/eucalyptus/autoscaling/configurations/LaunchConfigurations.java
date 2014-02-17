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
package com.eucalyptus.autoscaling.configurations;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.backend.msgs.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.backend.msgs.BlockDeviceMappings;
import com.eucalyptus.autoscaling.common.backend.msgs.InstanceMonitoring;
import com.eucalyptus.autoscaling.common.backend.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.backend.msgs.SecurityGroups;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.EbsDeviceMapping;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

/**
 *
 */
public abstract class LaunchConfigurations {

  public abstract <T> List<T> list( OwnerFullName ownerFullName,
                                    Predicate<? super LaunchConfiguration> filter,
                                    Function<? super LaunchConfiguration, T> transform ) throws AutoScalingMetadataException;

  public abstract <T> T lookup( OwnerFullName ownerFullName,
                                String launchConfigurationNameOrArn,
                                Function<? super LaunchConfiguration, T> transform ) throws AutoScalingMetadataException;
  
  public abstract boolean delete( LaunchConfigurationMetadata launchConfiguration ) throws AutoScalingMetadataException;

  public abstract LaunchConfiguration save( LaunchConfiguration launchConfiguration ) throws AutoScalingMetadataException;
  
  public final PersistingBuilder create( final OwnerFullName ownerFullName,
                                   final String launchConfigurationName,
                                   final String imageId,
                                   final String instanceType ) {
    return new PersistingBuilder( this, ownerFullName, launchConfigurationName, imageId, instanceType );
  }

  public static boolean containsSecurityGroupIdentifiers( final Iterable<String> groups ) {
    return !Iterables.isEmpty( groups ) && Iterables.get( groups, 0 ).matches( "sg-[0-9A-Fa-f]{8}" );
  }

  public static class PersistingBuilder extends LaunchConfiguration.BaseBuilder<PersistingBuilder> {
    private final LaunchConfigurations launchConfigurations;
    
    PersistingBuilder( final LaunchConfigurations launchConfigurations,
                       final OwnerFullName ownerFullName, 
                       final String name, 
                       final String imageId, 
                       final String instanceType ) {
      super( ownerFullName, name, imageId, instanceType );
      this.launchConfigurations = launchConfigurations;
    }

    @Override
    protected PersistingBuilder builder() {
      return this;
    }
    
    public LaunchConfiguration persist() throws AutoScalingMetadataException {
      return launchConfigurations.save( build() );
    }
  }

  @TypeMapper
  public enum LaunchConfigurationCoreViewTransform implements Function<LaunchConfiguration,LaunchConfigurationCoreView> {
    INSTANCE;

    @Override
    public LaunchConfigurationCoreView apply( final LaunchConfiguration launchConfiguration ) {
      return new LaunchConfigurationCoreView( launchConfiguration );
    }
  }

  @TypeMapper
  public enum LaunchConfigurationMinimumViewTransform implements Function<LaunchConfiguration,LaunchConfigurationMinimumView> {
    INSTANCE;

    @Override
    public LaunchConfigurationMinimumView apply( final LaunchConfiguration launchConfiguration ) {
      return new LaunchConfigurationMinimumView( launchConfiguration );
    }
  }

  @TypeMapper
  public enum LaunchConfigurationTransform implements Function<LaunchConfiguration, LaunchConfigurationType> {
    INSTANCE;

    @Override
    public LaunchConfigurationType apply( final LaunchConfiguration launchConfiguration ) {
      final LaunchConfigurationType type = new LaunchConfigurationType();

      type.setCreatedTime( launchConfiguration.getCreationTimestamp() );
      type.setIamInstanceProfile( launchConfiguration.getIamInstanceProfile() );
      type.setImageId( launchConfiguration.getImageId() );
      if (launchConfiguration.getInstanceMonitoring() != null) 
        type.setInstanceMonitoring( new InstanceMonitoring( launchConfiguration.getInstanceMonitoring() ) );
      type.setInstanceType( launchConfiguration.getInstanceType() );
      type.setKernelId( launchConfiguration.getKernelId() );
      type.setKeyName( launchConfiguration.getKeyName() );
      type.setLaunchConfigurationARN( launchConfiguration.getArn() );
      type.setLaunchConfigurationName( launchConfiguration.getLaunchConfigurationName() );
      type.setRamdiskId( launchConfiguration.getRamdiskId() );
      if ( launchConfiguration.getSecurityGroups() != null &&
          !launchConfiguration.getSecurityGroups().isEmpty() )
        type.setSecurityGroups( new SecurityGroups( launchConfiguration.getSecurityGroups() ) );
      type.setUserData( launchConfiguration.getUserData() ); 
      if ( launchConfiguration.getBlockDeviceMappings() != null && 
          !launchConfiguration.getBlockDeviceMappings().isEmpty() )
        type.setBlockDeviceMappings( new BlockDeviceMappings( Collections2.transform( 
          launchConfiguration.getBlockDeviceMappings(), BlockDeviceTransform.INSTANCE
        ) ) );
      
      return type;
    }
  }

  @TypeMapper
  public enum BlockDeviceTransform implements Function<BlockDeviceMapping, BlockDeviceMappingType> {
    INSTANCE;

    @Override
    public BlockDeviceMappingType apply( final BlockDeviceMapping blockDeviceMapping ) {
      return new BlockDeviceMappingType(
          blockDeviceMapping.getDeviceName(),
          blockDeviceMapping.getVirtualName(),
          blockDeviceMapping.getSnapshotId(),
          blockDeviceMapping.getVolumeSize() );
    }
  }

  @TypeMapper
  public enum LaunchConfigurationToRunInstances implements Function<LaunchConfigurationCoreView,RunInstancesType> {
    INSTANCE;

    @Override
    public RunInstancesType apply( final LaunchConfigurationCoreView launchConfiguration ) {
      final RunInstancesType runInstances = new RunInstancesType();
      runInstances.setKernelId( launchConfiguration.getKernelId() );
      runInstances.setRamdiskId( launchConfiguration.getRamdiskId() );
      runInstances.setImageId( launchConfiguration.getImageId() );
      runInstances.setInstanceType( launchConfiguration.getInstanceType() );
      runInstances.setMinCount( 1 );
      runInstances.setMaxCount( 1 );
      for ( final BlockDeviceMapping mapping : launchConfiguration.getBlockDeviceMappings() ) {
        final BlockDeviceMappingItemType type = new BlockDeviceMappingItemType();
        type.setDeviceName( mapping.getDeviceName() );
        type.setVirtualName( mapping.getVirtualName() );
        if ( mapping.getSnapshotId() != null || mapping.getVolumeSize() != null ) {
          final EbsDeviceMapping ebsType = new EbsDeviceMapping();
          ebsType.setSnapshotId( mapping.getSnapshotId() );
          ebsType.setVolumeSize( mapping.getVolumeSize() );
          ebsType.setDeleteOnTermination( true );
          type.setEbs( ebsType );
        }
        runInstances.getBlockDeviceMapping().add( type );
      }
      runInstances.setKeyName( launchConfiguration.getKeyName() );
      final ArrayList<String> securityGroups =
          Lists.newArrayList( launchConfiguration.getSecurityGroups() );
      if ( containsSecurityGroupIdentifiers( securityGroups ) ) {
        runInstances.setGroupIdSet( securityGroups );
      } else {
        runInstances.setGroupSet( securityGroups );
      }
      runInstances.setMonitoring( launchConfiguration.getInstanceMonitoring() );
      if ( launchConfiguration.getIamInstanceProfile() != null ) {
        runInstances.setInstanceProfileNameOrArn( launchConfiguration.getIamInstanceProfile() );
      }
      runInstances.setUserData(launchConfiguration.getUserData());
      return runInstances;
    }
  }
  
  @RestrictedTypes.QuantityMetricFunction( LaunchConfigurationMetadata.class )
  public enum CountLaunchConfigurations implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( LaunchConfiguration.class );
      try {
        return Entities.count( LaunchConfiguration.withOwner( input ) );
      } finally {
        db.rollback( );
      }
    }
  }  
}
