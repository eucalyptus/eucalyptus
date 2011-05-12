/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.blockstorage.WalrusUtil;
import com.eucalyptus.cloud.Image;
import com.eucalyptus.cloud.Image.Platform;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.images.ImageManifests.ImageManifest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.Lookups;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.util.XMLParser;

public class ImageManifests {
  private static Logger LOG = Logger.getLogger( ImageManifests.class );
  
  private static boolean verifyBucketAcl( String bucketName ) {
    Context ctx = Contexts.lookup( );
    GetBucketAccessControlPolicyType getBukkitInfo = new GetBucketAccessControlPolicyType( );
    getBukkitInfo.setBucket( bucketName );
    try {
      GetBucketAccessControlPolicyResponseType reply = ( GetBucketAccessControlPolicyResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).send( getBukkitInfo );
      String ownerName = reply.getAccessControlPolicy( ).getOwner( ).getDisplayName( );
      return ctx.getUserFullName( ).getUserId( ).equals( ownerName );
    } catch ( EucalyptusCloudException ex ) {
      LOG.error( ex, ex );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
    return false;
  }
  
  private static boolean verifyManifestSignature( final X509Certificate cert, final String signature, String pad ) {
    Signature sigVerifier;
    try {
      sigVerifier = Signature.getInstance( "SHA1withRSA" );
      PublicKey publicKey = cert.getPublicKey( );
      sigVerifier.initVerify( publicKey );
      sigVerifier.update( ( pad ).getBytes( ) );
      return sigVerifier.verify( hexToBytes( signature ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      return false;
    }
  }
  
  private static byte[] hexToBytes( String data ) {
    int k = 0;
    byte[] results = new byte[data.length( ) / 2];
    for ( int i = 0; i < data.length( ); ) {
      results[k] = ( byte ) ( Character.digit( data.charAt( i++ ), 16 ) << 4 );
      results[k] += ( byte ) ( Character.digit( data.charAt( i++ ), 16 ) );
      k++;
    }

    return results;
  }

  
  private static boolean checkManifest( User user, String manifest, Document inputSource ) throws EucalyptusCloudException {
    XPath xpath = XPathFactory.newInstance( ).newXPath( );
    final String signature;
    try {
      String encryptedKey = ( String ) xpath.evaluate( "//ec2_encrypted_key" , inputSource, XPathConstants.STRING );
      String encryptedIV = ( String ) xpath.evaluate( "//ec2_encrypted_iv" , inputSource, XPathConstants.STRING );
      signature = ( String ) xpath.evaluate( "//signature" , inputSource, XPathConstants.STRING );
    } catch ( XPathExpressionException ex1 ) {
      LOG.error( ex1 );
      throw new EucalyptusCloudException( "Failed to parse manifest file for one of the following required fields:  ec2_encrypted_iv, ec2_encrypted_key, or signature.  Cause: " + ex1.getMessage( ), ex1 );
    }
    String image = manifest.replaceAll(".*<image>","<image>").replaceAll("</image>.*","</image>");
    String machineConfiguration = manifest.replaceAll(".*<machine_configuration>","<machine_configuration>").replaceAll("</machine_configuration>.*","</machine_configuration>");
    final String pad = ( machineConfiguration + image );
    Predicate<Certificate> tryVerifyWithCert = new Predicate<Certificate>( ) {
      
      @Override
      public boolean apply( Certificate checkCert ) {
        if ( checkCert instanceof X509Certificate ) {
          X509Certificate cert = ( X509Certificate ) checkCert;
          Signature sigVerifier;
          try {
            sigVerifier = Signature.getInstance( "SHA1withRSA" );
            PublicKey publicKey = cert.getPublicKey( );
            sigVerifier.initVerify( publicKey );
            sigVerifier.update( ( pad ).getBytes( ) );
            return sigVerifier.verify( hexToBytes( signature ) );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
            return false;
          }
        } else {
          return false;
        }
      }
    };
    Function<com.eucalyptus.auth.principal.Certificate, X509Certificate> euareToX509 = new Function<com.eucalyptus.auth.principal.Certificate, X509Certificate>( ) {
      
      @Override
      public X509Certificate apply( com.eucalyptus.auth.principal.Certificate input ) {
        return input.getX509Certificate( );
      }
    };
    
    try {
      if ( Iterables.any( Lists.transform( user.getCertificates( ), euareToX509 ), tryVerifyWithCert ) ) {
        return true;
      } else if ( tryVerifyWithCert.apply( SystemCredentialProvider.getCredentialProvider( Eucalyptus.class ).getCertificate( ) ) ) {
        return true;
      } else {
        for ( User u : Accounts.listAllUsers( ) ) {
          if ( Iterables.any( Lists.transform( u.getCertificates( ), euareToX509 ), tryVerifyWithCert ) ) {
            return true;
          }
        }
      }
    } catch ( AuthException e ) {
      throw new EucalyptusCloudException( "Invalid Manifest: Failed to verify signature because of missing (deleted?) user certificate.", e );
    }
    return false;
  }
  
  private static String requestManifestData( FullName userName, String bucketName, String objectName ) throws EucalyptusCloudException {
    GetObjectResponseType reply = null;
    try {
      GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true );
//TODO:GRZE:WTF.      
//      User user = Accounts.lookupUserById( userName.getNamespace( ) );
//      msg.setUserId( user.getName( ) );
      msg.regarding( );
      msg.setCorrelationId( Contexts.lookup( ).getRequest( ).getCorrelationId( ) );
      reply = ( GetObjectResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).send( msg );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
    }
    return B64.decString( reply.getBase64Data( ).getBytes( ) );
  }
  
  public static class ImageManifest {
    private final String             imageLocation;
    private final Image.Architecture architecture;
    private final String             kernelId;
    private final String             ramdiskId;
    private final Image.Type         imageType;
    private final Image.Platform     platform;
    private final String             signature;
    private final String             manifest;
    private final Document           inputSource;
    
    private ImageManifest( String imageLocation ) throws EucalyptusCloudException {
      Context ctx = Contexts.lookup( );
      String cleanLocation = imageLocation.replaceAll( "^/*", "" );
      this.imageLocation = cleanLocation;
      int index = cleanLocation.indexOf( '/' );
      if ( index < 2 || index + 1 >= cleanLocation.length( ) ) {
        throw new EucalyptusCloudException( "Image registration failed:  Invalid image location: " + imageLocation );
      }
      String bucketName = cleanLocation.substring( 0, index );
      String manifestKey = cleanLocation.substring( index + 1 );
      String manifestName = manifestKey.replaceAll( ".*/", "" );
      if ( !ImageManifests.verifyBucketAcl( bucketName ) ) {
        throw new EucalyptusCloudException( "Image registration failed: you must own the bucket containing the image." );
      }
      this.manifest = ImageManifests.requestManifestData( ctx.getUserFullName( ), bucketName, manifestKey );
      try {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( );
        this.inputSource = builder.parse( new ByteArrayInputStream( this.manifest.getBytes( ) ) );
      } catch ( Exception e ) {
        throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + manifestKey, e );
      }
      XPath xpath = XPathFactory.newInstance( ).newXPath( );
      String arch = null;
      String kId = null;
      String rId = null;
      try {
        this.signature = ( String ) xpath.evaluate( "//signature" , inputSource, XPathConstants.STRING );
      } catch ( XPathExpressionException e ) {
        LOG.warn( e.getMessage( ) );
        throw new EucalyptusCloudException( "Failed to parse manifest file for the required field:  signature.  Cause: " + e.getMessage( ), e );
      }
      try {
        arch = ( String ) xpath.evaluate( "/manifest/machine_configuration/architecture/text()", inputSource, XPathConstants.STRING );
      } catch ( XPathExpressionException e ) {
        LOG.warn( e.getMessage( ) );
      }
      try {
        kId = ( String ) xpath.evaluate( "/manifest/machine_configuration/kernel_id/text()", inputSource, XPathConstants.STRING );
      } catch ( XPathExpressionException e ) {
        LOG.warn( e.getMessage( ) );
      }
      try {
        rId = ( String ) xpath.evaluate( "/manifest/machine_configuration/ramdisk_id/text()", inputSource, XPathConstants.STRING );
      } catch ( XPathExpressionException e ) {
        LOG.warn( e.getMessage( ) );
      }
      String architecture = ( ( arch == null )
          ? "i386"
          : arch );
      this.architecture = Image.Architecture.valueOf( architecture );
      if ( "yes".equals( kId ) || "true".equals( kId ) || manifestName.startsWith( "vmlinuz" ) ) {
        if ( !ctx.hasAdministrativePrivileges( ) ) {
          throw new EucalyptusCloudException( "Only administrators can register kernel images." );
        }
        this.imageType = Image.Type.kernel;
        this.platform = Image.Platform.linux;
        this.kernelId = null;
        this.ramdiskId = null;
      } else if ( "yes".equals( rId ) || "true".equals( rId ) || manifestName.startsWith( "initrd" ) ) {
        if ( !Contexts.lookup( ).hasAdministrativePrivileges( ) ) {
          throw new EucalyptusCloudException( "Only administrators can register ramdisk images." );
        }
        this.imageType = Image.Type.ramdisk;
        this.platform = Image.Platform.linux;
        this.kernelId = null;
        this.ramdiskId = null;
      } else {
        this.imageType = Image.Type.machine;
        this.kernelId = kId;
        this.ramdiskId = rId;
        if ( !manifestName.startsWith( Image.Platform.windows.toString( ) ) ) {
          this.platform = Image.Platform.linux;
          if ( kId != null ) {
            ImageInfo k = null;
            try {
              k = Images.lookupImage( kId );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
              throw new EucalyptusCloudException( "Referenced kernel id is invalid: " + kId, ex );
            }
            if ( !Lookups.checkPrivilege( ctx.getRequest( ), PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_IMAGE, kId, k.getOwner( ) ) ) {
              throw new EucalyptusCloudException( "Access to kernel image " + kId + " is denied for " + ctx.getUser( ).getName( ) );
            }
          }
          if ( kId != null ) {
            ImageInfo r = null;
            try {
              r = Images.lookupImage( rId );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
              throw new EucalyptusCloudException( "Referenced ramdisk id is invalid: " + rId, ex );
            }
            if ( !Lookups.checkPrivilege( ctx.getRequest( ), PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_IMAGE, rId, r.getOwner( ) ) ) {
              throw new EucalyptusCloudException( "Access to ramdisk image " + rId + " is denied for " + ctx.getUser( ).getName( ) );
            }
          }
        } else {
          this.platform = Image.Platform.windows;
        }
      }
    }
    
    public String getSignature( ) {
      return this.signature;
    }
    
    public Image.Platform getPlatform( ) {
      return this.platform;
    }
    
    public Image.Architecture getArchitecture( ) {
      return this.architecture;
    }
    
    public String getKernelId( ) {
      return this.kernelId;
    }
    
    public String getRamdiskId( ) {
      return this.ramdiskId;
    }
    
    public Image.Type getImageType( ) {
      return this.imageType;
    }
    
    public String getImageLocation( ) {
      return this.imageLocation;
    }
    
    public String getManifest( ) {
      return this.manifest;
    }
    
  }
  
  public static ImageManifest lookup( String imageLocation ) throws EucalyptusCloudException {
    return new ImageManifest( imageLocation );
  }
}
