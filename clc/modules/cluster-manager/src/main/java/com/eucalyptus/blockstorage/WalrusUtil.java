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
import java.security.cert.X509Certificate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.ImageUtil;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FullName;
import com.eucalyptus.ws.client.ServiceDispatcher;
import edu.ucsb.eucalyptus.msgs.CacheImageType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;
import edu.ucsb.eucalyptus.util.XMLParser;

public class WalrusUtil {
  private static Logger LOG = Logger.getLogger( WalrusUtil.class );
  
  public static void triggerCaching( ImageMetadata.StaticDiskImage imgInfo ) {
    String[] parts = imgInfo.getManifestLocation( ).split( "/" );
    CacheImageType cache = new CacheImageType( ).regarding( Contexts.lookup( ).getRequest( ) );
    cache.setBucket( parts[0] );
    cache.setKey( parts[1] );
    ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).dispatch( cache );
  }
  
  public static void invalidate( ImageMetadata.StaticDiskImage imgInfo ) {
    String[] parts = imgInfo.getManifestLocation( ).split( "/" );
    try {
      ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).dispatch( new FlushCachedImageType( parts[0], parts[1] ) );
    } catch ( Exception e ) {}
  }
  
  public static Document getManifestData( FullName userName, String bucketName, String objectName ) throws EucalyptusCloudException {
    GetObjectResponseType reply = null;
    try {
      GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true );
//TODO:GRZE:WTF.      
//      User user = Accounts.lookupUserById( userName.getNamespace( ) );
//      msg.setUserId( user.getName( ) );
      msg.regarding( );
//      msg.setCorrelationId( Contexts.lookup( ).getRequest( ).getCorrelationId( ) );
      
      reply = ( GetObjectResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).send( msg );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
    }
    
    Document inputSource = null;
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( );
      inputSource = builder.parse( new ByteArrayInputStream( Hashes.base64decode( reply.getBase64Data( ) ).getBytes( ) ) );//OMG:WTF:GRZE:what am i doing.
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName, e );
    }
    return inputSource;
  }
  
  public static GetBucketAccessControlPolicyResponseType getBucketAcl( RegisterImageType request, String[] imagePathParts ) throws EucalyptusCloudException {
    GetBucketAccessControlPolicyType getBukkitInfo = new GetBucketAccessControlPolicyType( ).regarding( request );
    if ( getBukkitInfo != null ) {
      getBukkitInfo.setBucket( imagePathParts[0] );
      GetBucketAccessControlPolicyResponseType reply = ( GetBucketAccessControlPolicyResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).send( getBukkitInfo );
      return reply;
    }
    return null;
  }
  
  public static void verifyManifestIntegrity( User user, String imageLocation ) throws EucalyptusCloudException {
    if( true ) return;//TODO:GRZE:BUG:BUG
    String[] imagePathParts = imageLocation.split( "/" );
    GetObjectResponseType reply = null;
    GetObjectType msg = new GetObjectType( imagePathParts[0], imagePathParts[1], true, false, true ).regarding( );
    try {
      reply = ( GetObjectResponseType ) ServiceDispatcher.lookupSingle( Components.lookup( Walrus.class ) ).send( msg );
      if ( reply == null || reply.getBase64Data( ) == null ) {
        throw new EucalyptusCloudException( "No data: " + imageLocation );
      } else {
        Logs.exhaust( ).debug( "Got the manifest to verify: " );
        Logs.exhaust( ).debug( Hashes.base64decode( reply.getBase64Data( ) ) );
        if( checkManifest( user, reply.getBase64Data( ) ) ) {
          return;
        } else {
          throw new EucalyptusCloudException( "Failed to verify signature." );
        }
      }
    } catch ( EucalyptusCloudException e ) {
      LOG.error( e, e );
      LOG.debug( e );
      throw new EucalyptusCloudException( "Invalid manifest reference: " + imageLocation + " because of " + e.getMessage( ), e );
    }
  }

  private static boolean checkManifest( User user, String manifest ) throws EucalyptusCloudException {
    XMLParser parser = new XMLParser( Hashes.base64decode( manifest ) );
    String encryptedKey = parser.getValue( "//ec2_encrypted_key" );
    String encryptedIV = parser.getValue( "//ec2_encrypted_iv" );
    String signature = parser.getValue( "//signature" );
    String image = parser.getXML( "image" );
    String machineConfiguration = parser.getXML( "machine_configuration" );
    String pad = (machineConfiguration + image);
    try {
      for ( Certificate cert : user.getCertificates( ) ) {
        if ( cert != null && cert instanceof X509Certificate && ImageUtil.verifyManifestSignature( (X509Certificate) cert, signature, pad  )) {
          return true;
        }
      }
      if ( ImageUtil.verifyManifestSignature( SystemCredentials.lookup(Eucalyptus.class).getCertificate(), signature, pad  )) {
        return true;
      }
      for ( User u : Accounts.listAllUsers( ) ) {
        for ( Certificate cert : u.getCertificates( ) ) {
          if ( cert != null && cert instanceof X509Certificate && ImageUtil.verifyManifestSignature( (X509Certificate) cert, signature, pad  )) {
            return true;
          }
        }
      }
      return false;
    } catch ( AuthException e ) {
      throw new EucalyptusCloudException( "Invalid Manifest: Failed to verify signature because of missing (deleted?) user certificate.", e );
    }
  }
  
}
