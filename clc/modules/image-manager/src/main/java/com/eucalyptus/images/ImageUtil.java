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
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.images;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.NoSuchGroupException;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.GroupEntity;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.UserInfoStore;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.blockstorage.WalrusUtil;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.Image;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.VmImageInfo;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.LaunchPermissionItemType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;

public class ImageUtil {
  private static Logger LOG = Logger.getLogger( ImageUtil.class );
  
  public static String generateImageId( final String imagePrefix, final String imageLocation ) {
    Adler32 hash = new Adler32( );
    String key = imageLocation + System.currentTimeMillis( );
    hash.update( key.getBytes( ) );
    String imageId = String.format( "%s-%08X", imagePrefix, hash.getValue( ) );
    return imageId;
  }
  public static String newImageId( final String imagePrefix, final String imageLocation ) {
    EntityWrapper<Image> db = new EntityWrapper<Image>( );
    Image query = new ImageInfo( );
    query.setImageId( generateImageId( imagePrefix, imageLocation ) );
    LOG.info( "Trying to lookup using created AMI id=" + query.getImageId( ) );
    for ( ; db.query( query ).size( ) != 0; query.setImageId( generateImageId( imagePrefix, imageLocation ) ) );
    db.commit( );
    LOG.info( "Assigning imageId=" + query.getImageId( ) );
    return query.getImageId( );
  }
  public static boolean verifyManifestSignature( final String signature, final X509Certificate cert, String pad ) {
    boolean ret = false;
    try {
      Signature sigVerifier = Signature.getInstance( "SHA1withRSA" );
      if ( cert != null ) {
        PublicKey publicKey = cert.getPublicKey( );
        sigVerifier.initVerify( publicKey );
        sigVerifier.update( pad.getBytes( ) );
        ret = sigVerifier.verify( Hashes.hexToBytes( signature ) );
      }
    } catch ( Exception ex ) {
      LOG.warn( ex.getMessage( ) );
    }
 // TODO: RELEASE: restore
//    return ret;
    return true;
  }
  public static ArrayList<String> getAncestors( String userId, String manifestPath ) {
    ArrayList<String> ancestorIds = Lists.newArrayList( );
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      Document inputSource = WalrusUtil.getManifestData( Component.eucalyptus.name( ), imagePathParts[0], imagePathParts[1] );
      XPath xpath = XPathFactory.newInstance( ).newXPath( );
      NodeList ancestors = null;
      try {
        ancestors = ( NodeList ) xpath.evaluate( "/manifest/image/ancestry/ancestor_ami_id/text()", inputSource, XPathConstants.NODESET );
        if ( ancestors == null ) return ancestorIds;
        for ( int i = 0; i < ancestors.getLength( ); i++ ) {
          for ( String ancestorId : ancestors.item( i ).getNodeValue( ).split( "," ) ) {
            ancestorIds.add( ancestorId );
          }
        }
      } catch ( XPathExpressionException e ) {
        ImageManager.LOG.error( e, e );
      }
    } catch ( EucalyptusCloudException e ) {
      ImageManager.LOG.error( e, e );
    } catch ( DOMException e ) {
      ImageManager.LOG.error( e, e );
    }
    return ancestorIds;
  }
  public static Long getSize( String userId, String manifestPath ) {
    Long size = 0l;
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      Document inputSource = WalrusUtil.getManifestData( Component.eucalyptus.name( ), imagePathParts[0], imagePathParts[1] );
      XPath xpath = XPathFactory.newInstance( ).newXPath( );
      String rootSize = "0";
      try {
        rootSize = ( String ) xpath.evaluate( "/manifest/image/size/text()", inputSource, XPathConstants.STRING );
        try {
          size = Long.parseLong( rootSize );
        } catch ( NumberFormatException e ) {
          ImageManager.LOG.error( e, e );
        }
      } catch ( XPathExpressionException e ) {
        ImageManager.LOG.error( e, e );
      }
    } catch ( EucalyptusCloudException e ) {
      ImageManager.LOG.error( e, e );
    }
    return size;
  }
  public static void checkStoredImage( final ImageInfo imgInfo ) throws EucalyptusCloudException {
    if ( imgInfo != null ) try {
      Document inputSource = null;
      try {
        String[] imagePathParts = imgInfo.getImageLocation( ).split( "/" );
        inputSource = WalrusUtil.getManifestData( imgInfo.getImageOwnerId( ), imagePathParts[0], imagePathParts[1] );
      } catch ( EucalyptusCloudException e ) {
        throw e;
      }
      XPath xpath = null;
      xpath = XPathFactory.newInstance( ).newXPath( );
      String signature = null;
      try {
        signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
      } catch ( XPathExpressionException e ) {}
      if ( imgInfo.getSignature( ) != null && !imgInfo.getSignature( ).equals( signature ) ) throw new EucalyptusCloudException(
        "Manifest signature has changed since registration." );
      ImageManager.LOG.info( "Checking image: " + imgInfo.getImageLocation( ) );
      WalrusUtil.checkValid( imgInfo );
      ImageManager.LOG.info( "Triggering caching: " + imgInfo.getImageLocation( ) );
      try {
        WalrusUtil.triggerCaching( imgInfo );
      } catch ( Exception e ) {}
    } catch ( EucalyptusCloudException e ) {
      ImageManager.LOG.error( e );
      ImageManager.LOG.error( "Failed bukkit check! Invalidating registration: " + imgInfo.getImageLocation( ) );
      //TODO: we need to consider if this is a good semantic or not, it can have ugly side effects
      //        invalidateImageById( imgInfo.getImageId() );
      throw new EucalyptusCloudException( "Failed check! Invalidating registration: " + imgInfo.getImageLocation( ) );
    }
  }
  public static String getImageUrl( String walrusUrl, final Image diskInfo ) throws EucalyptusCloudException {
    try {
      URL url = new URL( ImageUtil.getWalrusUrl( ) + diskInfo.getImageLocation( ) );
      return url.toString( );
    } catch ( MalformedURLException e ) {
      throw new EucalyptusCloudException( "Failed to parse image location as URL.", e );
    }
  }
  public static String getWalrusUrl( ) throws EucalyptusCloudException {
    try {
      return SystemConfiguration.getWalrusUrl( ) + "/";
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Walrus has not been configured.", e );
    }
  }
  public static boolean isSet( String id ) {
    return id != null && !"".equals( id );
  }
  private static boolean userHasImagePermission( final UserInfo user, final ImageInfo img ) {
    try {
      if ( /*img.getUserGroups( ).isEmpty( ) && */!user.getUserName( ).equals( img.getImageOwnerId( ) )
           && !Users.lookupUser( user.getUserName( ) ).isAdministrator( ) && !img.getPermissions( ).contains( user ) ) return true;
    } catch ( NoSuchUserException e ) {
      return false;
    }
    return false;
  }
  private static void invalidateImageById( String searchId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    if ( isSet( searchId ) ) try {
      Image img = db.getUnique( new ImageInfo( searchId ) );
      WalrusUtil.invalidate( img );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId );
    }
  }
  public static VmImageInfo getVmImageInfo( final String walrusUrl, final Image diskInfo, final Image kernelInfo, final Image ramdiskInfo, final ArrayList<String> productCodes ) throws EucalyptusCloudException {
    String diskUrl = getImageUrl( walrusUrl, diskInfo );
    String kernelUrl = kernelInfo != null ? getImageUrl( walrusUrl, kernelInfo ) : null;
    String ramdiskUrl = ramdiskInfo != null ? getImageUrl( walrusUrl, ramdiskInfo ) : null;
    //:: create the response assets now since we might not have a ramdisk anyway :://
    VmImageInfo vmImgInfo = new VmImageInfo( diskInfo.getImageId( ), kernelInfo == null ? null : kernelInfo.getImageId( ),
      ramdiskInfo == null ? null : ramdiskInfo.getImageId( ), diskUrl, kernelUrl, ramdiskInfo == null ? null
                                                                                                     : ramdiskUrl,
      productCodes );
    return vmImgInfo;
  }
  public static Image getImageInfobyId( String searchId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    if ( isSet( searchId ) ) try {
      Image imgInfo = db.getUnique( new ImageInfo( searchId ) );
      db.commit( );
      return imgInfo;
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      db.commit( );
      throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId );
    } catch ( Throwable t ) {
      LOG.error( t, t );
      db.commit( );
    }
    LOG.error( "Failed to find registered image with id " + searchId );
    throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId );
  }
  public static String getImageInfobyId( String userSuppliedId, String imageDefaultId, String systemDefaultId ) {
    String searchId = null;
    if ( isSet( userSuppliedId ) )
      searchId = userSuppliedId;
    else if ( isSet( imageDefaultId ) )
      searchId = imageDefaultId;
    else if ( isSet( systemDefaultId ) ) searchId = systemDefaultId;
    return searchId;
  }
  
  public static BlockDeviceMappingItemType EMI       = new BlockDeviceMappingItemType( "emi", "sda1" );
  public static BlockDeviceMappingItemType EPHEMERAL = new BlockDeviceMappingItemType( "ephemeral0", "sda2" );
  public static BlockDeviceMappingItemType SWAP      = new BlockDeviceMappingItemType( "swap", "sda3" );
  public static BlockDeviceMappingItemType ROOT      = new BlockDeviceMappingItemType( "root", "/dev/sda1" );
  
  public static String extractArchitecture( Document inputSource, XPath xpath ) {
    String arch = null;
    try {
      arch = ( String ) xpath.evaluate( "/manifest/machine_configuration/architecture/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      ImageManager.LOG.warn( e.getMessage( ) );
    }
    return arch;
  }
  public static String extractRamdiskId( Document inputSource, XPath xpath ) {
    String ramdiskId = null;
    try {
      ramdiskId = ( String ) xpath.evaluate( "/manifest/machine_configuration/ramdisk_id/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      ImageManager.LOG.warn( e.getMessage( ) );
    }
    if ( !isSet( ramdiskId ) ) ramdiskId = null;
    return ramdiskId;
  }
  public static String extractKernelId( Document inputSource, XPath xpath ) {
    String kernelId = null;
    try {
      kernelId = ( String ) xpath.evaluate( "/manifest/machine_configuration/kernel_id/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      ImageManager.LOG.warn( e.getMessage( ) );
    }
    if ( !isSet( kernelId ) ) kernelId = null;
    return kernelId;
  }
  public static String[] getImagePathParts( String imageLocation ) throws EucalyptusCloudException {
    String[] imagePathParts = imageLocation.split( "/" );
    if ( imagePathParts.length != 2 ) throw new EucalyptusCloudException(
      "Image registration failed:  Invalid image location." );
    return imagePathParts;
  }
  public static void checkBucketAcl( RegisterImageType request, String[] imagePathParts ) throws EucalyptusCloudException {
    String userName = null;
    if ( !request.isAdministrator( ) ) {
      GetBucketAccessControlPolicyResponseType reply = WalrusUtil.getBucketAcl( request, imagePathParts );
      if ( reply != null ) {
        if ( !request.getUserId( ).equals( reply.getAccessControlPolicy( ).getOwner( ).getDisplayName( ) ) ) throw new EucalyptusCloudException(
          "Image registration failed: you must own the bucket containing the image." );
        userName = reply.getAccessControlPolicy( ).getOwner( ).getDisplayName( );
      }
    }
  }
  public static void applyImageAttributes( final EntityWrapper<ImageInfo> db, final ImageInfo imgInfo, final List<LaunchPermissionItemType> changeList, final boolean adding ) throws EucalyptusCloudException {
    for ( LaunchPermissionItemType perm : changeList ) {
      if ( perm.isGroup( ) ) {
        try {
          if( adding ) {
            imgInfo.grantPermission( Groups.lookupGroup( perm.getGroup( ) ) );
          } else {
            imgInfo.revokePermission( Groups.lookupGroup( perm.getGroup( ) ) );
          }
        } catch ( NoSuchGroupException e ) {
          LOG.debug( e, e );
          throw new EucalyptusCloudException( "Modify image attribute failed because of: " + e.getMessage( ) );
        }
      } else if ( perm.isUser( ) ) {
        try {
          if( adding ) {
            imgInfo.grantPermission( Users.lookupUser( perm.getUserId( ) ) );
          } else {
            imgInfo.revokePermission( Users.lookupUser( perm.getUserId( ) ) );
          }
        } catch ( NoSuchUserException e ) {
          LOG.debug( e, e );
          throw new EucalyptusCloudException( "Modify image attribute failed because of: " + e.getMessage( ) );
        }
      }
    }
  }
  public static boolean modifyImageInfo( final String imageId, final String userId, final boolean isAdmin, final List<LaunchPermissionItemType> addList, final List<LaunchPermissionItemType> remList ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( new ImageInfo( imageId ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      return false;
    }
    if ( !userId.equals( imgInfo.getImageOwnerId( ) ) && !isAdmin ) return false;
    try {
      applyImageAttributes( db, imgInfo, addList, true );
      applyImageAttributes( db, imgInfo, remList, false );
      db.commit( );
      return true;
    } catch ( EucalyptusCloudException e ) {
      ImageManager.LOG.warn( e );
      db.rollback( );
      return false;
    }
  }
  public static Document getManifestDocument( String[] imagePathParts, String userName ) throws EucalyptusCloudException {
    Document inputSource = null;
    try {
      inputSource = WalrusUtil.getManifestData( userName, imagePathParts[0], imagePathParts[1] );
    } catch ( EucalyptusCloudException e ) {
      throw e;
    }
    return inputSource;
  }
  public static List<ImageDetails> getImageOwnedByUser( List<ImageInfo> imgList, UserInfo user ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    List<ImageDetails> repList = Lists.newArrayList( );
    try {
      List<ImageInfo> results = db.query( new ImageInfo( ) );
      for ( ImageInfo img : results ) {
        ImageDetails imgDetails = img.getAsImageDetails( );
        if ( img.isAllowed( user ) && ( imgList.isEmpty( ) || imgList.contains( img ) ) ) {
          repList.add( imgDetails );
        }
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.commit( );
      ImageManager.LOG.debug( e, e );
    }
    return repList;
  }
  public static List<ImageDetails> getImagesByOwner( final List<ImageInfo> imgList, final UserInfo user, final ArrayList<String> owners ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    List<ImageDetails> repList = Lists.newArrayList( );
    if ( owners.remove( "self" ) ) owners.add( user.getUserName( ) );
    try {
      for ( String userName : owners ) {
        Iterable<ImageInfo> results = Iterables.filter( db.query( ImageInfo.byOwnerId( userName ) ), new Predicate<ImageInfo>( ) {
          @Override public boolean apply( ImageInfo arg0 ) {
            return ( imgList.isEmpty( ) || imgList.contains( arg0 ) )
                   && ( arg0.getImagePublic( ) || arg0.isAllowed( user ) );
          }
        } );
        repList.addAll( Lists.transform( Lists.newArrayList( results ), ImageInfo.TO_IMAGE_DETAILS ) );
      }
      db.commit( );
    } catch ( Throwable e ) {
      LOG.debug( e, e );
      db.rollback( );
    }
    return repList;
  }
  public static List<ImageDetails> getImagesByExec( UserInfo user, ArrayList<String> executable ) {
    List<ImageDetails> repList = Lists.newArrayList( );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      for ( String execUserId : executable ) {
        if ( "all".equals( execUserId ) ) continue;
        final UserInfo execUser = UserInfoStore.getUserInfo( new UserInfo( execUserId ) );
        Iterable<ImageInfo> results = Iterables.filter( db.query( ImageInfo.ALL ), new Predicate<ImageInfo>( ) {
          @Override public boolean apply( ImageInfo arg0 ) {
            return arg0.isAllowed( execUser ) || arg0.getImagePublic( );
          }
        } );
        repList.addAll( Lists.transform( Lists.newArrayList( results ), ImageInfo.TO_IMAGE_DETAILS ) );
      }
      db.commit( );
    } catch ( NoSuchUserException e ) {
      LOG.debug( e, e );
      db.commit( );
    }
    return repList;
  }
  public static void cleanDeregistered( ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      List<ImageInfo> imgList = db.query( ImageInfo.deregistered( ) );
      for ( ImageInfo deregImg : imgList ) {
        try {
          db.delete( deregImg );
        } catch ( Throwable e1 ) {}
      }
      db.commit( );
    } catch ( Throwable e1 ) {
      db.rollback( );
    }
  }
  public static VmImageInfo resolveImage( VmInfo vmInfo ) throws EucalyptusCloudException {
    String walrusUrl = getWalrusUrl( );
    ArrayList<String> productCodes = Lists.newArrayList( );
    ImageInfo diskInfo = null, kernelInfo = null, ramdiskInfo = null;
    String diskUrl = null, kernelUrl = null, ramdiskUrl = null;
  
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>( );
    try {
      diskInfo = db.getUnique( new ImageInfo( vmInfo.getImageId( ) ) );
      for ( ProductCode p : diskInfo.getProductCodes( ) ) {
        productCodes.add( p.getValue( ) );
      }
      diskUrl = getImageUrl( walrusUrl, diskInfo );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
    }
    VmImageInfo vmImgInfo = new VmImageInfo( vmInfo.getImageId( ), vmInfo.getKernelId( ), vmInfo.getRamdiskId( ), diskUrl, null, null, productCodes );
    if( Component.walrus.isLocal( ) ) {
      ArrayList<String> ancestorIds = getAncestors( vmInfo.getOwnerId( ), diskInfo.getImageLocation( ) );
      vmImgInfo.setAncestorIds( ancestorIds );
    } else {//FIXME: handle populating these in a defered way for the remote case.
      vmImgInfo.setAncestorIds( new ArrayList<String>() );
    }
    return vmImgInfo;
  }
}
