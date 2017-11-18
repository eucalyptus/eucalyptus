/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.util;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ServiceLoader;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;

/**
 *
 */
public class Identifiers {

  private static final BaseEncoding identifierEncoding = BaseEncoding.base32( );

  public static String generateAccountNumber( ) {
    return getRegionAccountNumberPartition( ) + String.format( "%09d", ( long ) ( Math.pow( 10, 9 ) * Math.random( ) ) );
  }

  public static String generateIdentifier( final String prefix ) {
    return prefix + getRegionIdentifierPartition( ) + getRandomPart( );
  }

  public static String generateAccessKeyIdentifier( ) {
    return "AKI" + getRegionIdentifierPartition( ) + getRandomPart( ).substring( 1 ); // AKI is only 20 characters
  }

  public static String generateCertificateIdentifier( final X509Certificate certificate ) throws CertificateEncodingException {
    return identifierEncoding.encode( Digest.SHA1.digestBinary( certificate.getEncoded( ) ) );
  }

  private static String getRandomPart( ) {
    final byte[] random = new byte[10];
    Crypto.getSecureRandomSupplier( ).get( ).nextBytes( random );
    return identifierEncoding.encode( random );
  }

  private static String getRegionAccountNumberPartition( ) {
    return Iterables.getFirst( Partition.supplier.getAccountNumberPartitions( ), "000" );
  }

  private static String getRegionIdentifierPartition( ) {
    return Iterables.getFirst( Partition.supplier.getIdentifierPartitions( ), "AA" );
  }

  private static class Partition {
    private static final IdentifierPartitionSupplier supplier =
        Iterables.get( ServiceLoader.load( IdentifierPartitionSupplier.class ), 0 );
  }

  public static interface IdentifierPartitionSupplier {
    public Iterable<String> getAccountNumberPartitions( );
    public Iterable<String> getIdentifierPartitions( );
  }
}
