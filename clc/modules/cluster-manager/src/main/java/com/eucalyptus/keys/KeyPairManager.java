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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;

import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.encoders.DecoderException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.tags.Filter;
import com.eucalyptus.tags.Filters;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import edu.ucsb.eucalyptus.msgs.CreateKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.CreateKeyPairType;
import edu.ucsb.eucalyptus.msgs.DeleteKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteKeyPairType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsType;
import edu.ucsb.eucalyptus.msgs.ImportKeyPairResponseType;
import edu.ucsb.eucalyptus.msgs.ImportKeyPairType;

public class KeyPairManager {
  private static Logger LOG = Logger.getLogger( KeyPairManager.class );
  
  public DescribeKeyPairsResponseType describe( DescribeKeyPairsType request ) throws Exception {
    final DescribeKeyPairsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.getKeySet( ).remove( "verbose" );
    final OwnerFullName ownerFullName = ctx.hasAdministrativePrivileges( ) &&  showAll  ? null : Contexts.lookup( ).getUserFullName( ).asAccountFullName( );
    final Filter filter = Filters.generate( request.getFilterSet(), SshKeyPair.class );
    final Predicate<? super SshKeyPair> requestedAndAccessible = CloudMetadatas.filteringFor( SshKeyPair.class )
        .byId( request.getKeySet( ) )
        .byPredicate( filter.asPredicate() )
        .byPrivileges()
        .buildPredicate();
    for ( final SshKeyPair kp : KeyPairs.list( ownerFullName, requestedAndAccessible, filter.asCriterion(), filter.getAliases() ) ) {
      reply.getKeySet( ).add( new DescribeKeyPairsResponseItemType( kp.getDisplayName( ), kp.getFingerPrint( ) ) );
    }
    return reply;
  }
  
  public DeleteKeyPairResponseType delete( DeleteKeyPairType request ) throws EucalyptusCloudException {
    DeleteKeyPairResponseType reply = ( DeleteKeyPairResponseType ) request.getReply( );
    Context ctx = Contexts.lookup( );
    try {
      SshKeyPair key = KeyPairs.lookup( ctx.getUserFullName( ).asAccountFullName( ), request.getKeyName( ) );
      if ( !RestrictedTypes.filterPrivileged( ).apply( key ) ) {
        throw new EucalyptusCloudException( "Permission denied while trying to delete keypair " + key.getName( ) + " by " + ctx.getUser( ) );
      }
      KeyPairs.delete( ctx.getUserFullName( ).asAccountFullName( ), request.getKeyName( ) );
      reply.set_return( true );
    } catch ( Exception e1 ) {
      LOG.error( e1 );
      reply.set_return( false );
    }
    return reply;
  }
  
  public CreateKeyPairResponseType create( final CreateKeyPairType request ) throws AuthException, EucalyptusCloudException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    final CreateKeyPairResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final String keyName = request.getKeyName( );

    if (!CharMatcher.ASCII.matchesAllOf( keyName )) {
        throw new ClientComputeException("InvalidParameterValue", "Value ("+keyName+") for parameter KeyName is invalid. Character sets beyond ASCII are not supported.");
    }
    try{
      Supplier<SshKeyPair> allocator = new Supplier<SshKeyPair>( ) {
      
      @Override
      public SshKeyPair get( ) {
        try {
          PrivateKey pk = KeyPairs.create( ctx.getUserFullName( ), keyName );
          reply.setKeyFingerprint( Certs.getFingerPrint( pk ) );
          ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
          PEMWriter privOut = new PEMWriter( new OutputStreamWriter( byteOut ) );
          try {
            privOut.writeObject( pk );
            privOut.close( );
          } catch ( IOException e ) {
              LOG.error( e );
              throw new EucalyptusCloudException( e );
          }
          reply.setKeyName( keyName );
          reply.setKeyMaterial( byteOut.toString( ) );
          return KeyPairs.lookup( ctx.getUserFullName( ), keyName );
        } catch ( Exception ex ) {
            throw new RuntimeException( ex );
        }
      }
    };
    RestrictedTypes.allocateUnitlessResource( allocator );
    return reply;
    } catch ( final Exception ex ) {
        String cause = Exceptions.causeString( ex );
        if ( Exceptions.isCausedBy( ex, DuplicateMetadataException.class ) )
          throw new ClientComputeException( "InvalidKeyPair.Duplicate", "The keypair '" + keyName + "' already exists." );
        else
          throw new EucalyptusCloudException( "CreateKeyPair failed because: " + cause, ex );
    }
  }
  
  public ImportKeyPairResponseType importKeyPair( final ImportKeyPairType request ) throws AuthException, EucalyptusCloudException {
    final ImportKeyPairResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    boolean duplicate = false;
    try {
      KeyPairs.lookup( ctx.getUserFullName( ), request.getKeyName( ) );
      duplicate = true;
    } catch ( Exception e1 ) {
      try {
        final RSAPublicKey key = decodeKeyMaterial( request.getPublicKeyMaterial( ) );
        final String keyFingerprint = KeyPairs.getPublicKeyFingerprint( key );
        if ( keyFingerprint == null ) {
          throw new GeneralSecurityException( "Error generating fingerprint" );  
        }
        final Supplier<SshKeyPair> allocator = new Supplier<SshKeyPair>() {
          @Override
          public SshKeyPair get( ) {
            final SshKeyPair newKey = new SshKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
            newKey.setFingerPrint( keyFingerprint );
            newKey.setPublicKey( KeyPairs.rfc4253Format( ctx.getUserFullName( ), request.getKeyName( ), key ) );
            try {
              Transactions.save(newKey);
              reply.setKeyName( newKey.getDisplayName() );
              reply.setKeyFingerprint( newKey.getFingerPrint() );
            } catch ( TransactionException e ) {
              LOG.warn( "Error saving imported key", e );
            }
            return newKey;
          }};
        RestrictedTypes.allocateUnitlessResource( allocator );
      } catch (GeneralSecurityException e) {
        LOG.warn("Error importing SSH public key", e);
        throw new EucalyptusCloudException("Key import error.");
      }
    }
    if ( duplicate ) {
      throw new EucalyptusCloudException("Duplicate key '"+request.getKeyName()+"'");
    }
    return reply;
  }

  /**
   * Decode any supported key material.
   *
   * Supported formats:
   * OpenSSH public key format (e.g., the format in ~/.ssh/authorized_keys)  http://tools.ietf.org/html/rfc4253#section-6.6 
   * Base64 encoded DER format
   * SSH public key file format as specified in RFC4716
   *
   * Supported lengths: 1024, 2048, and 4096.
   * 
   * @return The RSA public key
   */
  private static RSAPublicKey decodeKeyMaterial( final String b64EncodedKeyMaterial ) throws GeneralSecurityException {
    final String decoded;
    try {
      decoded = B64.standard.decString(b64EncodedKeyMaterial);
    } catch ( ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | DecoderException e ) {
      throw new GeneralSecurityException( "Invalid key material (expected Base64)" );
    }
    final RSAPublicKey key;
    if ( decoded.startsWith( "ssh-rsa " ) ) {
      final String[] keyParts = decoded.split("\\s+");
      if ( keyParts.length > 1 ) {
        key = KeyPairs.decodeSshRsaPublicKey(keyParts[1]);
      } else {
        throw new GeneralSecurityException( "Invalid SSH key format" );
      }
    } else if ( decoded.startsWith("---- BEGIN SSH2 PUBLIC KEY ----") ) {
      final String keyB64;
      try {
        keyB64 = Joiner.on("\n").join(Iterables.filter(CharStreams.readLines(new StringReader(decoded)), new Predicate<String>() {
          boolean continueLine = false;
          boolean sawEnd = false;

          @Override
          public boolean apply(final String line) {
            // skip start / end lines
            sawEnd = sawEnd || line.contains("---- END SSH2 PUBLIC KEY ----");
            if (line.contains("---- BEGIN SSH2 PUBLIC KEY ----") || sawEnd) {
              continueLine = false;
              return false;
            }

            // skip headers and header continuations
            if (continueLine || line.contains(":")) {
              continueLine = line.endsWith("\\");
              return false;
            }

            return true;
          }
        }) );
      } catch (IOException e) {
        throw new GeneralSecurityException( "Error reading key data" );
      }
      key = KeyPairs.decodeSshRsaPublicKey(keyB64);
    } else {
      try {
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final X509EncodedKeySpec encodedSpec = new X509EncodedKeySpec( B64.standard.dec( decoded ) );
        key = (RSAPublicKey) keyFactory.generatePublic( encodedSpec );
      } catch ( ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | DecoderException e ) {
        throw new GeneralSecurityException( "Invalid key material" );
      }
    }
    
    // verify key properties
    final int keySize = key.getModulus().bitLength();
    if ( keySize != 1024 &&
         keySize != 2048 &&
         keySize != 4096 ) {
      throw new GeneralSecurityException( "Invalid key size: " + keySize );  
    }
    
    return key;
  }
}
