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

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.ImageMetadata.DeviceMappingType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.XMLParser;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ImageManifests {
  private static Logger LOG = Logger.getLogger( ImageManifests.class );

  static String requestManifestData( String bucketName, String objectName ) throws EucalyptusCloudException {
    try {
      try ( final EucaS3Client s3Client = EucaS3ClientFactory.getEucaS3ClientForUser(
          Accounts.lookupSystemAccountByAlias( AccountIdentifiers.AWS_EXEC_READ_SYSTEM_ACCOUNT ),
          (int)TimeUnit.MINUTES.toSeconds( 15 )) ) {
        return s3Client.getObjectContent(
            bucketName,
            objectName,
            ImageConfiguration.getInstance( ).getMaxManifestSizeBytes( ) );
      }
    } catch ( Exception e ) {
      LOG.error("Can't read manifest due to: " + e);
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
    }
  }
  
  public static class ManifestDeviceMapping {
    ManifestDeviceMapping( DeviceMappingType type, String virtualName, String deviceName ) {
      super( );
      this.type = type;
      this.virtualName = virtualName;
      this.deviceName = deviceName;
    }
    
    DeviceMappingType type;
    String            virtualName;
    String            deviceName;
  }
  
  public static class ImageManifest {
    private final String                           imageLocation;
    private final ImageMetadata.Architecture       architecture;
    private final String                           kernelId;
    private final String                           ramdiskId;
    private final ImageMetadata.Type               imageType;
    private final ImageMetadata.Platform           platform;
    private final ImageMetadata.VirtualizationType virtualizationType;
    private final String                           signature;
    private final String                           checksum;
    private final String                           checksumType;
    private final String                           manifest;
    private final Document                         inputSource;
    private final String                           name;
    private final Long                             size;
    private final Long                             bundledSize;
    private final List<String>                     ancestors      = Lists.newArrayList( );
    private XPath                                  xpath;
    private Function<String, String>               xpathHelper;
    private String                                 encryptedKey;
    private String                                 encryptedIV;
    private String                                 userId;
    private List<ManifestDeviceMapping>            deviceMappings = Lists.newArrayList( );
    
    ImageManifest( @Nonnull  final String imageLocation,
                   @Nullable final User user ) throws EucalyptusCloudException {
      ManifestLocation mLoc = new ManifestLocation( imageLocation );
      this.imageLocation = mLoc.cleanLocation;
      String bucketName = mLoc.bucketName;
      String manifestKey = mLoc.manifestKey;
      final String manifestName = manifestKey.replaceAll( ".*/", "" );
//GRZE:TODO: restore this ACL check
//      if ( !ImageManifests.verifyBucketAcl( bucketName ) ) {
//        throw new EucalyptusCloudException( "Image registration failed: you must own the bucket containing the image." );
//      }
      this.xpath = XPathFactory.newInstance( ).newXPath( );
      this.xpathHelper = new Function<String, String>( ) {

          @Override
          public String apply( String input ) {
              try {
                  return ( String ) ImageManifest.this.xpath.evaluate( input, ImageManifest.this.inputSource, XPathConstants.STRING );
              } catch ( XPathExpressionException ex ) {
                  return null;
              }
          }
      };
      this.encryptedKey = this.xpathHelper.apply( "//ec2_encrypted_key" );
      this.encryptedIV = this.xpathHelper.apply( "//ec2_encrypted_iv" );
      Predicate<ImageMetadata.Type> checkIdType = new Predicate<ImageMetadata.Type>( ) {

          @Override
          public boolean apply( ImageMetadata.Type input ) {
              final String type = ImageManifest.this.xpathHelper.apply( "/manifest/image/type/text()" );
              if ( type != null && type.equals( input.name( ) ) ) {
                return true;
              }

              String value = ImageManifest.this.xpathHelper.apply( input.getManifestPath( ) );
              return "yes".equals( value ) || "true".equals( value ) || manifestName.startsWith( input.getNamePrefix() );
          }
      };
      if ( checkIdType.apply( ImageMetadata.Type.kernel ) && user != null && !user.isSystemAdmin( ) ) {
        throw new EucalyptusCloudException( "Only administrators can register kernel images." );
      } else if  ( checkIdType.apply( ImageMetadata.Type.ramdisk ) && user != null && !user.isSystemAdmin( ) ) {
        throw new EucalyptusCloudException( "Only administrators can register ramdisk images." );
      }
      this.manifest = ImageManifests.requestManifestData( bucketName, manifestKey );
      try {
        DocumentBuilder builder = XMLParser.getDocBuilder( );
        this.inputSource = builder.parse( new ByteArrayInputStream( this.manifest.getBytes( ) ) );
      } catch ( Exception e ) {
        throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + manifestKey, e );
      }
      String temp;
      this.name = ( ( temp = this.xpathHelper.apply( "/manifest/image/name/text()" ) ) != null )
        ? temp
        : manifestName.replace( ".manifest.xml", "" );
      this.checksum = ( ( temp = this.xpathHelper.apply( "/manifest/image/digest/text()" ) ) != null )
        ? temp
        : "0000000000000000000000000000000000000000";
      this.checksumType = ( ( temp = this.xpathHelper.apply( "/manifest/image/digest/@algorithm" ) ) != null )
        ? temp
        : "SHA1";
      this.signature = ( ( temp = this.xpathHelper.apply( "//signature" ) ) != null )
        ? temp
        : null;
      this.userId = ( ( temp = this.xpathHelper.apply( "//user" ) ) != null )
        ? temp
        : null;
      String typeInManifest = this.xpathHelper.apply( ImageMetadata.TYPE_MANIFEST_XPATH );
      
      this.size = ( ( temp = this.xpathHelper.apply( "/manifest/image/size/text()" ) ) != null )
        ? Long.parseLong( temp )
        : -1l;
      this.bundledSize = ( ( temp = this.xpathHelper.apply( "/manifest/image/bundled_size/text()" ) ) != null )
        ? Long.parseLong( temp )
        : -1l;
      
      String arch = this.xpathHelper.apply( "/manifest/machine_configuration/architecture/text()" );
      this.architecture = ImageMetadata.Architecture.valueOf( ( ( arch == null )
        ? "i386"
        : arch ) );
      try {
        NodeList ancestorNodes = ( NodeList ) xpath.evaluate( "/manifest/image/ancestry/ancestor_ami_id/text()",
                                                              inputSource,
                                                              XPathConstants.NODESET );
        if ( ancestorNodes != null ) {
          for ( int i = 0; i < ancestorNodes.getLength( ); i++ ) {
            for ( String ancestorId : ancestorNodes.item( i ).getNodeValue( ).split( "," ) ) {
              this.ancestors.add( ancestorId );
            }
          }
        }
      } catch ( XPathExpressionException ex ) {
        LOG.error( ex, ex );
      }
      try {
        NodeList devMapList = ( NodeList ) this.xpath.evaluate( "/manifest/machine_configuration/block_device_mapping/mapping",
                                                                inputSource,
                                                                XPathConstants.NODESET );
        for ( int i = 0; i < devMapList.getLength( ); i++ ) {
          Node node = devMapList.item( i );
          NodeList children = node.getChildNodes( );
          String virtualName = null;
          String device = null;
          for ( int j = 0; j < children.getLength( ); j++ ) {
            Node childNode = children.item( j );
            String nodeType = childNode.getNodeName( );
            if ( "virtual".equals( nodeType ) && childNode.getTextContent( ) != null ) {
              virtualName = childNode.getTextContent( );
            } else if ( "device".equals( nodeType ) && childNode.getTextContent( ) != null ) {
              device = childNode.getTextContent( );
            }
          }
          if ( virtualName != null && device != null ) {
            if ( "ami".equals( virtualName ) ) {
              this.deviceMappings.add( new ManifestDeviceMapping( DeviceMappingType.ami, virtualName, device ) );;
            } else if ( "root".equals( virtualName ) ) {
              this.deviceMappings.add( new ManifestDeviceMapping( DeviceMappingType.root, virtualName, device ) );
            } else if ( "swap".equals( virtualName ) ) {
              this.deviceMappings.add( new ManifestDeviceMapping( DeviceMappingType.swap, virtualName, device ) );
            } else if ( virtualName.startsWith( "ephemeral" ) ) {
              this.deviceMappings.add( new ManifestDeviceMapping( DeviceMappingType.ephemeral, virtualName, device ) );
            }
          }
        }
      } catch ( XPathExpressionException ex ) {
        LOG.error( ex, ex );
      }

      if ( checkIdType.apply( ImageMetadata.Type.kernel ) ) {
        this.imageType = ImageMetadata.Type.kernel;
        this.platform = ImageMetadata.Platform.linux;
        this.virtualizationType = ImageMetadata.VirtualizationType.paravirtualized;
        this.kernelId = null;
        this.ramdiskId = null;
      } else if ( checkIdType.apply( ImageMetadata.Type.ramdisk ) ) {
        this.imageType = ImageMetadata.Type.ramdisk;
        this.platform = ImageMetadata.Platform.linux;
        this.virtualizationType = ImageMetadata.VirtualizationType.paravirtualized;
        this.kernelId = null;
        this.ramdiskId = null;
      } else {
        String kId = this.xpathHelper.apply( ImageMetadata.Type.kernel.getManifestPath( ) );
        String rId = this.xpathHelper.apply( ImageMetadata.Type.ramdisk.getManifestPath( ) );
        this.imageType = ImageMetadata.Type.machine;
        if ( !manifestName.startsWith( ImageMetadata.Platform.windows.toString( ) )
             && !( kId != null && ImageMetadata.Platform.windows.name( ).equals( kId ) ) ) {
          this.platform = ImageMetadata.Platform.linux;
          this.virtualizationType = ImageMetadata.VirtualizationType.paravirtualized;
          if ( CloudMetadatas.isKernelImageIdentifier( kId ) ) {
            //TODO EUCA-3109
            //ImageManifests.checkPrivileges( this.kernelId );
            this.kernelId = kId;
          } else {
            this.kernelId = null;
          }
          if ( CloudMetadatas.isRamdiskImageIdentifier( rId ) ) {
            //TODO EUCA-3109
            //ImageManifests.checkPrivileges( this.ramdiskId );
            this.ramdiskId = rId;
          } else {
            this.ramdiskId = null;
          }
        } else {
          this.platform = ImageMetadata.Platform.windows;
          this.virtualizationType = ImageMetadata.VirtualizationType.hvm;
          this.kernelId = null;
          this.ramdiskId = null;
        }
      }
    }
    
    public String getSignature( ) {
      return this.signature;
    }
    
    public ImageMetadata.Platform getPlatform( ) {
      return this.platform;
    }
    
    public ImageMetadata.Architecture getArchitecture( ) {
      return this.architecture;
    }
    
    public String getKernelId( ) {
      return this.kernelId;
    }

    public List<ManifestDeviceMapping> getDeviceMappings() {
      return this.deviceMappings;
    }

    public String getAmi() {
	  try {
        ManifestDeviceMapping root = Iterables.find(this.deviceMappings, new Predicate<ManifestDeviceMapping>() {
          @Override
          public boolean apply(ManifestDeviceMapping man) {
            return man.type == DeviceMappingType.ami;
          }});
        return root.deviceName;
      } catch (NoSuchElementException ex) {
        return "";
      }
    }

    public String getRoot() {
      try {
        ManifestDeviceMapping root = Iterables.find(this.deviceMappings, new Predicate<ManifestDeviceMapping>() {
          @Override
          public boolean apply(ManifestDeviceMapping man) {
            return man.type == DeviceMappingType.root;
          }});
        return root.deviceName;
      } catch (NoSuchElementException ex) {
        return "";
      }
    }

    public String getRamdiskId( ) {
      return this.ramdiskId;
    }
    
    public ImageMetadata.Type getImageType( ) {
      return this.imageType;
    }
    
    public String getImageLocation( ) {
      return this.imageLocation;
    }
    
    public String getManifest( ) {
      return this.manifest;
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public String getAccountId( ) {
      return this.userId;
    }
    
    public Long getSize( ) {
      return this.size;
    }
    
    public Long getBundledSize( ) {
      return this.bundledSize;
    }
    
    public String getChecksum( ) {
      return this.checksum;
    }
    
    public String getChecksumType( ) {
      return this.checksumType;
    }

    public ImageMetadata.VirtualizationType getVirtualizationType( ) {
      return this.virtualizationType;
    }
    
    public static class ManifestLocation {
      public final String bucketName;
      public final String manifestKey;
      public final String cleanLocation;

      public ManifestLocation(String imageLocation) throws EucalyptusCloudException {
        cleanLocation = imageLocation.replaceAll( "^/*", "" );
        int index = cleanLocation.indexOf( '/' );
        if ( index < 2 || index + 1 >= cleanLocation.length( ) ) {
          throw new EucalyptusCloudException( "Invalid image location: " + imageLocation );
        }
        bucketName = cleanLocation.substring( 0, index );
        manifestKey = cleanLocation.substring( index + 1 );
      }
    }
  }

  public static String getManifestHash( String manifestLocation ) throws EucalyptusCloudException {
    ImageManifest.ManifestLocation mLoc = new ImageManifest.ManifestLocation( manifestLocation );
    String manifest = ImageManifests.requestManifestData( mLoc.bucketName, mLoc.manifestKey );
    return calculateManifestHash( manifest );
  }

  public static String calculateManifestHash( String content )  throws EucalyptusCloudException {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(content.getBytes());
      return Base64.toBase64String(messageDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new EucalyptusCloudException("Can't load SHA-256 algorithm", e);
    }
  }

  public static ImageManifest lookup( String imageLocation ) throws EucalyptusCloudException {
    return new ImageManifest( imageLocation, null );
  }

  /**
   * Lookup an ImageManifest, verifying permissions for the given user
   */
  public static ImageManifest lookup( String imageLocation, User owner) throws EucalyptusCloudException {
    final ImageManifest manifest = new ImageManifest( imageLocation, owner );
    try{
      final String ownerAcctId = owner.getAccountNumber();
      if(ownerAcctId.equals(manifest.getAccountId()))
        return manifest;
    }catch(final AuthException ex){
      throw new ClientComputeException("AuthFailure","Manifest is not accessible");
    }
    throw new ClientComputeException("AuthFailure","Manifest is not accessible");
  }
  
  static void checkPrivileges( String diskId ) throws EucalyptusCloudException {
    Context ctx = Contexts.lookup( );
    if ( diskId != null ) {
      ImageInfo disk = null;
      try {
        disk = Images.lookupImage( diskId );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new EucalyptusCloudException( "Referenced image id is invalid: " + diskId, ex );
      }
      if ( !RestrictedTypes.filterPrivileged( ).apply( disk ) ) {
        throw new EucalyptusCloudException( "Access to "
                                            + disk.getImageType( ).toString( )
                                            + " image "
                                            + diskId
                                            + " is denied for "
                                            + ctx.getUser( ).getName( ) );
      }
    }
  }
}
