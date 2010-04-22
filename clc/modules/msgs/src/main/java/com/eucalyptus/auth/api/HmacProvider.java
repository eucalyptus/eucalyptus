package com.eucalyptus.auth.api;


public interface HmacProvider extends BaseSecurityProvider  {
  /**
   * TODO: DOCUMENT CryptoProvider.java
   * @param userName
   * @return
   */
  public abstract String generateQueryId( String userName );
  public abstract String generateSecretKey( String userName );

  
  /**
   * TODO: DOCUMENT CryptoProvider.java
   * @return
   */
  public abstract String generateSystemSignature( );
  public abstract String generateSystemToken( byte[] data );

}
