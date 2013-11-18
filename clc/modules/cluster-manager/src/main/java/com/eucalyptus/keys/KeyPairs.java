/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.keys;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.DecoderException;
import org.hibernate.criterion.Criterion;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.cloud.util.MetadataCreationException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.primitives.Ints;

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

  /**
   * List key pairs for the given owner.
   *
   * @param ownerFullName The key pair owner
   * @param filter Predicate to restrict the results
   * @param criterion The database criterion to restrict the results
   * @param aliases Aliases for the database criterion
   * @return The list of key pairs.
   * @throws NoSuchMetadataException If an error occurs
   */
  public static List<SshKeyPair> list( final OwnerFullName ownerFullName,
                                       final Predicate<? super SshKeyPair> filter,
                                       final Criterion criterion,
                                       final Map<String,String> aliases ) throws NoSuchMetadataException {
    try {
      return Transactions.filter(  SshKeyPair.named( ownerFullName, null ), filter, criterion, aliases );
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
        Logs.exhaust(  ).error( ex );
        throw new DuplicateMetadataException( "Failed to create keypair '" + keyName + "', already exists." );
    }
    return newKeys.getPrivate( );
  }

  /**
   * Decode a SSH B64 format RSA public key.
   *
   * @param publicKeyB64 The base64 formatted key
   * @return The RSA public key
   * @throws java.security.GeneralSecurityException If an error occurs
   */
  public static RSAPublicKey decodeSshRsaPublicKey(final String publicKeyB64) throws GeneralSecurityException {
    final byte[] data;
    try {
      data = B64.standard.dec( publicKeyB64 );
    } catch ( ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | DecoderException e ) {
      throw new GeneralSecurityException( "Invalid key format (expected Base64)" );
    }

    if ( data.length < 64 ) {
      throw new InvalidKeyException("Data too short");
    }

    int length = Ints.fromBytes(data[0], data[1], data[2], data[3]);
    if ( length != 7 ||
        data[ 4]!='s' ||
        data[ 5]!='s' ||
        data[ 6]!='h' ||
        data[ 7]!='-' ||
        data[ 8]!='r' ||
        data[ 9]!='s' ||
        data[10]!='a' ) {
      throw new InvalidKeyException("Not an RSA key");
    }

    int exponentLength = Ints.fromBytes( data[11], data[12], data[13], data[14] );
    if ( exponentLength <= 0 || exponentLength > 4 ) {
      throw new InvalidKeyException("Invalid RSA exponent");
    }

    int modulusLength = Ints.fromBytes(
        data[15+exponentLength],
        data[16+exponentLength],
        data[17+exponentLength],
        data[18+exponentLength]);
    if ( modulusLength <= 0 || modulusLength > 513 || data.length < 19+exponentLength+modulusLength ) {
      throw new InvalidKeyException("Invalid RSA modulus");
    }

    final BigInteger modulus = intFromBytes( data, 19+exponentLength, modulusLength );
    final BigInteger exponent = intFromBytes( data, 15, exponentLength );
    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    final RSAPublicKeySpec spec = new RSAPublicKeySpec( modulus, exponent );
    final PublicKey key = keyFactory.generatePublic( spec );
    return (RSAPublicKey) key;
  }    
  
  public static String rfc4253Format( final UserFullName userName, 
                                      final String keyName, 
                                      final RSAPublicKey publicKey ) {
    return getAuthKeyString( userName.getAccountNumber( ), keyName, publicKey );
  }

  /**
   * An MD5 SSH RSA public key fingerprint as specified in section 4 of RFC 4716
   * 
   * @param publicKey The RSA public key
   * @return The fingerprint or null if an error occurred.
   */
  public static String getPublicKeyFingerprint( final RSAPublicKey publicKey ) {
      try {
        final MessageDigest digest = Digest.MD5.get( );        
        final byte[] fingerprint = digest.digest( encodeSshRsaPublicKey(publicKey) );
        final StringBuilder sb = new StringBuilder( );
        for ( final byte b : fingerprint )
          sb.append( String.format( "%02X:", b ) );
        return sb.substring( 0, sb.length( ) - 1 ).toLowerCase( );
      } catch ( Exception e ) {
        LOG.error( "Error generating fingerprint: " + e.getMessage(), e );
        return null;
      }
  }
  
  private static String getAuthKeyString( UserFullName userName, String keyName, KeyPair newKeys ) {
    final RSAPublicKey publicKey = ( RSAPublicKey ) newKeys.getPublic( );
    return getAuthKeyString( userName.getAccountNumber( ), keyName, publicKey );
  }
  
  private static byte[] encodeSshRsaPublicKey(final RSAPublicKey publicKey) {
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

    return authKeyBlob;
  }
  
  private static String getAuthKeyString( String userDesc, String keyName, RSAPublicKey publicKey ) {
    final byte[] authKeyBlob = encodeSshRsaPublicKey(publicKey);
    return String.format("ssh-rsa %s %s@eucalyptus.%s", B64.standard.encString(authKeyBlob), userDesc, keyName);
  }

  private static BigInteger intFromBytes( byte[] data, int index, int length ) {
    final byte[] intBytes = new byte[length];
    System.arraycopy( data, index, intBytes, 0, length );
    return new BigInteger( intBytes );
  }

  public static class KeyPairFilterSupport extends FilterSupport<SshKeyPair> {
    public KeyPairFilterSupport() {
      super( builderFor( SshKeyPair.class )
          .withStringProperty( "fingerprint", FilterFunctions.FINGERPRINT )
          .withStringProperty( "key-name", CloudMetadatas.toDisplayName() )
          .withPersistenceFilter( "fingerprint", "fingerPrint" )
          .withPersistenceFilter( "key-name", "displayName" ) );
    }
  }

  private enum FilterFunctions implements Function<SshKeyPair,String> {
    FINGERPRINT {
      @Override
      public String apply( final SshKeyPair keyPair ) {
        return keyPair.getFingerPrint();
      }
    }
  }
}
