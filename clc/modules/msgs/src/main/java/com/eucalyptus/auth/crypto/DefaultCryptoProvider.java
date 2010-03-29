package com.eucalyptus.auth.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;
import org.bouncycastle.util.encoders.UrlBase64;

public class DefaultCryptoProvider implements CryptoProvider {
  
  static {
    CryptoProviders.setProvider( new DefaultCryptoProvider( ) );
  }
  
  private DefaultCryptoProvider( ) {}
  
  /**
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateHashedPassword(java.lang.String)
   */
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
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateQueryId(java.lang.String)
   */
  public String generateQueryId( String userName ) {
    return this.getDigestBase64( userName, Digest.MD5, false ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSecretKey(java.lang.String)
   */
  public String generateSecretKey( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA224, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateCertificateCode(java.lang.String)
   */
  public String generateCertificateCode( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA512, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateConfirmationCode(java.lang.String)
   */
  public String generateConfirmationCode( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA512, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSessionToken(java.lang.String)
   */
  public String generateSessionToken( String userName ) {
    return this.getDigestBase64( userName, Digest.SHA512, true ).replaceAll( "\\p{Punct}", "" );
  }
  
  /**
   * @see com.eucalyptus.auth.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.auth.crypto.Digest, boolean)
   */
  public String getDigestBase64( String input, Digest hash, boolean randomize ) {
    byte[] inputBytes = input.getBytes( );
    byte[] digestBytes = null;
    MessageDigest digest = hash.get( );
    digest.update( inputBytes );
    if ( randomize ) {
      SecureRandom random = new SecureRandom( );
      random.setSeed( System.currentTimeMillis( ) );
      byte[] randomBytes = random.generateSeed( inputBytes.length );
      digest.update( randomBytes );
    }
    digestBytes = digest.digest( );
    return new String( UrlBase64.encode( digestBytes ) );
  }
  
}
