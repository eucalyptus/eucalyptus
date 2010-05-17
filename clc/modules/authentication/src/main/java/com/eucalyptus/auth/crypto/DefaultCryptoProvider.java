package com.eucalyptus.auth.crypto;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.zip.Adler32;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.util.encoders.UrlBase64;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.api.CertificateProvider;
import com.eucalyptus.auth.api.CryptoProvider;
import com.eucalyptus.auth.api.HmacProvider;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.EventRecord;

public class DefaultCryptoProvider implements CryptoProvider, CertificateProvider, HmacProvider {
  public static String  KEY_ALGORITHM         = "RSA";
  public static String  KEY_SIGNING_ALGORITHM = "SHA512WithRSA";
  public static int     KEY_SIZE              = 2048;
  public static String  PROVIDER              = "BC";
  private static Logger LOG                   = Logger.getLogger( DefaultCryptoProvider.class );
  
  public DefaultCryptoProvider( ) {}
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  @Override
  public String generateHashedPassword( String password ) {
    byte[] data = Digest.MD5.get( ).digest( password.getBytes( ) );
    StringBuffer buf = new StringBuffer( );
    for ( int i = 0; i < data.length; i++ ) {
      int halfbyte = ( data[i] >>> 4 ) & 0x0F;
      int two_halfs = 0;
      do {
        if ( ( 0 <= halfbyte ) && ( halfbyte <= 9 ) )
          buf.append( ( char ) ( '0' + halfbyte ) );
        else buf.append( ( char ) ( 'a' + ( halfbyte - 10 ) ) );
        halfbyte = data[i] & 0x0F;
      } while ( two_halfs++ < 1 );
    }
    return buf.toString( ).toLowerCase( );
  }
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateQueryId(java.lang.String)
   */
  @Override
  public String generateQueryId( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA224, false ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateSecretKey(java.lang.String)
   */
  @Override
  public String generateSecretKey( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA224, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateCertificateCode(java.lang.String)
   */
  @Override
  public String generateCertificateCode( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA512, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateConfirmationCode(java.lang.String)
   */
  @Override
  public String generateConfirmationCode( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA512, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateSessionToken(java.lang.String)
   */
  @Override
  public String generateSessionToken( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA512, true ).replaceAll( "\\p{Punct}", "" );
  }

  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.auth.crypto.Digest, boolean)
   */
  @Override
  public String getDigestBase64( String input, Digest hash, boolean randomize ) {
    byte[] inputBytes = input.getBytes( );
    byte[] digestBytes = null;
    MessageDigest digest = hash.get( );
    digest.update( inputBytes );
    if ( randomize ) {
      SecureRandom random = new SecureRandom( );
//TODO: RELEASE:      random.setSeed( System.currentTimeMillis( ) );
      byte[] randomBytes = random.generateSeed( inputBytes.length );
      digest.update( randomBytes );
    }
    digestBytes = digest.digest( );
    return new String( UrlBase64.encode( digestBytes ) );
  }
  
  public X509Certificate generateServiceCertificate( KeyPair keys, String serviceName ) {
    X500Principal x500 = new X500Principal( String.format( "CN=%s, OU=Eucalyptus, O=Cloud, C=US", serviceName ) );
    SystemCredentialProvider sys = SystemCredentialProvider.getCredentialProvider( Component.eucalyptus );
//    if( sys.getCertificate( ) != null ) {
//      return generateCertificate( keys, x500, sys.getCertificate( ).getSubjectX500Principal( ), sys.getPrivateKey( ) );
//    } else {
      return generateCertificate( keys, x500, x500, null );
//    }
  }
  
  public X509Certificate generateCertificate( KeyPair keys, String userName ) {
    return generateCertificate( keys, new X500Principal( String.format( "CN=%s, OU=Eucalyptus, O=User, C=US", userName ) ) );
  }
  
  public X509Certificate generateCertificate( KeyPair keys, X500Principal dn ) {
    return generateCertificate( keys, dn, dn, null );
  }
  
  @Override
  public X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn, X500Principal signer, PrivateKey signingKey ) {
    signer = ( signingKey == null ? signer : subjectDn );
    signingKey = ( signingKey == null ? keys.getPrivate( ) : signingKey );
    EventRecord.caller( DefaultCryptoProvider.class, EventType.GENERATE_CERTIFICATE, signer.toString( ), subjectDn.toString( ) ).info();
    X509V3CertificateGenerator certGen = new X509V3CertificateGenerator( );
    certGen.setSerialNumber( BigInteger.valueOf( System.nanoTime( ) ).shiftLeft( 4 ).add( BigInteger.valueOf( ( long ) Math.rint( Math.random( ) * 1000 ) ) ) );
    certGen.setIssuerDN( signer );
    certGen.addExtension( X509Extensions.BasicConstraints, true, new BasicConstraints( true ) );
    Calendar cal = Calendar.getInstance( );
    certGen.setNotBefore( cal.getTime( ) );
    cal.add( Calendar.YEAR, 5 );
    certGen.setNotAfter( cal.getTime( ) );
    certGen.setSubjectDN( subjectDn );
    certGen.setPublicKey( keys.getPublic( ) );
    certGen.setSignatureAlgorithm( KEY_SIGNING_ALGORITHM );
    try {
      X509Certificate cert = certGen.generate( signingKey, PROVIDER );
      cert.checkValidity( );
      return cert;
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      System.exit( -3 );
      return null;
    }
  }
  
  /**
   * @see com.eucalyptus.auth.api.CryptoProvider#generateKeyPair()
   */
  @Override
  public KeyPair generateKeyPair( ) {
    KeyPairGenerator keyGen = null;
    try {
      EventRecord.caller( DefaultCryptoProvider.class, EventType.GENERATE_KEYPAIR );
      keyGen = KeyPairGenerator.getInstance( KEY_ALGORITHM, "BC" );
      SecureRandom random = new SecureRandom( );
      random.setSeed( System.currentTimeMillis( ) );
      keyGen.initialize( KEY_SIZE, random );
      KeyPair keyPair = keyGen.generateKeyPair( );
      return keyPair;
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      System.exit( -3 );
      return null;
    }
  }

  @Override
  public String generateSystemSignature( ) {
    return this.generateSystemToken( Component.eucalyptus.name( ).getBytes( ) );
  }

  @Override
  public String generateSystemToken( byte[] data ) {
    PrivateKey pk = SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getPrivateKey( );
    return Signatures.SHA256withRSA.trySign( pk, data );    
  }

  @Override
  public String generateId( final String userId, final String prefix ) {
    Adler32 hash = new Adler32( );
    String key = userId + (System.currentTimeMillis( ) * Math.random( ));
    hash.update( key.getBytes( ) );
    String imageId = String.format( "%s-%08X", prefix, hash.getValue( ) );
    return imageId;
  }

  @Override
  public String getFingerPrint( Key privKey ) {
    try {
      byte[] fp = Digest.SHA1.get( ).digest( privKey.getEncoded( ) );
      StringBuffer sb = new StringBuffer( );
      for ( byte b : fp )
        sb.append( String.format( "%02X:", b ) );
      return sb.substring( 0, sb.length( ) - 1 ).toLowerCase( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }

}
