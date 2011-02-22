package com.eucalyptus.auth.principal.credential;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.AuthException;

/**
 * Interface for a principal with X509 certificates.
 * 
 * @author decker
 */
public interface X509Principal extends CredentialPrincipal, Serializable {
  
  /**
   * @param id The certificate ID.
   * @return The X509 certificate for this principal by its ID
   */
  public X509Certificate getX509Certificate( String id );
  
  /**
   * @return The full list of X509 certificates for this user, including revoked and inactive ones.
   */
  public List<X509Certificate> getAllX509Certificates( );
  
  /**
   * Add a new certificate.
   * 
   * @param cert The new certificate.
   */
  public void addX509Certificate( X509Certificate cert ) throws AuthException;
  
  /**
   * Set a certificate to be active.
   * 
   * @param id The certificate ID.
   */
  public void activateX509Certificate( String id ) throws AuthException;
  
  /**
   * Set a certificate to be inactive.
   * 
   * @param id The certificate ID.
   */
  public void deactivateX509Certificate( String id ) throws AuthException;
  
  /**
   * Revoke a certificate.
   * 
   * @param id The ID of certificate to revoke.
   */
  public void revokeX509Certificate( String id ) throws AuthException;
  
  /**
   * Lookup the ID of a certificate.
   * 
   * @param cert The certificate to lookup.
   * @return the ID of the certificate.
   */
  public String lookupX509Certificate( X509Certificate cert );
  
  /**
   * Get IDs of active certificates.
   * 
   * @return the list of IDs of the certificates.
   */
  public List<String> getActiveX509CertificateIds( );
  
  /**
   * Get IDs of inactive certificates.
   * 
   * @return the list of IDs of the certificates.
   */
  public List<String> getInactiveX509CertificateIds( );
  
}
