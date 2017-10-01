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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.apache.xml.security.algorithms.implementations.SignatureECDSA;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import io.vavr.control.Option;

/**
 *
 */
public interface JsonWebSignatureAlgorithm {

  String name( );

  String getJcaSignatureAlgorithm( );

  Option<String> getJcaSignatureProvider( );

  Option<AlgorithmParameterSpec> getJcaSignatureAlgorithmParameterSpec( );

  Class<? extends JsonWebKey> keyType( );

  <K extends JsonWebKey> PublicKey publicKey( K key ) throws GeneralSecurityException;

  boolean matches( JsonWebKey key, X509Certificate keyCertificate ) throws GeneralSecurityException;

  byte[] signature( byte[] signature ) throws GeneralSecurityException;

  static Option<JsonWebSignatureAlgorithm> lookup( final String algorithm ) {
    return Option.of( JsonWebSignatureAlgorithmRegistry.algorithmMap.get( algorithm ) );
  }

  @SuppressWarnings( "WeakerAccess" )
  abstract class JsonWebSignatureAlgorithmSupport implements JsonWebSignatureAlgorithm {
    private final String name;
    private final String jcaSignatureAlgorithm;

    protected JsonWebSignatureAlgorithmSupport(
        final String name,
        final String jcaSignatureAlgorithm
    ) {
      this.name = name;
      this.jcaSignatureAlgorithm = jcaSignatureAlgorithm;
    }

    public String name( ) {
      return name;
    }

    @Override
    public byte[] signature( final byte[] signature ) throws GeneralSecurityException {
      return signature;
    }

    @Override
    public Option<AlgorithmParameterSpec> getJcaSignatureAlgorithmParameterSpec() {
      return Option.none( );
    }

    @Override
    public String getJcaSignatureAlgorithm( ) {
      return jcaSignatureAlgorithm;
    }

    @Override
    public Option<String> getJcaSignatureProvider() {
      return Option.none( );
    }

    protected <T extends JsonWebKey> T key( final JsonWebKey key, final Class<T> keyType ) throws GeneralSecurityException {
      if (  !keyType.isInstance( key ) ) {
        throw new GeneralSecurityException( "Incorrect key type: " + key.getKty( ) + " for " + name( ) );
      }
      return keyType.cast( key );
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  abstract class EsJsonWebSignatureAlgorithmSupport extends JsonWebSignatureAlgorithmSupport {
    private final String expectedCurve;
    private final String jcaCurve;

    protected EsJsonWebSignatureAlgorithmSupport(
        final String name,
        final String jcaSignatureAlgorithm,
        final String expectedCurve, final String jcaCurve ) {
      super( name, jcaSignatureAlgorithm );
      this.expectedCurve = expectedCurve;
      this.jcaCurve = jcaCurve;
    }

    @Override
    public Class<? extends JsonWebKey> keyType( ) {
      return EcJsonWebKey.class;
    }

    public <K extends JsonWebKey> PublicKey publicKey( final K key ) throws GeneralSecurityException {
      final EcJsonWebKey webKey = key( key, EcJsonWebKey.class );
      if ( !name( ).equals( webKey.getAlg( ) ) ) {
        throw new GeneralSecurityException( "Invalid key algorithm " + webKey.getAlg( ) + " for " + name( ) );
      }
      if ( !expectedCurve.equals( webKey.getCrv( ) ) ) {
        throw new GeneralSecurityException( "Invalid curve " + webKey.getCrv( ) + " for " + name( ) );
      }
      final BigInteger x = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getX( ) ) );
      final BigInteger y = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getY( ) ) );
      final AlgorithmParameters parameters = AlgorithmParameters.getInstance( "EC" );
      parameters.init( new ECGenParameterSpec( jcaCurve ) );
      final ECParameterSpec ecParameters = parameters.getParameterSpec( ECParameterSpec.class );
      return KeyFactory.getInstance( "EC" ).generatePublic( new ECPublicKeySpec( new ECPoint( x, y ), ecParameters ) );
    }

    @Override
    public boolean matches(
        final JsonWebKey key,
        final X509Certificate keyCertificate
    ) throws GeneralSecurityException {
      final EcJsonWebKey webKey = key( key, EcJsonWebKey.class );
      final PublicKey certPublicKey = keyCertificate.getPublicKey( );
      if ( (certPublicKey instanceof ECPublicKey) ) {
        final ECPublicKey ecCertPublicKey = (ECPublicKey) certPublicKey;
        final BigInteger x = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getX( ) ) );
        final BigInteger y = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getY( ) ) );
        return ecCertPublicKey.getW( ).equals( new ECPoint( x, y ) );
      }
      return false;
    }

    @Override
    public byte[] signature( final byte[] signature ) throws GeneralSecurityException {
      try {
        return SignatureECDSA.convertXMLDSIGtoASN1( signature );
      } catch ( final IOException e ) {
        throw new GeneralSecurityException( "ECDSA ASN.1 conversion failed", e );
      }
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  abstract class RsaJsonWebSignatureAlgorithmSupport extends JsonWebSignatureAlgorithmSupport {
    protected RsaJsonWebSignatureAlgorithmSupport( final String name, final String jcaSignatureAlgorithm ) {
      super( name, jcaSignatureAlgorithm );
    }

    @Override
    public Class<? extends JsonWebKey> keyType( ) {
      return RsaJsonWebKey.class;
    }

    @Override
    public boolean matches(
        final JsonWebKey key,
        final X509Certificate keyCertificate
    ) throws GeneralSecurityException {
      final RsaJsonWebKey webKey = key( key, RsaJsonWebKey.class );
      final PublicKey certPublicKey = keyCertificate.getPublicKey( );
      if ( (certPublicKey instanceof RSAPublicKey ) ) {
        final RSAPublicKey rsaCertPublicKey = (RSAPublicKey) certPublicKey;
        final BigInteger modulus = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getN( ) ) );
        final BigInteger publicExponent = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getE( ) ) );
        return
            rsaCertPublicKey.getPublicExponent( ).equals( publicExponent ) &&
            rsaCertPublicKey.getModulus( ).equals( modulus );
      }
      return false;
    }

    public <K extends JsonWebKey> PublicKey publicKey( final K key ) throws GeneralSecurityException {
      final RsaJsonWebKey webKey = key( key, RsaJsonWebKey.class );
      final BigInteger modulus = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getN( ) ) );
      final BigInteger publicExponent = new BigInteger( 1, BaseEncoding.base64Url( ).decode( webKey.getE( ) ) );
      final int keyBits = modulus.bitLength( );
      if ( keyBits < 2048 ) {
        throw new GeneralSecurityException( "RSA key too small " +keyBits+ "bits" );
      }
      return KeyFactory.getInstance( "RSA" ).generatePublic( new RSAPublicKeySpec( modulus, publicExponent ) );
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  abstract class PsJsonWebSignatureAlgorithmSupport extends RsaJsonWebSignatureAlgorithmSupport {
    private final String psDigest;
    private final MGF1ParameterSpec mgf1ParameterSpec;
    private final int saltLen;

    protected PsJsonWebSignatureAlgorithmSupport(
        final String name,
        final String jcaSignatureAlgorithm,
        final String psDigest,
        final MGF1ParameterSpec mgf1ParameterSpec,
        final int saltLen
    ) {
      super( name, jcaSignatureAlgorithm );
      this.psDigest = psDigest;
      this.mgf1ParameterSpec = mgf1ParameterSpec;
      this.saltLen = saltLen;
    }

    @Override
    public Option<AlgorithmParameterSpec> getJcaSignatureAlgorithmParameterSpec( ) {
      return Option.some( new PSSParameterSpec( psDigest, "MGF1", mgf1ParameterSpec, saltLen, 1 ) );
    }
  }

  class Es256JsonWebSignatureAlgorithm extends EsJsonWebSignatureAlgorithmSupport {
    public Es256JsonWebSignatureAlgorithm( ) {
      super( "ES256", "SHA256withECDSA", "P-256", "secp256r1" );
    }
  }

  class Es384JsonWebSignatureAlgorithm extends EsJsonWebSignatureAlgorithmSupport {
    public Es384JsonWebSignatureAlgorithm( ) {
      super( "ES384", "SHA384withECDSA", "P-384", "secp384r1" );
    }
  }

  class Es512JsonWebSignatureAlgorithm extends EsJsonWebSignatureAlgorithmSupport {
    public Es512JsonWebSignatureAlgorithm( ) {
      super( "ES512", "SHA512withECDSA", "P-521", "secp521r1" ); // 521 is not a typo
    }
  }

  class Ps256JsonWebSignatureAlgorithm extends PsJsonWebSignatureAlgorithmSupport {
    public Ps256JsonWebSignatureAlgorithm( ) {
      super( "PS256", "SHA256withRSAandMGF1", "SHA256", MGF1ParameterSpec.SHA256, 32 );
    }
  }

  class Ps384JsonWebSignatureAlgorithm extends PsJsonWebSignatureAlgorithmSupport {
    public Ps384JsonWebSignatureAlgorithm( ) {
      super( "PS384", "SHA384withRSAandMGF1", "SHA384", MGF1ParameterSpec.SHA384, 48 );
    }
  }

  class Ps512JsonWebSignatureAlgorithm extends PsJsonWebSignatureAlgorithmSupport {
    public Ps512JsonWebSignatureAlgorithm( ) {
      super( "PS512", "SHA512withRSAandMGF1", "SHA512", MGF1ParameterSpec.SHA512, 64 );
    }
  }

  class Rs256JsonWebSignatureAlgorithm extends RsaJsonWebSignatureAlgorithmSupport {
    public Rs256JsonWebSignatureAlgorithm( ) {
      super( "RS256", "SHA256withRSA" );
    }
  }

  class Rs384JsonWebSignatureAlgorithm extends RsaJsonWebSignatureAlgorithmSupport {
    public Rs384JsonWebSignatureAlgorithm( ) {
      super( "RS384", "SHA384withRSA" );
    }
  }

  class Rs512JsonWebSignatureAlgorithm extends RsaJsonWebSignatureAlgorithmSupport {
    public Rs512JsonWebSignatureAlgorithm( ) {
      super( "RS512", "SHA512withRSA" );
    }
  }

  class JsonWebSignatureAlgorithmRegistry {
    private static final ConcurrentMap<String,JsonWebSignatureAlgorithm> algorithmMap = Maps.newConcurrentMap( );

    public static void register( final JsonWebSignatureAlgorithm algorithm ) {
      algorithmMap.putIfAbsent( algorithm.name( ), algorithm );
    }
  }

  @SuppressWarnings( "unused" )
  class JsonWebSignatureAlgorithmDiscovery extends ServiceJarDiscovery {
    private static final Logger logger = Logger.getLogger( JsonWebSignatureAlgorithmDiscovery.class );

    @Override
    public Double getPriority() {
      return 1.0d;
    }

    @Override
    public boolean processClass( final Class candidate ) {
      if ( JsonWebSignatureAlgorithm.class.isAssignableFrom( candidate ) &&
          !Modifier.isAbstract( candidate.getModifiers( ) ) &&
          Modifier.isPublic( candidate.getModifiers( ) ) ) {
        try {
          final JsonWebSignatureAlgorithm instance = (JsonWebSignatureAlgorithm) candidate.newInstance( );
          JsonWebSignatureAlgorithmRegistry.register( instance );
        } catch ( InstantiationException | IllegalAccessException e ) {
          logger.error( "Error registering JSON Web Signature algorithm class: " + candidate, e );
        }
        return true;
      }
      return false;
    }
  }
}
