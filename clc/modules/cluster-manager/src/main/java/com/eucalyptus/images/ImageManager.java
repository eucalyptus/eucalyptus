/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.ConfirmProductInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.ConfirmProductInstanceType;
import edu.ucsb.eucalyptus.msgs.CreateImageResponseType;
import edu.ucsb.eucalyptus.msgs.CreateImageType;
import edu.ucsb.eucalyptus.msgs.DeregisterImageResponseType;
import edu.ucsb.eucalyptus.msgs.DeregisterImageType;
import edu.ucsb.eucalyptus.msgs.DescribeImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImageAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.LaunchPermissionItemType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeType;
import edu.ucsb.eucalyptus.msgs.RegisterImageResponseType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;
import edu.ucsb.eucalyptus.msgs.ResetImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetImageAttributeType;

public class ImageManager {
  
  public static Logger        LOG = Logger.getLogger( ImageManager.class );
  
  private static final String ADD = "add";
  
  public DescribeImagesResponseType describe( final DescribeImagesType request ) throws EucalyptusCloudException, TransactionException {
    DescribeImagesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    final List<String> ownersSet = request.getOwnersSet( );
    if ( ownersSet.remove( Images.SELF ) ) {
      ownersSet.add( requestAccountId );
    }
    Predicate<ImageInfo> rangeFilter = Predicates.and( CloudMetadatas.filterById( request.getImagesSet( ) ),
                                                       CloudMetadatas.filterByOwningAccount( request.getOwnersSet( ) ),
                                                       Images.filterExecutableBy( request.getExecutableBySet( ) ) );
    Predicate<ImageInfo> privilegesFilter = Predicates.and( Images.FilterPermissions.INSTANCE, RestrictedTypes.filterPrivilegedWithoutOwner( ) );
    Predicate<ImageInfo> filter = Predicates.and( privilegesFilter, rangeFilter );
    List<ImageDetails> imageDetailsList = Transactions.filteredTransform( new ImageInfo( ), filter, Images.TO_IMAGE_DETAILS );
    reply.getImagesSet( ).addAll( imageDetailsList );
    ImageUtil.cleanDeregistered( );
    return reply;
  }
  
  public RegisterImageResponseType register( final RegisterImageType request ) throws EucalyptusCloudException, AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    final Context ctx = Contexts.lookup( );
    ImageInfo imageInfo = null;
    final String rootDevName = ( request.getRootDeviceName( ) != null )
      ? request.getRootDeviceName( )
      : "/dev/sda1";
    final String eki = request.getKernelId( );
    final String eri = request.getRamdiskId( );
    if ( request.getImageLocation( ) != null ) {
      final ImageManifest manifest = ImageManifests.lookup( request.getImageLocation( ) );
      LOG.debug( "Obtained manifest information for requested image registration: " + manifest );
      List<DeviceMapping> vbr = Lists.transform( request.getBlockDeviceMappings( ), Images.deviceMappingGenerator( imageInfo ) );
      final ImageMetadata.Architecture arch = ( request.getArchitecture( ) == null
        ? null
        : ImageMetadata.Architecture.valueOf( request.getArchitecture( ) ) );
      Supplier<ImageInfo> allocator = new Supplier<ImageInfo>( ) {
        
        @Override
        public ImageInfo get( ) {
          try {
            return Images.createFromManifest( ctx.getUserFullName( ), request.getName( ), request.getDescription( ), arch, eki, eri, manifest );
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
      Supplier<ImageInfo> allocator = new Supplier<ImageInfo>( ) {
        
        @Override
        public ImageInfo get( ) {
          try {
            return Images.createFromDeviceMapping( ctx.getUserFullName( ), request.getName( ),
                                                   request.getDescription( ), eki, eri, rootDevName,
                                                   request.getBlockDeviceMappings( ) );
          } catch ( EucalyptusCloudException ex ) {
            throw new RuntimeException( ex );
          }
        }
      };
      imageInfo = RestrictedTypes.allocateUnitlessResource( allocator );
    } else {
      throw new EucalyptusCloudException( "Malformed registration. A request must specify either " +
                                          "a manifest path or a snapshot to use for BFE. Provided values are: imageLocation="
                                          + request.getImageLocation( ) + " blockDeviceMappings=" + request.getBlockDeviceMappings( ) );
    }
    
    RegisterImageResponseType reply = ( RegisterImageResponseType ) request.getReply( );
    reply.setImageId( imageInfo.getDisplayName( ) );
    return reply;
  }
  
  private List<String> extractProductCodes( Document inputSource, XPath xpath ) {
    List<String> prodCodes = Lists.newArrayList( );
    NodeList productCodes = null;
    try {
      productCodes = ( NodeList ) xpath.evaluate( "/manifest/machine_configuration/product_codes/product_code/text()", inputSource, XPathConstants.NODESET );
      for ( int i = 0; i < productCodes.getLength( ); i++ ) {
        for ( String productCode : productCodes.item( i ).getNodeValue( ).split( "," ) ) {
          prodCodes.add( productCode );
        }
      }
    } catch ( XPathExpressionException e ) {
      LOG.error( e, e );
    }
    return prodCodes;
  }
  
  public DeregisterImageResponseType deregister( DeregisterImageType request ) throws EucalyptusCloudException {
    DeregisterImageResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    final User requestUser = Contexts.lookup( ).getUser( );
    final String action = PolicySpec.requestToAction( request );
    
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( !ctx.hasAdministrativePrivileges( ) &&
           ( !imgInfo.getOwnerAccountNumber( ).equals( requestAccountId ) ||
               !RestrictedTypes.filterPrivileged( ).apply( imgInfo ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to deregister image" );
      }
      Images.deregisterImage( imgInfo.getDisplayName( ) );
      return reply;
    } catch ( NoSuchImageException ex ) {
      LOG.trace( ex );
      reply.set_return( false );
      return reply;
    } catch ( NoSuchElementException ex ) {
      LOG.trace( ex );
      reply.set_return( false );
      return reply;
    } finally {
      db.commit( );
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
  
  public DescribeImageAttributeResponseType describeImageAttribute( DescribeImageAttributeType request ) throws EucalyptusCloudException {
    DescribeImageAttributeResponseType reply = ( DescribeImageAttributeResponseType ) request.getReply( );
    reply.setImageId( request.getImageId( ) );
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    final User requestUser = Contexts.lookup( ).getUser( );
    final String action = PolicySpec.requestToAction( request );
    
    if ( request.getAttribute( ) != null ) request.applyAttribute( );
    
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( !ctx.hasAdministrativePrivileges( ) &&
           ( !imgInfo.getOwnerAccountNumber( ).equals( requestAccountId ) || !RestrictedTypes.filterPrivileged( ).apply( imgInfo ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to describe image attribute" );
      }
      if ( request.getKernel( ) != null ) {
        reply.setRealResponse( reply.getKernel( ) );
        if ( imgInfo instanceof MachineImageInfo ) {
          if ( ( ( MachineImageInfo ) imgInfo ).getKernelId( ) != null ) {
            reply.getKernel( ).add( ( ( MachineImageInfo ) imgInfo ).getKernelId( ) );
          }
        }
      } else if ( request.getRamdisk( ) != null ) {
        reply.setRealResponse( reply.getRamdisk( ) );
        if ( imgInfo instanceof MachineImageInfo ) {
          if ( ( ( MachineImageInfo ) imgInfo ).getRamdiskId( ) != null ) {
            reply.getRamdisk( ).add( ( ( MachineImageInfo ) imgInfo ).getRamdiskId( ) );
          }
        }
      } else if ( request.getLaunchPermission( ) != null ) {
        reply.setRealResponse( reply.getLaunchPermission( ) );
        if ( imgInfo.getImagePublic( ) ) {
          reply.getLaunchPermission( ).add( LaunchPermissionItemType.getGroup( ) );
        }
        /**
         * TODO:GRZE:RESTORE: this is so very wrong now
         * 
         * @see {@link com.eucalyptus.auth.Accounts} for ID details
         * @see {@link com.eucalyptus.auth.Account} for Account ID details
         */
//        for ( LaunchPermission auth : imgInfo.getPermissions( ) )
        reply.getLaunchPermission( ).add( LaunchPermissionItemType.getUser( Contexts.lookup( ).getAccount( ).getAccountNumber( ) ) );
      } else if ( request.getProductCodes( ) != null ) {
        reply.setRealResponse( reply.getProductCodes( ) );
        reply.getProductCodes( ).addAll( imgInfo.getProductCodes( ) );
      } else if ( request.getBlockDeviceMapping( ) != null ) {
        reply.setRealResponse( reply.getBlockDeviceMapping( ) );
        reply.getBlockDeviceMapping( ).add( ImageUtil.EMI );
        reply.getBlockDeviceMapping( ).add( ImageUtil.EPHEMERAL );
        reply.getBlockDeviceMapping( ).add( ImageUtil.SWAP );
        reply.getBlockDeviceMapping( ).add( ImageUtil.ROOT );
      } else {
        throw new EucalyptusCloudException( "invalid image attribute request." );
      }
    } finally {
      db.commit( );
    }
    return reply;
  }
  
  public ModifyImageAttributeResponseType modifyImageAttribute( ModifyImageAttributeType request ) throws EucalyptusCloudException {
    ModifyImageAttributeResponseType reply = ( ModifyImageAttributeResponseType ) request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    final User requestUser = ctx.getUser( );
    final String action = PolicySpec.requestToAction( request );
    
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( !ctx.hasAdministrativePrivileges( ) &&
           ( !imgInfo.getOwnerAccountNumber( ).equals( requestAccountId ) ||
               !RestrictedTypes.filterPrivileged( ).apply( imgInfo ) ) ) {
        throw new EucalyptusCloudException( "Not authorized to modify image attribute" );
      }
      // Product codes
      for ( String productCode : request.getProductCodes( ) ) {
        imgInfo.addProductCode( productCode );
      }
      // Launch permissions
      if ( request.getAttribute( ) != null ) {
        if ( ADD.equals( request.getOperationType( ) ) ) {
          imgInfo.addPermissions( request.getQueryUserId( ) );
          // Only "all" is valid
          if ( !request.getQueryUserGroup( ).isEmpty( ) ) {
            imgInfo.setImagePublic( true );
          }
        } else {
          imgInfo.removePermissions( request.getQueryUserId( ) );
          // Only "all" is valid
          if ( !request.getQueryUserGroup( ).isEmpty( ) ) {
            imgInfo.setImagePublic( false );
          }
        }
      }
      db.commit( );
      reply.set_return( true );
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      db.rollback( );
      reply.set_return( false );
    }
    
    return reply;
  }
  
  public ResetImageAttributeResponseType resetImageAttribute( ResetImageAttributeType request ) throws EucalyptusCloudException {
    ResetImageAttributeResponseType reply = ( ResetImageAttributeResponseType ) request.getReply( );
    reply.set_return( true );
    final Context ctx = Contexts.lookup( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountNumber( );
    final User requestUser = ctx.getUser( );
    final String action = PolicySpec.requestToAction( request );
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( ctx.hasAdministrativePrivileges( ) ||
           ( imgInfo.getOwnerAccountNumber( ).equals( requestAccountId ) &&
               !RestrictedTypes.filterPrivileged( ).apply( imgInfo ) ) ) {
        imgInfo.resetPermission( );
        db.commit( );
      } else {
        db.rollback( );
        reply.set_return( false );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      db.rollback( );
      reply.set_return( false );
    }
    return reply;
  }
  
  public CreateImageResponseType createImage( CreateImageType request ) throws EucalyptusCloudException {
    CreateImageResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    VmInstance vm;
    try {
      vm = RestrictedTypes.doPrivileged( request.getInstanceId( ), VmInstance.class );
      if ( !VmState.RUNNING.equals( vm.getState( ) ) && !VmState.STOPPED.equals( vm.getState( ) ) ) {
        throw new EucalyptusCloudException( "Cannot create an image from an instance which is not in either the 'running' or 'stopped' state: "
                                            + vm.getInstanceId( ) + " is in state " + vm.getState( ).getName( ) );
      }
      if ( vm.isBlockStorage( ) && !ctx.hasAdministrativePrivileges( ) ) {
        throw new EucalyptusCloudException( "Cannot create an image from an instance which is not booted from a volume: " + vm.getInstanceId( )
                                            + " is in state "
                                            + vm.getState( ).getName( ) );
      } else if ( vm.isBlockStorage( ) ) {

      } else {
        Cluster cluster = null;
        try {
          ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, vm.lookupPartition( ) );
          cluster = Clusters.lookup( ccConfig );
        } catch ( NoSuchElementException e ) {
          LOG.debug( e );
          throw new EucalyptusCloudException( "Cluster does not exist: " + vm.getPartition( )  );
        }
      }
    } catch ( AuthException ex ) {
      throw new EucalyptusCloudException( "Not authorized to create an image from instance " + request.getInstanceId( ) + " as " + ctx.getUser( ).getName( ) );
    } catch ( NoSuchElementException ex ) {
      throw new EucalyptusCloudException( "Instance does not exist: " + request.getInstanceId( ), ex );
    } catch ( PersistenceException ex ) {
      throw new EucalyptusCloudException( "Instance does not exist: " + request.getInstanceId( ), ex );
    }
    //save instance state
    //terminate the instance
    //clone the volume
    //-> start the instance
    //-> snapshot the volume
    //   |-> mark registration as available
    
    return reply;
  }
  
}
