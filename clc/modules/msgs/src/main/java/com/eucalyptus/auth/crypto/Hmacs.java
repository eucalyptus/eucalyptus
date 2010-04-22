package com.eucalyptus.auth.crypto;

import com.eucalyptus.auth.Authentication;

/**
 * Facade for generator methods related to hashed message authentication codes.
 * 
 * @author decker
 */
public class Hmacs {
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#generateQueryId(java.lang.String)
   */
  public static String generateQueryId( final String userName ) {
    return Authentication.getHmacProvider( ).generateQueryId( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#generateSecretKey(java.lang.String)
   */
  public static String generateSecretKey( final String userName ) {
    return Authentication.getHmacProvider( ).generateSecretKey( userName );
  }
  
  /**
   * TODO: DOCUMENT Hmacs.java
   * 
   * @return
   */
  public static String generateSystemSignature( ) {
    return Authentication.getHmacProvider( ).generateSystemSignature( );
  }
  public static String generateSystemToken( byte[] data ) {
    return Authentication.getHmacProvider( ).generateSystemToken( data );
  }

}
