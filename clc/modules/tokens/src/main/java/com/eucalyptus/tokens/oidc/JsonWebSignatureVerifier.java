/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens.oidc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Pair;
import com.google.common.io.BaseEncoding;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
public class JsonWebSignatureVerifier {

  public interface KeyResolver {
    <K extends JsonWebKey> Option<K> resolve( Option<String> kid, Class<K> keyType );
  }

  public static boolean isValid(
      @Nonnull final String jsonHeaderB64,
      @Nonnull final String jsonBodyB64,
      @Nonnull final String signatureB64,
      @Nonnull final KeyResolver keyResolver,
      @Nonnull final Predicate<String> signatureAlgorithmPredicate
  ) throws GeneralSecurityException, OidcParseException {
    // decode / syntax validation
    final Pair<JoseHeader,byte[]> decoded = decode( jsonHeaderB64, jsonBodyB64, signatureB64 );
    final JoseHeader header = decoded.getLeft( );
    final byte[] signature = decoded.getRight( );

    // resolve and validate signing algorithm / key
    final Pair<JsonWebSignatureAlgorithm,JsonWebKey> resolved =
        resolve( header, keyResolver, signatureAlgorithmPredicate );
    final JsonWebSignatureAlgorithm algorithm = resolved.getLeft( );
    final JsonWebKey key = resolved.getRight( );

    // verify
    final byte [] bytesToSign = ( jsonHeaderB64 + "." + jsonBodyB64 ).getBytes( StandardCharsets.UTF_8 );
    final String sigAlgorithm = algorithm.getJcaSignatureAlgorithm( );
    final Option<String> sigProvider = algorithm.getJcaSignatureProvider( );
    final Signature jcaSignature = sigProvider.isDefined( ) ?
        Signature.getInstance( sigAlgorithm, sigProvider.get( ) ) :
        Signature.getInstance( sigAlgorithm );
    final Option<AlgorithmParameterSpec> sigAlgorithmParameterSpec =
        algorithm.getJcaSignatureAlgorithmParameterSpec( );
    if ( sigAlgorithmParameterSpec.isDefined( ) ) {
      jcaSignature.setParameter( sigAlgorithmParameterSpec.get( ) );
    }
    jcaSignature.initVerify( algorithm.publicKey( key ) );
    jcaSignature.update( bytesToSign );
    return jcaSignature.verify( algorithm.signature( signature ) );
  }

  private static Pair<JoseHeader,byte[]> decode(
      @Nonnull final String jsonHeaderB64,
      @Nonnull final String jsonBodyB64,
      @Nonnull final String signatureB64
  ) throws GeneralSecurityException, OidcParseException {
    final String jsonHeader;
    final byte [] signature;
    try {
      final BaseEncoding b64Url = BaseEncoding.base64Url( ); // allow padding?
      jsonHeader = new String( b64Url.decode( jsonHeaderB64 ), StandardCharsets.UTF_8 );
      b64Url.decode( jsonBodyB64 ); // ensures valid b64url encoding
      signature = b64Url.decode( signatureB64 );
    } catch ( final IllegalArgumentException e ) {
      throw new GeneralSecurityException( "Unable to decode", e );
    }
    final JoseHeader header = JoseHeader.parse( jsonHeader );
    if ( header.getCrit( ).map( List::size ).getOrElse( 0 ) > 0 ) {
      throw new GeneralSecurityException( "Unsupported critical extension " + header.getCrit( ) );
    }
    return Pair.pair( header, signature );
  }

  private static Pair<JsonWebSignatureAlgorithm,JsonWebKey> resolve(
      @Nonnull final JoseHeader header,
      @Nonnull final KeyResolver keyResolver,
      @Nonnull final Predicate<String> signatureAlgorithmPredicate
  ) throws GeneralSecurityException {
    final JsonWebSignatureAlgorithm algorithm =
        JsonWebSignatureAlgorithm.lookup(  header.getAlg( ) )
            .filter( alg -> signatureAlgorithmPredicate.test( alg.name( ) ) )
            .getOrElseThrow( () -> new GeneralSecurityException( "Unsupported algorithm: " + header.getAlg( ) ) );
    final JsonWebKey key =  keyResolver.resolve( header.getKid( ), algorithm.keyType( ) )
        .getOrElseThrow( () -> new GeneralSecurityException( "Signing key not found"  ) );
    final Option<String> certB64 = key.getX5c( ).flatMap( list -> Stream.ofAll( list ).headOption( ) );
    if ( certB64.isDefined( ) && !certB64.toTry( )
        .mapTry( JsonWebSignatureVerifier::certificateFromB64Der )
        .mapTry( cert -> algorithm.matches( key, cert ) )
        .getOrElseThrow( ex -> new GeneralSecurityException( "Certificate decode error", ex ) )
        ) {
      throw new GeneralSecurityException( "Certificate does not match public key material" );
    }
    return Pair.pair( algorithm, key );
  }

  private static X509Certificate certificateFromB64Der( @Nonnull final String b64 ) throws GeneralSecurityException {
    return (X509Certificate) CertificateFactory.getInstance( "X.509" )
        .generateCertificate( new ByteArrayInputStream( BaseEncoding.base64( ).decode( b64 ) ) );
  }
}
