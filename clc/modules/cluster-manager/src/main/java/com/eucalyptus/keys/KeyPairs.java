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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.keys;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.cloud.util.MetadataCreationException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.OwnerFullName;

public class KeyPairs {
  private static Logger     LOG         = Logger.getLogger( KeyPairs.class );
  private static SshKeyPair NO_KEY      = SshKeyPair.noKey( );
  public static String      NO_KEY_NAME = "";
  
  public static SshKeyPair noKey( ) {
    return NO_KEY;
  }
  
  public static List<SshKeyPair> list( OwnerFullName ownerFullName ) throws NoSuchMetadataException {
    try {
      return Transactions.findAll( SshKeyPair.named( ownerFullName, null ) );
    } catch ( Exception e ) {
      throw new NoSuchMetadataException( "Failed to find key pairs for " + ownerFullName, e );
    }
  }

  public static SshKeyPair lookup( OwnerFullName ownerFullName, String keyName ) throws NoSuchMetadataException {
    try {
      return Transactions.find( new SshKeyPair( ownerFullName, keyName ) );
    } catch ( Exception e ) {
      throw new NoSuchMetadataException( "Failed to find key pair: " + keyName + " for " + ownerFullName, e );
    }
  }

  public static void delete( OwnerFullName ownerFullName, String keyName ) throws NoSuchMetadataException {
    EntityTransaction db = Entities.get( SshKeyPair.class );
    try {
      SshKeyPair entity = Entities.uniqueResult( SshKeyPair.named( ownerFullName, keyName ) );
      Entities.delete( entity );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchMetadataException( "Failed to find key pair: " + keyName + " for " + ownerFullName, ex );
    }
  }

  public static SshKeyPair fromPublicKey( OwnerFullName ownerFullName, String keyValue ) throws NoSuchMetadataException {
    try {
      return Transactions.find( SshKeyPair.withPublicKey( ownerFullName, keyValue ) );
    } catch ( Exception e ) {
      throw new NoSuchMetadataException( "Failed to find key pair with public key: " + keyValue + " for " + ownerFullName, e );
    }
    
  }
  
  public static PrivateKey create( UserFullName userName, String keyName ) throws MetadataException, TransactionException {
    SshKeyPair newKey = SshKeyPair.create( userName, keyName );
    KeyPair newKeys = null;
    try {
      newKeys = Certs.generateKeyPair( );
      String authKeyString = KeyPairs.getAuthKeyString( userName, keyName, newKeys );
      newKey.setPublicKey( authKeyString );
      newKey.setFingerPrint( Certs.getFingerPrint( newKeys.getPrivate( ) ) );
    } catch ( Exception e ) {
      throw new MetadataCreationException( "KeyPair generation error: Key pair creation failed.", e );
    }
    try {
      Transactions.save( newKey );
    } catch ( ConstraintViolationException ex ) {
      throw new DuplicateMetadataException( "Keypair already exists: " + keyName + ": " + ex.getMessage( ), ex );
    }
    return newKeys.getPrivate( );
  }
  
  private static String getAuthKeyString( UserFullName userName, String keyName, KeyPair newKeys ) {
    RSAPublicKey publicKey = ( RSAPublicKey ) newKeys.getPublic( );
    byte[] keyType = "ssh-rsa".getBytes( );
    byte[] expBlob = publicKey.getPublicExponent( ).toByteArray( );
    byte[] modBlob = publicKey.getModulus( ).toByteArray( );
    byte[] authKeyBlob = new byte[3 * 4 + keyType.length + expBlob.length + modBlob.length];
    
    byte[] lenArray = null;
    lenArray = BigInteger.valueOf( keyType.length ).toByteArray( );
    System.arraycopy( lenArray, 0, authKeyBlob, 4 - lenArray.length, lenArray.length );
    System.arraycopy( keyType, 0, authKeyBlob, 4, keyType.length );
    
    lenArray = BigInteger.valueOf( expBlob.length ).toByteArray( );
    System.arraycopy( lenArray, 0, authKeyBlob, 4 + keyType.length + 4 - lenArray.length, lenArray.length );
    System.arraycopy( expBlob, 0, authKeyBlob, 4 + ( 4 + keyType.length ), expBlob.length );
    
    lenArray = BigInteger.valueOf( modBlob.length ).toByteArray( );
    System.arraycopy( lenArray, 0, authKeyBlob, 4 + expBlob.length + 4 + keyType.length + 4 - lenArray.length, lenArray.length );
    System.arraycopy( modBlob, 0, authKeyBlob, 4 + ( 4 + expBlob.length + ( 4 + keyType.length ) ), modBlob.length );
    
    String authKeyString = String.format( "%s %s %s@eucalyptus.%s", new String( keyType ), new String( Base64.encode( authKeyBlob ) ), userName.getAccountNumber( ), keyName );
    return authKeyString;
  }

}
