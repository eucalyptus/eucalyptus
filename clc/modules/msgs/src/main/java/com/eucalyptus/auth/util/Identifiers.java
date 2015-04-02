/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
