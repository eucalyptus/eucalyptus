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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.images;

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
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.blockstorage.WalrusUtil;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.Architecture;
import com.eucalyptus.cloud.ImageMetadata.StaticDiskImage;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.LaunchPermissionItemType;
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;

public class ImageUtil {
  private static Logger LOG = Logger.getLogger( ImageUtil.class );
  
  public static String newImageId( final String imagePrefix, final String imageLocation ) {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    String testId = Crypto.generateId( imageLocation, imagePrefix );
    ImageInfo query = Images.exampleWithImageId( testId );
    LOG.info( "Trying to lookup using created AMI id=" + query.getDisplayName( ) );
    for ( ; db.query( query ).size( ) != 0; query.setDisplayName( Crypto.generateId( imageLocation, imagePrefix ) ) );
    db.commit( );
    LOG.info( "Assigning imageId=" + query.getDisplayName( ) );
    return query.getDisplayName( );
  }
  
  public static boolean verifyManifestSignature( final X509Certificate cert, final String signature, String pad ) {
    Signature sigVerifier;
    try {
      sigVerifier = Signature.getInstance( "SHA1withRSA" );
      PublicKey publicKey = cert.getPublicKey( );
      sigVerifier.initVerify( publicKey );
      sigVerifier.update( ( pad ).getBytes( ) );
      return sigVerifier.verify( Hashes.hexToBytes( signature ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      return false;
    }
  }
  
  public static ArrayList<String> getAncestors( String userId, String manifestPath ) {
    ArrayList<String> ancestorIds = Lists.newArrayList( );
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      Document inputSource = WalrusUtil.getManifestData( ComponentIds.lookup( Eucalyptus.class ).getFullName( ), imagePathParts[0], imagePathParts[1] );
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
        LOG.error( e, e );
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
    } catch ( DOMException e ) {
      LOG.error( e, e );
    }
    return ancestorIds;
  }
  
  public static Long getSize( String manifestPath ) {
    Long size = 0l;
    try {
      String[] imagePathParts = manifestPath.split( "/" );
      Document inputSource = WalrusUtil.getManifestData( ComponentIds.lookup( Eucalyptus.class ).getFullName( ), imagePathParts[0], imagePathParts[1] );
      XPath xpath = XPathFactory.newInstance( ).newXPath( );
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
  
  public static void checkStoredImage( final ImageMetadata.StaticDiskImage imgInfo ) throws EucalyptusCloudException {
    if ( imgInfo != null ) try {
      Document inputSource = null;
      try {
        String[] imagePathParts = imgInfo.getManifestLocation( ).split( "/" );
        inputSource = WalrusUtil.getManifestData( imgInfo.getOwner( ), imagePathParts[0], imagePathParts[1] );
      } catch ( EucalyptusCloudException e ) {
        throw e;
      }
      XPath xpath = null;
      xpath = XPathFactory.newInstance( ).newXPath( );
      String signature = null;
      try {
        signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
      } catch ( XPathExpressionException e ) {}
      if ( imgInfo.getSignature( ) != null && !imgInfo.getSignature( ).equals( signature ) ) throw new EucalyptusCloudException( "Manifest signature has changed since registration." );
      LOG.info( "Triggering caching: " + imgInfo.getManifestLocation( ) );
      try {
        if( imgInfo instanceof ImageMetadata.StaticDiskImage ) {
          WalrusUtil.triggerCaching( ( StaticDiskImage ) imgInfo );
        }
      } catch ( Exception e ) {}
      } catch ( EucalyptusCloudException e ) {
      LOG.error( e );
      LOG.error( "Failed bukkit check! Invalidating registration: " + imgInfo.getManifestLocation( ) );
      //TODO: we need to consider if this is a good semantic or not, it can have ugly side effects
      //        invalidateImageById( imgInfo.getImageId() );
      throw new EucalyptusCloudException( "Failed check! Invalidating registration: " + imgInfo.getManifestLocation( ) );
      }
  }
  
  public static boolean isSet( String id ) {
    return id != null && !"".equals( id );
  }
  
  /*
  private static boolean userHasImagePermission( final UserInfo user, final ImageInfo img ) {
    try {
      if ( !user.getUserName( ).equals( img.getImageOwnerId( ) )
           && !Users.lookupUser( user.getUserName( ) ).isAdministrator( ) && !img.getPermissions( ).contains( user ) ) return true;
    } catch ( NoSuchUserException e ) {
      return false;
    }
    return false;
  }
  */
//  private static void invalidateImageById( String searchId ) throws EucalyptusCloudException {
//    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
//    if ( isSet( searchId ) ) try {
//      ImageInfo img = db.getUnique( Images.exampleWithImageId( searchId ) );
//      WalrusUtil.invalidate( img );
//      db.commit( );
//      } catch ( EucalyptusCloudException e ) {
//      db.rollback( );
//      throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId, e );
//      }
//  }
  
  public static ImageInfo getImageInfobyId( String searchId ) throws EucalyptusCloudException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    if ( isSet( searchId ) ) try {
      ImageInfo imgInfo = db.getUnique( Images.exampleWithImageId( searchId ) );
      db.commit( );
      return imgInfo;
      } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      db.commit( );
      throw new EucalyptusCloudException( "Failed to find registered image with id " + searchId, e );
      } catch ( Exception t ) {
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
  
  public static Architecture extractArchitecture( Document inputSource, XPath xpath ) {
    String arch = null;
    try {
      arch = ( String ) xpath.evaluate( "/manifest/machine_configuration/architecture/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage( ) );
    }
    String architecture = ( ( arch == null )
        ? "i386"
        : arch );
    return ImageMetadata.Architecture.valueOf( architecture );
  }
  
  public static String extractRamdiskId( Document inputSource, XPath xpath ) {
    String ramdiskId = null;
    try {
      ramdiskId = ( String ) xpath.evaluate( "/manifest/machine_configuration/ramdisk_id/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage( ) );
    }
    if ( !isSet( ramdiskId ) ) ramdiskId = null;
    return ramdiskId;
  }
  
  public static String extractKernelId( Document inputSource, XPath xpath ) {
    String kernelId = null;
    try {
      kernelId = ( String ) xpath.evaluate( "/manifest/machine_configuration/kernel_id/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage( ) );
    }
    if ( !isSet( kernelId ) ) kernelId = null;
    return kernelId;
  }
  
  public static void checkBucketAcl( RegisterImageType request, String[] imagePathParts ) throws EucalyptusCloudException {
    String userName = null;
    Context ctx = Contexts.lookup( );
    if ( !ctx.hasAdministrativePrivileges( ) ) {
      GetBucketAccessControlPolicyResponseType reply = WalrusUtil.getBucketAcl( request, imagePathParts );
      if ( reply != null ) {
        if ( !Contexts.lookup( ).getUserFullName( ).getUserId( ).equals( reply.getAccessControlPolicy( ).getOwner( ).getDisplayName( ) ) ) throw new EucalyptusCloudException(
                                                                                                                                                                               "Image registration failed: you must own the bucket containing the image." );
        userName = reply.getAccessControlPolicy( ).getOwner( ).getDisplayName( );
      }
    }
  }
  
  public static void applyImageAttributes( final EntityWrapper<ImageInfo> db, final ImageInfo imgInfo, final List<LaunchPermissionItemType> changeList, final boolean adding ) throws EucalyptusCloudException {
    for ( LaunchPermissionItemType perm : changeList ) {
      if ( perm.isGroup( ) ) {
        try {
          if ( adding ) {
            //TODO:GRZE:RESTORE            imgInfo.grantPermission( new ImageUserGroup( perm.getGroup( ) ) );
          } else {
            //TODO:GRZE:RESTORE            imgInfo.revokePermission( new ImageUserGroup( perm.getGroup( ) ) );
          }
        } catch ( Exception e ) {
          LOG.debug( e, e );
          throw new EucalyptusCloudException( "Modify image attribute failed because of: " + e.getMessage( ) );
        }
      } else if ( perm.isUser( ) ) {
        try {
          if ( adding ) {
            imgInfo.grantPermission( Accounts.lookupAccountById( perm.getUserId( ) ) );
          } else {
            imgInfo.revokePermission( Accounts.lookupAccountById( perm.getUserId( ) ) );
          }
        } catch ( AuthException e ) {
          LOG.debug( e, e );
          throw new EucalyptusCloudException( "Modify image attribute failed because of: " + e.getMessage( ) );
        }
      }
    }
  }
  
  public static boolean modifyImageInfo( ModifyImageAttributeType request ) {
    final String imageId = request.getImageId( );
    final List<LaunchPermissionItemType> addList = request.getAdd( );
    final List<LaunchPermissionItemType> remList = request.getRemove( );
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( Images.exampleWithImageId( imageId ) );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      return false;
    }
    if ( RestrictedTypes.filterPrivileged( ).apply( imgInfo ) ) {
      return false;
    }
    try {
      applyImageAttributes( db, imgInfo, addList, true );
      applyImageAttributes( db, imgInfo, remList, false );
      db.commit( );
      return true;
    } catch ( EucalyptusCloudException e ) {
      LOG.warn( e );
      db.rollback( );
      return false;
    }
  }
  
  public static Document getManifestDocument( String[] imagePathParts, FullName userName ) throws EucalyptusCloudException {
    Document inputSource = null;
    try {
      inputSource = WalrusUtil.getManifestData( userName, imagePathParts[0], imagePathParts[1] );
    } catch ( EucalyptusCloudException e ) {
      throw e;
    }
    return inputSource;
  }
  
  public static void cleanDeregistered( ) {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      List<ImageInfo> imgList = db.query( Images.exampleWithImageState( ImageMetadata.State.deregistered ) );
      for ( ImageInfo deregImg : imgList ) {
        try {
          db.delete( deregImg );
        } catch ( Exception e1 ) {}
      }
      db.commit( );
    } catch ( Exception e1 ) {
      db.rollback( );
    }
  }
  
  public static int countByAccount( String accountId ) throws AuthException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      List<ImageInfo> images = db.query( new ImageInfo( ) );
      int imageNum = 0;
      for ( ImageInfo img : images ) {
        if ( img.getOwnerAccountNumber( ).equals( accountId ) ) {
          imageNum ++;
        }
      }
      db.commit( );
      return imageNum;
    } catch ( Exception e ) {
      db.rollback( );
      throw new AuthException( "Image database query failed", e );
    }
  }
  
  public static int countByUser( String userId ) throws AuthException {
    EntityWrapper<ImageInfo> db = EntityWrapper.get( ImageInfo.class );
    try {
      List<ImageInfo> images = db.query( new ImageInfo( ) );
      int imageNum = 0;
      for ( ImageInfo img : images ) {
        if ( img.getOwnerUserId( ).equals( userId ) ) {
          imageNum ++;
        }
      }
      db.commit( );
      return imageNum;
    } catch ( Exception e ) {
      db.rollback( );
      throw new AuthException( "Image database query failed", e );
    }
  }
  
}
