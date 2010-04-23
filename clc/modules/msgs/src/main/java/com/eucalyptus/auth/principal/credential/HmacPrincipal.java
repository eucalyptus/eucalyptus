package com.eucalyptus.auth.principal.credential;

public interface HmacPrincipal extends CredentialPrincipal {
  /**
   * Revoke the secret key for this principal.
   */
  public abstract void revokeSecretKey( );
  /**
   * Get the access key for this principal.
   */
  public abstract String getQueryId( );
  /**
   * Get the secret key for this principal.
   */
  public abstract String getSecretKey( );
  /**
   * Set the access key for this principal.
   */
  public abstract void setQueryId( String queryId );
  /**
   * Set the secret key for this principal.
   */
  public abstract void setSecretKey( String secretKey );
}
