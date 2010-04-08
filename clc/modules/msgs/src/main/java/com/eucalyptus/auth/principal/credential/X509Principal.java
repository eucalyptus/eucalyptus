package com.eucalyptus.auth.principal.credential;

import java.security.cert.X509Certificate;

public interface X509Principal extends CredentialPrincipal, java.io.Serializable {
  /**
   * Get the authorized X509 credentials for this principal.
   */
  public abstract X509Certificate getX509Certificate( );
  /**
   * Change the authorized X509 certificate for this principal.
   */
  public abstract void setX509Certificate( X509Certificate cert );
  /**
   * Revoke the X509 credentials for this principal.
   */
  public abstract void revokeX509Certificate( );
}
