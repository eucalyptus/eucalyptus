/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.images;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.ImageMetadata.State;
import com.eucalyptus.compute.common.ImageMetadata.StaticDiskImage;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.vm.VmVolumeAttachment;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.EbsDeviceMapping;
import edu.ucsb.eucalyptus.msgs.ImageDetails;

public class Images {
  private static Logger LOG  = Logger.getLogger( Images.class );
  
  static final String   SELF = "self";
  public static final String DEFAULT_ROOT_DEVICE = "/dev/sda";
  public static final String DEFAULT_PARTITIONED_ROOT_DEVICE = "/dev/sda1";

  public static Predicate<ImageInfo> filterExecutableBy( final Collection<String> executableSet ) {
    final boolean executableSelf = executableSet.remove( SELF );
    final boolean executableAll = executableSet.remove( "all" );
    return new Predicate<ImageInfo>( ) {
      @Override
      public boolean apply( ImageInfo image ) {
        if ( executableSet.isEmpty( ) && !executableSelf && !executableAll ) {
          return true;
        } else {
          UserFullName userFullName = Contexts.lookup( ).getUserFullName( );
          return
              ( executableAll && image.getImagePublic( ) ) ||
              ( executableSelf && image.hasPermission( userFullName.getAccountNumber( ) ) ) ||
              image.hasPermission( executableSet.toArray( new String[ executableSet.size() ] ) );
        }
      }
      
    };
  }
  
  public enum FilterImageStates implements Predicate<ImageInfo> {
	  INSTANCE;
	  @Override
	  public boolean apply( ImageInfo input ) {
		  if (ImageMetadata.State.available.name().equals(input.getState().getExternalStateName()))
			  return true;
		  else
			  return false;
	  }
  }
  
  public enum FilterPermissions implements Predicate<ImageInfo> {
    INSTANCE;
    
    @Override
    public boolean apply( ImageInfo input ) {
      try {
        Context ctx = Contexts.lookup( );
        if ( ctx.isAdministrator( ) ) {
          return true;
        } else {
          UserFullName luser = ctx.getUserFullName( );
          /** GRZE: record why this must be so **/
          if ( input.getImagePublic( ) ) {
            return true;
          } else if ( input.getOwnerAccountNumber( ).equals( luser.getAccountNumber( ) ) ) {
            return true;
          } else if ( input.hasPermission( luser.getAccountNumber( ), luser.getUserId( ) ) ) {
            return true;
          } else {
            for ( AccessKey key : ctx.getUser( ).getKeys( ) ) {
              if ( input.hasPermission( key.getAccessKey( ) ) ) {
                return true;
              }
            }
            return false;
          }
        }
      } catch ( Exception ex ) {
        return false;
      }
    }
  }
  
  @QuantityMetricFunction( ImageMetadata.class )
  public enum CountImages implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( ImageInfo.class );
      try {
        return Entities.count( ImageInfo.named( input, null ) );
      } finally {
        db.rollback( );
      }
    }
  }
  
  @TypeMapper
  public enum KernelImageDetails implements Function<KernelImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( KernelImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getImageName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString( ) );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).getExternalStateName() );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setPlatform( ImageMetadata.Platform.linux.toString( ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum RamdiskImageDetails implements Function<RamdiskImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( RamdiskImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getImageName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setImageId( arg0.getDisplayName( ) );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString( ) );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState().getExternalStateName() );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setPlatform( ImageMetadata.Platform.linux.toString( ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum BlockStorageImageDetails implements Function<BlockStorageImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( BlockStorageImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getImageName( ) );
      i.setDescription( arg0.getDescription() );
      i.setArchitecture( arg0.getArchitecture().toString() );
      i.setRootDeviceName( arg0.getRootDeviceName() );
      i.setRootDeviceType( arg0.getRootDeviceType() );
      i.setImageId( arg0.getDisplayName() );
      i.setImageLocation( arg0.getOwnerAccountNumber( ) + "/" + arg0.getImageName( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString() );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).getExternalStateName() );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setKernelId( arg0.getKernelId( ) );
      i.setRamdiskId( arg0.getRamdiskId( ) );
      i.setPlatform( arg0.getPlatform( ).toString( ) );
      if (arg0.getVirtualizationType() == null)
    	  i.setVirtualizationType(ImageMetadata.VirtualizationType.hvm.toString());
      else
    	  i.setVirtualizationType(arg0.getVirtualizationType().toString());
      i.getBlockDeviceMappings( ).addAll( Collections2.transform( arg0.getDeviceMappings( ), DeviceMappingDetails.INSTANCE ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.getTags().addAll( arg0.getTags() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum MachineImageDetails implements Function<MachineImageInfo, ImageDetails> {
    INSTANCE;
    
    @Override
    public ImageDetails apply( MachineImageInfo arg0 ) {
      ImageDetails i = new ImageDetails( );
      i.setName( arg0.getImageName( ) );
      i.setDescription( arg0.getDescription( ) );
      i.setArchitecture( arg0.getArchitecture( ).toString( ) );
      i.setRootDeviceName( arg0.getRootDeviceName() );
      i.setRootDeviceType( arg0.getRootDeviceType() );
      i.setImageId( arg0.getDisplayName() );
      i.setImageLocation( arg0.getManifestLocation( ) );
      i.setImageOwnerId( arg0.getOwnerAccountNumber( ).toString() );//TODO:GRZE:verify imageOwnerAlias
      i.setImageState( arg0.getState( ).getExternalStateName() );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setIsPublic( arg0.getImagePublic( ) );
      i.setImageType( arg0.getImageType( ).toString( ) );
      i.setKernelId( arg0.getKernelId( ) );
      i.setRamdiskId( arg0.getRamdiskId( ) );
      i.setPlatform( arg0.getPlatform( ).toString( ) );
      if (arg0.getVirtualizationType() == null){
    	  if(ImageMetadata.Platform.windows.equals( arg0.getPlatform( )))
        	  i.setVirtualizationType(ImageMetadata.VirtualizationType.hvm.toString());
    	  else
    		  i.setVirtualizationType(ImageMetadata.VirtualizationType.paravirtualized.toString());
      }else
    	  i.setVirtualizationType(arg0.getVirtualizationType().toString());
      i.getBlockDeviceMappings( ).addAll( Collections2.transform( arg0.getDeviceMappings( ), DeviceMappingDetails.INSTANCE ) );
//      i.setStateReason( arg0.getStateReason( ) );//TODO:GRZE:NOW
//      i.setVirtualizationType( arg0.getVirtualizationType( ) );//TODO:GRZE:NOW
//      i.getProductCodes().addAll( arg0.getProductCodes() );//TODO:GRZE:NOW
//      i.setHypervisor( arg0.getHypervisor( ) );//TODO:GRZE:NOW
      return i;
    }
  }
  
  @TypeMapper
  public enum DeviceMappingDetails implements Function<DeviceMapping, BlockDeviceMappingItemType> {
    INSTANCE;
    @Override
    public BlockDeviceMappingItemType apply( DeviceMapping input ) {
      BlockDeviceMappingItemType ret = new BlockDeviceMappingItemType( );
      ret.setDeviceName( input.getDeviceName( ) );
      if ( input instanceof BlockStorageDeviceMapping ) {
        final BlockStorageDeviceMapping ebsDev = ( BlockStorageDeviceMapping ) input;
        ret.setEbs( new EbsDeviceMapping( ) {
          {
            this.setVirtualName( ebsDev.getVirtualName( ) );
            this.setSnapshotId( ebsDev.getSnapshotId( ) );
            this.setVolumeSize( ebsDev.getSize( ) );
            this.setDeleteOnTermination( ebsDev.getDelete( ) );
          }
        } );
      } else {
        ret.setVirtualName( input.getVirtualName( ) );
      }
      return ret;
    }
  }
  
  // Changing to method signature to accept a default size for generating ebs mappings. 
  // The default size will be used when both snapshot ID and volume size are missing. (AWS compliance)
  // The default size is usually size of the root device volume in case of boot from ebs images.
  public static Function<BlockDeviceMappingItemType, DeviceMapping> deviceMappingGenerator( final ImageInfo parent, 
                                                                                            final Integer rootVolSize ) {
    return deviceMappingGenerator( parent, rootVolSize, Collections.<String,String>emptyMap() );
  }
  
  public static Function<BlockDeviceMappingItemType, DeviceMapping> deviceMappingGenerator( 
      final ImageInfo parent, 
      final Integer rootVolSize,
      final Map<String,String> deviceNameMap
  ) {
    return new Function<BlockDeviceMappingItemType, DeviceMapping>( ) {
      @Override
      public DeviceMapping apply( BlockDeviceMappingItemType input ) {
        checkParam( input, notNullValue() );
        checkParam( input.getDeviceName(), notNullValue() );
        if ( isEbsMapping( input ) ) {
          final EbsDeviceMapping ebsInfo = input.getEbs( );
          Integer size = -1;
          final String snapshotId = ResourceIdentifiers.tryNormalize( ).apply( ebsInfo.getSnapshotId( ) );
          if ( ebsInfo.getVolumeSize() != null ) {
            size = ebsInfo.getVolumeSize();
          } else if ( ebsInfo.getSnapshotId() != null ){
            try {
              Snapshot snap = Transactions.find( Snapshot.named( null, snapshotId ) );
              size = snap.getVolumeSize( );
              if ( ebsInfo.getVolumeSize( ) != null && ebsInfo.getVolumeSize( ) >= snap.getVolumeSize( ) ) {
                size = ebsInfo.getVolumeSize( );
              }
            } catch ( NoSuchElementException ex ) {
              throw Exceptions.toUndeclared( new MetadataException( "Snapshot " + ebsInfo.getSnapshotId() + " does not exist" ) );
            } catch ( ExecutionException ex ) {
              LOG.error( "Unable to find snapshot " + ebsInfo.getSnapshotId() , ex );
              throw Exceptions.toUndeclared( new MetadataException( "Snapshot " + ebsInfo.getSnapshotId() + " does not exist" ) );
            }   
          } else {
            size = rootVolSize;
          }
          final String mappedDeviceName = deviceNameMap.containsKey( input.getDeviceName() ) ?
              deviceNameMap.get( input.getDeviceName() ) :
              input.getDeviceName();
          return new BlockStorageDeviceMapping( parent, mappedDeviceName, input.getEbs( ).getVirtualName( ), snapshotId, size, ebsInfo.getDeleteOnTermination( ) );
        } else if ( input.getVirtualName( ) != null ) {
          return new EphemeralDeviceMapping( parent, input.getDeviceName( ), input.getVirtualName( ) );
        } else {
          return new SuppressDeviceMappping( parent, input.getDeviceName( ) );
        }
      }
    };
  }
  
  // Utility method (and refactored code) for figuring out if a device mapping is an ebs mapping 
  public static Boolean isEbsMapping( BlockDeviceMappingItemType input ) {
	if( input.getEbs( ) != null && input.getVirtualName() == null && ( input.getNoDevice() == null || !input.getNoDevice() ) ) {
	  return Boolean.TRUE;
	} 
	return Boolean.FALSE;
  }
  
  public static <T extends ImageInfo> Predicate<T> inState( final Set<ImageMetadata.State> states ) {
    return new Predicate<T>() {
      @Override
      public boolean apply( final T imageInfo ) {
        return states.contains( imageInfo.getState() );
      }
    };
  }
  
  // Predicate for comparing image state deduced from ebs mapping against a set of input states 
  public static Predicate<BlockStorageDeviceMapping> imageInState( final Set<ImageMetadata.State> states ) {
    return new Predicate<BlockStorageDeviceMapping>() {
	  @Override
	  public boolean apply( final BlockStorageDeviceMapping arg0 ) {
	    return states.contains( arg0.getParent().getState() );
	  }
    };
  }
  
  public static enum DeviceMappingValidationOption {
    AllowSuppressMapping,
    AllowEbsMapping,
    AllowDevSda1,
    ;

    /**
     * Is this option present in the given set? 
     */
    public boolean present( final Set<DeviceMappingValidationOption> options ) {
      return options != null && options.contains( this );
    }
  }
  
  /**
   * <p>Validates the correctness of a block device mapping</p>
   * 
   * <p>Invalid cases:</p> 
   * <ul>
   * <li>Device name is assigned multiple times</li>
   * <li>Device name is a partition</li>
   * <li>Suppress mapping is not valid if <code>isSuppressMappingValid</code> argument is set to <code>Boolean.False<code></li>
   * <li>Ebs mapping is not valid if <code>isEbsMappingValid</code> argument is set to <code>Boolean.False<code></li>
   * <li>Volume size is 0</li>
   * <li>Volume size is smaller than the snapshot size </li>
   * </ul>
   * 
   * @param bdms List of <code>BlockDeviceMappingItemType</code>
   * @param options Validation options
   * @throws MetadataException for any validation failure
   */
  public static void validateBlockDeviceMappings(
      final List<BlockDeviceMappingItemType> bdms,
      final Set<DeviceMappingValidationOption> options
  ) throws MetadataException {
    if ( bdms != null ) {
      Set<String> deviceNames = Sets.newHashSet();
      int ephemeralCount = 0;

      for ( final BlockDeviceMappingItemType bdm : bdms ) {
        checkParam( bdm, notNullValue( ) );
        checkParam( bdm.getDeviceName( ), notNullValue( ) );
        if( !deviceNames.add( bdm.getDeviceName( ).replace("/dev/","") ) ) {
          throw new MetadataException( bdm.getDeviceName() + " is assigned multiple times" );
        }
      }
      final Set<String> fullDeviceNames = Sets.newHashSet();
      for(final String name : deviceNames){
    	  fullDeviceNames.add(String.format("/dev/%s", name));
      }
      deviceNames = fullDeviceNames;

      for ( final BlockDeviceMappingItemType bdm : bdms ) {
    	if ( !bdm.getDeviceName().matches("(/dev/)?([svh]|xv)d[a-z]([1-9])*")){
    		throw new MetadataException("Device name " + bdm.getDeviceName() + " is invalid");
    	} else if( bdm.getDeviceName().matches(".*\\d\\Z") && 
            !(DeviceMappingValidationOption.AllowDevSda1.present(options) && DEFAULT_PARTITIONED_ROOT_DEVICE.equals( bdm.getDeviceName() ) && !deviceNames.contains(DEFAULT_ROOT_DEVICE)) ) {
          throw new MetadataException( bdm.getDeviceName() + " is not supported. Device name cannot be a partition");  
        } else if ( bdm.getNoDevice() != null && bdm.getNoDevice() ) {
          if ( !DeviceMappingValidationOption.AllowSuppressMapping.present( options ) ) {
            throw new MetadataException( "Block device mapping for " + bdm.getDeviceName() + " cannot be suppressed" );
          }
        } else if ( StringUtils.isNotBlank( bdm.getVirtualName( ) ) ) {
          if ( !bdm.getVirtualName( ).matches( "ephemeral[0123]" ) ) {
            throw new MetadataException( "Virtual device name must be of the form ephemeral[0123]. Fix the mapping for " + bdm.getDeviceName() );
          }
          ephemeralCount ++;
          if( ephemeralCount > 1 ) {
            throw new MetadataException( "Only one ephemeral device is supported. More than one ephemeral device mappings found" );
          }
        } else if ( null != bdm.getEbs( ) ) {
          if ( !DeviceMappingValidationOption.AllowEbsMapping.present( options ) ) {
            throw new MetadataException( "Ebs block device mappings are not supported" );
          }
          final EbsDeviceMapping ebsInfo = bdm.getEbs( );
          if ( ebsInfo.getSnapshotId() != null ) {
            final Snapshot snap;
            try {
              snap = Transactions.find( Snapshot.named( null, ResourceIdentifiers.tryNormalize( ).apply( ebsInfo.getSnapshotId( ) ) ) );
            } catch ( Exception ex ) {
              LOG.error("Failed to find snapshot " + ebsInfo.getSnapshotId(), ex);
              throw new MetadataException("Unable to find snapshot " + ebsInfo.getSnapshotId() + " in the block device mapping for " + bdm.getDeviceName());
            }
            if ( ebsInfo.getVolumeSize( ) != null && ebsInfo.getVolumeSize( ) < snap.getVolumeSize( ) ) {
              throw new MetadataException( "Size of the volume cannot be smaller than the source snapshot for " + bdm.getDeviceName() );
            }
          } else if ( ebsInfo.getVolumeSize() != null && ebsInfo.getVolumeSize() == 0 ) {
            throw new MetadataException( "Volume size for " + bdm.getDeviceName() + " cannot be 0");
          }
        } else {
          // It should never get here
          throw new MetadataException( "Incorrectly constructed block device mapping for " + bdm.getDeviceName() + " . Refer to documentation" );
        }
      }
    }
  }
  
  public static boolean isImageNameValid(final String imgName){
	if(imgName==null)
		return false;
	if (!imgName.matches("[A-Za-z0-9()./_-]+"))
		return false;
	
	if (imgName.length() < 3 || imgName.length() > 128)
		return false;
	
	return true;
  }
  
  public static boolean isImageDescriptionValid(final String imgDescription){
	  if(imgDescription==null)
		  return false;
	  if(imgDescription.length()> 255)
		  return false;
	  return true;
  }
  
  public static Function<ImageInfo, ImageDetails> TO_IMAGE_DETAILS = new Function<ImageInfo, ImageDetails>( ) {
                                                                     
                                                                     @Override
                                                                     public ImageDetails apply( ImageInfo input ) {
                                                                       return TypeMappers.transform( input, ImageDetails.class );
                                                                     }
                                                                   };
  public static ImageInfo                         ALL              = new ImageInfo( );
  
  public static List<ImageInfo> listAllImages( ) {
    final List<ImageInfo> images = Lists.newArrayList( );
    final EntityTransaction db = Entities.get(ImageInfo.class);
    try {
      final List<ImageInfo> found = Entities.query( Images.ALL, true );
      images.addAll( found );
      db.rollback( );
    } catch ( final Exception e ) {
      db.rollback( );
      LOG.error("failed to query images", e);
    } finally{
      if(db.isActive())
        db.rollback();
    }
    return images;
  }
  
  public static void enableImage( String imageId ) throws NoSuchImageException {
    final EntityTransaction db = Entities.get(ImageInfo.class);
    try {
      final ImageInfo img = Entities.uniqueResult(Images.exampleWithImageId( imageId ));
      img.setState( ImageMetadata.State.available );
      db.commit();
    } catch ( final NoSuchElementException e ) {
      db.rollback();
      throw new NoSuchImageException("Failed to lookup image: " + imageId, e); 
    } catch ( final Exception e) {
      db.rollback();
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, e );
    } finally{
      if(db.isActive())
        db.rollback();
    }
  }

  public static void setImageState( String imageId, ImageMetadata.State state) throws NoSuchImageException {
	  final EntityTransaction db = Entities.get( ImageInfo.class );
	  try {
		  ImageInfo img = Entities.uniqueResult( Images.exampleWithImageId( imageId ) );
		  img.setState( state );
		  db.commit( );
	  } catch ( final Exception e ) {
		  db.rollback( );
		  throw new NoSuchImageException( "Failed to update image state: "+imageId);
	  } finally{
		  if(db.isActive())
			  db.rollback();
	  }
  }
  
  public static void deregisterImage( String imageId ) throws NoSuchImageException, InstanceNotTerminatedException {
    EntityTransaction tx = Entities.get( ImageInfo.class );
    try {
      ImageInfo img = Entities.uniqueResult( Images.exampleWithImageId( imageId ) );
      if ( ImageMetadata.State.deregistered.equals( img.getState( ) ) || 
    		  ImageMetadata.State.failed.equals( img.getState())) {
        Entities.delete( img );
      } else {
        if( img instanceof MachineImageInfo){
          final String runManifestLocation = ((MachineImageInfo)img).getRunManifestLocation();
          final String manifestLocation = ((MachineImageInfo)img).getManifestLocation();
          // cleanup system generated buckets if exist
          if(!manifestLocation.equals(runManifestLocation))
            img.setState(ImageMetadata.State.deregistered_cleanup);
          else
            img.setState( ImageMetadata.State.deregistered );
        } else
          img.setState( ImageMetadata.State.deregistered );
      }
      tx.commit( );
      if ( img instanceof ImageMetadata.StaticDiskImage ) {
        StaticDiskImages.flush( (StaticDiskImage) img );
      }
    } catch ( ConstraintViolationException cve ) {
      tx.rollback( );
      throw new InstanceNotTerminatedException("To deregister " + imageId + " all associated instances must be in the terminated state.");
    } catch ( TransactionException ex ) {
      tx.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, ex );
    } catch ( NoSuchElementException ex ) {
      tx.rollback( );
      throw new NoSuchImageException( "Failed to lookup image: " + imageId, ex );
    } finally {
        if ( tx.isActive() ) tx.rollback();
    }
  }
   
  public static MachineImageInfo exampleMachineWithImageId( final String imageId ) {
    return new MachineImageInfo( imageId );
  }
  
  public static BlockStorageImageInfo exampleBlockStorageWithImageId( final String imageId ) {
    return new BlockStorageImageInfo( imageId );
  }

  public static BlockStorageImageInfo exampleBlockStorageWithSnapshotId( final String snapshotId ) {
    final BlockStorageImageInfo info = new BlockStorageImageInfo();
    info.setSnapshotId( snapshotId );
    return info;
  }
  
  public static BlockStorageDeviceMapping exampleBSDMappingWithSnapshotId( final String snapshotId ) {
    final BlockStorageDeviceMapping bsdm = new BlockStorageDeviceMapping();
    bsdm.setSnapshotId(snapshotId);
    return bsdm;
  }

  public static KernelImageInfo exampleKernelWithImageId( final String imageId ) {
    return new KernelImageInfo( imageId );
  }

  public static KernelImageInfo lookupKernel( final String kernelId ) {
    EntityTransaction tx = Entities.get( KernelImageInfo.class );
    KernelImageInfo ret = new KernelImageInfo(  );
    try {
      ret = Entities.uniqueResult( Images.exampleKernelWithImageId( kernelId ) );
      tx.commit( );
    } catch ( Exception e ) {
      LOG.error( "Kernel '" + kernelId + "' does not exist" + e );
      throw new NoSuchElementException( "InvalidAMIID.NotFound" );
    } finally {
      if ( tx.isActive( ) ) tx.rollback( );
    }
    return ret;
  }
  
  public static RamdiskImageInfo exampleRamdiskWithImageId( final String imageId ) {
    return new RamdiskImageInfo( imageId );
  }

  public static RamdiskImageInfo lookupRamdisk( final String ramdiskId ) {
    EntityTransaction tx = Entities.get( RamdiskImageInfo.class );
    RamdiskImageInfo ret = new RamdiskImageInfo(  );
    try {
      ret = Entities.uniqueResult( Images.exampleRamdiskWithImageId( ramdiskId ) );
      tx.commit( );
    } catch ( Exception e ) {
      LOG.error( "Ramdisk '" + ramdiskId + "' does not exist" + e );
      throw new NoSuchElementException( "InvalidAMIID.NotFound" );
    } finally {
      if ( tx.isActive( ) ) tx.rollback( );
    }
    return ret;
  }

  public static ImageInfo lookupImage( String imageId ) {
    final EntityTransaction db = Entities.get(ImageInfo.class);
    try{
      final ImageInfo found = Entities.uniqueResult(Images.exampleWithImageId(imageId));
      db.commit();
      return found;
    }catch(final NoSuchElementException ex){
      db.rollback();
      throw ex;
    }catch(final Exception ex){
      db.rollback();
      throw Exceptions.toUndeclared(ex);
    }finally{
      if(db.isActive())
        db.rollback();
    }
  }
  
  public static ImageInfo exampleWithImageId( final String imageId ) {
    return new ImageInfo( imageId );
  }
  
  public static ImageInfo exampleWithName( @Nullable final OwnerFullName owner,
                                           @Nullable final String name ) {
    final ImageInfo example = new ImageInfo( );
    example.setOwner( owner );
    example.setImageName( name );
    return example;
  }
  
  public static ImageInfo exampleWithImageState( final ImageMetadata.State state ) {
    final ImageInfo img = new ImageInfo( );
    img.setState( state );
    img.setStateChangeStack( null );
    img.setLastState( null );
    return img;
  }
  
  public static ImageInfo exampleWithImageFormat( final ImageMetadata.ImageFormat format ) {
    final ImageInfo img = new ImageInfo( );
    img.setImageFormat(format.toString());
    img.setStateChangeStack( null );
    img.setLastState( null );
    return img;
  }
  
  public static Predicate<BlockDeviceMappingItemType> findEbsRoot( final String rootDevName ) {
    return findEbsRoot( rootDevName, true );
  }

  public static Predicate<BlockDeviceMappingItemType> findEbsRootOptionalSnapshot( final String rootDevName ) {
    return findEbsRoot( rootDevName, false );
  }

  private static Predicate<BlockDeviceMappingItemType> findEbsRoot( final String rootDevName,
                                                                    final boolean requireSnapshotId ) {
    return new Predicate<BlockDeviceMappingItemType>( ) {
      @Override
      public boolean apply( BlockDeviceMappingItemType input ) {
        return
            rootDevName.equals( input.getDeviceName( ) ) &&
            input.getEbs( ) != null &&
            (!requireSnapshotId || input.getEbs( ).getSnapshotId( ) != null);
      }
    };
  }
  
  public static Predicate<BlockDeviceMappingItemType> findCreateImageRoot( ) {
	  return new Predicate<BlockDeviceMappingItemType> ( ) {
		  @Override
		  public boolean apply ( BlockDeviceMappingItemType input) {
			  return input.getEbs() != null &&
					  "snap-EUCARESERVED".equals(input.getEbs().getSnapshotId( ));
		  }
	  };
  }
  
  public static Predicate<DeviceMapping> findDeviceMap ( final String deviceName ) {
    return new Predicate<DeviceMapping>( ) {
	  @Override
	  public boolean apply( DeviceMapping input ) {
	    return deviceName.equals( input.getDeviceName( ) );
	  }
	};
  }
  
  public static Predicate<BlockDeviceMappingItemType> findBlockDeviceMappingItempType ( final String deviceName ) {
    return new Predicate<BlockDeviceMappingItemType>( ) {
	  @Override
	  public boolean apply( BlockDeviceMappingItemType input ) {
		return deviceName.equals( input.getDeviceName( ) );
	  }
	};
  }
  
  public static Predicate<VmVolumeAttachment> findEbsRootVolumeAttachment( final String rootDevName ) {
	return new Predicate<VmVolumeAttachment>() {
  	  @Override
	  public boolean apply( VmVolumeAttachment input ) {
		return ( input.getDevice().equals(rootDevName) || input.getIsRootDevice() );
	  }
	};
  }
  
  public static ImageInfo createFromDeviceMapping( 
      final UserFullName userFullName,
      final String imageName,
      final String imageDescription,
      final ImageMetadata.Platform platform,
      String eki,
      String eri,
      final String rootDeviceName, 
      final List<BlockDeviceMappingItemType> blockDeviceMappings 
  ) throws EucalyptusCloudException {
    final ImageMetadata.Architecture imageArch = ImageMetadata.Architecture.x86_64;//TODO:GRZE:OMGFIXME: track parent vol info; needed here 
    final ImageMetadata.Platform imagePlatform = platform;
    if(ImageMetadata.Platform.windows.equals(imagePlatform)){
      eki = null;
      eri = null;
    }
    // Block device mappings have been verified before control gets here. 
    // If anything has changed with regard to the snapshot state, it will be caught while data structures for the image.
    final BlockDeviceMappingItemType rootBlockDevice = Iterables.find( blockDeviceMappings, findEbsRoot( rootDeviceName ), null );
    if ( rootBlockDevice == null ) {
      throw new EucalyptusCloudException( "Failed to create image, root device mapping not found: " + rootDeviceName );
    }

    final String snapshotId = ResourceIdentifiers.tryNormalize( ).apply( rootBlockDevice.getEbs( ).getSnapshotId( ) );
    Snapshot snap;
    try {
      snap = Transactions.one(
          Snapshot.named( userFullName.asAccountFullName(), snapshotId ),
          RestrictedTypes.filterPrivileged( ),
          Functions.<Snapshot>identity( ) );
    } catch ( NoSuchElementException ex ) {
      throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice + " because of: Snapshot not found " + snapshotId );
    } catch ( TransactionExecutionException ex ) {
      throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice + " because of: " + ex.getMessage( ) );
    } catch ( ExecutionException ex ) {
      LOG.error( ex, ex );
      throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice + " because of: " + ex.getMessage( ) );
    }

    final Integer suppliedVolumeSize = rootBlockDevice.getEbs().getVolumeSize() != null ? rootBlockDevice.getEbs().getVolumeSize() : snap.getVolumeSize();
    final Long imageSizeBytes = suppliedVolumeSize * 1024l * 1024l * 1024l;
    final Boolean targetDeleteOnTermination = Boolean.TRUE.equals( rootBlockDevice.getEbs( ).getDeleteOnTermination( ) );
    final String imageId = ResourceIdentifiers.generateString( ImageMetadata.Type.machine.getTypePrefix() );

    final boolean mapRoot = DEFAULT_PARTITIONED_ROOT_DEVICE.equals( rootDeviceName );
    BlockStorageImageInfo ret = new BlockStorageImageInfo( userFullName, imageId, imageName, imageDescription, imageSizeBytes,
                                                           imageArch, imagePlatform,
                                                           eki, eri,
                                                           snap.getDisplayName( ), targetDeleteOnTermination, mapRoot ? DEFAULT_ROOT_DEVICE : rootDeviceName );
    final EntityTransaction tx = Entities.get( BlockStorageImageInfo.class );
    try {
      ret = Entities.merge( ret );
      Iterables.addAll(
          ret.getDeviceMappings( ),
          Iterables.transform( blockDeviceMappings, Images.deviceMappingGenerator(
              ret,
              suppliedVolumeSize,
              mapRoot ?
                  Collections.singletonMap( DEFAULT_PARTITIONED_ROOT_DEVICE, DEFAULT_ROOT_DEVICE ) :
                  Collections.<String,String>emptyMap( )) ) );
      ret.setImageFormat(ImageMetadata.ImageFormat.fulldisk.toString());
      ret.setState( ImageMetadata.State.available );
      tx.commit( );
      LOG.info( "Registering image pk=" + ret.getDisplayName( ) + " ownerId=" + userFullName );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to register image using snapshot: " + snapshotId + " because of: " + e.getMessage( ), e );
    } finally {
      if ( tx.isActive() ) tx.rollback();
    }
    
    return ret;
  }

  public static ImageInfo createPendingFromDeviceMapping(UserFullName creator,
		  String imageNameArg,
		  String imageDescription,
		  ImageMetadata.Architecture requestArch,
		  ImageMetadata.Platform imagePlatform,
		  final List<BlockDeviceMappingItemType> blockDeviceMappings
		  ) throws Exception {

	  final String imageId = ResourceIdentifiers.generateString( ImageMetadata.Type.machine.getTypePrefix() );
	  BlockStorageImageInfo ret = new BlockStorageImageInfo( creator, imageId, imageNameArg, imageDescription, 
			  new Long(-1), requestArch, imagePlatform, null, null, "snap-EUCARESERVED", false, Images.DEFAULT_ROOT_DEVICE ); 
	  /// device with snap-EUCARESERVED is the placeholder to indicate register is for create-image only
	  /// actual root device with snapshot is filled in later 
	  BlockDeviceMappingItemType toRemove = null;
	  for(final BlockDeviceMappingItemType device : blockDeviceMappings){
		  if(Images.findCreateImageRoot().apply(device))
			  toRemove = device;
	  }
	  if(toRemove!=null)
		  blockDeviceMappings.remove(toRemove);
	  
	  final EntityTransaction tx = Entities.get( BlockStorageImageInfo.class );
	  try {
		  ret = Entities.merge( ret );
		  ret.setState(ImageMetadata.State.pending);
      ret.setImageFormat(ImageMetadata.ImageFormat.fulldisk.toString());
	      ret.getDeviceMappings( ).addAll( Lists.transform( blockDeviceMappings, Images.deviceMappingGenerator( ret, -1 ) ) );
		  tx.commit( );
		  LOG.info( "Registering image pk=" + ret.getDisplayName( ) + " ownerId=" + creator );
	  } catch ( Exception e ) {
		  tx.rollback( );
		  throw new EucalyptusCloudException( "Failed to register pending bfebs image because of: " + e.getMessage( ), e );
	  } finally {
		  if (tx.isActive())
			  tx.rollback();
	  }
	  return ret;
  }
  

  /***
   * @param imageId: id of an image already registered as pending state
   * @param userFullName
   * @param blockDeviceMappings: the mapping that contains the root device
   * @return
   * @throws EucalyptusCloudException
   */
  public static ImageInfo updateWithDeviceMapping( String imageId, UserFullName userFullName, final String rootDeviceName,
		  final List<BlockDeviceMappingItemType> blockDeviceMappings ) throws EucalyptusCloudException {
	  // Block device mappings have been verified before control gets here. 
	  // If anything has changed with regard to the snapshot state, it will be caught while data structures for the image.
	  final BlockDeviceMappingItemType rootBlockDevice = Iterables.find( blockDeviceMappings, findEbsRoot( rootDeviceName ) );
	  final String snapshotId = ResourceIdentifiers.tryNormalize( ).apply( rootBlockDevice.getEbs( ).getSnapshotId( ) );
	  try {
		  Snapshot snap = Transactions.find( Snapshot.named( userFullName, snapshotId ) );
		  if ( !userFullName.getUserId( ).equals( snap.getOwnerUserId( ) ) ) {
			  throw new EucalyptusCloudException( "Failed to create image from specified block device mapping: " + rootBlockDevice
					  + " because of: you must be the owner of the source snapshot." );
		  }

		  Integer suppliedVolumeSize = rootBlockDevice.getEbs().getVolumeSize() != null ? rootBlockDevice.getEbs().getVolumeSize() : snap.getVolumeSize();

		  Long imageSizeBytes = suppliedVolumeSize * 1024l * 1024l * 1024l;
		  Boolean targetDeleteOnTermination = Boolean.TRUE.equals( rootBlockDevice.getEbs( ).getDeleteOnTermination( ) );
		 
		  BlockStorageImageInfo ret = null;
		  final EntityTransaction tx = Entities.get( BlockStorageImageInfo.class );
		  try {
			  ret = (BlockStorageImageInfo) Entities.uniqueResult(BlockStorageImageInfo.named(imageId));
			  final List<DeviceMapping> mappings = Lists.transform( blockDeviceMappings, Images.deviceMappingGenerator( ret, suppliedVolumeSize ) );
			  ret.getDeviceMappings( ).addAll( mappings );
			  ret.setSnapshotId(snap.getDisplayName());
			  ret.setDeleteOnTerminate(targetDeleteOnTermination);
			  ret.setImageSizeBytes(imageSizeBytes);
			  ret.setRootDeviceName(rootDeviceName);
			  ret.setState( ImageMetadata.State.available );
			  Entities.persist(ret);
			  tx.commit( );
			  LOG.info( "Registering image pk=" + ret.getDisplayName( ) + " ownerId=" + userFullName );
		  } catch ( Exception e ) {
			  tx.rollback( );
			  throw new EucalyptusCloudException( "Failed to register image using snapshot: " + snapshotId + " because of: " + e.getMessage( ), e );
		  }
		  return ret;
	  } catch ( TransactionExecutionException ex ) {
		  throw new EucalyptusCloudException( "Failed to update image with specified block device mapping: " + rootBlockDevice + " because of: " + ex.getMessage( ) );
	  } catch ( ExecutionException ex ) {
		  LOG.error( ex, ex );
		  throw new EucalyptusCloudException( "Failed to update image with specified block device mapping: " + rootBlockDevice + " because of: " + ex.getMessage( ) );
	  }
  }
  
  public static ImageInfo registerFromManifest( UserFullName creator,
                                                String imageNameArg,
                                                String imageDescription,
                                                ImageMetadata.Architecture requestArch,
                                                ImageMetadata.VirtualizationType virtType,
                                                ImageMetadata.Platform platform,
                                                ImageMetadata.ImageFormat imgFormat,
                                                String eki,
                                                String eri,
                                                ImageManifest manifest ) throws Exception {
    PutGetImageInfo ret = prepareFromManifest( creator, imageNameArg, imageDescription, requestArch, virtType, platform, imgFormat, eki, eri, manifest );
    ret.setState( ImageMetadata.State.available );
    ret = persistRegistration( creator, manifest, ret );
    return ret;
  }
  
  public static ImageInfo createPendingAvailableFromManifest( UserFullName creator,
                                                     String imageNameArg,
                                                     String imageDescription,
                                                     ImageMetadata.Architecture requestArch,
                                                     ImageMetadata.VirtualizationType virtType,
                                                     ImageMetadata.Platform platform,
                                                     ImageMetadata.ImageFormat imgFormat,
                                                     String eki,
                                                     String eri,
                                                     ImageManifest manifest ) throws Exception {
    PutGetImageInfo ret = prepareFromManifest( creator, imageNameArg, imageDescription, requestArch, 
        virtType, platform, imgFormat, eki, eri, manifest );
    ret.setState( ImageMetadata.State.pending_available );
    ret = persistRegistration( creator, manifest, ret );
    return ret;
  }
    
  public static void registerFromPendingImage( String imageId ) throws Exception {
    EntityTransaction tx = Entities.get( PutGetImageInfo.class );
    try {
      ImageInfo ret = Entities.uniqueResult( Images.exampleWithImageId( imageId ) );
      ret.setState( State.available );
      tx.commit( );
    } catch ( Exception e ) {
      tx.rollback( );
      throw new EucalyptusCloudException( "Failed to update image: " + imageId + " because of: " + e.getMessage( ), e );
    }
  }
  
  private static PutGetImageInfo prepareFromManifest( UserFullName creator,
                                                      String imageNameArg,
                                                      String imageDescription,
                                                      ImageMetadata.Architecture requestArch,
                                                      ImageMetadata.VirtualizationType virtType, 
                                                      ImageMetadata.Platform platform,
                                                      ImageMetadata.ImageFormat format,
                                                      String eki,
                                                      String eri,
                                                      ImageManifest manifest ) throws Exception {
    PutGetImageInfo ret = null;
    String imageName = ( imageNameArg != null )
      ? imageNameArg
      : manifest.getName( );
    eki = ( eki != null )
      ? eki
      : manifest.getKernelId( );
    eri = ( eri != null )
      ? eri
      : manifest.getRamdiskId( );
    ImageMetadata.Architecture imageArch = ( requestArch != null )
      ? requestArch
      : manifest.getArchitecture( );
    final ImageMetadata.Platform imagePlatform = platform;    
    switch ( manifest.getImageType( ) ) {
      case kernel:
        ret = new KernelImageInfo( creator, ResourceIdentifiers.generateString( ImageMetadata.Type.kernel.getTypePrefix() ),
                                   imageName, imageDescription, manifest.getSize( ), imageArch, imagePlatform,
                                    manifest.getImageLocation( ), manifest.getBundledSize( ), manifest.getChecksum( ), manifest.getChecksumType( ) );
        break;
      case ramdisk:
        ret = new RamdiskImageInfo( creator, ResourceIdentifiers.generateString( ImageMetadata.Type.ramdisk.getTypePrefix() ),
                                    imageName, imageDescription, manifest.getSize( ), imageArch, imagePlatform,
                                    manifest.getImageLocation( ), manifest.getBundledSize( ), manifest.getChecksum( ), manifest.getChecksumType( ) );
        break;
      case machine:
    	if(ImageMetadata.Platform.windows.equals(imagePlatform)){
    		  virtType = ImageMetadata.VirtualizationType.hvm;
    	}
    	
    	ret = new MachineImageInfo( creator, ResourceIdentifiers.generateString( ImageMetadata.Type.machine.getTypePrefix() ),
    	    imageName, imageDescription, manifest.getSize( ), imageArch, imagePlatform,
    	    manifest.getImageLocation( ), manifest.getBundledSize( ), manifest.getChecksum( ), manifest.getChecksumType( ), eki, eri , virtType);
    	ret.setImageFormat(format.toString());
    	if( ImageMetadata.VirtualizationType.hvm.equals(virtType) ){
    	  ((MachineImageInfo) ret).setRunManifestLocation(manifest.getImageLocation());
    	}
    	break;
    }
    if ( ret == null ) {
      throw new IllegalArgumentException( "Failed to prepare image using the provided image manifest: " + manifest );
    } else {
      ret.setSignature( manifest.getSignature( ) );
      return ret;
    }
  }
  
  private static PutGetImageInfo persistRegistration( UserFullName creator, ImageManifest manifest, PutGetImageInfo ret ) throws Exception {
    
    EntityTransaction tx = Entities.get( PutGetImageInfo.class );
    try {
      ret = Entities.merge( ret );
      tx.commit( );
      LOG.info( "Registering image pk=" + ret.getDisplayName( ) + " ownerId=" + creator );
    } catch ( Exception e ) {
      tx.rollback( );
      throw new EucalyptusCloudException( "Failed to register image: " + manifest + " because of: " + e.getMessage( ), e );
    }
    // TODO:GRZE:RESTORE
// for( String p : extractProductCodes( inputSource, xpath ) ) {
// imageInfo.addProductCode( p );
// }
// imageInfo.grantPermission( ctx.getAccount( ) );
    LOG.info( "Triggering cache population in Walrus for: " + ret.getDisplayName( ) );
    if ( ret instanceof ImageMetadata.StaticDiskImage && ret.getRunManifestLocation()!=null) {
      StaticDiskImages.prepare( ret.getRunManifestLocation( ) );
    }
    return ret;
  }
  
  public static ImageConfiguration configuration( ) {
    return ImageConfiguration.getInstance( );
  }
  

  public static void setConversionTaskId(final String imageId, final String taskId){
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      try{
        final ImageInfo entity = Entities.uniqueResult(Images.exampleWithImageId(imageId));
        ((MachineImageInfo)entity).setImageConversionId(taskId);
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    } 
  }
  
  public static void setImageFormat(final String imageId, ImageMetadata.ImageFormat format){
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      try{
        final ImageInfo entity = Entities.uniqueResult(Images.exampleWithImageId(imageId));
        entity.setImageFormat(format.toString());
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void setRunManifestLocation(final String imageId, final String runManifestLocation){
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      try{
        final ImageInfo entity = Entities.uniqueResult(Images.exampleWithImageId(imageId));
        ((MachineImageInfo)entity).setRunManifestLocation(runManifestLocation);
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public static void setImageVirtualizationType(final String imageId, ImageMetadata.VirtualizationType virtType){
    try ( final TransactionResource db =
        Entities.transactionFor( ImageInfo.class ) ) {
      try{
        final ImageInfo entity = Entities.uniqueResult(Images.exampleWithImageId(imageId));
        ((MachineImageInfo) entity).setVirtualizationType(virtType);
        Entities.persist(entity);
        db.commit();
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }

  public static class ImageInfoFilterSupport extends FilterSupport<ImageInfo> {
    public ImageInfoFilterSupport() {
      super( builderFor( ImageInfo.class )
          .withTagFiltering( ImageInfoTag.class, "image" )
          .withStringProperty( "architecture", FilterStringFunctions.ARCHITECTURE )
          .withBooleanSetProperty( "block-device-mapping.delete-on-termination", FilterBooleanSetFunctions.BLOCK_DEVICE_MAPPING_DELETE_ON_TERMINATION )
          .withStringSetProperty( "block-device-mapping.device-name", FilterStringSetFunctions.BLOCK_DEVICE_MAPPING_DEVICE_NAME )
          .withStringSetProperty( "block-device-mapping.snapshot-id", FilterStringSetFunctions.BLOCK_DEVICE_MAPPING_SNAPSHOT_ID )
          .withIntegerSetProperty( "block-device-mapping.volume-size", FilterIntegerSetFunctions.BLOCK_DEVICE_MAPPING_VOLUME_SIZE )
          .withConstantProperty( "block-device-mapping.volume-type", "standard" )
          .withStringProperty( "description", FilterStringFunctions.DESCRIPTION )
          .withStringProperty( "image-id", CloudMetadatas.toDisplayName() )
          .withStringProperty( "image-type", FilterStringFunctions.IMAGE_TYPE )
          .withBooleanProperty( "is-public", FilterBooleanFunctions.IS_PUBLIC )
          .withStringProperty( "kernel-id", asImageInfoFunction( BootableImageInfoFilterStringFunctions.KERNEL_ID ) )
          .withStringProperty( "manifest-location", FilterStringFunctions.MANIFEST_LOCATION )
          .withStringProperty( "name", FilterStringFunctions.NAME )
          .withLikeExplodedProperty( "owner-alias", FilterStringFunctions.OWNER_ID, accountAliasExploder() )
          .withStringProperty( "owner-id", FilterStringFunctions.OWNER_ID )
          .withStringProperty( "platform", FilterStringFunctions.PLATFORM )
          .withStringSetProperty( "product-code", FilterStringSetFunctions.PRODUCT_CODE )
          .withUnsupportedProperty( "product-code.type" )
          .withStringProperty( "ramdisk-id", asImageInfoFunction( BootableImageInfoFilterStringFunctions.RAMDISK_ID ) )
          .withStringProperty( "root-device-name", asImageInfoFunction( BootableImageInfoFilterStringFunctions.ROOT_DEVICE_NAME ) )
          .withStringProperty( "root-device-type", asImageInfoFunction( BootableImageInfoFilterStringFunctions.ROOT_DEVICE_TYPE ) )
          .withStringProperty( "state", FilterStringFunctions.STATE )
          .withUnsupportedProperty( "state-reason-code" )
          .withUnsupportedProperty( "state-reason-message" )
          .withStringProperty( "virtualization-type", FilterStringFunctions.VIRTUALIZATION_TYPE )
          .withUnsupportedProperty( "hypervisor" )
          .withPersistenceAlias( "deviceMappings", "deviceMappings" )
          .withPersistenceFilter( "architecture", "architecture", Enums.valueOfFunction( ImageMetadata.Architecture.class ) )
          .withPersistenceFilter( "block-device-mapping.delete-on-termination", "deviceMappings.delete", PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "block-device-mapping.device-name", "deviceMappings.deviceName" )
          .withPersistenceFilter( "block-device-mapping.snapshot-id", "deviceMappings.snapshotId" )
          .withPersistenceFilter( "block-device-mapping.volume-size", "deviceMappings.size", PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "description" )
          .withPersistenceFilter( "image-id", "displayName" )
          .withPersistenceFilter( "image-type", "imageType", Enums.valueOfFunction( ImageMetadata.Type.class ) )
          .withPersistenceFilter( "is-public", "imagePublic", PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "kernel-id", "kernelId" )
          .withPersistenceFilter( "manifest-location", "manifestLocation" )
          .withPersistenceFilter( "name", "imageName" )
          .withLikeExplodingPersistenceFilter( "owner-alias", "ownerAccountNumber", accountAliasExploder() )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "platform", "platform", Enums.valueOfFunction( ImageMetadata.Platform.class ) )
          .withPersistenceFilter( "ramdisk-id", "ramdiskId" )
          .withPersistenceFilter( "state", "state", Enums.valueOfFunction( ImageMetadata.State.class ) )
          .withPersistenceFilter( "virtualization-type", "virtType", ImageMetadata.VirtualizationType.fromString( ) )
      );
    }
  }

  private static Function<String,Collection> accountAliasExploder() {
    return new Function<String,Collection>() {
      @Override
      public Collection<String> apply( final String accountAliasExpression ) {
        try {
          return Accounts.resolveAccountNumbersForName( accountAliasExpression );
        } catch ( AuthException e ) {
          LOG.error( e, e );
          return Collections.emptySet();
        }
      }
    };  
  }
  
  private static <T> Function<ImageInfo,T> asImageInfoFunction( final Function<BootableImageInfo,T> bootableImageInfoFunction ) {
    return Images.typedFunction( bootableImageInfoFunction, BootableImageInfo.class, null );        
  }
  
  private static <R, T, TT> Function<T, R> typedFunction( final Function<TT,R> typeSpecificFunction, 
                                                                      final Class<TT> subClass,
                                                                      @Nullable final R defaultValue ) {
    return new Function<T,R>() {
      @Override
      public R apply( final T parameter ) {
        return subClass.isInstance( parameter ) ?
            typeSpecificFunction.apply( subClass.cast( parameter ) ) :
            defaultValue;
      }
    };     
  }
  
  private static <T> Set<T> blockDeviceSet( final ImageInfo imageInfo,
                                            final Function<DeviceMapping,T> transform ) {
    return Sets.newHashSet( Iterables.transform(
        imageInfo.getDeviceMappings(),
        transform ) );
  }

  private enum BlockDeviceMappingBooleanFilterFunctions implements Function<BlockStorageDeviceMapping,Boolean>  {
    DELETE_ON_TERMINATION {
      @Override
      public Boolean apply( final BlockStorageDeviceMapping deviceMapping ) {
        return deviceMapping.getDelete();
      }
    }
  }

  private enum BlockDeviceMappingIntegerFilterFunctions implements Function<BlockStorageDeviceMapping,Integer>  {
    SIZE {
      @Override
      public Integer apply( final BlockStorageDeviceMapping deviceMapping ) {
        return deviceMapping.getSize();
      }
    }
  }

  private enum BlockDeviceMappingFilterFunctions implements Function<BlockStorageDeviceMapping, String> {
    SNAPSHOT_ID {
      @Override
      public String apply( final BlockStorageDeviceMapping deviceMapping ) {
        return deviceMapping.getSnapshotId();
      }
    }
  }

  private enum DeviceMappingFilterFunctions implements Function<DeviceMapping, String> {
    DEVICE_NAME {
      @Override
      public String apply( final DeviceMapping deviceMapping ) {
        return deviceMapping.getDeviceName();
      }
    }
  }

  private enum FilterBooleanFunctions implements Function<ImageInfo,Boolean> {
    IS_PUBLIC {
      @Override
      public Boolean apply( final ImageInfo imageInfo ) {
        return imageInfo.getImagePublic();
      }
    } 
  }  
  
  private enum FilterBooleanSetFunctions implements Function<ImageInfo,Set<Boolean>> {
    BLOCK_DEVICE_MAPPING_DELETE_ON_TERMINATION {
      @Override
      public Set<Boolean> apply( final ImageInfo imageInfo ) {
        return blockDeviceSet( imageInfo,
            Images.<Boolean, DeviceMapping, BlockStorageDeviceMapping>typedFunction( BlockDeviceMappingBooleanFilterFunctions.DELETE_ON_TERMINATION, BlockStorageDeviceMapping.class, null ) );
      }
    } 
  }

  private enum FilterIntegerSetFunctions implements Function<ImageInfo,Set<Integer>> {
    BLOCK_DEVICE_MAPPING_VOLUME_SIZE {
      @Override
      public Set<Integer> apply( final ImageInfo imageInfo ) {
        return blockDeviceSet( imageInfo,
            Images.<Integer, DeviceMapping, BlockStorageDeviceMapping>typedFunction( BlockDeviceMappingIntegerFilterFunctions.SIZE, BlockStorageDeviceMapping.class, null ) );
      }
    } 
  }

  private enum BootableImageInfoFilterStringFunctions implements Function<BootableImageInfo,String> {
    KERNEL_ID {
      @Override
      public String apply( final BootableImageInfo imageInfo ) {
        return imageInfo.getKernelId();
      }
    },
    RAMDISK_ID {
      @Override
      public String apply( final BootableImageInfo imageInfo ) {
        return imageInfo.getRamdiskId();
      }
    },
    ROOT_DEVICE_NAME {
      @Override
      public String apply( final BootableImageInfo imageInfo ) {
        return imageInfo.getRootDeviceName();
      }
    },
    ROOT_DEVICE_TYPE {
      @Override
      public String apply( final BootableImageInfo imageInfo ) {
        return imageInfo.getRootDeviceType();
      }
    },
  }

  private enum FilterStringFunctions implements Function<ImageInfo,String> {
    ARCHITECTURE {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return Strings.toString( imageInfo.getArchitecture() );
      }
    }, 
    DESCRIPTION {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return imageInfo.getDescription();
      }
    }, 
    IMAGE_TYPE {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return Strings.toString( imageInfo.getImageType() );
      }
    },
    MANIFEST_LOCATION {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return imageInfo instanceof PutGetImageInfo ? 
          ((PutGetImageInfo) imageInfo).getManifestLocation() :
          null; 
      }
    },  
    NAME {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return imageInfo.getImageName();
      }
    },
    OWNER_ID {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return imageInfo.getOwnerAccountNumber();
      }
    },
    PLATFORM {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return Strings.toString( imageInfo.getPlatform() );
      }
    },
    STATE {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return Strings.toString( imageInfo.getState() );
      }
    },
    VIRTUALIZATION_TYPE {
      @Override
      public String apply( final ImageInfo imageInfo ) {
        return imageInfo instanceof MachineImageInfo ?
            Strings.toString( ((MachineImageInfo)imageInfo).getVirtualizationType() ) :
            null;
      }
    }
 }

  private enum FilterStringSetFunctions implements Function<ImageInfo,Set<String>> {
    BLOCK_DEVICE_MAPPING_DEVICE_NAME {
      @Override
      public Set<String> apply( final ImageInfo imageInfo ) {
        return blockDeviceSet( imageInfo, DeviceMappingFilterFunctions.DEVICE_NAME );
      }
    }, 
    BLOCK_DEVICE_MAPPING_SNAPSHOT_ID {
      @Override
      public Set<String> apply( final ImageInfo imageInfo ) {
        return blockDeviceSet( imageInfo,
            Images.<String,DeviceMapping,BlockStorageDeviceMapping>typedFunction(BlockDeviceMappingFilterFunctions.SNAPSHOT_ID, BlockStorageDeviceMapping.class, null ) );
      }
    }, 
    PRODUCT_CODE {
      @Override
      public Set<String> apply( final ImageInfo imageInfo ) {
        return imageInfo.getProductCodes();
      }
    }
  }

  public static void cleanDeregistered( ) {
    List<String> imageIdentifiers;
    try {
      imageIdentifiers = Transactions.filteredTransform(
          Images.exampleWithImageState( ImageMetadata.State.deregistered ),
          Predicates.alwaysTrue(),
          RestrictedTypes.toDisplayName() );
    } catch ( TransactionException e ) {
      LOG.error( "Error loading deregistered image list", e );
      imageIdentifiers = Collections.emptyList( );
    }

    for ( final String imageIdentifier : imageIdentifiers ) try {
      Transactions.delete( Images.exampleWithImageId( imageIdentifier ) );
    } catch ( RuntimeException | TransactionException e ) {
      Logs.extreme().debug( "Attempted image delete failed (image still referenced?): " + imageIdentifier, e );
    }
  }

  /**
   * Predicate matching images in a standard state.
   *
   * @see com.eucalyptus.compute.common.ImageMetadata.State#standardState( )
   */
  public static Predicate<ImageInfo> standardStatePredicate( ) {
    return StandardStatePredicate.INSTANCE;
  }

  private enum StandardStatePredicate implements Predicate<ImageInfo> {
    INSTANCE;

    @Override
    public boolean apply( final com.eucalyptus.images.ImageInfo imageInfo ) {
      return imageInfo.getState( ).standardState( );
    }
  }

  public static class ImageCleanupEventListener implements EventListener<Hertz> {
    private static final Supplier<Long> periodSupplier = 
        Suppliers.memoizeWithExpiration( ConfigurationValueSupplier.INSTANCE, 10, TimeUnit.SECONDS );
    
    public static void register( ) {
      Listeners.register( Hertz.class, new ImageCleanupEventListener( ) );
    }

    @Override
    public void fireEvent( final Hertz hertz ) {
      if ( Bootstrap.isOperational( ) &&
          !Databases.isVolatile( ) &&
          periodSupplier.get( ) > 0 && 
          hertz.isAsserted( TimeUnit.MILLISECONDS.toSeconds( periodSupplier.get( ) ) ) &&
          Topology.isEnabledLocally( Eucalyptus.class ) ) {
        cleanDeregistered( );
      }
    }
    
    private enum ConfigurationValueSupplier implements Supplier<Long> {
      INSTANCE;      
      @Override
      public Long get() {
        return ImageConfiguration.getInstance( ).getCleanupPeriodMillis( );
      }
    }
  }

}
