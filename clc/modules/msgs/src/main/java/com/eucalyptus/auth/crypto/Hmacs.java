package com.eucalyptus.auth.crypto;

import com.eucalyptus.auth.Credentials;

/**
 * Facade for generator methods related to hashed message authentication codes.
 * 
 * @author decker
 */
public class Hmacs {
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateQueryId(java.lang.String)
   */
  public static String generateQueryId( final String userName ) {
    return Credentials.getHmacProvider( ).generateQueryId( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSecretKey(java.lang.String)
   */
  public static String generateSecretKey( final String userName ) {
    return Credentials.getHmacProvider( ).generateSecretKey( userName );
  }
  
  /**
   * TODO: DOCUMENT Hmacs.java
   * 
   * @return
   */
  public static String generateSystemSignature( ) {
    return Credentials.getHmacProvider( ).generateSystemSignature( );
  }
  public static String generateSystemToken( byte[] data ) {
    return Credentials.getHmacProvider( ).generateSystemToken( data );
  }

}
