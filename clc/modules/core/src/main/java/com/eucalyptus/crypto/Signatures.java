/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.auth.SystemCredentials;

public enum Signatures {
  SHA256withRSA,
  SHA1WithRSA;
  private static Logger LOG = Logger.getLogger( Signatures.class );
  private static final String HEXES = "0123456789abcdef";

  public String trySign( final Class<? extends ComponentId> component, final byte[] data ) {
    return trySign( componentPk( component ), data );
  }

  /**
   * Identical to Signatures#sign() except in that it throws no checked exceptions and instead returns null in the case of a failure.
   */
  public String trySign( final PrivateKey pk, final byte[] data ) {
    try {
      return this.sign( pk, data );
    } catch ( Exception e ) {
      return null;
    }
  }

  public String sign( final PrivateKey pk, final byte[] data ) throws InvalidKeyException, SignatureException {
    return bytesToHex( signBinary( pk, data ) );
  }

  public byte[] signBinary(
      final Class<? extends ComponentId> component,
      final byte[] data
  ) throws InvalidKeyException, SignatureException {
    return signBinary( componentPk( component ), data );
  }

  public byte[] signBinary( final PrivateKey pk, final byte[] data ) throws InvalidKeyException, SignatureException {
    final Signature signer = this.getInstance( );
    signer.initSign( pk );
    try {
      signer.update( data );
      return signer.sign( );
    } catch ( SignatureException e ) {
      LOG.debug( e, e );
      throw e;
    }
  }

  public Signature getInstance( ) {
    try {
      return Signature.getInstance( this.toString( ) );
    } catch ( NoSuchAlgorithmException e ) {
      LOG.fatal( e, e );
      throw new RuntimeException( e );
    }
  }

  static String bytesToHex( final byte[] bytes ) {
    final char[] hex = new char[ bytes.length * 2 ];
    for ( int i = 0; i < bytes.length; i++ ) {
      int b = bytes[i] & 0xFF;
      hex[ i * 2 ]     = HEXES.charAt( b >>> 4 );
      hex[ i * 2 + 1 ] = HEXES.charAt( b & 0x0F );
    }
    return new String( hex );
  }

  private PrivateKey componentPk( final Class<? extends ComponentId> component ) {
    return SystemCredentials.lookup( component ).getPrivateKey( );
  }
}
