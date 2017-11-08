/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
