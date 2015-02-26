/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

/**
 * The interface for a user in Eucalyptus.
 * 
 * @author decker
 */
public interface User extends AccountScopedPrincipal, Serializable {
  
  String USER_GROUP_PREFIX = "_";  
  String ACCOUNT_ADMIN = "admin";
  String ACCOUNT_NOBODY = "nobody";
  
  String EMAIL = "email";
  // LDAP user full DN
  String DN = "dn";
  // LDAP user SASL ID
  String SASLID = "saslid";
  
  enum RegistrationStatus {
    REGISTERED,
    APPROVED,
    CONFIRMED,
  }
  
  String getUserId( );
  void setName( String name ) throws AuthException;
  
  String getPath( );
  void setPath( String path ) throws AuthException;

  Date getCreateDate( );

  Boolean isEnabled( );
  void setEnabled( Boolean enabled ) throws AuthException;
  
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
  Certificate addCertificate( X509Certificate certificate ) throws AuthException;
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
