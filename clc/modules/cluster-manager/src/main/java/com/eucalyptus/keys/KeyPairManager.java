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
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
    DescribeKeyPairsResponseType reply = request.getReply( );
    Context ctx = Contexts.lookup( );
    boolean showAll = request.getKeySet( ).remove( "verbose" );
    for ( SshKeyPair kp : Iterables.filter( KeyPairs.list( ( ctx.hasAdministrativePrivileges( ) &&  showAll ) ? null : Contexts.lookup( ).getUserFullName( ).asAccountFullName( ) ), RestrictedTypes.filterPrivileged( ) ) ) {
      if ( request.getKeySet( ).isEmpty( ) || request.getKeySet( ).contains( kp.getDisplayName( ) ) ) {
        reply.getKeySet( ).add( new DescribeKeyPairsResponseItemType( kp.getDisplayName( ), kp.getFingerPrint( ) ) );
      }
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
  
  public CreateKeyPairResponseType create( final CreateKeyPairType request ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    final CreateKeyPairResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    Supplier<SshKeyPair> allocator = new Supplier<SshKeyPair>( ) {
      
      @Override
      public SshKeyPair get( ) {
        try {
          PrivateKey pk = KeyPairs.create( ctx.getUserFullName( ), request.getKeyName( ) );
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
          reply.setKeyName( request.getKeyName( ) );
          reply.setKeyMaterial( byteOut.toString( ) );
          return KeyPairs.lookup( ctx.getUserFullName( ), request.getKeyName( ) );
        } catch ( Exception ex ) {
          throw new RuntimeException( ex );
        }
      }
    };
    RestrictedTypes.allocateUnitlessResource( allocator );
    return reply;
  }
  
  public ImportKeyPairResponseType importKeyPair( final ImportKeyPairType request ) throws AuthException {
    final ImportKeyPairResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    try {
      KeyPairs.lookup( ctx.getUserFullName( ), request.getKeyName( ) );
    } catch ( Exception e1 ) {
      Supplier<SshKeyPair> allocator = new Supplier<SshKeyPair>() {

        @Override
        public SshKeyPair get( ) {
          SshKeyPair newKey = new SshKeyPair( ctx.getUserFullName( ), request.getKeyName( ) );
          newKey.setPublicKey( request.getPublicKeyMaterial( ) );
          /**
           * TODO:GRZE:OMGFIXME:RELEASE
           * Supported formats:
           * OpenSSH public key format (e.g., the format in ~/.ssh/authorized_keys)
           * Base64 encoded DER format
           * SSH public key file format as specified in RFC4716
           * 
           * DSA keys are not supported. Make sure your key generator is set up to create RSA keys.
           * Supported lengths: 1024, 2048, and 4096.
           */
          //TODO:GRZE:replace bogus initial impl.
          byte[] digest = Digest.MD5.get( ).digest( request.getPublicKeyMaterial( ).getBytes( ) );
          String fingerprint = String.format( "%032X", new BigInteger( digest ) );
          newKey.setFingerPrint( fingerprint );
          reply.setKeyName( request.getKeyName( ) );
          return newKey;
        }};
      RestrictedTypes.allocateUnitlessResource( allocator );
    }
    return reply;
  }
}
