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

package com.eucalyptus.crypto;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import com.google.common.base.Optional;

public enum Digest {
  GOST3411,
  Tiger,
  Whirlpool,
  MD2,
  MD4,
  MD5,
  RipeMD128,
  RipeMD160,
  RipeMD256,
  RipeMD320,
  SHA1("SHA-1"),
  SHA224("SHA-224"),
  SHA256("SHA-256"),
  SHA384("SHA-384"),
  SHA512("SHA-512");

  private final ThreadLocal<MessageDigest> threadlocalDigest = new ThreadLocal<MessageDigest>() {
    @Override
    protected MessageDigest initialValue( ) {
      final MessageDigest digest = Digest.this.get( );
      digest.reset( );
      return digest;
    }
  };

  public static Optional<Digest> forAlgorithm( final String algorithm ) {
    Optional<Digest> digest = Optional.absent( );
    for ( final Digest candidateDigest : values( ) ) {
      if ( candidateDigest.algorithm.equals( algorithm )  ) {
        digest = Optional.of( candidateDigest );
        break;
      }
    }
    return digest;
  }

  public byte[] digestBinary( final byte[] data ) {
    return threadlocalDigest.get( ).digest( data );
  }

  public byte[] digestBinary( final ByteBuffer data ) {
    final MessageDigest digest = threadlocalDigest.get( );
    digest.update( data );
    return digest.digest( );
  }

  public String digestHex( final byte[] data ) {
    return Signatures.bytesToHex( digestBinary( data ) );
  }

  public MessageDigest get( ) {
    try {
      return MessageDigest.getInstance( algorithm );
    } catch ( Exception e ) {
      e.printStackTrace( );
      return null;
    }
  }

  public String algorithm( ) {
    return algorithm;
  }

  private final String algorithm;

  private Digest() {
    this.algorithm = name();
  }

  private Digest( final String algorithm ) {
    this.algorithm = algorithm;
  }
}
