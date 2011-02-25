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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.ImageUserGroup;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.blockstorage.WalrusUtil;
import com.eucalyptus.cloud.Image;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.component.ResourceLookup;
import com.eucalyptus.component.ResourceLookupException;
import com.eucalyptus.component.ResourceOwnerLookup;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.ProductCode;
import com.eucalyptus.util.CheckedFunction;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Lookup;
import com.eucalyptus.util.Lookups;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.VirtualBootRecord;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
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
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class ImageManager {
  public static Logger LOG = Logger.getLogger( ImageManager.class );
  
  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    RunInstancesType msg = vmAllocInfo.getRequest( );
    String imageId = msg.getImageId( );
    BootableSet bootSet = Emis.newBootableSet( imageId );
    vmAllocInfo.setPlatform( bootSet.getMachine( ).getPlatform( ).name( ) );
    
    if ( bootSet.isLinux( ) ) {
      bootSet = Emis.bootsetWithKernel( bootSet );
      bootSet = Emis.bootsetWithRamdisk( bootSet );
    }
    ArrayList<String> ancestorIds = Lists.newArrayList( );//GRZE: fixme ImageUtil.getAncestors( msg.getUserId( ), diskInfo.getImageLocation( ) );
    
    Emis.checkStoredImage( bootSet );
    
    VmTypeInfo vmType = vmAllocInfo.getVmTypeInfo( );
    bootSet.populateVirtualBootRecord( vmType );
    
    return vmAllocInfo;
  }
  
  public DescribeImagesResponseType describe( DescribeImagesType request ) throws EucalyptusCloudException {
    DescribeImagesResponseType reply = request.getReply( );
    ImageUtil.cleanDeregistered( );
    final Context ctx = Contexts.lookup( );
    final User requestUser = ctx.getUser( );
    final Account requestAccount = ctx.getAccount( );
    final String requestAccountId = ctx.getUserFullName( ).getAccountId( );
    final List<String> imageList = request.getImagesSet( );
    final List<String> owners = request.getOwnersSet( );
    final List<String> executable = request.getExecutableBySet( );
    final boolean showPublic = executable.remove( "all" );
    final boolean showMyImages = owners.remove( "self" );
    final boolean showMyAllowedImages = executable.remove( "self" );
    List<ImageInfo> images = Transactions.filter( new ImageInfo( ), new Predicate<ImageInfo>( ) {
      
      @Override
      public boolean apply( ImageInfo t ) {
        if ( showMyImages && requestAccountId.equals( t.getOwnerAccountId( ) ) ) {
          LOG.trace( "Considering image " + t.getFullName( ) + " because user wants to see their images and is owner." );
        } else if ( showMyAllowedImages && t.isAllowed( requestAccount ) ) {
          LOG.trace( "Considering image " + t.getFullName( ) + " because user wants to see their executable images and is allowed." );
        } else if ( showPublic && t.getImagePublic( ) ) {
          LOG.trace( "Considering image " + t.getFullName( ) + " because user wants to see public images and it is public." );
        } else if ( !t.isAllowed( requestAccount ) ) {
          LOG.trace( "Rejecting image " + t.getFullName( ) + " because user is not allowed." );
          return false;
        }
        if ( !imageList.isEmpty( ) && !imageList.contains( t.getDisplayName( ) ) ) {
          LOG.trace( "Rejecting image " + t.getFullName( ) + " because user provide an image id list which does not contain the result: " + imageList );
          return false;
        } else if ( !owners.isEmpty( ) && !owners.contains( t.getOwnerAccountId( ) ) ) {
          LOG.trace( "Rejecting image " + t.getFullName( ) + " because user provide an image id list which does not contain the result: " + owners );
          return false;
        } else if ( !executable.isEmpty( ) ) {
          for ( String accountId : executable ) {
            try {
              if ( t.isAllowed( Accounts.lookupAccountById( accountId ) ) ) {
                return true;
              }
            } catch ( AuthException ex ) {
              LOG.error( ex );
            }
          }
          LOG.trace( "Rejecting image " + t.getFullName( ) + " because user provide an image id list which does not contain the result: " + owners );
          return false;
        }
        return true;
      }
    } );
    List<ImageDetails> imageDetailsList = Lists.transform( images, Images.TO_IMAGE_DETAILS );
    reply.getImagesSet( ).addAll( imageDetailsList );
    return reply;
  }
  
  public RegisterImageResponseType register( RegisterImageType request ) throws EucalyptusCloudException {
    final Context ctx = Contexts.lookup( );
    String imageLocation = request.getImageLocation( );
    User requestUser = Contexts.lookup( ).getUser( );
    String[] imagePathParts;
    try {
      imagePathParts = ImageUtil.getImagePathParts( imageLocation );
      ImageUtil.checkBucketAcl( request, imagePathParts );
    } catch ( EucalyptusCloudException e ) {
      LOG.trace( e, e );
      throw e;
    }
    try {
      WalrusUtil.verifyManifestIntegrity( ctx.getUser( ), imageLocation );
    } catch ( EucalyptusCloudException e ) {
      throw new EucalyptusCloudException( "Image registration failed because the manifest referenced is invalid or unavailable." );
    }
    // FIXME: wrap this manifest junk in a helper class.
    Document inputSource = ImageUtil.getManifestDocument( imagePathParts, ctx.getUserFullName( ) );
    XPath xpath = XPathFactory.newInstance( ).newXPath( );
    Image.Architecture arch = ImageUtil.extractArchitecture( inputSource, xpath );
    ImageInfo imageInfo = null;
    String kernelId = ImageUtil.extractKernelId( inputSource, xpath );
    String ramdiskId = ImageUtil.extractRamdiskId( inputSource, xpath );
    Image.Type imageType = Image.Type.machine;
    Image.Platform platform = Image.Platform.linux;
    String newImageId = null;
    String signature = null;
    try {
      signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage( ) );
    }
    if ( "yes".equals( kernelId ) || "true".equals( kernelId ) || imagePathParts[1].startsWith( "vmlinuz" ) ) {
      if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
        throw new EucalyptusCloudException( "Only administrators can register kernel images." );
      }
      imageType = Image.Type.kernel;
      imageInfo = new KernelImageInfo( ctx.getUserFullName( ), ImageUtil.newImageId( imageType.getTypePrefix( ), imageLocation ), imageLocation,
                                       arch, Image.Platform.linux );
    } else if ( "yes".equals( ramdiskId ) || "true".equals( ramdiskId ) || imagePathParts[1].startsWith( "initrd" ) ) {
      if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
        throw new EucalyptusCloudException( "Only administrators can register ramdisk images." );
      }
      imageType = Image.Type.ramdisk;
      imageInfo = new RamdiskImageInfo( ctx.getUserFullName( ), ImageUtil.newImageId( imageType.getTypePrefix( ), imageLocation ), imageLocation,
                                        arch, Image.Platform.linux );
    } else {
      if ( imagePathParts[1].startsWith( Image.Platform.windows.toString( ) ) ) {
        imageType = Image.Type.machine;
        imageInfo = new MachineImageInfo( ctx.getUserFullName( ), ImageUtil.newImageId( imageType.getTypePrefix( ), imageLocation ), imageLocation,
                                          arch, Image.Platform.windows );
      } else {
        if ( kernelId != null ) {
          try {
            ImageUtil.getImageInfobyId( kernelId );
          } catch ( EucalyptusCloudException e ) {
            throw new EucalyptusCloudException( "Referenced kernel id is invalid: " + kernelId );
          }
        }
        if ( ramdiskId != null ) {
          try {
            ImageUtil.getImageInfobyId( ramdiskId );
          } catch ( EucalyptusCloudException e ) {
            throw new EucalyptusCloudException( "Referenced ramdisk id is invalid: " + ramdiskId );
          }
        }
        imageType = Image.Type.machine;
        imageInfo = new MachineImageInfo( ctx.getUserFullName( ), ImageUtil.newImageId( imageType.getTypePrefix( ), imageLocation ), imageLocation,
                                          arch, Image.Platform.linux, kernelId, ramdiskId );
      }
    }
    imageInfo.setSignature( signature );
    imageInfo.setState( Image.State.available );
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      db.add( imageInfo );
      db.commit( );
      LOG.info( "Registering image pk=" + imageInfo.getId( ) + " ownerId=" + ctx.getUserFullName( ) );
    } catch ( Exception e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "failed to register image." );
    }
    for( String p : extractProductCodes( inputSource, xpath ) ) {
      imageInfo.addProductCode( p );
    }
    imageInfo.grantPermission( ctx.getAccount( ) );
    
    LOG.info( "Triggering cache population in Walrus for: " + imageInfo.getId( ) );
    WalrusUtil.checkValid( imageInfo );
    WalrusUtil.triggerCaching( imageInfo );
    
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
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account requestAccount = ctx.getAccount( );
    try {
      ImageInfo imgInfo = EntityWrapper.get( ImageInfo.class ).lookupAndClose( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( requestUser.isAccountAdmin( ) && imgInfo.getOwnerAccountId( ).equals( ctx.getAccount( ).getId( ) ) ) {
        Images.deregisterImage( imgInfo.getDisplayName( ) );
      } else if ( ctx.hasAdministrativePrivileges( ) ) {
        Images.deregisterImage( imgInfo.getDisplayName( ) );
      } else {
        throw new EucalyptusCloudException( "Only the owner of a registered image or the administrator can deregister it." );
      }
      return reply;
    } catch ( NoSuchImageException ex ) {
      LOG.trace( ex );
      reply.set_return( false );
      return reply;
    } catch ( NoSuchElementException ex ) {
      LOG.trace( ex );
      reply.set_return( false );
      return reply;
    }
  }
  
  public ConfirmProductInstanceResponseType confirmProductInstance( ConfirmProductInstanceType request ) throws EucalyptusCloudException {
    ConfirmProductInstanceResponseType reply = ( ConfirmProductInstanceResponseType ) request.getReply( );
    reply.set_return( false );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance( ).lookup( request.getInstanceId( ) );
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
    
    if ( request.getAttribute( ) != null ) request.applyAttribute( );
    
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( !imgInfo.isAllowed( Contexts.lookup( ).getAccount( ) ) ) throw new EucalyptusCloudException( "image attribute: not authorized." );
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
//TODO:GRZE:RESTORE
//        for ( LaunchPermission auth : imgInfo.getPermissions( ) )
          reply.getLaunchPermission( ).add( LaunchPermissionItemType.getUser( Contexts.lookup( ).getAccount( ).getId( ) ) );
      } else if ( request.getProductCodes( ) != null ) {
        reply.setRealResponse( reply.getProductCodes( ) );
        for ( String p : imgInfo.listProductCodes( ) ) {
          reply.getProductCodes( ).add( p );
        }
      } else if ( request.getBlockDeviceMapping( ) != null ) {
        reply.setRealResponse( reply.getBlockDeviceMapping( ) );
        reply.getBlockDeviceMapping( ).add( ImageUtil.EMI );
        reply.getBlockDeviceMapping( ).add( ImageUtil.EPHEMERAL );
        reply.getBlockDeviceMapping( ).add( ImageUtil.SWAP );
        reply.getBlockDeviceMapping( ).add( ImageUtil.ROOT );
      } else {
        throw new EucalyptusCloudException( "invalid image attribute request." );
      }
    } catch ( EucalyptusCloudException e ) {
      db.commit( );
      throw e;
    }
    return reply;
  }
  
  public ModifyImageAttributeResponseType modifyImageAttribute( ModifyImageAttributeType request ) throws EucalyptusCloudException {
    ModifyImageAttributeResponseType reply = ( ModifyImageAttributeResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    
    if ( request.getAttribute( ) != null ) request.applyAttribute( );
    
    if ( request.getProductCodes( ).isEmpty( ) ) {
      reply.set_return( ImageUtil.modifyImageInfo( ctx.getUser( ), request.getImageId( ), request.getAdd( ), request.getRemove( ) ) );
    } else {
      EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
      ImageInfo imgInfo = null;
      try {
        imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
        for ( String productCode : request.getProductCodes( ) ) {
          imgInfo.addProductCode( productCode );
        }
        db.commit( );
        reply.set_return( true );
      } catch ( EucalyptusCloudException e ) {
        db.rollback( );
        reply.set_return( false );
      }
    }
    return reply;
  }
  
  public ResetImageAttributeResponseType resetImageAttribute( ResetImageAttributeType request ) throws EucalyptusCloudException {
    ResetImageAttributeResponseType reply = ( ResetImageAttributeResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    reply.set_return( true );
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      ImageInfo imgInfo = db.getUnique( Images.exampleWithImageId( request.getImageId( ) ) );
      if ( ctx.getUserFullName( ).getUniqueId( ).equals( imgInfo.getOwner( ).getUniqueId( ) ) || Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
        imgInfo.resetPermission( );
        db.commit( );
      } else {
        db.rollback( );
        reply.set_return( false );
      }
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      reply.set_return( false );
    }
    return reply;
  }
  
  public CreateImageResponseType createImage( CreateImageType request ) {
    CreateImageResponseType reply = request.getReply( );
    return reply;
  }
  
}
