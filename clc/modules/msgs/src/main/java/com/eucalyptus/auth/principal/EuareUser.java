/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.principal;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

/**
 * This will move to the euare module. Use UserPrincipal elsewhere
 *
 * @deprecated This class is temporary, do not use
 * @see com.eucalyptus.auth.principal.User
 * @see com.eucalyptus.auth.principal.UserPrincipal
 */
@Deprecated
public interface EuareUser extends User, AccountScopedPrincipal {

  void setName( String name ) throws AuthException;

  String getPath( );
  void setPath( String path ) throws AuthException;

  Date getCreateDate( );

  void setEnabled( boolean enabled ) throws AuthException;

  String getToken( );
  void setToken( String token ) throws AuthException;
  String resetToken( ) throws AuthException;

  String getPassword( );
  void setPassword( String password ) throws AuthException;

  Long getPasswordExpires( );
  void setPasswordExpires( Long time ) throws AuthException;

  String getInfo( String key ) throws AuthException;
  Map<String, String> getInfo( ) throws AuthException;
  void setInfo( String key, String value ) throws AuthException;
  void setInfo( Map<String, String> newInfo ) throws AuthException;
  void removeInfo( String key ) throws AuthException;

  List<AccessKey> getKeys( ) throws AuthException;
  AccessKey getKey( String keyId ) throws AuthException;
  void removeKey( String keyId ) throws AuthException;
  AccessKey createKey( ) throws AuthException;

  List<Certificate> getCertificates( ) throws AuthException;
  Certificate getCertificate( String certificateId ) throws AuthException;
  Certificate addCertificate( String certificateId, X509Certificate certificate ) throws AuthException;
  void removeCertificate( String certificateId ) throws AuthException;

  List<Group> getGroups( ) throws AuthException;

  List<Policy> getPolicies( ) throws AuthException;

  /**
   * Add a policy, fail if exists.
   */
  Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException;

  /**
   * Add or update the named policy.
   */
  Policy putPolicy( String name, String policy ) throws AuthException, PolicyParseException;
  void removePolicy( String name ) throws AuthException;

  boolean isSystemAdmin( );

  boolean isSystemUser( );

  boolean isAccountAdmin( );

}
