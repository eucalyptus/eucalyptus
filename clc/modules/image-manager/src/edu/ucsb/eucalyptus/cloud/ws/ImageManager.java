/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.keys.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.crypto.Cipher;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.Adler32;

public class ImageManager {

  private static Logger LOG = Logger.getLogger( ImageManager.class );

  public VmAllocationInfo verify( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    SystemConfiguration conf = EucalyptusProperties.getSystemConfiguration();
    String walrusUrl = getStorageUrl( conf );

    RunInstancesType msg = vmAllocInfo.getRequest();
    ImageInfo searchDiskInfo = new ImageInfo( msg.getImageId() );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo diskInfo = db.getUnique( searchDiskInfo );
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

    this.checkStoredImage( kernelInfo );
    this.checkStoredImage( diskInfo );
    this.checkStoredImage( ramdiskInfo );

    //:: get together the required URLs ::/
    VmImageInfo vmImgInfo = getVmImageInfo( walrusUrl, diskInfo, kernelInfo, ramdiskInfo );
    vmAllocInfo.setImageInfo( vmImgInfo );
    return vmAllocInfo;
  }

  private void verifyManifestIntegrity( final ImageInfo imgInfo ) throws EucalyptusCloudException {
    String[] imagePathParts = imgInfo.getImageLocation().split( "/" );
    GetObjectResponseType reply = null;
    GetObjectType msg = new GetObjectType( imagePathParts[ 0 ], imagePathParts[ 1 ], true, false, true );
    msg.setUserId( EucalyptusProperties.NAME );
    msg.setEffectiveUserId( EucalyptusProperties.NAME );
    try {
      reply = ( GetObjectResponseType ) Messaging.send( WalrusProperties.WALRUS_REF, msg );
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( "Invalid manifest reference: " + imgInfo.getImageLocation() );
    }

    if ( reply == null || reply.getBase64Data() == null ) throw new EucalyptusCloudException( "Invalid manifest reference: " + imgInfo.getImageLocation() );
    XMLParser parser = new XMLParser( reply.getBase64Data() );
    String encryptedKey = parser.getValue( "//ec2_encrypted_key" );
    String encryptedIV = parser.getValue( "//ec2_encrypted_iv" );
    String signature = parser.getValue( "//signature" );
    String image = parser.getXML( "image" );
    String machineConfiguration = parser.getXML( "machine_configuration" );

    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    List<String> aliases = Lists.newArrayList();
    List<UserInfo> users = db.query( new UserInfo() );
    for ( UserInfo user : users )
      for ( CertificateInfo certInfo : user.getCertificates() )
        aliases.add( certInfo.getCertAlias() );
    boolean found = false;
    for ( String alias : aliases )
      found |= this.verifyManifestSignature( signature, alias, machineConfiguration + image );
    if ( !found ) throw new EucalyptusCloudException( "Invalid Manifest: Failed to verify signature." );

    try {
      PrivateKey pk = ( PrivateKey ) UserKeyStore.getInstance().getKey( EucalyptusProperties.NAME, EucalyptusProperties.NAME );
      Cipher cipher = Cipher.getInstance( "RSA/ECB/PKCS1Padding" );
      cipher.init( Cipher.DECRYPT_MODE, pk );
      cipher.doFinal( Hashes.hexToBytes( encryptedKey ) );
      cipher.doFinal( Hashes.hexToBytes( encryptedIV ) );
    } catch ( Exception ex ) {
      throw new EucalyptusCloudException( "Invalid Manifest: Failed to recover keys." );
    }
  }

  private boolean verifyManifestSignature( final String signature, final String alias, String pad ) {
    boolean ret = false;
    try {
      final AbstractKeyStore userKeyStore = UserKeyStore.getInstance();
      Signature sigVerifier = Signature.getInstance( "SHA1withRSA" );
      X509Certificate cert = userKeyStore.getCertificate( alias );
      PublicKey publicKey = cert.getPublicKey();
      sigVerifier.initVerify( publicKey );
      sigVerifier.update( pad.getBytes() );
      sigVerifier.verify( Hashes.hexToBytes( signature ) );
      ret = true;
    } catch ( Exception ex ) {
      LOG.warn( ex.getMessage() );
    } finally {
      return ret;
    }
  }

  private void checkStoredImage( final ImageInfo imgInfo ) throws EucalyptusCloudException {
    if ( imgInfo != null ) try {
      Document inputSource = null;
      try {
        String[] imagePathParts = imgInfo.getImageLocation().split( "/" );
        inputSource = ImageManager.getManifestData( imgInfo.getImageOwnerId(), imagePathParts[ 0 ], imagePathParts[ 1 ] );
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
      imgInfo.checkValid();
      LOG.info( "Triggering caching: " + imgInfo.getImageLocation() );
      try {
        imgInfo.triggerCaching();
      } catch ( Exception e ) {}
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e );
      LOG.error( "Failed check! Invalidating registration: " + imgInfo.getImageLocation() );
      this.invalidateImageById( imgInfo.getImageId() );
    }
  }

  private VmImageInfo getVmImageInfo( final String walrusUrl, final ImageInfo diskInfo, final ImageInfo kernelInfo, final ImageInfo ramdiskInfo ) throws EucalyptusCloudException {
    String diskUrl = this.getImageUrl( walrusUrl, diskInfo );
    String kernelUrl = this.getImageUrl( walrusUrl, kernelInfo );
    String ramdiskUrl = null;
    if ( ramdiskInfo != null )
      ramdiskUrl = this.getImageUrl( walrusUrl, ramdiskInfo );

    //:: create the response assets now since we might not have a ramdisk anyway :://
    VmImageInfo vmImgInfo = new VmImageInfo( diskInfo.getImageId(), kernelInfo.getImageId(), ramdiskInfo == null ? null : ramdiskInfo.getImageId(),
                                             diskUrl, kernelUrl, ramdiskInfo == null ? null : ramdiskUrl );
    return vmImgInfo;
  }

  private String getImageUrl( final String walrusUrl, final ImageInfo diskInfo ) throws EucalyptusCloudException {
    String diskUrl;
    try {
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
      img.invalidate();
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
    if ( imageList.isEmpty() && owners.isEmpty() && executable.isEmpty() )
      executable.add( "self" );

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
      GetBucketAccessControlPolicyType getBukkitInfo = Admin.makeMsg( GetBucketAccessControlPolicyType.class, request );
      getBukkitInfo.setBucket( imagePathParts[ 0 ] );
      GetBucketAccessControlPolicyResponseType reply = ( GetBucketAccessControlPolicyResponseType ) Messaging.send( WalrusProperties.WALRUS_REF, getBukkitInfo );

      if ( !request.getUserId().equals( reply.getAccessControlPolicy().getOwner().getDisplayName() ) )
        throw new EucalyptusCloudException( "Image registration failed: you must own the bucket containing the image." );
      userName = reply.getAccessControlPolicy().getOwner().getDisplayName();
    }
    //:: prepare the imageInfo object :://
    ImageInfo imageInfo = new ImageInfo( imageLocation, request.getUserId(), "available", true );
    //:: verify the image manifest :://
    try {
      this.verifyManifestIntegrity( imageInfo );
    } catch ( EucalyptusCloudException e ) {
      throw new EucalyptusCloudException( "Image registration failed because the manifest referenced is invalid or unavailable." );
    }
    //:: this parses the manifest for extra information :://
    Document inputSource = null;
    try {
      inputSource = ImageManager.getManifestData( request.getUserId(), imagePathParts[ 0 ], imagePathParts[ 1 ] );
    } catch ( EucalyptusCloudException e ) {
      throw e;
    }
    XPath xpath = null;
    xpath = XPathFactory.newInstance().newXPath();

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


    if ( "yes".equals( kernelId ) || "true".equals( kernelId ) || imagePathParts[ 1 ].startsWith( "vmlinuz" ) ) {
      if ( !request.isAdministrator() ) throw new EucalyptusCloudException( "Only administrators can register kernel images." );
      imageInfo.setImageType( EucalyptusProperties.IMAGE_KERNEL );
      imageInfo.setImageId( this.newImageId( EucalyptusProperties.IMAGE_KERNEL_PREFIX, imageInfo.getImageLocation() ) );
    } else if ( "yes".equals( ramdiskId ) || "true".equals( ramdiskId ) || imagePathParts[ 1 ].startsWith( "initrd" ) ) {
      if ( !request.isAdministrator() ) throw new EucalyptusCloudException( "Only administrators can register ramdisk images." );
      imageInfo.setImageType( EucalyptusProperties.IMAGE_RAMDISK );
      imageInfo.setImageId( this.newImageId( EucalyptusProperties.IMAGE_RAMDISK_PREFIX, imageInfo.getImageLocation() ) );
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
      imageInfo.setImageId( this.newImageId( EucalyptusProperties.IMAGE_MACHINE_PREFIX, imageInfo.getImageLocation() ) );
    }

    String signature = null;
    try {
      signature = ( String ) xpath.evaluate( "/manifest/signature/text()", inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException e ) {
      LOG.warn( e.getMessage() );
    }
    imageInfo.setSignature( signature );


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
    imageInfo.checkValid();
    imageInfo.triggerCaching();

    RegisterImageResponseType reply = ( RegisterImageResponseType ) request.getReply();
    reply.setImageId( imageInfo.getImageId() );
    return reply;
  }

  private String generateImageId( final String imagePrefix, final String imageLocation ) {
    Adler32 hash = new Adler32();
    String key = imageLocation + System.currentTimeMillis();
    hash.update( key.getBytes() );
    String imageId = String.format( "%s-%08X", imagePrefix, hash.getValue() );
    return imageId;
  }

  private String newImageId( final String imagePrefix, final String imageLocation ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo query = new ImageInfo();
    query.setImageId( generateImageId( imagePrefix, imageLocation ) );
    LOG.info( "Trying to lookup using created AMI id=" + query.getImageId() );
    for ( ; db.query( query ).size() != 0; query.setImageId( generateImageId( imagePrefix, imageLocation ) ) ) ;
    db.commit();
    LOG.info( "Assigning imageId=" + query.getImageId() );
    return query.getImageId();
  }

  public DeregisterImageResponseType DeregisterImage( DeregisterImageType request ) throws EucalyptusCloudException {
    DeregisterImageResponseType reply = ( DeregisterImageResponseType ) request.getReply();
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();

    ImageInfo imgInfo = null;
    try {
      imgInfo = db.getUnique( new ImageInfo( request.getImageId() ) );
      if ( !imgInfo.getImageOwnerId().equals( request.getUserId() ) && !request.isAdministrator() )
        throw new EucalyptusCloudException( "Only the owner of a registered image or the administrator can deregister it." );
      imgInfo.invalidate();
      db.commit();
      reply.set_return( true );
    } catch ( EucalyptusCloudException e ) {
      reply.set_return( false );
      db.rollback();
    }
    return reply;
  }

  public ConfirmProductInstanceResponseType ConfirmProductInstance( ConfirmProductInstanceType request ) throws EucalyptusCloudException {
    ConfirmProductInstanceResponseType reply = (ConfirmProductInstanceResponseType) request.getReply();
    reply.set_return( false );
    VmInstance vm = null;
    try {
      vm = VmInstances.getInstance().lookup( request.getInstanceId() );
    } catch ( NoSuchElementException e ) {
      return reply;
    }
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    try {
      ImageInfo found = db.getUnique( new ImageInfo( vm.getImageInfo().getImageId() ) );
      if ( vm.getImageInfo().getImageId().equals( found ) ) {
        reply.set_return( true );
      }
    } catch ( EucalyptusCloudException e ) {
    } finally {
      db.commit();
    }
    return reply;
  }

  public DescribeImageAttributeResponseType DescribeImageAttribute( DescribeImageAttributeType request ) throws EucalyptusCloudException {
    DescribeImageAttributeResponseType reply = ( DescribeImageAttributeResponseType ) request.getReply();
    reply.setImageId( request.getImageId() );

    if ( request.getAttribute() != null )
      request.applyAttribute();

    if ( request.getBlockDeviceMapping() != null )
      throw new EucalyptusCloudException( "image attribute: block device mappings: not implemented" );

    reply.setProductCodes( null );
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    try {
      ImageInfo imgInfo = db.getUnique( new ImageInfo( request.getImageId() ) );
      if ( !imgInfo.isAllowed( db.recast( UserInfo.class ).getUnique( new UserInfo( request.getUserId() ) ) ) )
        throw new EucalyptusCloudException( "image attribute: not authorized." );
      if ( request.getKernel() != null ) {
        reply.setKernel( imgInfo.getKernelId() );
      } else if ( request.getRamdisk() != null ) {
        reply.setRamdisk( imgInfo.getRamdiskId() );
      } else if ( request.getLaunchPermission() != null ) {
        for ( UserGroupInfo userGroup : imgInfo.getUserGroups() )
          reply.getLaunchPermission().add( LaunchPermissionItemType.getGroup( userGroup.getName() ) );
        for ( UserInfo user : imgInfo.getPermissions() )
          reply.getLaunchPermission().add( LaunchPermissionItemType.getUser( user.getUserName() ) );
      } else if ( !request.getProductCodes().isEmpty() ) {
        for ( ProductCode p : imgInfo.getProductCodes() ) {
          reply.getProductCodes().add( p.getValue() );
        }
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
        for ( String productCode : request.getProductCodes() )
          imgInfo.getProductCodes().add( new ProductCode( productCode ) );
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

  private static Document getManifestData( String userId, String bucketName, String objectName ) throws EucalyptusCloudException {
    GetObjectResponseType reply = null;
    try {
      GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true );
      msg.setUserId( userId );
      reply = ( GetObjectResponseType ) Messaging.send( WalrusProperties.WALRUS_REF, msg );
    }
    catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName );
    }

    Document inputSource = null;
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      inputSource = builder.parse( new ByteArrayInputStream( reply.getBase64Data().getBytes() ) );
    }
    catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName );
    }
    return inputSource;
  }

  public boolean isSet( String id ) {
    return id != null && !"".equals( id );
  }
}
