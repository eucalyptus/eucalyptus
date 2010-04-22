package com.eucalyptus.auth.principal.credential;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author decker
 */
public interface X509Principal extends CredentialPrincipal, java.io.Serializable {
  /**
   * Get the authorized X509 credentials for this principal.
   * 
   * @return
   */
  public abstract X509Certificate getX509Certificate( );
  
  /**
   * Get the list of all certificates that have ever been associated with this user identity. In the case the underlying implementation does not record past
   * certificates this method may return a list consisting of the single currently valid certificate.
   * 
   * @return
   */
  public List<X509Certificate> getAllX509Certificates( );
  
  /**
   * Change the authorized X509 certificate for this principal.
   * 
   * @param cert
   */
  public abstract void setX509Certificate( X509Certificate cert );
  
  /**
   * Revoke the X509 credentials for this principal.
   */
  public abstract void revokeX509Certificate( );
}
