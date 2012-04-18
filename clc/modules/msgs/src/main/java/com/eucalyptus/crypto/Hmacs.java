package com.eucalyptus.crypto;



/**
 * Facade for generator methods related to hashed message authentication codes.
 * 
 * @author decker
 */
public class Hmacs {
  
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
