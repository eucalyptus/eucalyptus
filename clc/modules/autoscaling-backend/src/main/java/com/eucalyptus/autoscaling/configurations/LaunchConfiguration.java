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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_launch_configurations" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LaunchConfiguration extends AbstractOwnedPersistent implements LaunchConfigurationMetadata {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_image_id", nullable = false )
  private String imageId;

  @Column( name = "metadata_kernel_id" )
  private String kernelId;

  @Column( name = "metadata_ramdisk_id" )
  private String ramdiskId;

  @Column( name = "metadata_key_name" )
  private String keyName;

  /**
   * Security groups can be names or identifiers, but not a mix.
   */
  @ElementCollection
  @CollectionTable( name = "metadata_launch_configuration_security_groups" )
  @Column( name = "metadata_security_group" )
  @JoinColumn( name = "metadata_launch_configuration_id" )
  @OrderColumn( name = "metadata_security_group_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<String> securityGroups = Lists.newArrayList();

  @Column( name = "metadata_user_data" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")  
  private String userData;

  @Column( name = "metadata_instance_type", nullable = false )
  private String instanceType;
  
  @ElementCollection  
  @CollectionTable( name = "metadata_launch_configuration_block_device_mappings" )
  @JoinColumn( name = "metadata_launch_configuration_id" )
  @OrderColumn( name = "metadata_device_mapping_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<BlockDeviceMapping> blockDeviceMappings = Lists.newArrayList();

  @Column( name = "metadata_instance_monitoring" )
  private Boolean instanceMonitoring;

  @Column( name = "metadata_instance_profile", length = 1600)
  private String iamInstanceProfile;

  protected LaunchConfiguration() {
  }

  protected LaunchConfiguration( final OwnerFullName owner ) {
    super( owner );
  }

  protected LaunchConfiguration( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public String getLaunchConfigurationName() {
    return getDisplayName();
  }

  public String getImageId() {
    return imageId;
  }

  public void setImageId( final String imageId ) {
    this.imageId = imageId;
  }

  public String getKernelId() {
    return kernelId;
  }

  public void setKernelId( final String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getRamdiskId() {
    return ramdiskId;
  }

  public void setRamdiskId( final String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public String getKeyName() {
    return keyName;
  }

  public void setKeyName( final String keyName ) {
    this.keyName = keyName;
  }

  public List<String> getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups( final List<String> securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public String getUserData() {
    return userData;
  }

  public void setUserData( final String userData ) {
    this.userData = userData;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public void setInstanceType( final String instanceType ) {
    this.instanceType = instanceType;
  }

  public List<BlockDeviceMapping> getBlockDeviceMappings() {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( final List<BlockDeviceMapping> blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public Boolean getInstanceMonitoring() {
    return instanceMonitoring;
  }

  public void setInstanceMonitoring( final Boolean instanceMonitoring ) {
    this.instanceMonitoring = instanceMonitoring;
  }

  public String getIamInstanceProfile() {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( final String iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  @Override
  public String getArn() {
    return String.format( 
        "arn:aws:autoscaling::%1s:launchConfiguration:%2s:launchConfigurationName/%3s", 
        getOwnerAccountNumber(), 
        getNaturalId(), 
        getDisplayName() );
  }

  /**
   * Create an example LaunchConfiguration for the given owner. 
   * 
   * @param ownerFullName The owner
   * @return The example
   */
  public static LaunchConfiguration withOwner( final OwnerFullName ownerFullName ) {
    return new LaunchConfiguration( ownerFullName );
  }

  /**
   * Create an example LaunchConfiguration for the given owner and name. 
   *
   * @param ownerFullName The owner
   * @param name The name
   * @return The example
   */
  public static LaunchConfiguration named( final OwnerFullName ownerFullName,
                                           final String name ) {
    return new LaunchConfiguration( ownerFullName, name );
  }

  public static LaunchConfiguration withId( final String id ) {
    final LaunchConfiguration example = new LaunchConfiguration();
    example.setId( id );
    return example;
  }

  public static LaunchConfiguration withUuid( final String uuid ) {
    final LaunchConfiguration example = new LaunchConfiguration();
    example.setNaturalId( uuid );
    return example;
  }

  public static LaunchConfiguration create( final OwnerFullName ownerFullName,
                                            final String name,
                                            final String imageId,
                                            final String instanceType ) {
    final LaunchConfiguration launchConfiguration = new LaunchConfiguration( ownerFullName, name );
    launchConfiguration.setImageId( imageId );
    launchConfiguration.setInstanceType( instanceType );
    return launchConfiguration;
  }
    
  protected static abstract class BaseBuilder<T extends BaseBuilder<T>> {
    private OwnerFullName ownerFullName;
    private String name;
    private String instanceType;
    private String imageId;
    private String kernelId;
    private String ramdiskId;
    private String keyName;
    private String userData;
    private Boolean instanceMonitoring;
    private String iamInstanceProfile;
    private Set<String> securityGroups = Sets.newLinkedHashSet();
    private Set<BlockDeviceMapping> blockDeviceMappings = Sets.newLinkedHashSet();

    BaseBuilder( final OwnerFullName ownerFullName,
                 final String name,
                 final String imageId,
                 final String instanceType ) { 
      this.ownerFullName = ownerFullName;
      this.name = name;
      this.imageId = imageId;
      this.instanceType = instanceType;
    } 
    
    protected abstract T builder();
    
    public T withKernelId( final String kernelId ) {
      this.kernelId  = kernelId;      
      return builder();
    }

    public T withRamdiskId( final String ramdiskId ) {
      this.ramdiskId  = ramdiskId;
      return builder();
    }

    public T withKeyName( final String keyName ) {
      this.keyName  = keyName;
      return builder();
    }

    public T withUserData( final String userData ) {
      this.userData  = userData;
      return builder();
    }

    public T withInstanceMonitoring( final Boolean instanceMonitoring ) {
      this.instanceMonitoring  = instanceMonitoring;
      return builder();
    }

    public T withInstanceProfile( final String iamInstanceProfile ) {
      this.iamInstanceProfile  = iamInstanceProfile;
      return builder();
    }

    public T withSecurityGroups( final Collection<String> securityGroups ) {
      if ( securityGroups != null ) {
        this.securityGroups.addAll( securityGroups );
      }
      return builder();
    }

    public T withBlockDeviceMapping( final String deviceName,
                                     final String virtualName,
                                     final String snapshotId,
                                     final Integer volumeSize ) {
      this.blockDeviceMappings.add( 
          new BlockDeviceMapping( deviceName, virtualName, snapshotId, volumeSize ) );
      return builder();
    }

    protected LaunchConfiguration build() {
      final LaunchConfiguration configuration =
          LaunchConfiguration.create( ownerFullName, name, imageId, instanceType );
      configuration.setKernelId( kernelId );
      configuration.setRamdiskId( ramdiskId );
      configuration.setKeyName( keyName );
      configuration.setUserData( userData );
      configuration.setInstanceMonitoring( Objects.firstNonNull( instanceMonitoring, Boolean.TRUE ) );
      configuration.setIamInstanceProfile( iamInstanceProfile );
      configuration.setSecurityGroups( Lists.newArrayList( securityGroups ) );
      configuration.setBlockDeviceMappings( Lists.newArrayList( blockDeviceMappings ) );
      return configuration;
    }
  }
}
