/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.run;

import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowEbsMapping;
import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowSuppressMapping;
import static com.eucalyptus.images.Images.DeviceMappingValidationOption.SkipExtraEphemeral;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.VmInstanceLifecycleHelpers;
import com.eucalyptus.cloud.run.Allocations.AllocationType;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.EbsDeviceMapping;
import com.eucalyptus.compute.common.LaunchTemplateBlockDeviceMappingRequest;
import com.eucalyptus.compute.common.LaunchTemplateEbsBlockDeviceRequest;
import com.eucalyptus.compute.common.LaunchTemplateTagSpecificationRequestList;
import com.eucalyptus.compute.common.RequestLaunchTemplateData;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.ResourceTagSpecification;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.util.InvalidInstanceProfileMetadataException;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.ImageMetadata.Platform;
import com.eucalyptus.compute.common.backend.RunInstancesType;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.compute.common.internal.util.IllegalMetadataAccessException;
import com.eucalyptus.compute.common.internal.util.InvalidMetadataException;
import com.eucalyptus.compute.common.internal.util.MetadataCreationException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.BootableImageInfo;
import com.eucalyptus.compute.common.internal.images.DeviceMapping;
import com.eucalyptus.compute.common.internal.util.NoSuchImageIdException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.internal.vm.LaunchTemplate;
import com.eucalyptus.compute.common.internal.vm.LaunchTemplates;
import com.eucalyptus.compute.common.policy.ComputePolicySpec;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.compute.common.internal.keys.KeyPairs;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.tags.TagHelper;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.vavr.control.Option;
import net.sf.json.JSONException;

@SuppressWarnings("Guava")
public class VerifyMetadata {
  private static Logger LOG = Logger.getLogger( VerifyMetadata.class );
  private static final long BYTES_PER_GB = ( 1024L * 1024L * 1024L );
  
  public static Predicate<Allocation> get( ) {
    return Predicates.and( Lists.transform( verifiers, AsPredicate.INSTANCE ) );
  }

  private interface MetadataVerifier {
    boolean apply( Allocation allocInfo ) throws MetadataException, AuthException, VerificationException;
  }
  
  private static final ArrayList<? extends MetadataVerifier> verifiers = Lists.newArrayList(
      LaunchTemplateVerifier.INSTANCE, LaunchTemplateDataVerifier.INSTANCE,
      VmTypeVerifier.INSTANCE, PartitionVerifier.INSTANCE, ImageVerifier.INSTANCE,
      KeyPairVerifier.INSTANCE, NetworkResourceVerifier.INSTANCE, RoleVerifier.INSTANCE,
      BlockDeviceMapVerifier.INSTANCE, UserDataVerifier.INSTANCE, ResourceTagVerifier.INSTANCE );
  
  @SuppressWarnings("Convert2Lambda")
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

  enum LaunchTemplateVerifier implements MetadataVerifier {
    INSTANCE;

    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      final UserFullName ownerFullName = allocInfo.getOwnerFullName();
      final String id = ResourceIdentifiers.tryNormalize().apply(allocInfo.getRequest( ).getLaunchTemplateId( ));
      final String name = allocInfo.getRequest( ).getLaunchTemplateName( );
      if ( !Strings.isNullOrEmpty( id ) ||
          !Strings.isNullOrEmpty( name ) ) {
        try (@SuppressWarnings("unused") final TransactionResource tx = Entities.transactionFor(LaunchTemplate.class)) {
          final LaunchTemplate template =
              Entities.criteriaQuery(LaunchTemplates.restriction(ownerFullName, id, name)).uniqueResult();
          allocInfo.setLaunchTemplateArn(template.getArn());
          allocInfo.setLaunchTemplateData(template.readTemplateData());
          allocInfo.setLaunchTemplateResource(true);
          if (!RestrictedTypes.filterPrivileged( ).apply( template )) {
            throw new IllegalMetadataAccessException( "Not authorized to use launch template for " +
                allocInfo.getOwnerFullName() );
          }
        } catch (final NoSuchElementException e) {
          throw new NoSuchMetadataException("Launch template not found");
        } catch (final IOException e) {
          throw new MetadataCreationException( "Launch template data error", e );
        }
      }
      return true;
    }
  }

  enum LaunchTemplateDataVerifier implements MetadataVerifier {
    INSTANCE;

    @Override
    public boolean apply( final Allocation allocInfo ) {
      final Boolean templateDisableTerminate;
      final Boolean templateMonitoring;
      final LaunchTemplateTagSpecificationRequestList templateTags;
      final RequestLaunchTemplateData templateData = allocInfo.getLaunchTemplateData();
      if ( templateData != null ) {
        templateDisableTerminate = templateData.getDisableApiTermination();
        templateMonitoring = templateData.getMonitoring()==null ?
            null :
            templateData.getMonitoring().getEnabled();
        templateTags = templateData.getTagSpecifications();
      } else {
        templateDisableTerminate = null;
        templateMonitoring = null;
        templateTags = null;
      }

      final Boolean requestDisableTerminate = allocInfo.getRequest().getDisableTerminate();
      if ( requestDisableTerminate != null ) {
        allocInfo.setDisableApiTermination( requestDisableTerminate );
        if ( templateDisableTerminate != null ) {
          allocInfo.setLaunchTemplateResource(false);
        }
      } else if ( templateDisableTerminate != null ) {
        allocInfo.setDisableApiTermination( templateDisableTerminate );
      }

      final Boolean requestMonitoring = allocInfo.getRequest( ).getMonitoring();
      if ( requestMonitoring != null ) {
        allocInfo.setMonitoring( requestMonitoring );
        if ( templateMonitoring != null ) {
          allocInfo.setLaunchTemplateResource(false);
        }
      } else if ( templateMonitoring != null ) {
        allocInfo.setMonitoring( templateMonitoring );
      }

      final ArrayList<ResourceTagSpecification> requestTags = allocInfo.getRequest( ).getTagSpecification();
      if ( requestTags != null && !requestTags.isEmpty( ) ) {
        allocInfo.setTagsForResourceFunction(
            resource -> TagHelper.tagsForResource( requestTags, resource ) );
        if ( templateTags != null ) {
          allocInfo.setLaunchTemplateResource(false);
        }
      } else if ( templateTags != null ) {
        allocInfo.setTagsForResourceFunction(
            resource -> TagHelper.tagsForResourceFromTemplate( templateTags.getMember(), resource ) );
      }

      return true;
    }
  }

  enum VmTypeVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      String instanceType = allocInfo.getRequest( ).getInstanceType( );
      boolean checkAccess = true;
      if (instanceType==null &&  allocInfo.getLaunchTemplateData()!=null) {
        instanceType = allocInfo.getLaunchTemplateData().getInstanceType();
        checkAccess = instanceType==null || allocInfo.isVerifyLaunchTemplateData(); // authorized if set via launch template
      }
      if (instanceType==null) {
        instanceType = VmTypes.defaultTypeName();
      }
      allocInfo.getRequest().setInstanceType(instanceType);
      VmType vmType = VmTypes.lookup( instanceType );
      if ( !vmType.getEnabled() || (checkAccess && !RestrictedTypes.filterPrivileged( ).apply( vmType )) ) {
        throw new IllegalMetadataAccessException( "Not authorized to allocate vm type " + instanceType + " for " + allocInfo.getOwnerFullName() );
      }
      allocInfo.setVmType( vmType );
      return true;
    }
  }
  
  enum PartitionVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) {
      RunInstancesType request = allocInfo.getRequest( );
      String zoneName = request.getAvailabilityZone( );
      if (zoneName == null &&
          allocInfo.getLaunchTemplateData()!=null &&
          allocInfo.getLaunchTemplateData().getPlacement()!=null) {
        zoneName = allocInfo.getLaunchTemplateData().getPlacement().getAvailabilityZone();
      }
      if (zoneName == null) {
        zoneName = Partition.DEFAULT_NAME;
      }
      if ( Clusters.list( ).isEmpty( ) ) {
        LOG.debug( "enabled values: " + Joiner.on( "\n" ).join( Clusters.list( ) ) );
        LOG.debug( "disabled values: " + Joiner.on( "\n" ).join( Clusters.listDisabled( ) ) );
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
    public boolean apply( Allocation allocInfo ) throws MetadataException, VerificationException {
      final boolean launchTemplateSpecifiesImage =
          allocInfo.getLaunchTemplateData() != null && (
          allocInfo.getLaunchTemplateData().getImageId() != null ||
          allocInfo.getLaunchTemplateData().getKernelId() != null ||
          allocInfo.getLaunchTemplateData().getRamDiskId() != null );
      RunInstancesType msg = allocInfo.getRequest( );

      String imageId = msg.getImageId( );
      if ( launchTemplateSpecifiesImage ) {
        if ( imageId==null ) {
          imageId = allocInfo.getLaunchTemplateData().getImageId();
        } else {
          allocInfo.setLaunchTemplateResource(false);
        }
      }
      if (imageId==null) {
        if (AllocationType.Verify.matches(allocInfo)) {
          return true;
        }
        throw new NoSuchImageIdException("Image identifier required");
      }

      String kernelId = msg.getKernelId();
      if (kernelId==null) {
        kernelId = allocInfo.getLaunchTemplateData().getKernelId();
      } else if ( launchTemplateSpecifiesImage ) {
        allocInfo.setLaunchTemplateResource(false);
      }

      String ramdiskId = msg.getRamdiskId();
      if (ramdiskId==null) {
        ramdiskId = allocInfo.getLaunchTemplateData().getRamDiskId();
      } else if ( launchTemplateSpecifiesImage ) {
        allocInfo.setLaunchTemplateResource(false);
      }

      VmType vmType = allocInfo.getVmType( );
      try {
        BootableSet bootSet = Emis.newBootableSet( imageId, kernelId, ramdiskId );
        allocInfo.setBootableSet( bootSet );
        
        // Add (1024L * 1024L * 10) to handle NTFS min requirements.
        if ( ! bootSet.isBlockStorage( ) ) {
          if ( Platform.windows.equals( bootSet.getMachine( ).getPlatform( ) ) &&
            bootSet.getMachine( ).getImageSizeBytes( ) > ( ( 1024L * 1024L * 1024L * vmType.getDisk( ) ) + ( 1024L * 1024L * 10 ) ) ) {
          throw new ImageInstanceTypeVerificationException(
              "Unable to run instance " + bootSet.getMachine( ).getDisplayName( ) +
              " in which the size " + bootSet.getMachine( ).getImageSizeBytes( ) +
              " bytes of the instance is greater than the vmType " + vmType.getDisplayName( ) +
              " size " + vmType.getDisk( ) + " GB." );
          } else if ( bootSet.getMachine( ).getImageSizeBytes( ) > ( ( 1024L * 1024L * 1024L * vmType.getDisk( ) ) ) ) {
            throw new ImageInstanceTypeVerificationException(
                "Unable to run instance " + bootSet.getMachine( ).getDisplayName( ) +
                " in which the size " + bootSet.getMachine( ).getImageSizeBytes( ) +
                " bytes of the instance is greater than the vmType " + vmType.getDisplayName( ) +
                " size " + vmType.getDisk( ) + " GB." );
          }
        }
      } catch ( VerificationException e ) {
        throw e;
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
      RunInstancesType request = allocInfo.getRequest( );
      boolean checkAccess = true;
      String keyName = Strings.emptyToNull(request.getKeyName( ));
      if ( keyName == null && allocInfo.getLaunchTemplateData() != null ){
        keyName = allocInfo.getLaunchTemplateData().getKeyName();
        checkAccess = allocInfo.isVerifyLaunchTemplateData();
      } else if ( allocInfo.getLaunchTemplateData() != null &&
          allocInfo.getLaunchTemplateData().getKeyName() != null ) {
        allocInfo.setLaunchTemplateResource(false);
      }
      if ( keyName == null ) {
        allocInfo.setSshKeyPair( KeyPairs.noKey( ) );
        return true;
      }
      UserFullName ownerFullName = allocInfo.getOwnerFullName( );
      SshKeyPair key = KeyPairs.lookup( ownerFullName.asAccountFullName(), keyName );
      if ( checkAccess && !RestrictedTypes.filterPrivileged( ).apply( key ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use keypair " + keyName + " by " + ownerFullName.getUserName() );
      }
      allocInfo.setSshKeyPair( key );
      return true;
    }
  }
  
  enum NetworkResourceVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      VmInstanceLifecycleHelpers.get( ).verifyAllocation( allocInfo );
      return true;
    }
  }

  enum RoleVerifier implements MetadataVerifier {
    INSTANCE;

    @Override
    public boolean apply( final Allocation allocInfo ) throws MetadataException {
      final UserFullName ownerFullName = allocInfo.getOwnerFullName();
      String instanceProfileArn = allocInfo.getRequest( ).getIamInstanceProfileArn( );
      String instanceProfileName = allocInfo.getRequest( ).getIamInstanceProfileName( );
      boolean checkAccess = true;
      if (instanceProfileArn==null && instanceProfileName==null &&
          allocInfo.getLaunchTemplateData()!=null &&
          allocInfo.getLaunchTemplateData().getIamInstanceProfile()!=null) {
        instanceProfileArn = allocInfo.getLaunchTemplateData().getIamInstanceProfile().getArn();
        instanceProfileName = allocInfo.getLaunchTemplateData().getIamInstanceProfile().getName();
        checkAccess = allocInfo.isVerifyLaunchTemplateData();
      } else if (allocInfo.getLaunchTemplateData()!=null &&
          allocInfo.getLaunchTemplateData().getIamInstanceProfile()!=null) {
        allocInfo.setLaunchTemplateResource(false);
      }

      if ( !Strings.isNullOrEmpty( instanceProfileArn ) ||
          !Strings.isNullOrEmpty( instanceProfileName ) ) {

        final String profileAccount;
        final String profileName;
        if ( !Strings.isNullOrEmpty( instanceProfileArn ) ) try {
          final Ern name = Ern.parse( instanceProfileArn );
          if ( !( name instanceof EuareResourceName) ) {
            throw new InvalidInstanceProfileMetadataException( "Invalid IAM instance profile ARN: " + instanceProfileArn );
          }
          profileAccount = name.getAccount( );
          profileName = ((EuareResourceName) name).getName( );

        } catch ( JSONException e ) {
          throw new InvalidInstanceProfileMetadataException( "Invalid IAM instance profile ARN: " + instanceProfileArn, e );
        } else {
          profileAccount = ownerFullName.getAccountNumber( );
          profileName = instanceProfileName;
        }

        final InstanceProfile profile;
        try {
          profile = Accounts.lookupInstanceProfileByName( profileAccount, profileName );
        } catch ( AuthException e ) {
          throw new InvalidInstanceProfileMetadataException( "Invalid IAM instance profile: " + profileAccount + "/" + profileName, e );
        }

        if ( !Strings.isNullOrEmpty( instanceProfileName ) &&  !instanceProfileName.equals( profile.getName( ) ) ) {
          throw new InvalidInstanceProfileMetadataException( String.format(
              "Invalid IAM instance profile name '%s' for ARN: %s", profileName, instanceProfileArn) );
        }

        try {
          final AuthContextSupplier user = allocInfo.getAuthContext( );
          if ( checkAccess && !Permissions.isAuthorized(
              IamPolicySpec.VENDOR_IAM,
              IamPolicySpec.IAM_RESOURCE_INSTANCE_PROFILE,
              Accounts.getInstanceProfileFullName( profile ),
              AccountFullName.getInstance( profile.getAccountNumber( ) ),
              IamPolicySpec.IAM_LISTINSTANCEPROFILES,
              user ) ) {
            throw new IllegalMetadataAccessException( String.format(
                "Not authorized to access instance profile with ARN %s for %s",
                profile.getInstanceProfileArn( ),
                ownerFullName ) );
          }

          final Role role = profile.getRole( );
          if ( role != null && checkAccess && !Permissions.isAuthorized(
                  IamPolicySpec.VENDOR_IAM,
                  IamPolicySpec.IAM_RESOURCE_ROLE,
                  Accounts.getRoleFullName( role ),
                  AccountFullName.getInstance( role.getAccountNumber( ) ),
                  IamPolicySpec.IAM_PASSROLE,
                  user ) ) {
            throw new IllegalMetadataAccessException( String.format(
                "Not authorized to pass role with ARN %s for %s",
                role.getRoleArn( ),
                ownerFullName ) );
          }

          if ( role != null ) {
            allocInfo.setInstanceProfileArn( profile.getInstanceProfileArn( ) );
            allocInfo.setIamInstanceProfileId( profile.getInstanceProfileId( ) );
            allocInfo.setIamRoleArn( role.getRoleArn( ) );
          } else {
            throw new InvalidInstanceProfileMetadataException( "Role not found for IAM instance profile ARN: " + profile.getInstanceProfileArn( ) );
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
  @SuppressWarnings({"StaticPseudoFunctionalStyleMethod", "Convert2Lambda"})
  enum BlockDeviceMapVerifier implements MetadataVerifier {
    INSTANCE;

    private static final CompatFunction<LaunchTemplateEbsBlockDeviceRequest, EbsDeviceMapping> ebsTransform = ltEbs -> {
      final EbsDeviceMapping ebs = new EbsDeviceMapping();
      ebs.setDeleteOnTermination(ltEbs.getDeleteOnTermination());
      ebs.setEncrypted(ltEbs.getEncrypted());
      ebs.setIops(ltEbs.getIops());
      ebs.setSnapshotId(ltEbs.getSnapshotId());
      ebs.setVolumeSize(ltEbs.getVolumeSize());
      ebs.setVolumeType(ltEbs.getVolumeType());
      return ebs;
    };

    private static final CompatFunction<LaunchTemplateBlockDeviceMappingRequest, BlockDeviceMappingItemType> deviceTransform = ltDev -> {
      final BlockDeviceMappingItemType item = new BlockDeviceMappingItemType();
      item.setDeviceName(ltDev.getDeviceName());
      item.setVirtualName(ltDev.getVirtualName());
      item.setEbs(Option.of(ltDev.getEbs()).map(ebsTransform).getOrNull());
      item.setNoDevice(ltDev.getNoDevice()==null?null:true);
      return item;
    };

    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      if (AllocationType.Verify.matches(allocInfo)) {
        return true;
      }

      final BootableImageInfo imageInfo = allocInfo.getBootSet().getMachine();
      final List<BlockDeviceMappingItemType> instanceMappings =
          Option.of(allocInfo.getRequest().getBlockDeviceMapping()).getOrElse(ArrayList::new);
      if ( allocInfo.getLaunchTemplateData() != null &&
           allocInfo.getLaunchTemplateData().getBlockDeviceMappings() != null ) {
        if ( instanceMappings.isEmpty() ) {
          instanceMappings.addAll(allocInfo.getLaunchTemplateData().getBlockDeviceMappings().getMember().stream()
              .map(deviceTransform)
              .collect(Collectors.toList()));
        } else {
          allocInfo.setLaunchTemplateResource(false);
        }
      }
      final List<DeviceMapping> imageMappings = new ArrayList<>(((ImageInfo) imageInfo).getDeviceMappings());
      // Is this an overkill?? Should I rather go with the above logic. Damn complexity seems same... 
      // probably m * n where m is the number of image mappings and n is the number of instance mappings
      instanceMappings.addAll(Lists.transform(Lists.newArrayList(Iterables.filter(imageMappings, new Predicate<DeviceMapping>(){
    	  @Override
    	  public boolean apply(DeviceMapping arg0) {
    		  return !Iterables.any( instanceMappings, Images.findBlockDeviceMappingItempType( arg0.getDeviceName() ));
    	  }
      })), Images.DeviceMappingDetails.INSTANCE));

      ArrayList<BlockDeviceMappingItemType> resultedInstanceMappings = new ArrayList<>();
      if ( imageInfo instanceof BlockStorageImageInfo ) { //bfebs image   
     
        if ( !instanceMappings.isEmpty() ) {
          //Verify all block device mappings. Don't fuss if both snapshot id and volume size are left blank
          //Ignore (remove) extra ephemerals EUCA-9148
          resultedInstanceMappings = Images.validateBlockDeviceMappings( instanceMappings, EnumSet.of( AllowSuppressMapping, AllowEbsMapping, SkipExtraEphemeral ) );
          
          BlockStorageImageInfo bfebsImage = (BlockStorageImageInfo) imageInfo;
          Integer imageSizeGB = (int) ( bfebsImage.getImageSizeBytes( ) / BYTES_PER_GB );
          Integer userRequestedSizeGB;
          // Find the root block device mapping in the run instance request. Validate it
          BlockDeviceMappingItemType rootBlockDevice = Iterables.find( resultedInstanceMappings, Images.findEbsRootOptionalSnapshot( bfebsImage.getRootDeviceName() ), null );
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
            } else if ( rootBlockDevice.getNoDevice() != null ) {
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
        resultedInstanceMappings = Images.validateBlockDeviceMappings( instanceMappings, EnumSet.of( AllowSuppressMapping, SkipExtraEphemeral ) );
      }
      
      // Set the final list of block device mappings in the run instance request (necessary if the instance mappings were null). Checked with grze that its okay
      allocInfo.getRequest().setBlockDeviceMapping(resultedInstanceMappings);
      return true;
    }
  }
  enum UserDataVerifier implements MetadataVerifier {
    INSTANCE;
  
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      byte[] userData = allocInfo.getUserData();
      boolean launchTemplateHasUserData = allocInfo.getLaunchTemplateData()!=null &&
          Strings.emptyToNull(allocInfo.getLaunchTemplateData().getUserData())!=null;
      if (userData == null) {
        if (launchTemplateHasUserData) {
          try {
            allocInfo.setUserData(Base64.decode(allocInfo.getLaunchTemplateData().getUserData()));
            allocInfo.getRequest().setUserData(new String(Base64.encode(allocInfo.getUserData())));
            userData = allocInfo.getUserData();
          } catch (Exception ignored) {
          }
        }
      } else if (launchTemplateHasUserData) {
        allocInfo.setLaunchTemplateResource(false);
      }
      if (userData != null && userData.length > Integer.parseInt(VmInstances.USER_DATA_MAX_SIZE_KB) * 1024) {
        throw new InvalidMetadataException("User data may not exceed " + VmInstances.USER_DATA_MAX_SIZE_KB + " KB");
      }
      return true;
    }
  }

  enum ResourceTagVerifier implements MetadataVerifier {
    INSTANCE;

    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      TagHelper.validateTagSpecifications( allocInfo.getRequest( ).getTagSpecification( ) );
      if ( AllocationType.Verify.matches(allocInfo) &&
          allocInfo.getLaunchTemplateData( ) != null &&
          allocInfo.getLaunchTemplateData( ).getTagSpecifications( ) != null ) {
        TagHelper.validateTagSpecificationsForTemplate( allocInfo.getLaunchTemplateData().getTagSpecifications().getMember() );
      }

      final  AuthContextSupplier authContext;
      try {
        authContext = allocInfo.getAuthContext( );
      } catch ( AuthException e ) {
        throw new MetadataException( "Tag validation error", e );
      }
      final UserFullName userFullName = allocInfo.getOwnerFullName( );
      final AccountFullName accountFullName = userFullName.asAccountFullName( );
      for ( final String resource : ComputePolicySpec.EC2_RESOURCES ) {
        final List<ResourceTag> resourceTags = allocInfo.getTagsForResource( resource );
        if ( !resourceTags.isEmpty( ) ) {
          if ( !TagHelper.createTagsAuthorized( authContext, accountFullName, resource ) ) {
            throw new IllegalMetadataAccessException( "Not authorized to create "+resource+" tags by " + userFullName.getUserName( ) );
          }
        }
      }
      return true;
    }
  }

  public static class ImageInstanceTypeVerificationException extends VerificationException {
    private static final long serialVersionUID = -1L;

    public ImageInstanceTypeVerificationException( final String message ) {
      super( message );
    }
  }
}
