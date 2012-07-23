/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.auth.principal.credential;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.AuthException;

/**
 * Interface for a principal with X509 certificates.
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
