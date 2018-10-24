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
package com.eucalyptus.autoscaling.common.internal.configurations;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import java.util.List;
import javax.persistence.EntityTransaction;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappings;
import com.eucalyptus.autoscaling.common.msgs.InstanceMonitoring;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

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
    return !Iterables.isEmpty( groups ) && Iterables.get( groups, 0 ).matches( "sg-[0-9A-Fa-f]{8}(?:[0-9a-fA-F]{9})?" );
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
      type.setAssociatePublicIpAddress( launchConfiguration.getAssociatePublicIpAddress() );
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
