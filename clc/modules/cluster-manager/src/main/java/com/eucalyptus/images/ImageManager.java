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

package com.eucalyptus.images;

import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowDevSda1;
import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowEbsMapping;
import static com.eucalyptus.images.Images.DeviceMappingValidationOption.AllowSuppressMapping;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.DeviceMapping;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.ClientComputeException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.component.Topology;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.backend.CopyImageResponseType;
import com.eucalyptus.compute.common.backend.CopyImageType;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.CreateImageTask;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.eucalyptus.compute.common.backend.ConfirmProductInstanceResponseType;
import com.eucalyptus.compute.common.backend.ConfirmProductInstanceType;
import com.eucalyptus.compute.common.backend.CreateImageResponseType;
import com.eucalyptus.compute.common.backend.CreateImageType;
import com.eucalyptus.compute.common.backend.DeregisterImageResponseType;
import com.eucalyptus.compute.common.backend.DeregisterImageType;
import com.eucalyptus.compute.common.backend.ModifyImageAttributeResponseType;
import com.eucalyptus.compute.common.backend.ModifyImageAttributeType;
import com.eucalyptus.compute.common.backend.RegisterImageResponseType;
import com.eucalyptus.compute.common.backend.RegisterImageType;
import com.eucalyptus.compute.common.backend.ResetImageAttributeResponseType;
import com.eucalyptus.compute.common.backend.ResetImageAttributeType;

@ComponentNamed("computeImageManager")
public class ImageManager {
  
  public static Logger        LOG = Logger.getLogger( ImageManager.class );
  private static final long GB = 1024*1024*1024; //bytes-per-gb

  public CopyImageResponseType copyImage( final CopyImageType request ) {
    return request.getReply( );
  }
  
  public RegisterImageResponseType register( final RegisterImageType request ) throws EucalyptusCloudException, AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
	final Context ctx = Contexts.lookup();
    ImageInfo imageInfo = null;
    final String rootDevName = ( request.getRootDeviceName( ) != null )
      ? request.getRootDeviceName( )
      : Images.DEFAULT_ROOT_DEVICE;
    final String eki = ImageMetadata.Platform.windows.name( ).equals( request.getKernelId( ) ) ?
        request.getKernelId() :
        normalizeOptionalImageIdentifier( request.getKernelId() );
    final String eri = normalizeOptionalImageIdentifier( request.getRamdiskId() );

    verifyImageNameAndDescription( request.getName( ), request.getDescription( ) );
    
    ImageMetadata.VirtualizationType virtType = ImageMetadata.VirtualizationType.paravirtualized;
    if(request.getVirtualizationType() != null){
    	if(StringUtils.equalsIgnoreCase("paravirtual", request.getVirtualizationType()))
    		virtType = ImageMetadata.VirtualizationType.paravirtualized;
    	else if(StringUtils.equalsIgnoreCase("hvm", request.getVirtualizationType()))
    		virtType = ImageMetadata.VirtualizationType.hvm;
    	else
    		throw new EucalyptusCloudException("Unknown virtualization-type");
    }

    if ( request.getImageLocation( ) != null ) {
      // Verify all the device mappings first.
    	bdmInstanceStoreImageVerifier( ).apply( request );
    	
        // download manifest with AwsExecRead account
        final ImageManifest manifest = ImageManifests.lookup( request.getImageLocation( ) , ctx.getUser());
    	LOG.debug( "Obtained manifest information for requested image registration: " + manifest );
    	
      final ImageMetadata.Platform imagePlatform = request.getPlatform()!=null ? ImageMetadata.Platform.valueOf(request.getPlatform())
          : manifest.getPlatform( ); 
      if(ImageMetadata.Platform.windows.equals(imagePlatform))
        virtType = ImageMetadata.VirtualizationType.hvm;
      final ImageMetadata.VirtualizationType virtualizationType = virtType;
      
      if(ImageMetadata.Type.machine.equals(manifest.getImageType( )) &&
          ImageMetadata.VirtualizationType.paravirtualized.equals(virtualizationType)){
        // make sure kernel and ramdisk are present with the request or manifest
        if(request.getKernelId()==null && manifest.getKernelId()==null)
          throw new ClientComputeException("MissingParameter","Kernel ID must be specified");
        if(request.getRamdiskId()==null && manifest.getRamdiskId()==null)
          throw new ClientComputeException("MissingParameter","Ramdisk ID must be specified");
      }
      
    	//Check that the manifest-specified size of the image is within bounds.
    	//If null image max size then always allow
    	Integer maxSize = ImageConfiguration.getInstance().getMaxImageSizeGb();
    	if(maxSize != null && maxSize > 0 && manifest.getSize() > (maxSize * GB)) {
    		throw new EucalyptusCloudException("Cannot register image of size " + manifest.getSize() + " bytes because it exceeds the configured maximum instance-store image size of " + maxSize + " GB. Please contact your administrator ");
    	}

    	List<DeviceMapping> vbr = Lists.transform( request.getBlockDeviceMappings( ), Images.deviceMappingGenerator( imageInfo, null ) );
    	final ImageMetadata.Architecture arch = ( request.getArchitecture( ) == null
    			? null
    					: ImageMetadata.Architecture.valueOf( request.getArchitecture( ) ) );
		final String amiFromManifest = manifest.getAmi();

    	Supplier<ImageInfo> allocator = new Supplier<ImageInfo>( ) {
    		@Override
    		public ImageInfo get( ) {
    			try {
    			  if(ImageMetadata.Type.machine.equals(manifest.getImageType( )) &&
    	            ImageMetadata.VirtualizationType.paravirtualized.equals(virtualizationType) &&
    			      (amiFromManifest.isEmpty() || isPathAPartition(amiFromManifest)) )
    			    return Images.registerFromManifest( ctx.getUserFullName( ), request.getName( ),
                 request.getDescription( ), arch, virtualizationType, ImageMetadata.Platform.linux, ImageMetadata.ImageFormat.partitioned, eki, eri, manifest );
    			  else
    			    return Images.registerFromManifest( ctx.getUserFullName( ), request.getName( ), 
    			      request.getDescription( ), arch, virtualizationType, imagePlatform, ImageMetadata.ImageFormat.fulldisk, eki, eri, manifest );
    			} catch ( Exception ex ) {
    				LOG.error( ex );
    				Logs.extreme( ).error( ex, ex );
    				throw Exceptions.toUndeclared( ex );
    			}
    		}
    	};
    	imageInfo = RestrictedTypes.allocateUnitlessResource( allocator );
    	imageInfo.getDeviceMappings( ).addAll( vbr );
    } else if ( rootDevName != null && Iterables.any( request.getBlockDeviceMappings( ), Images.findEbsRoot( rootDevName ) ) ) {
    	Supplier<ImageInfo> allocator = null;
    	// Verify all the device mappings first. Dont fuss if both snapshot id and volume size are left blank
    	bdmBfebsImageVerifier( ).apply( request );
    	ImageMetadata.Platform platform = ImageMetadata.Platform.linux; 
    	if (request.getPlatform()!=null)
    	  platform = ImageMetadata.Platform.valueOf(request.getPlatform());
    	else if (ImageMetadata.Platform.windows.name( ).equals( eki ))
    	  platform = ImageMetadata.Platform.windows;
    	final ImageMetadata.Platform imagePlatform = platform;
    	final ImageMetadata.Architecture arch = ( request.getArchitecture( ) == null
    			? ImageMetadata.Architecture.i386
    			: ImageMetadata.Architecture.valueOf( request.getArchitecture( ) ) );
    	allocator = new Supplier<ImageInfo>( ) {

    		@Override
    		public ImageInfo get( ) {
    			try {
    				return Images.createFromDeviceMapping( ctx.getUserFullName( ), request.getName( ),
    						request.getDescription( ), imagePlatform, eki, eri, rootDevName,
    						request.getBlockDeviceMappings( ), arch );
    			} catch ( EucalyptusCloudException ex ) {
    				throw new RuntimeException( ex );
    			}
    		}
    	};

    	imageInfo = RestrictedTypes.allocateUnitlessResource( allocator );
    } else {
    	throw new EucalyptusCloudException( "Invalid request:  the request must specify either ImageLocation for an " +
    			"instance-store image or a snapshot for the root device for an EBS image.  " +
    			"Provided values were: ImageLocation=" + request.getImageLocation( ) +
    			" BlockDeviceMappings=" + request.getBlockDeviceMappings( ) );
    }

    RegisterImageResponseType reply = ( RegisterImageResponseType ) request.getReply( );
    reply.setImageId( imageInfo.getDisplayName( ) );
    return reply;
  }
  
  public static boolean isPathAPartition(String str) {
    char lastChar = str.charAt(str.length() - 1); // get last letter/number
    return !Character.isLetter(lastChar);
  }

  public DeregisterImageResponseType deregister( DeregisterImageType request ) throws EucalyptusCloudException {
    DeregisterImageResponseType reply = request.getReply( );

    EntityTransaction tx = Entities.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = Entities.uniqueResult( Images.exampleWithImageId( imageIdentifier( request.getImageId( ) ) ) );
      if ( !canModifyImage( imgInfo ) ) {
        throw new EucalyptusCloudException( "Not authorized to deregister image" );
      }
      Images.deregisterImage( imgInfo.getDisplayName( ) );
      tx.commit( );
      return reply;
    } catch ( NoSuchImageException | NoSuchElementException ex ) {
      throw new ClientComputeException( "InvalidAMIID.NotFound", "The image ID '" + request.getImageId() + "' does not exist");
    } catch ( InstanceNotTerminatedException | ConstraintViolationException re ) {
      throw new ClientComputeException( "InvalidAMIID.Unavailable", "The image ID '" + request.getImageId() + "' is no longer available" );
    } catch ( TransactionException ex ) {
      if ( ex.getCause() instanceof NoSuchElementException )
        throw new ClientComputeException( "InvalidAMIID.NotFound", "The image ID '" + request.getImageId() + "' does not exist");
      else throw new EucalyptusCloudException( ex );
    } finally {
        if ( tx.isActive() ) tx.rollback();
    }
  }

  public ConfirmProductInstanceResponseType confirmProductInstance( ConfirmProductInstanceType request ) throws EucalyptusCloudException {
    ConfirmProductInstanceResponseType reply = ( ConfirmProductInstanceResponseType ) request.getReply( );
    reply.set_return( false );
    VmInstance vm = null;
    try {
      vm = VmInstances.lookup( request.getInstanceId( ) );
//ASAP: FIXME: GRZE: RESTORE!
//      EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
//      try {
//        ImageInfo found = db.getUnique( new ImageInfo( vm.getImageInfo( ).getImageId( ) ) );
//        if ( found.getProductCodes( ).contains( new ProductCode( request.getProductCode( ) ) ) ) {
//          reply.set_return( true );
//          reply.setOwnerId( found.getImageOwnerId( ) );
//        }
//        db.commit( );
//      } catch ( EucalyptusCloudException e ) {
//        db.commit( );
//      }
    } catch ( NoSuchElementException e ) {}
    return reply;
  }

  private static List<String> verifyUserIds(final List<String> userIds) throws EucalyptusCloudException {
    for ( String userId : userIds ) {
      if ( !Accounts.isAccountNumber( userId ) ) {
        throw new EucalyptusCloudException( "Not a valid userId : " + userId );
      }
    }
    return Lists.newArrayList( userIds );
  }

  public ModifyImageAttributeResponseType modifyImageAttribute( final ModifyImageAttributeType request ) throws EucalyptusCloudException {
    final ModifyImageAttributeResponseType reply = ( ModifyImageAttributeResponseType ) request.getReply( );

    final EntityTransaction tx = Entities.get( ImageInfo.class );
    try {
      final ImageInfo imgInfo = Entities.uniqueResult( Images.exampleWithImageId( imageIdentifier( request.getImageId( ) ) ) );
      if ( !canModifyImage( imgInfo ) ) {
        throw new EucalyptusCloudException( "Not authorized to modify image attribute" );
      }

      switch ( request.imageAttribute() ) {
        case LaunchPermission:
          if ( request.add() ) {
            imgInfo.addPermissions( verifyUserIds( request.userIds() ) );
            if ( request.groupAll() ) {
              imgInfo.setImagePublic( true );
            }
          } else {
            imgInfo.removePermissions( request.userIds() );
            if ( request.groupAll() ) {
              imgInfo.setImagePublic( false );
            }
          }
          break;
        case ProductCode:
          for ( String productCode : request.getProductCodes( ) ) {
            imgInfo.addProductCode( productCode );
          }
          break;
        case Description:
          imgInfo.setDescription( request.getDescription() );
          break;
      }

      tx.commit( );
      reply.set_return( true );
    } catch ( EucalyptusCloudException e ) {
      tx.rollback( );
      reply.set_return( false );
      throw e;
    } catch ( TransactionException | NoSuchElementException ex ) {
      tx.rollback( );
      throw new EucalyptusCloudException( ex );
    }
    
    return reply;
  }
  
  public ResetImageAttributeResponseType resetImageAttribute( ResetImageAttributeType request ) throws EucalyptusCloudException {
    ResetImageAttributeResponseType reply = ( ResetImageAttributeResponseType ) request.getReply( );
    reply.set_return( true );
    EntityTransaction tx = Entities.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = Entities.uniqueResult( Images.exampleWithImageId( imageIdentifier( request.getImageId( ) ) ) );
      if ( canModifyImage( imgInfo ) ) {
        imgInfo.resetPermission( );
        tx.commit( );
        return reply.markWinning( );
      } else {
        tx.rollback( );
        return reply.markFailed( );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      tx.rollback( );
      return reply.markFailed( );
    } catch ( TransactionException | NoSuchElementException ex ) {
      tx.rollback( );
      return reply.markFailed( );
    }
  }
  
  public CreateImageResponseType createImage( CreateImageType request ) throws EucalyptusCloudException {
    final CreateImageResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );

    verifyImageNameAndDescription( request.getName( ), request.getDescription( ) );

    final String instanceId = normalizeInstanceIdentifier( request.getInstanceId( ) );
    VmInstance vm;
    // IAM auth check, validate instance states, etc
    try {
      vm = RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
      
      if (!vm.isBlockStorage())
    	  throw new EucalyptusCloudException("Cannot create an image from an instance which is not booted from an EBS volume");
      
      if ( !VmState.RUNNING.equals( vm.getState( ) ) && !VmState.STOPPED.equals( vm.getState( ) ) ) {
        throw new EucalyptusCloudException( "Cannot create an image from an instance which is not in either the 'running' or 'stopped' state: "
                                            + vm.getInstanceId( ) + " is in state " + vm.getState( ).getName( ) );
      }
      
	  try {
	      Topology.lookup( ClusterController.class, vm.lookupPartition( ) );
	  } catch ( NoSuchElementException e ) {
	      LOG.debug( e );
	      throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPartition( )  );
	  }
    } catch ( AuthException ex ) {
      throw new EucalyptusCloudException( "Not authorized to create an image from instance " + request.getInstanceId( ) + " as " + ctx.getUser( ).getName( ) );
    } catch ( NoSuchElementException ex ) {
      throw new EucalyptusCloudException( "Instance does not exist: " + request.getInstanceId( ), ex );
    } catch ( PersistenceException ex ) {
      throw new EucalyptusCloudException( "Instance does not exist: " + request.getInstanceId( ), ex );
    }
    
    final String name = request.getName();
    final boolean noReboot = true ? request.getNoReboot()!=null && request.getNoReboot().booleanValue() : false;
    final String desc = request.getDescription();
    String rootDeviceName = null;
    List<BlockDeviceMappingItemType> blockDevices = request.getBlockDeviceMappings();
    ImageMetadata.Architecture arch = null;
    ImageMetadata.Platform platform = null;
    try{
		final BlockStorageImageInfo image = Emis.LookupBlockStorage.INSTANCE.apply(vm.getImageId());
		arch= image.getArchitecture();
		platform = image.getPlatform();
		rootDeviceName = image.getRootDeviceName();
    }catch(final Exception ex){
    	throw new EucalyptusCloudException("Unable to get the image information");
    }
    final ImageMetadata.Architecture imageArch = arch;
    final ImageMetadata.Platform imagePlatform = platform;
    if( blockDevices==null )
        blockDevices = new ArrayList<>();

    // validate input
    List<String> suppressedDevice = new ArrayList<>();
    List<BlockDeviceMappingItemType> creteImageDevices = new ArrayList<>();
    List<String> existingNames = VmInstances.lookupPersistentDeviceNames(instanceId);

    for(final BlockDeviceMappingItemType device : blockDevices){
		if(rootDeviceName!=null && rootDeviceName.equals(device.getDeviceName()))
			throw new ClientComputeException("InvalidBlockDeviceMapping", "The device names should not contain root device");
		if(device.getNoDevice() != null)
			suppressedDevice.add(device.getDeviceName());
		if(device.getEbs() != null) {
			if ( existingNames.contains(device.getDeviceName()) )
				throw new ClientComputeException("InvalidBlockDeviceMapping",
						"Can't add new block device mapping with a device name that is already in use");
			else {
				creteImageDevices.add(device);
				// add name to the list of "existing names" so later ephemeral devices can be checked
				existingNames.add(device.getDeviceName());
			}
		}
		if(device.getVirtualName() != null) {
			existingNames.add(device.getDeviceName());
			creteImageDevices.add(device);
		}
    }

	if(! bdmCreateImageVerifier().apply(request)){
		throw new ClientComputeException("InvalidBlockDeviceMapping", "A block device mapping parameter is not valid");
	}
	// add ephemeral devices unless they need to be suppressed
	try{
		for(BlockDeviceMappingItemType device: Lists.transform(VmInstances.lookupEphemeralDevices(instanceId),
				VmInstances.EphemeralAttachmentToDevice)){
			String dName = device.getDeviceName();
			if ( dName != null && existingNames.contains(dName) )
				throw new ClientComputeException("InvalidBlockDeviceMapping",
						"Can't add new block device mapping with a device name that is already in use by an ephemeral device");
			if ( dName != null && !suppressedDevice.contains(dName) ){
				creteImageDevices.add(device);
			} else {
				blockDevices.add(device);
			}
		}
	} catch (ClientComputeException e) {
		throw e;
	} catch(final Exception ex){
		LOG.warn("Failed to retrieve ephemeral device information", ex);
	}

	try {
		Images.validateBlockDeviceMappings( creteImageDevices, EnumSet.of( AllowEbsMapping ) );
	} catch (MetadataException e) {
		throw new ClientComputeException("InvalidBlockDeviceMapping", e.getMessage());
	}

	final List<BlockDeviceMappingItemType> blockDeviceMapping = creteImageDevices;
    Supplier<ImageInfo> allocator = new Supplier<ImageInfo>( ) {
			@Override
			public ImageInfo get( ) {
				try{
					return Images.createPendingFromDeviceMapping(ctx.getUserFullName(), 
							name, desc, imageArch, 
							imagePlatform, blockDeviceMapping);
				}catch ( final Exception ex) {
					throw new RuntimeException( ex );
				}
			}
	};
	
	Predicate<ImageInfo> deallocator = new Predicate<ImageInfo>() {
		@Override
		public boolean apply(@Nullable ImageInfo input) {
			try{
				Images.setImageState(input.getDisplayName(), ImageMetadata.State.failed);
				Images.deregisterImage(input.getDisplayName());
			}catch(final Exception ex){
				LOG.error("failed to delete image from unsucccessful create-image request", ex);
				return false;
			}
			return true;
		}
	};
	
	ImageInfo imageInfo = null;
    try{
    	imageInfo = RestrictedTypes.allocateUnitlessResource( allocator );
    	reply.setImageId(imageInfo.getDisplayName());
    }catch (final AuthException ex){
        throw new ClientComputeException( "AuthFailure", "Not authorized to create an image" );
    }catch(final Exception ex){
    	LOG.error("Unable to register the image", ex);
    	throw new EucalyptusCloudException( "Unable to register the image", ex);
    }

    final CreateImageTask task = new CreateImageTask(ctx.getAccountNumber(), instanceId, imageInfo.getDisplayName(), noReboot, blockDevices);
    try{
    	task.create(imageInfo.getDisplayName());
    }catch(final Exception ex){
    	deallocator.apply(imageInfo);
    	LOG.error("CreateImage task failed", ex);
    	if(ex instanceof EucalyptusCloudException)
    		throw (EucalyptusCloudException) ex;
    	else
    		throw new EucalyptusCloudException("Create-image has failed", ex);
    }
    
    return reply;
  }

  private static String imageIdentifier( final String identifier ) throws EucalyptusCloudException {
    if( !CloudMetadatas.isImageIdentifier( identifier ) )
      throw new EucalyptusCloudException( "Invalid id: " + "\"" + identifier + "\"" );
    return normalizeImageIdentifier( identifier );
  }
  
  /**
   * <p>Predicate to validate the block device mappings in instance store image registration request.
   * Suppressing a device mapping is allowed and ebs mappings are considered invalid.</p>
   */
  private static Predicate<RegisterImageType> bdmInstanceStoreImageVerifier () {
    return new Predicate<RegisterImageType>( ) {
      @Override
      public boolean apply(RegisterImageType arg0) {
        checkParam( arg0, notNullValue( ) );
        try {
          Images.validateBlockDeviceMappings( arg0.getBlockDeviceMappings(), EnumSet.of( AllowSuppressMapping ) );
          return true;
        } catch ( MetadataException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }  
    };
  }
  
  /**
   * <p>Predicate to validate the block device mappings in boot from ebs image registration request.
   * Suppressing a device mapping is not allowed and ebs mappings are considered valid</p>
   */
  private static Predicate<RegisterImageType> bdmBfebsImageVerifier () {
    return new Predicate<RegisterImageType>( ) {
      @Override
      public boolean apply(RegisterImageType arg0) {
        checkParam( arg0, notNullValue( ) );
        try {
          Images.validateBlockDeviceMappings( arg0.getBlockDeviceMappings(), EnumSet.of( AllowEbsMapping, AllowDevSda1 ) );
          return true;
        } catch ( MetadataException e ) {
          throw Exceptions.toUndeclared( e );
        }
      } 
    };
  }


  /*
  * <p>Predicate to validate the block device mappings in create image request.</p>
  */
  private static Predicate<CreateImageType> bdmCreateImageVerifier ( ) {
    return new Predicate<CreateImageType> ( ) {
      @Override
      public boolean apply(CreateImageType arg0) {
        checkParam( arg0, notNullValue( ) );
        try {
          Images.validateBlockDeviceMappings( arg0.getBlockDeviceMappings(), EnumSet.of( AllowEbsMapping, AllowSuppressMapping ) );
          return true;
        } catch ( MetadataException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    };
  }

  private static void verifyImageNameAndDescription( final String name,
                                                     final String description ) throws ComputeException {
    final Context ctx = Contexts.lookup( );

    if ( name != null ) {
      if( !Images.isImageNameValid( name ) ){
        throw new ClientComputeException("InvalidAMIName.Malformed", "AMI names must be between 3 and 128 characters long, and may contain letters, numbers, '(', ')', '.', '-', '/' and '_'");
      }

      final EntityTransaction db = Entities.get( ImageInfo.class );
      try {
        final List<ImageInfo> images = Lists.newArrayList( Iterables.filter(
            Entities.query(
                Images.exampleWithName( ctx.getUserFullName( ).asAccountFullName( ), name ),
                Entities.queryOptions( ).withReadonly( true ).build( ) ), new Predicate<ImageInfo>(){
                  @Override
                  public boolean apply(ImageInfo arg0) {
                    return ImageMetadata.State.available.name().equals(arg0.getState().getExternalStateName()) ||
                    ImageMetadata.State.pending.name().equals(arg0.getState().getExternalStateName());
                  }
            } ) );
        if( images.size( ) > 0 )
          throw new ClientComputeException(
              "InvalidAMIName.Duplicate",
              String.format("AMI name %s is already in use by EMI %s", name, images.get(0).getDisplayName( ) ) );
      } catch ( final ComputeException e ) {
        throw e;
      } catch( final Exception ex ) {
        LOG.error( "Error checking for duplicate image name", ex );
        throw new ComputeException( "InternalError", "Error processing request." );
      } finally {
        db.rollback( );
      }
    }else {
      throw new ClientComputeException("InvalidAMIName.Malformed", "AMI names must be between 3 and 128 characters long, and may contain letters, numbers, '(', ')', '.', '-', '/' and '_'");
    }

    if( description!=null && !Images.isImageDescriptionValid( description ) ){
      throw new ClientComputeException("InvalidParameter", "AMI descriptions must be less than 256 characters long");
    }
  }

  private static boolean canModifyImage( final ImageInfo imgInfo ) {
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    return
        ( ctx.isAdministrator( ) || imgInfo.getOwnerAccountNumber( ).equals( requestAccountId ) ) &&
            RestrictedTypes.filterPrivileged( ).apply( imgInfo );
  }

  private static String normalizeIdentifier( final String identifier,
                                             final String prefix,
                                             final boolean required,
                                             final String message ) throws ClientComputeException {
    try {
      return Strings.emptyToNull( identifier ) == null && !required ?
          null :
          ResourceIdentifiers.parse( prefix, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidParameterValue", String.format( message, e.getIdentifier( ) ) );
    }
  }

  private static String normalizeInstanceIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, VmInstance.ID_PREFIX, true, "Value (%s) for parameter instanceId is invalid. Expected: 'i-...'." );
  }

  private static String normalizeImageIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, null, true, "Value (%s) for parameter image is invalid." );
  }

  @Nullable
  private static String normalizeOptionalImageIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, null, false, "Value (%s) for parameter image is invalid." );
  }
}
