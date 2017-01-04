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
package com.eucalyptus.auth.euare.principal;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;

/**
 *
 */
public interface EuareUser extends User, EuareAccountScopedPrincipal {

  String USER_GROUP_PREFIX = "_";

  String EMAIL = "email";
  // LDAP user full DN
  String DN = "dn";
  // LDAP user SASL ID
  String SASLID = "saslid";

  int MAX_PASSWORD_LENGTH = 128;
  
  EuareAccount getAccount( ) throws AuthException;

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
  EuareAccessKey getKey( String keyId ) throws AuthException;
  void removeKey( String keyId ) throws AuthException;
  EuareAccessKey createKey( ) throws AuthException;

  List<Certificate> getCertificates( ) throws AuthException;
  EuareCertificate getCertificate( String certificateId ) throws AuthException;
  EuareCertificate addCertificate( String certificateId, X509Certificate certificate ) throws AuthException;
  void removeCertificate( String certificateId ) throws AuthException;

  List<EuareGroup> getGroups( ) throws AuthException;

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


  List<EuareManagedPolicy> getAttachedPolicies() throws AuthException;
  void attachPolicy( EuareManagedPolicy policy ) throws AuthException;
  void detachPolicy( EuareManagedPolicy policy ) throws AuthException;

  boolean isSystemAdmin( );

  boolean isSystemUser( );

  boolean isAccountAdmin( );

}
