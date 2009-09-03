/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.images.WalrusUtil;
import com.eucalyptus.images.util.ImageUtil;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.ServiceDispatcher;

import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.entities.ProductCode;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.UserGroupInfo;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.ConfirmProductInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.ConfirmProductInstanceType;
import edu.ucsb.eucalyptus.msgs.DeregisterImageResponseType;
import edu.ucsb.eucalyptus.msgs.DeregisterImageType;
import edu.ucsb.eucalyptus.msgs.DescribeImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImageAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.LaunchPermissionItemType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeType;
import edu.ucsb.eucalyptus.msgs.RegisterImageResponseType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;
import edu.ucsb.eucalyptus.msgs.ResetImageAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetImageAttributeType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ImageManager {
  public static BlockDeviceMappingItemType EMI = new BlockDeviceMappingItemType("emi", "sda1");
  public static BlockDeviceMappingItemType EPHEMERAL = new BlockDeviceMappingItemType("ephemeral0", "sda2");
  public static BlockDeviceMappingItemType SWAP = new BlockDeviceMappingItemType("swap", "sda3");
  public static BlockDeviceMappingItemType ROOT = new BlockDeviceMappingItemType("root", "/dev/sda1");

  public static Logger LOG = Logger.getLogger( ImageManager.class );

  public VmImageInfo verify( VmInfo vmInfo ) throws EucalyptusCloudException {
    SystemConfiguration conf = EucalyptusProperties.getSystemConfiguration();
    String walrusUrl = getStorageUrl( conf );
    ArrayList<String> productCodes = Lists.newArrayList();
    ImageInfo diskInfo = null, kernelInfo = null, ramdiskInfo = null;
    String diskUrl = null, kernelUrl = null, ramdiskUrl = null;

    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    try {
      diskInfo = db.getUnique( ImageInfo.named( vmInfo.getImageId() ) );
      for( ProductCode p : diskInfo.getProductCodes() ) {
        productCodes.add( p.getValue());
      }
      diskUrl = this.getImageUrl( walrusUrl, diskInfo );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
    }

    ArrayList<String> ancestorIds = this.getAncestors( vmInfo.getOwnerId(), diskInfo.getImageLocation() );

    //:: create the response assets now since we might not have a ramdisk anyway :://
    VmImageInfo vmImgInfo = new VmImageInfo( vmInfo.getImageId(), vmInfo.getKernelId(), vmInfo.getRamdiskId(),
                                             diskUrl, null, null, productCodes );
    vmImgInfo.setAncestorIds( ancestorIds );
    return vmImgInfo;
  }

  private static Long getSize( String userId, String manifestPath ) {
    Long size = 0l;
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      Document inputSource = WalrusUtil.getManifestData( Component.eucalyptus.name(), imagePathParts[ 0 ], imagePathParts[ 1 ] );
      XPath xpath = XPathFactory.newInstance().newXPath();
      String rootSize = "0";
      try {
        rootSize = ( String ) xpath.evaluate( "/manifest/image/size/text()", inputSource, XPathConstants.STRING );
        try {
          size = Long.parseLong( rootSize );
        } catch ( NumberFormatException e ) {
          LOG.error( e, e );
        }
      } catch ( XPathExpressionException e ) {
        LOG.error( e, e );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
    }
    return size;
  }

  private static ArrayList<String> getAncestors( String userId, String manifestPath ) {
    ArrayList<String> ancestorIds = Lists.newArrayList();
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      Document inputSource = WalrusUtil.getManifestData( Component.eucalyptus.name(), imagePathParts[ 0 ], imagePathParts[ 1 ] );
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList ancestors = null;
      try {
        ancestors = ( NodeList ) xpath.evaluate( "/manifest/image/ancestry/ancestor_ami_id/text()", inputSource, XPathConstants.NODESET );
        for(int i = 0; i < ancestors.getLength(); i++ ) {
          for( String ancestorId : ancestors.item( i ).getNodeValue().split( "," ) ) {
            ancestorIds.add( ancestorId );
          }
        }
      } catch ( XPathExpressionException e ) {
        LOG.error( e, e );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
    } catch ( DOMException e ) {
      LOG.error( e, e );
    }
    return ancestorIds;
  }

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    SystemConfiguration conf = EucalyptusProperties.getSystemConfiguration();
    String walrusUrl = getStorageUrl( conf );

    RunInstancesType msg = vmAllocInfo.getRequest();
    ImageInfo searchDiskInfo = new ImageInfo( msg.getImageId() );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo diskInfo = null;
    ArrayList<String> productCodes = Lists.newArrayList();
    try {
      diskInfo = db.getUnique( searchDiskInfo );
      for( ProductCode p : diskInfo.getProductCodes() ) {
        productCodes.add( p.getValue());
      }
    } catch ( EucalyptusCloudException e ) {
      throw new EucalyptusCloudException( "Failed to find kernel image: " + msg.getImageId() );
    }
    UserInfo user = db.recast( UserInfo.class ).getUnique( new UserInfo( msg.getUserId() ) );
    if ( !diskInfo.isAllowed( user ) ) {
      db.rollback();
      throw new EucalyptusCloudException( "You do not have permissions to run this image." );
    }

    //:: now its time to determine the ramdisk and kernel info based on: 1) user input, 2) emi specific info, 3) system defaults ::/
    String kernelId = this.getImageInfobyId( msg.getKernelId(), diskInfo.getKernelId(), conf.getDefaultKernel() );
    if ( kernelId == null ) {
      db.commit();
      throw new EucalyptusCloudException( "Unable to determine required kernel image." );
    }

    String ramdiskId = this.getImageInfobyId( msg.getRamdiskId(), diskInfo.getRamdiskId(), conf.getDefaultRamdisk() );

    ImageInfo kernelInfo = null;
    try {
      kernelInfo = db.getUnique( new ImageInfo( kernelId ) );
    } catch ( EucalyptusCloudException e ) {
      db.commit();
      throw new EucalyptusCloudException( "Failed to find kernel image: " + kernelId );
    }
    if ( !diskInfo.isAllowed( user ) ) {
      db.commit();
      throw new EucalyptusCloudException( "You do not have permission to launch: " + diskInfo.getImageId() );
    }
    if ( !kernelInfo.isAllowed( user ) ) {
      db.commit();
      throw new EucalyptusCloudException( "You do not have permission to launch: " + kernelInfo.getImageId() );
    }
    ImageInfo ramdiskInfo = null;
    if ( ramdiskId != null ) {
      try {
        ramdiskInfo = db.getUnique( new ImageInfo( ramdiskId ) );
      } catch ( EucalyptusCloudException e ) {
        throw new EucalyptusCloudException( "Failed to find ramdisk image: " + ramdiskId );
      }
      if ( !ramdiskInfo.isAllowed( user ) ) {
        db.commit();
        throw new EucalyptusCloudException( "You do not have permission to launch: " + ramdiskInfo.getImageId() );
      }
    }

    //:: quietly add the ancestor and size information to the vm info object... this should never fail noisily :://
    ArrayList<String> ancestorIds = this.getAncestors( msg.getUserId(), diskInfo.getImageLocation() );
    Long imgSize = this.getSize( msg.getUserId(), diskInfo.getImageLocation() );
    this.checkStoredImage( kernelInfo );
    this.checkStoredImage( diskInfo );
    this.checkStoredImage( ramdiskInfo );

    //:: get together the required URLs ::/
    VmImageInfo vmImgInfo = getVmImageInfo( walrusUrl, diskInfo, kernelInfo, ramdiskInfo, productCodes );
    vmImgInfo.setAncestorIds( ancestorIds );
    vmImgInfo.setSize( imgSize );
    vmAllocInfo.setImageInfo( vmImgInfo );
    return vmAllocInfo;
  }

  private void checkStoredImage( final ImageInfo imgInfo ) throws EucalyptusCloudException {
    if ( imgInfo != null )
      try {
        Document inputSource = null;
        try {
          String[] imagePathParts = imgInfo.getImageLocation().split( "/" );
          inputSource = WalrusUtil.getManifestData( imgInfo.getImageOwnerId(), imagePathParts[ 0 ], imagePathParts[ 1 ] );
        } catch ( EucalyptusCloudException e ) {
          throw e;
        }
        XPath xpath = null;
        xpath = XPathFactory.newInstance().newXPath();
        String signature = null;
        try {
          signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
        } catch ( XPathExpressionException e ) {}
        if ( !imgInfo.getSignature().equals( signature ) )
          throw new EucalyptusCloudException( "Manifest signature has changed since registration." );
        LOG.info( "Checking image: " + imgInfo.getImageLocation() );
        WalrusUtil.checkValid(imgInfo);
        LOG.info( "Triggering caching: " + imgInfo.getImageLocation() );
        try {
          WalrusUtil.triggerCaching(imgInfo);
        } catch ( Exception e ) {}
      } catch ( EucalyptusCloudException e ) {
        LOG.error( e );
        LOG.error( "Failed bukkit check! Invalidating registration: " + imgInfo.getImageLocation() );
        this.invalidateImageById( imgInfo.getImageId() );
        throw new EucalyptusCloudException( "Failed check! Invalidating registration: " + imgInfo.getImageLocation() );
      }
  }

  private VmImageInfo getVmImageInfo( final String walrusUrl, final ImageInfo diskInfo, final ImageInfo kernelInfo, final ImageInfo ramdiskInfo, final ArrayList<String> productCodes ) throws EucalyptusCloudException {
    String diskUrl = this.getImageUrl( walrusUrl, diskInfo );
    String kernelUrl = this.getImageUrl( walrusUrl, kernelInfo );
    String ramdiskUrl = null;
    if ( ramdiskInfo != null )
      ramdiskUrl = this.getImageUrl( walrusUrl, ramdiskInfo );

    //:: create the response assets now since we might not have a ramdisk anyway :://
    VmImageInfo vmImgInfo = new VmImageInfo( diskInfo.getImageId(), kernelInfo.getImageId(), ramdiskInfo == null ? null : ramdiskInfo.getImageId(),
                                             diskUrl, kernelUrl, ramdiskInfo == null ? null : ramdiskUrl, productCodes );
    return vmImgInfo;
  }

  private String getImageUrl( String walrusUrl, final ImageInfo diskInfo ) throws EucalyptusCloudException {
    String diskUrl;
    try {//TODO: clean up getting the walrus URL
      if( !Component.walrus.isLocal( ) ) {
        walrusUrl = "http://" + Component.walrus.getUri( ).getHost( ) + ":8773/services/Walrus/";
      }
      diskUrl = ( new URL( walrusUrl + diskInfo.getImageLocation() ) ).toString();
    }
    catch ( MalformedURLException e ) {
      throw new EucalyptusCloudException( "Failed to parse image location as URL.", e );
    }
    return diskUrl;
  }

  private String getStorageUrl( final SystemConfiguration conf ) throws EucalyptusCloudException {
    String walrusUrl;
    try {
      walrusUrl = ( new URL( conf.getStorageUrl() + "/" ) ).toString();
    }
    catch ( MalformedURLException e ) {
      throw new EucalyptusCloudException( "System is misconfigured: cannot parse Walrus URL.", e );
    }
    return walrusUrl;
  }

  private String getImageInfobyId( String userSuppliedId, String imageDefaultId, String systemDefaultId ) {
    String searchId = null;
    if ( this.isSet( userSuppliedId ) )
      searchId = userSuppliedId;
    else if ( this.isSet( imageDefaultId ) )
      searchId = imageDefaultId;
    else if ( this.isSet( systemDefaultId ) )
      searchId = systemDefaultId;
    return searchId;
  }

  private ImageInfo getImageInfobyId( String searchId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    if ( isSet( searchId ) ) try {
      return db.getUnique( new ImageInfo( searchId ) );
    } catch ( EucalyptusCloudException e ) {
      throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId );
    } finally { db.commit(); }
    throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId );
  }

  private void invalidateImageById( String searchId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    if ( isSet( searchId ) ) try {
      ImageInfo img = db.getUnique( new ImageInfo( searchId ) );
      WalrusUtil.invalidate(img);
      db.commit();
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId );
    }
  }

  public DescribeImagesResponseType DescribeImages( DescribeImagesType request ) throws EucalyptusCloudException {
    DescribeImagesResponseType reply = ( DescribeImagesResponseType ) request.getReply();

    ArrayList<String> remList = Lists.newArrayList();
    //:: remove all deregistered images ::/
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    List<ImageInfo> imgList = db.query( ImageInfo.deregistered() );
    for ( ImageInfo deregImg : imgList )
      db.delete( deregImg );
    db.commit();
/*
If you specify one or more AMI IDs, only AMIs that have the specified IDs are returned.
If you specify an invalid AMI ID, a fault is returned.
If you specify an AMI ID for which you do not have access, it will not be included in the returned results.
*/

/*
If you specify one or more AMI owners, only AMIs from the specified owners and for which you have access are returned. The results can include the account IDs of the specified owners, amazon for AMIs owned by Amazon or self for AMIs that you own.

If you specify a list of executable users, only users that have launch permissions for the AMIs are returned. You can specify account IDs (if you own the AMI(s)), self for AMIs for which you own or have explicit permissions, or all for public AMIs.
 */
    db = new EntityWrapper<ImageInfo>();
    String userId = request.getUserId();
    Boolean isAdmin = request.isAdministrator();
    UserInfo user = null;
    try {
      user = db.recast( UserInfo.class ).getUnique( new UserInfo( userId ) );
    } catch ( EucalyptusCloudException e ) {
      db.commit();
      throw new EucalyptusCloudException( "Failed to find user information for: " + userId );
    }

    ArrayList<String> imageList = request.getImagesSet();
    if ( imageList == null ) imageList = Lists.newArrayList();
    ArrayList<String> owners = request.getOwnersSet();
    if ( owners == null ) owners = Lists.newArrayList();
    ArrayList<String> executable = request.getExecutableBySet();
    if ( executable == null ) executable = Lists.newArrayList();

    List<ImageDetails> repList = reply.getImagesSet();
    //:: handle easy case first ::/
    if ( owners.isEmpty() && executable.isEmpty() ) {
      executable.add( "self" );
    }

    if ( !owners.isEmpty() ) {
      if ( owners.remove( "self" ) ) owners.add( user.getUserName() );
      for ( String userName : owners ) {
        List<ImageInfo> results = db.query( ImageInfo.byOwnerId( userName ) );
        for ( ImageInfo img : results ) {
          ImageDetails imgDetails = img.getAsImageDetails();
          if ( img.isAllowed( user ) && !repList.contains( imgDetails ) && ( imgList.isEmpty() || imgList.contains( imgDetails.getImageId() ) ) )
            repList.add( imgDetails );
        }
      }
    }
    if ( !executable.isEmpty() ) {
      if ( executable.remove( "self" ) ) {
        List<ImageInfo> results = db.query( new ImageInfo() );
        for ( ImageInfo img : results ) {
          ImageDetails imgDetails = img.getAsImageDetails();
          if ( img.isAllowed( user ) && !repList.contains( imgDetails ) && ( imgList.isEmpty() || imgList.contains( imgDetails.getImageId() ) ) )
            repList.add( imgDetails );
        }
      }
      for ( String execUserId : executable ) {
        try {
          UserInfo execUser = db.recast( UserInfo.class ).getUnique( new UserInfo( execUserId ) );
          List<ImageInfo> results = db.query( new ImageInfo() );
          for ( ImageInfo img : results ) {
            ImageDetails imgDetails = img.getAsImageDetails();
            if ( img.isAllowed( execUser ) && img.getImageOwnerId().equals( user.getUserName() ) && !repList.contains( imgDetails ) )
              repList.add( imgDetails );
          }
        } catch ( Exception e ) {}
      }
    }
    db.commit();

    if( !imageList.isEmpty() ) {
      ArrayList<ImageDetails> newList = Lists.newArrayList();
      for( ImageDetails img : repList ) {
        if( imageList.contains( img.getImageId() ) ) {
          newList.add( img );
        }
      }
      reply.setImagesSet( newList );
    }

    return reply;
  }

  private boolean userHasImagePermission( final UserInfo user, final ImageInfo img ) {
    if ( img.getUserGroups().isEmpty()
         && !user.getUserName().equals( img.getImageOwnerId() )
         && !user.isAdministrator()
         && !img.getPermissions().contains( user ) )
      return true;
    return false;
  }

  public RegisterImageResponseType RegisterImage( RegisterImageType request ) throws EucalyptusCloudException {
    String imageLocation = request.getImageLocation();
    String[] imagePathParts = imageLocation.split( "/" );
    if ( imagePathParts.length != 2 )
      throw new EucalyptusCloudException( "Image registration failed:  Invalid image location." );

    //:: check the bucket ownership & get the user name :://
    String userName = null;
    if ( !request.isAdministrator() ) {
      GetBucketAccessControlPolicyResponseType reply = WalrusUtil.getBucketAcl( request, imagePathParts );

      if ( !request.getUserId().equals( reply.getAccessControlPolicy().getOwner().getDisplayName() ) )
        throw new EucalyptusCloudException( "Image registration failed: you must own the bucket containing the image." );
      userName = reply.getAccessControlPolicy().getOwner().getDisplayName();
    }
    //:: prepare the imageInfo object :://
    ImageInfo imageInfo = new ImageInfo( imageLocation, request.getUserId(), "available", true );
    //:: verify the image manifest :://
    try {
      WalrusUtil.verifyManifestIntegrity( imageInfo );
    } catch ( EucalyptusCloudException e ) {
      throw new EucalyptusCloudException( "Image registration failed because the manifest referenced is invalid or unavailable." );
    }
    //:: this parses the manifest for extra information :://
    Document inputSource = null;
    try {
      inputSource = WalrusUtil.getManifestData( request.getUserId(), imagePathParts[ 0 ], imagePathParts[ 1 ] );
    } catch ( EucalyptusCloudException e ) {
      throw e;
    }
    XPath xpath = XPathFactory.newInstance().newXPath();

    String arch = null;
    try {
      arch = ( String ) xpath.evaluate( "/manifest/machine_configuration/architecture/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage() );
    }
    finally {
      imageInfo.setArchitecture( ( arch == null ) ? "i386" : arch );
    }

    String kernelId = null;
    try {
      kernelId = ( String ) xpath.evaluate( "/manifest/machine_configuration/kernel_id/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage() );
    }
    if ( !isSet( kernelId ) ) kernelId = null;

    String ramdiskId = null;
    try {
      ramdiskId = ( String ) xpath.evaluate( "/manifest/machine_configuration/ramdisk_id/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage() );
    }
    if ( !isSet( ramdiskId ) ) ramdiskId = null;

    NodeList productCodes = null;
    try {
      productCodes = ( NodeList ) xpath.evaluate( "/manifest/machine_configuration/product_codes/product_code/text()", inputSource, XPathConstants.NODESET );
      for(int i = 0; i < productCodes.getLength(); i++ ) {
        for( String productCode : productCodes.item( i ).getNodeValue().split( "," ) ) {
          imageInfo.getProductCodes().add( new ProductCode( productCode ) );
        }
      }
    } catch ( XPathExpressionException e ) {
      LOG.error( e, e );
    }


    if ( "yes".equals( kernelId ) || "true".equals( kernelId ) || imagePathParts[ 1 ].startsWith( "vmlinuz" ) ) {
      if ( !request.isAdministrator() ) throw new EucalyptusCloudException( "Only administrators can register kernel images." );
      imageInfo.setImageType( EucalyptusProperties.IMAGE_KERNEL );
      imageInfo.setImageId( ImageUtil.newImageId( EucalyptusProperties.IMAGE_KERNEL_PREFIX, imageInfo.getImageLocation() ) );
    } else if ( "yes".equals( ramdiskId ) || "true".equals( ramdiskId ) || imagePathParts[ 1 ].startsWith( "initrd" ) ) {
      if ( !request.isAdministrator() ) throw new EucalyptusCloudException( "Only administrators can register ramdisk images." );
      imageInfo.setImageType( EucalyptusProperties.IMAGE_RAMDISK );
      imageInfo.setImageId( ImageUtil.newImageId( EucalyptusProperties.IMAGE_RAMDISK_PREFIX, imageInfo.getImageLocation() ) );
    } else {
      if ( kernelId != null ) {
        try {
          this.getImageInfobyId( kernelId );
        } catch ( EucalyptusCloudException e ) {
          throw new EucalyptusCloudException( "Referenced kernel id is invalid: " + kernelId );
        }
      }
      if ( ramdiskId != null ) {
        try {
          this.getImageInfobyId( ramdiskId );
        } catch ( EucalyptusCloudException e ) {
          throw new EucalyptusCloudException( "Referenced ramdisk id is invalid: " + ramdiskId );
        }
      }
      imageInfo.setImageType( EucalyptusProperties.IMAGE_MACHINE );
      imageInfo.setKernelId( kernelId );
      imageInfo.setRamdiskId( ramdiskId );
      imageInfo.setImageId( ImageUtil.newImageId( EucalyptusProperties.IMAGE_MACHINE_PREFIX, imageInfo.getImageLocation() ) );
    }

    String signature = null;
    try {
      signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage() );
    }
    imageInfo.setSignature( signature );


//TODO: update this to use the new user.
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    try {
      db.add( imageInfo );
      UserInfo user = db.recast( UserInfo.class ).getUnique( new UserInfo( request.getUserId() ) );
      UserGroupInfo group = db.recast( UserGroupInfo.class ).getUnique( new UserGroupInfo( "all" ) );
      imageInfo.getPermissions().add( user );
      imageInfo.getUserGroups().add( group );
      db.commit();
      LOG.info( "Registering image pk=" + imageInfo.getId() + " ownerId=" + user.getUserName() );
    } catch ( EucalyptusCloudException e ) {
      db.rollback();
      throw e;
    }

    LOG.info( "Triggering cache population in Walrus for: " + imageInfo.getId() );
    WalrusUtil.checkValid(imageInfo);
    WalrusUtil.triggerCaching(imageInfo);

    RegisterImageResponseType reply = ( RegisterImageResponseType ) request.getReply();
    reply.setImageId( imageInfo.getImageId() );
    return reply;
  }

  public DeregisterImageResponseType DeregisterImage( DeregisterImageType request ) throws EucalyptusCloudException {
    DeregisterImageResponseType reply = ( DeregisterImageResponseType ) request.getReply();
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();

    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( new ImageInfo( request.getImageId() ) );
      if ( !imgInfo.getImageOwnerId().equals( request.getUserId() ) && !request.isAdministrator() )
        throw new EucalyptusCloudException( "Only the owner of a registered image or the administrator can deregister it." );
      WalrusUtil.invalidate(imgInfo);
      db.commit();
      reply.set_return( true );
    } catch ( EucalyptusCloudException e ) {
      reply.set_return( false );
      db.rollback();
    }
    return reply;
  }

  public ConfirmProductInstanceResponseType ConfirmProductInstance( ConfirmProductInstanceType request ) throws EucalyptusCloudException {
    ConfirmProductInstanceResponseType reply = ( ConfirmProductInstanceResponseType ) request.getReply();
    reply.set_return( false );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( request.getInstanceId() );
      EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
      try {
        ImageInfo found = db.getUnique( new ImageInfo( vm.getImageInfo().getImageId() ) );
        if ( found.getProductCodes().contains( new ProductCode( request.getProductCode() ) ) ) {
          reply.set_return( true );
          reply.setOwnerId( found.getImageOwnerId() );
        }
      } catch ( EucalyptusCloudException e ) {
      } finally {
        db.commit();
      }
    } catch ( NoSuchElementException e ) {}
    return reply;
  }

  public DescribeImageAttributeResponseType DescribeImageAttribute( DescribeImageAttributeType request ) throws EucalyptusCloudException {
    DescribeImageAttributeResponseType reply = ( DescribeImageAttributeResponseType ) request.getReply();
    reply.setImageId( request.getImageId() );

    if ( request.getAttribute() != null )
      request.applyAttribute();

    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    try {
      ImageInfo imgInfo = db.getUnique( new ImageInfo( request.getImageId() ) );
      if ( !imgInfo.isAllowed( db.recast( UserInfo.class ).getUnique( new UserInfo( request.getUserId() ) ) ) )
        throw new EucalyptusCloudException( "image attribute: not authorized." );
      if ( request.getKernel() != null ) {
        reply.setRealResponse( reply.getKernel() );
        if( imgInfo.getKernelId() != null ) {
          reply.getKernel().add(imgInfo.getKernelId() );
        }
      } else if ( request.getRamdisk() != null ) {
        reply.setRealResponse( reply.getRamdisk() );
        if( imgInfo.getRamdiskId() != null ) {
          reply.getRamdisk().add( imgInfo.getRamdiskId() );
        }
      } else if ( request.getLaunchPermission() != null ) {
        reply.setRealResponse( reply.getLaunchPermission() );
        for ( UserGroupInfo userGroup : imgInfo.getUserGroups() )
          reply.getLaunchPermission().add( LaunchPermissionItemType.getGroup( userGroup.getName() ) );
        for ( UserInfo user : imgInfo.getPermissions() )
          reply.getLaunchPermission().add( LaunchPermissionItemType.getUser( user.getUserName() ) );
      } else if ( request.getProductCodes() != null ) {
        reply.setRealResponse( reply.getProductCodes() );
        for ( ProductCode p : imgInfo.getProductCodes() ) {
          reply.getProductCodes().add( p.getValue() );
        }
      } else if ( request.getBlockDeviceMapping() != null ) {
        reply.setRealResponse( reply.getBlockDeviceMapping() );
        reply.getBlockDeviceMapping().add( EMI );
        reply.getBlockDeviceMapping().add( EPHEMERAL );
        reply.getBlockDeviceMapping().add( SWAP );
        reply.getBlockDeviceMapping().add( ROOT );
      } else {
        throw new EucalyptusCloudException( "invalid image attribute request." );
      }
    } catch ( EucalyptusCloudException e ) {
      db.commit();
      throw e;
    }
    return reply;
  }

  public ModifyImageAttributeResponseType ModifyImageAttribute( ModifyImageAttributeType request ) throws EucalyptusCloudException {
    ModifyImageAttributeResponseType reply = ( ModifyImageAttributeResponseType ) request.getReply();

    if ( request.getAttribute() != null )
      request.applyAttribute();

    if ( request.getProductCodes().isEmpty() ) {
      reply.set_return( this.modifyImageInfo( request.getImageId(), request.getUserId(), request.isAdministrator(), request.getAdd(), request.getRemove() ) );
    } else {
      EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
      ImageInfo imgInfo = null;
      try {
        imgInfo = db.getUnique( new ImageInfo( request.getImageId() ) );
        for ( String productCode : request.getProductCodes() ) {
          ProductCode prodCode = new ProductCode( productCode );
          if( !imgInfo.getProductCodes().contains( prodCode  ) ) {
            imgInfo.getProductCodes().add( prodCode );
          }
        }
        db.commit();
        reply.set_return( true );
      }
      catch ( EucalyptusCloudException e ) {
        db.rollback();
        reply.set_return( false );
      }
    }
    return reply;
  }

  private boolean modifyImageInfo( final String imageId, final String userId, final boolean isAdmin, final List<LaunchPermissionItemType> addList, final List<LaunchPermissionItemType> remList ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( new ImageInfo( imageId ) );
    }
    catch ( EucalyptusCloudException e ) {
      db.rollback();
      return false;
    }

    if ( !userId.equals( imgInfo.getImageOwnerId() ) && !isAdmin ) return false;

    try {
      this.applyImageAttributes( db, imgInfo, addList, true );
      this.applyImageAttributes( db, imgInfo, remList, false );
      db.commit();
      return true;
    } catch ( EucalyptusCloudException e ) {
      LOG.warn( e );
      db.rollback();
      return false;
    }
  }

  private void applyImageAttributes( final EntityWrapper<ImageInfo> db, final ImageInfo imgInfo, final List<LaunchPermissionItemType> changeList, final boolean adding ) throws EucalyptusCloudException {
    for ( LaunchPermissionItemType perm : changeList ) {
      if ( perm.isGroup() ) {
        UserGroupInfo target = new UserGroupInfo( perm.getGroup() );

        if ( adding && !imgInfo.getUserGroups().contains( target ) ) {
          EntityWrapper<UserGroupInfo> dbGroup = db.recast( UserGroupInfo.class );
          try {
            target = dbGroup.getUnique( target );
          } catch ( EucalyptusCloudException e ) {
          } finally {
            imgInfo.getUserGroups().add( target );
          }
        } else if ( !adding && imgInfo.getUserGroups().contains( target ) ) {
          imgInfo.getUserGroups().remove( target );
        } else if ( !adding ) {
          throw new EucalyptusCloudException( "image attribute: cant remove nonexistant permission." );
        }
      } else if ( perm.isUser() ) {
        UserInfo target = new UserInfo( perm.getUserId() );
        if ( adding && !imgInfo.getPermissions().contains( target ) ) {
          EntityWrapper<UserInfo> dbUser = db.recast( UserInfo.class );
          try {
            target = dbUser.getUnique( target );
          } catch ( EucalyptusCloudException e ) {
            throw new EucalyptusCloudException( "image attribute: invalid user id." );
          } finally {
            imgInfo.getPermissions().add( target );
          }
        } else if ( !adding && imgInfo.getPermissions().contains( target ) ) {
          imgInfo.getPermissions().remove( target );
        } else if ( !adding ) {
          throw new EucalyptusCloudException( "image attribute: cant remove nonexistant permission." );
        }
      }
    }
  }

  public ResetImageAttributeResponseType ResetImageAttribute( ResetImageAttributeType request ) throws EucalyptusCloudException {
    ResetImageAttributeResponseType reply = ( ResetImageAttributeResponseType ) request.getReply();
    reply.set_return( true );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    try {
      ImageInfo imgInfo = db.getUnique( new ImageInfo( request.getImageId() ) );

      if ( !request.getUserId().equals( imgInfo.getImageOwnerId() ) && !request.isAdministrator() )
        throw new EucalyptusCloudException( "Not allowed to modify image: " + imgInfo.getImageId() );
      imgInfo.getPermissions().clear();
      imgInfo.getUserGroups().clear();
      imgInfo.getUserGroups().add( db.recast( UserGroupInfo.class ).getUnique( UserGroupInfo.named( "all" ) ) );
      db.commit();
    }
    catch ( EucalyptusCloudException e ) {
      db.rollback();
      reply.set_return( false );
    }
    return reply;
  }

  public boolean isSet( String id ) {
    return id != null && !"".equals( id );
  }
}
