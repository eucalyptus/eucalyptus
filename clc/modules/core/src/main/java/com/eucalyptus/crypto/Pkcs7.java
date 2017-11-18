/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.crypto;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

/**
 * Utility class for creating PKCS7 signed data.
 *
 * You can verify signatures using openssl:
 *
 *  openssl smime -verify -in PKCS7 -inform DER -content DATA -certfile CERTIFICATE -noverify
 *
 * where PKCS7 is a file containing the output from signing, DATA is the
 * optional signed data (not needed unless the signature is detached) and
 * CERTIFICATE is the optional PEM encoded certificate (not needed if the
 * certificate is included in the signed data)
 */
public class Pkcs7 {

  private static final String PROVIDER = "BC";

  public enum Option {
    /**
     * Create a detached signature, do not include the signed content
     */
    Detached,

    /**
     * Include the full certificate in the signed output
     */
    IncludeCertificate,
  }

  /**
   * Create PKCS7 signed data with the default options
   *
   * @param data The data to sign
   * @param key The key to use for signing
   * @param certificate The certificate to use for signature verification
   * @return The signed data
   * @throws Exception If an error occurs
   */
  public static byte[] sign(
      final String data,
      final PrivateKey key,
      final X509Certificate certificate
  ) throws Exception {
    return sign( data.getBytes( StandardCharsets.UTF_8 ), key, certificate, EnumSet.noneOf( Option.class ) );
  }

  /**
   * Create PKCS7 signed data with the given options
   *
   * @param data The data to sign
   * @param key The key to use for signing
   * @param certificate The certificate to use for signature verification
   * @param options Signing options
   * @return The signed data
   * @throws Exception If an error occurs
   */
  public static byte[] sign(
      final String data,
      final PrivateKey key,
      final X509Certificate certificate,
      final Set<Option> options
  ) throws Exception {
    return sign( data.getBytes( StandardCharsets.UTF_8 ), key, certificate, options );
  }

  /**
   * Create PKCS7 signed data with the given options
   *
   * @param data The data to sign
   * @param key The key to use for signing
   * @param certificate The certificate to use for signature verification
   * @param options Signing options
   * @return The signed data
   * @throws Exception If an error occurs
   */
  public static byte[] sign(
      final byte[] data,
      final PrivateKey key,
      final X509Certificate certificate,
      final Set<Option> options
  ) throws Exception {
    final CMSTypedData msg = new CMSProcessableByteArray( data );
    final ContentSigner sha1Signer = new JcaContentSignerBuilder( "SHA1with" + certificate.getPublicKey( ).getAlgorithm( ) )
        .setProvider( PROVIDER )
        .setSecureRandom( Crypto.getSecureRandomSupplier( ).get( ) )
        .build( key );
    final CMSSignedDataGenerator gen = new CMSSignedDataGenerator( );
    gen.addSignerInfoGenerator(
        new JcaSignerInfoGeneratorBuilder(
            new JcaDigestCalculatorProviderBuilder( ).setProvider( PROVIDER ).build( ) )
            .build( sha1Signer, certificate ) );

    if ( options.contains( Option.IncludeCertificate ) ) {
      final Store certs = new JcaCertStore( Collections.singleton( certificate ) );
      gen.addCertificates( certs );
    }

    final CMSSignedData sigData = gen.generate( msg, !options.contains( Option.Detached ) );

    return sigData.getEncoded();
  }

}
