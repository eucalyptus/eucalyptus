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

import java.util.List;
import com.eucalyptus.auth.AuthException;

/**
 * Interface for principal with HMAC secret keys.
 */
public interface HmacPrincipal extends CredentialPrincipal {

  /**
   * @param id The ID of the key to get.
   * @return The secret key with id.
   */
  public String getSecretKey( String id );

  /**
   * Add a new key.
   * 
   * @param key The new key to add.
   */
  public void addSecretKey( String key ) throws AuthException;

  /**
   * Activate a secret key.
   * 
   * @param id The ID of the key.
   */
  public void activateSecretKey( String id ) throws AuthException;
  
  /**
   * Deactivate a secret key.
   * @param id The ID of the key.
   */
  public void deactivateSecretKey( String id ) throws AuthException;

  /**
   * Revoke a secret key.
   * @param id The ID of the key.
   */
  public void revokeSecretKey( String id ) throws AuthException;
  
  /**
   * Lookup the ID of a key.
   * @param key The key to lookup
   * @return the ID of the key.
   */
  public String lookupSecretKeyId( String key );
  
  /**
   * Get the first active secret key's ID.
   * @return The ID of the found key.
   */
  public String getFirstActiveSecretKeyId( );

  /**
   * Get active secret key IDs.
   * 
   * @return the list secret key IDs.
   */
  public List<String> getActiveSecretKeyIds( );
  
  /**
   * Get inactive secret key IDs.
   * 
   * @return the list of secret key IDs.
   */
  public List<String> getInactiveSecretKeyIds( );
  
}
