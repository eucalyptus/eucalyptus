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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.run;

import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowEbsMapping;
import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowSuppressMapping;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.Platform;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.IllegalMetadataAccessException;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.VerificationException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.context.Context;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.BootableImageInfo;
import com.eucalyptus.images.DeviceMapping;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class VerifyMetadata {
  private static Logger LOG = Logger.getLogger( VerifyMetadata.class );
  private static final long BYTES_PER_GB = ( 1024L * 1024L * 1024L );
  
  public static Predicate<Allocation> get( ) {
    return Predicates.and( Lists.transform( verifiers, AsPredicate.INSTANCE ) );
  }
  

  private interface MetadataVerifier {
    public abstract boolean apply( Allocation allocInfo ) throws MetadataException, AuthException;
  }
  
  private static final ArrayList<? extends MetadataVerifier> verifiers = Lists.newArrayList( VmTypeVerifier.INSTANCE, PartitionVerifier.INSTANCE,
                                                                                                ImageVerifier.INSTANCE, KeyPairVerifier.INSTANCE,
                                                                                                NetworkGroupVerifier.INSTANCE, RoleVerifier.INSTANCE,
                                                                                                BlockDeviceMapVerifier.INSTANCE, UserDataVerifier.INSTANCE );
  
  private enum AsPredicate implements Function<MetadataVerifier, Predicate<Allocation>> {
    INSTANCE;
    @Override
    public Predicate<Allocation> apply( final MetadataVerifier arg0 ) {
      return new Predicate<Allocation>( ) {
        
        @Override
        public boolean apply( Allocation allocInfo ) {
          try {
            return arg0.apply( allocInfo );
          } catch ( Exception ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        }
      };
    }
  }
  
  enum VmTypeVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      Context ctx = allocInfo.getContext( );
      User user = ctx.getUser( );
      String instanceType = allocInfo.getRequest( ).getInstanceType( );
      VmType vmType = VmTypes.lookup( instanceType );
      if ( !ctx.hasAdministrativePrivileges( ) && !RestrictedTypes.filterPrivileged( ).apply( vmType ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to allocate vm type " + instanceType + " for " + ctx.getUserFullName( ) );
      }
      allocInfo.setVmType( vmType );
      return true;
    }
  }
  
  enum PartitionVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      RunInstancesType request = allocInfo.getRequest( );
      String zoneName = request.getAvailabilityZone( );
      if ( Clusters.getInstance( ).listValues( ).isEmpty( ) ) {
        LOG.debug( "enabled values: " + Joiner.on( "\n" ).join( Clusters.getInstance( ).listValues( ) ) );
        LOG.debug( "disabled values: " + Joiner.on( "\n" ).join( Clusters.getInstance( ).listValues( ) ) );
        throw new VerificationException( "Not enough resources: no cluster controller is currently available to run instances." );
      } else if ( Partitions.exists( zoneName ) ) {
        Partition partition = Partitions.lookupByName( zoneName );
        allocInfo.setPartition( partition );
      } else if ( Partition.DEFAULT_NAME.equals( zoneName ) ) {
        Partition partition = Partition.DEFAULT;
        allocInfo.setPartition( partition );
      } else {
        throw new VerificationException( "Not enough resources: no cluster controller is currently available to run instances." );
      }
      return true;
    }
  }
  
  enum ImageVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException, AuthException {
      RunInstancesType msg = allocInfo.getRequest( );
      String imageId = msg.getImageId( );
      VmType vmType = allocInfo.getVmType( );
      try {
        BootableSet bootSet = Emis.newBootableSet( imageId );
        allocInfo.setBootableSet( bootSet );
        
        // Add (1024L * 1024L * 10) to handle NTFS min requirements.
        if ( bootSet.isBlockStorage( ) ) {
        } else if ( Platform.windows.equals( bootSet.getMachine( ).getPlatform( ) ) && bootSet.getMachine( ).getImageSizeBytes( ) > ( ( 1024L * 1024L * 1024L * vmType.getDisk( ) ) + ( 1024L * 1024L * 10 ) ) ) {
          throw new VerificationException( "Unable to run instance " + bootSet.getMachine( ).getDisplayName( ) +
                                           " in which the size " + bootSet.getMachine( ).getImageSizeBytes( ) +
                                           " bytes of the instance is greater than the vmType " + vmType.getDisplayName( ) + " size " + vmType.getDisk( )
                                           + " GB." );
        } else if ( bootSet.getMachine( ).getImageSizeBytes( ) >= ( ( 1024L * 1024L * 1024L * vmType.getDisk( ) ) ) ) {
            throw new VerificationException( "Unable to run instance " + bootSet.getMachine( ).getDisplayName( ) +
                    " in which the size " + bootSet.getMachine( ).getImageSizeBytes( ) +
                    " bytes of the instance is greater than the vmType " + vmType.getDisplayName( ) + " size " + vmType.getDisk( )
                    + " GB." );
        }
        
      } catch ( MetadataException ex ) {
        LOG.error( ex );
        throw ex;
      } catch ( RuntimeException ex ) {
        LOG.error( ex );
        throw new VerificationException( "Failed to verify references for request: " + msg.toSimpleString( ) + " because of: " + ex.getMessage( ), ex );
      }
      return true;
    }
  }
  
  enum KeyPairVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      if ( allocInfo.getRequest( ).getKeyName( ) == null || "".equals( allocInfo.getRequest( ).getKeyName( ) ) ) {
        if ( ImageMetadata.Platform.windows.equals( allocInfo.getBootSet( ).getMachine( ).getPlatform( ) ) ) {
          throw new InvalidMetadataException( "You must specify a keypair when running a windows vm: " + allocInfo.getRequest( ).getImageId( ) );
        } else {
          allocInfo.setSshKeyPair( KeyPairs.noKey( ) );
          return true;
        }
      }
      Context ctx = allocInfo.getContext( );
      RunInstancesType request = allocInfo.getRequest( );
      String keyName = request.getKeyName( );
      SshKeyPair key = KeyPairs.lookup( ctx.getUserFullName( ).asAccountFullName( ), keyName );
      if ( !ctx.hasAdministrativePrivileges( ) && !RestrictedTypes.filterPrivileged( ).apply( key ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use keypair " + keyName + " by " + ctx.getUser( ).getName( ) );
      }
      allocInfo.setSshKeyPair( key );
      return true;
    }
  }
  
  enum NetworkGroupVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      final Context ctx = allocInfo.getContext( );
      final AccountFullName accountFullName = ctx.getUserFullName().asAccountFullName();
      NetworkGroups.lookup( accountFullName, NetworkGroups.defaultNetworkName( ) );


      final Set<String> networkNames = Sets.newLinkedHashSet( allocInfo.getRequest( ).securityGroupNames( ) );
      final Set<String> networkIds = Sets.newLinkedHashSet( allocInfo.getRequest().securityGroupsIds() );
      if ( networkNames.isEmpty( ) && networkIds.isEmpty() ) {
        networkNames.add( NetworkGroups.defaultNetworkName( ) );
      }

      final List<NetworkGroup> groups = Lists.newArrayList( );
      // AWS EC2 lets you pass a name as an ID, but not an ID as a name.
      for ( String groupName : networkNames ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupName, RestrictedTypes.toDisplayName() ) ).isPresent() ) {
          groups.add( NetworkGroups.lookup( accountFullName, groupName ) );
        }
      }
      for ( String groupId : networkIds ) {
        if ( !Iterables.tryFind( groups, CollectionUtils.propertyPredicate( groupId, NetworkGroups.groupId() ) ).isPresent() ) {
          groups.add( NetworkGroups.lookupByGroupId( accountFullName, groupId ) );
        }
      }

      final Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( );
      for ( final NetworkGroup group : groups ) {
        if ( !ctx.hasAdministrativePrivileges( ) && !RestrictedTypes.filterPrivileged( ).apply( group ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use network group " +group.getGroupId()+ "/" + group.getDisplayName() + " for " + ctx.getUser( ).getName( ) );
        }
        networkRuleGroups.put( group.getDisplayName(), group );
      }

      allocInfo.setNetworkRules( networkRuleGroups );

      return true;
    }
  }

  enum RoleVerifier implements MetadataVerifier {
    INSTANCE;

    @Override
    public boolean apply( final Allocation allocInfo ) throws MetadataException {
      final Context context = allocInfo.getContext( );
      final String instanceProfileArn = allocInfo.getRequest( ).getIamInstanceProfileArn( );
      final String instanceProfileName = allocInfo.getRequest( ).getIamInstanceProfileName( );
      if ( !Strings.isNullOrEmpty( instanceProfileArn ) ||
          !Strings.isNullOrEmpty( instanceProfileName ) ) {

        final InstanceProfile profile;
        if ( !Strings.isNullOrEmpty( instanceProfileArn ) ) try {
          final Ern name = Ern.parse( instanceProfileArn );
          if ( !( name instanceof EuareResourceName) ) {
            throw new MetadataException( "Invalid IAM instance profile ARN: " + instanceProfileArn );
          }
          profile = Accounts.lookupAccountById( name.getNamespace( ) )
              .lookupInstanceProfileByName( ((EuareResourceName) name).getName() );
          if ( !Strings.isNullOrEmpty( instanceProfileName ) &&
              !instanceProfileName.equals( profile.getName() ) ) {
            throw new MetadataException( String.format(
                "Invalid IAM instance profile name '%s' for ARN: %s", name, instanceProfileArn) );
          }
        } catch ( AuthException e ) {
          throw new NoSuchMetadataException( "Invalid IAM instance profile ARN: " + instanceProfileArn, e );
        } else if ( !Strings.isNullOrEmpty( instanceProfileName ) ) try {
          profile = context.getAccount( ).lookupInstanceProfileByName( instanceProfileName );
        } catch ( AuthException e ) {
          throw new NoSuchMetadataException( "Invalid IAM instance profile name: " + instanceProfileName, e );
        } else {
          profile = null;
        }

        if ( profile != null ) try {
          final String profileArn = Accounts.getInstanceProfileArn( profile );
          if ( !context.hasAdministrativePrivileges( ) &&
              !Permissions.isAuthorized(
                  PolicySpec.VENDOR_IAM,
                  PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE,
                  Accounts.getInstanceProfileFullName( profile ),
                  profile.getAccount( ),
                  PolicySpec.IAM_LISTINSTANCEPROFILES,
                  context.getUser( ) ) ) {
            throw new IllegalMetadataAccessException( String.format(
                "Not authorized to access instance profile with ARN %s for %s",
                profileArn,
                context.getUserFullName( ) ) );
          }

          final Role role = profile.getRole( );
          final String roleArn = role == null ? null : Accounts.getRoleArn( role );
          if ( role != null && !context.hasAdministrativePrivileges( ) &&
              !Permissions.isAuthorized(
                  PolicySpec.VENDOR_IAM,
                  PolicySpec.IAM_RESOURCE_ROLE,
                  Accounts.getRoleFullName( role ),
                  role.getAccount( ),
                  PolicySpec.IAM_PASSROLE,
                  context.getUser( ) ) ) {
            throw new IllegalMetadataAccessException( String.format(
                "Not authorized to pass role with ARN %s for %s",
                roleArn,
                context.getUserFullName( ) ) );
          }

          if ( role != null ) {
            allocInfo.setInstanceProfileArn( profileArn );
            allocInfo.setIamInstanceProfileId( profile.getInstanceProfileId( ) );
            allocInfo.setIamRoleArn( roleArn );
          } else {
            throw new MetadataException( "Role not found for IAM instance profile ARN: " + instanceProfileArn );
          }
        } catch ( AuthException e ) {
          throw new MetadataException( "IAM instance profile error", e );
        }
      }
      return true;
    }
  }

  /**
   * <p>Verification logic for block device mappings in the run instance request. 
   * Merges device mappings from the image registration with those from the run instance request, the later getting higher priority. 
   * Populates the final set of device mappings for boot from ebs instances only. </p>
   * <p>Fixes EUCA-4047 and implements EUCA-4786</p> 
   */
  enum BlockDeviceMapVerifier implements MetadataVerifier {
    INSTANCE;
    
	@Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      
      BootableImageInfo imageInfo = allocInfo.getBootSet().getMachine();   
      final ArrayList<BlockDeviceMappingItemType> instanceMappings = allocInfo.getRequest().getBlockDeviceMapping() != null 
    		  													? allocInfo.getRequest().getBlockDeviceMapping() 
    		  													: new ArrayList<BlockDeviceMappingItemType>()  ;
      List<DeviceMapping> imageMappings = new ArrayList<DeviceMapping>(((ImageInfo) imageInfo).getDeviceMappings());
      // Is this an overkill?? Should I rather go with the above logic. Damn complexity seems same... 
      // probably m * n where m is the number of image mappings and n is the number of instance mappings
      instanceMappings.addAll(Lists.transform(Lists.newArrayList(Iterables.filter(imageMappings, new Predicate<DeviceMapping>(){
    	  @Override
    	  public boolean apply(DeviceMapping arg0) {
    		  return !Iterables.any( instanceMappings, Images.findBlockDeviceMappingItempType( arg0.getDeviceName() ));
    	  }
      })), Images.DeviceMappingDetails.INSTANCE));

      if ( imageInfo instanceof BlockStorageImageInfo ) { //bfebs image   
     
        if ( !instanceMappings.isEmpty() ) {
        
          //Verify all block device mappings. Dont fuss if both snapshot id and volume size are left blank
          Images.validateBlockDeviceMappings( instanceMappings, EnumSet.of( AllowSuppressMapping, AllowEbsMapping ) );
          
          BlockStorageImageInfo bfebsImage = (BlockStorageImageInfo) imageInfo;
          Integer imageSizeGB = (int) ( bfebsImage.getImageSizeBytes( ) / BYTES_PER_GB );
          Integer userRequestedSizeGB = null;
           
          // Find the root block device mapping in the run instance request. Validate it
          BlockDeviceMappingItemType rootBlockDevice = Iterables.find( instanceMappings, Images.findEbsRootOptionalSnapshot( bfebsImage.getRootDeviceName() ), null );
          if( rootBlockDevice != null) {
            // Ensure that root device is not mapped to a different snapshot, logical device or suppressed.
            // Verify that the root device size is not smaller than the image size
            if ( StringUtils.isNotBlank(rootBlockDevice.getEbs().getSnapshotId()) && 
        		  !StringUtils.equals(rootBlockDevice.getEbs().getSnapshotId(), bfebsImage.getSnapshotId()) ) {
              throw new InvalidMetadataException( "Snapshot ID cannot be modified for the root device. " +
                					"Source snapshot from the image registration will be used for creating the root device" );
            } else if ( StringUtils.isNotBlank(rootBlockDevice.getVirtualName()) ) {
              throw new InvalidMetadataException( "Logical type cannot be modified for the root device. " +
             		"Source snapshot from the image registration will be used for creating the root device" );
            } else if ( rootBlockDevice.getNoDevice() != null && rootBlockDevice.getNoDevice() ) {
              throw new InvalidMetadataException( "Root device cannot be suppressed. " + 
              		"Source snapshot from the image registration will be used for creating the root device" );
            } else if ( (userRequestedSizeGB = rootBlockDevice.getEbs().getVolumeSize() ) != null && userRequestedSizeGB < imageSizeGB ) {
              throw new InvalidMetadataException("Root device volume cannot be smaller than the image size");
            }
            
            // Gather all the information for the root device mapping and populate it in the run instance request
            if( rootBlockDevice.getEbs().getSnapshotId() == null ) {
            	rootBlockDevice.getEbs().setSnapshotId(bfebsImage.getSnapshotId());
            }
            if( rootBlockDevice.getEbs().getVolumeSize() == null ) {
            	rootBlockDevice.getEbs().setVolumeSize(imageSizeGB);	
            } 
            if( rootBlockDevice.getEbs().getDeleteOnTermination() == null ) {
            	rootBlockDevice.getEbs().setDeleteOnTermination(bfebsImage.getDeleteOnTerminate());
            }
          } else {
        	  // This should never happen. Root device mapping will always exist in the block storage image and or run instance request 
        	  throw new InvalidMetadataException("Root block device mapping not found\n"); 
          }
        }
      } else { // Instance store image
        //Verify all block device mappings. EBS mappings must be considered invalid since AWS doesn't support it
        Images.validateBlockDeviceMappings( instanceMappings, EnumSet.of( AllowSuppressMapping ) );
      }
      
      // Set the final list of block device mappings in the run instance request (necessary if the instance mappings were null). Checked with grze that its okay
      allocInfo.getRequest().setBlockDeviceMapping(instanceMappings);
      return true;
    }
  }
  enum UserDataVerifier implements MetadataVerifier {
    INSTANCE;
  
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
     byte[] userData = allocInfo.getUserData();
      if (userData != null && userData.length > Integer.parseInt(VmInstances.USER_DATA_MAX_SIZE_KB) * 1024) {
        throw new InvalidMetadataException("User data may not exceed " + VmInstances.USER_DATA_MAX_SIZE_KB + " KB");
      }
      return true;
    }
  }
}
