package com.eucalyptus.crypto;



/**
 * Facade for generator methods related to hashed message authentication codes.
 * 
 * @author decker
 */
public class Hmacs {
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.crypto.CryptoProvider#generateQueryId(java.lang.String)
   */
  public static String generateQueryId( final String userName ) {
    return Crypto.getHmacProvider( ).generateQueryId( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.crypto.CryptoProvider#generateSecretKey(java.lang.String)
   */
  public static String generateSecretKey( final String userName ) {
    return Crypto.getHmacProvider( ).generateSecretKey( userName );
  }
  
  /**
   * TODO: DOCUMENT Hmacs.java
   * 
   * @return
   */
  public static String generateSystemSignature( ) {
    return Crypto.getHmacProvider( ).generateSystemSignature( );
  }
  public static String generateSystemToken( byte[] data ) {
    return Crypto.getHmacProvider( ).generateSystemToken( data );
  }

}
