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
package com.eucalyptus.blockstorage;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.images.Image;
import com.eucalyptus.images.ImageManager;
import com.eucalyptus.images.ImageUtil;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.RemoteDispatcher;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.CacheImageType;
import edu.ucsb.eucalyptus.msgs.CheckImageType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;
import edu.ucsb.eucalyptus.util.XMLParser;

public class WalrusUtil {
  private static Logger LOG = Logger.getLogger( WalrusUtil.class );
  
  public static void checkValid( Image imgInfo ) {
    String[] parts = imgInfo.getImageLocation( ).split( "/" );
    CheckImageType check = new CheckImageType( ).regarding( );
    check.setBucket( parts[0] );
    check.setKey( parts[1] );
    RemoteDispatcher.lookupSingle( Components.lookup( "walrus" ) ).dispatch( check );
  }
  
  public static void triggerCaching( Image imgInfo ) {
    String[] parts = imgInfo.getImageLocation( ).split( "/" );
    CacheImageType cache = new CacheImageType( ).regarding( );
    cache.setBucket( parts[0] );
    cache.setKey( parts[1] );
    RemoteDispatcher.lookupSingle( Components.lookup( "walrus" ) ).dispatch( cache );
  }
  
  public static void invalidate( Image imgInfo ) {
    String[] parts = imgInfo.getImageLocation( ).split( "/" );
    imgInfo.setImageState( "deregistered" );
    try {
      RemoteDispatcher.lookupSingle( Components.lookup( "walrus" ) ).dispatch( new FlushCachedImageType( parts[0], parts[1] ) );
    } catch ( Exception e ) {}
  }
  
  public static Document getManifestData( String userId, String bucketName, String objectName ) throws EucalyptusCloudException {
    GetObjectResponseType reply = null;
    try {
      GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true );
      User user = Accounts.lookupUserByName( userId );
      msg.setUser( user );
      
      reply = ( GetObjectResponseType ) RemoteDispatcher.lookupSingle( Components.lookup( "walrus" ) ).send( msg );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
    }
    
    Document inputSource = null;
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( );
      inputSource = builder.parse( new ByteArrayInputStream( Hashes.base64decode( reply.getBase64Data( ) ).getBytes( ) ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
    }
    return inputSource;
  }
  
  public static GetBucketAccessControlPolicyResponseType getBucketAcl( RegisterImageType request, String[] imagePathParts ) throws EucalyptusCloudException {
    GetBucketAccessControlPolicyType getBukkitInfo = new GetBucketAccessControlPolicyType( ).regarding( request );
    if ( getBukkitInfo != null ) {
      getBukkitInfo.setBucket( imagePathParts[0] );
      GetBucketAccessControlPolicyResponseType reply = ( GetBucketAccessControlPolicyResponseType ) RemoteDispatcher.lookupSingle( Components.lookup( "walrus" ) ).send( getBukkitInfo );
      return reply;
    }
    return null;
  }
  
  public static void verifyManifestIntegrity( final Image imgInfo ) throws EucalyptusCloudException {
    String[] imagePathParts = imgInfo.getImageLocation( ).split( "/" );
    GetObjectResponseType reply = null;
    GetObjectType msg = new GetObjectType( imagePathParts[0], imagePathParts[1], true, false, true );
    msg.setUser( User.SYSTEM );
    try {
      reply = ( GetObjectResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( "walrus" ) ).send( msg );
      if ( reply == null || reply.getBase64Data( ) == null ) {
        throw new EucalyptusCloudException( "No data: " + imgInfo.getImageLocation( ) );
      } else {
        if ( LogLevels.DEBUG ) {
          LOG.debug( "Got the manifest to verify: " );
          LOG.debug( Hashes.base64decode( reply.getBase64Data( ) ) );
        }
        if( checkManifest( imgInfo, reply.getBase64Data( ) ) ) {
          return;
        } else {
          throw new EucalyptusCloudException( "Failed to verify signature." );
        }
        
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      LOG.debug( e );
      throw new EucalyptusCloudException( "Invalid manifest reference: " + imgInfo.getImageLocation( ) + " because of " + e.getMessage( ), e );
    }
  }

  private static boolean checkManifest( final Image imgInfo, String manifest ) throws EucalyptusCloudException {
    XMLParser parser = new XMLParser( Hashes.base64decode( manifest ) );
    String encryptedKey = parser.getValue( "//ec2_encrypted_key" );
    String encryptedIV = parser.getValue( "//ec2_encrypted_iv" );
    String signature = parser.getValue( "//signature" );
    String image = parser.getXML( "image" );
    String machineConfiguration = parser.getXML( "machine_configuration" );
    String pad = (machineConfiguration + image);
    try {
      User user = Accounts.lookupUserById( imgInfo.getImageOwnerId( ) );
      for ( Certificate cert : user.getCertificates( ) ) {
        X509Certificate x509 = (X509Certificate) cert;
        if ( cert != null && cert instanceof X509Certificate && ImageUtil.verifyManifestSignature( x509, signature, pad  )) {
          return true;
        }
      }
      if ( ImageUtil.verifyManifestSignature( SystemCredentialProvider.getCredentialProvider(Eucalyptus.class).getCertificate(), signature, pad  )) {
        return true;
      }
      try {
        for ( User u : Accounts.listAllUsers( ) ) {
          for ( Certificate cert : u.getCertificates( ) ) {
            X509Certificate x509 = (X509Certificate) cert;
            if ( cert != null && cert instanceof X509Certificate && ImageUtil.verifyManifestSignature( x509, signature, pad  )) {
              return true;
            }
          }
        }
      } catch ( AuthException e ) {
        throw new EucalyptusCloudException( "Can't get user certificates", e );
      }
      return false;
    } catch ( AuthException e ) {
      throw new EucalyptusCloudException( "Invalid Manifest: Failed to verify signature because of missing (deleted?) user certificate.", e );
    }
  }
  
}
