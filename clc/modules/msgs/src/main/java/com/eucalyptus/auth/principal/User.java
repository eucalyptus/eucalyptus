/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;

/**
 * The interface for a user in Eucalyptus.
 * 
 * @author decker
 *
 */
public interface User extends /*HasId, */AuthorizedPrincipal, Serializable {
  
  public static final String USER_GROUP_PREFIX = "_";  
  public static final String ACCOUNT_ADMIN = "admin";
  public static final String ACCOUNT_NOBODY = "nobody";
  
  public static final String EMAIL = "email";
  // LDAP user full DN
  public static final String DN = "dn";
  // LDAP user SASL ID
  public static final String SASLID = "saslid";
  
  public static final Long PASSWORD_LIFETIME = 1000 * 60 * 60 * 24 * 60L; // 2 months
  public static final Long RECOVERY_EXPIRATION = 1000 * 60 * 30L; // 30 minutes
  
  public static enum RegistrationStatus {
    REGISTERED,
    APPROVED,
    CONFIRMED,
  }
  
  public String getUserId( );
  public void setName( String name ) throws AuthException;
  
  public String getPath( );
  public void setPath( String path ) throws AuthException;
    
  public RegistrationStatus getRegistrationStatus( );
  public void setRegistrationStatus( RegistrationStatus stat ) throws AuthException;

  public Boolean isEnabled( );
  public void setEnabled( Boolean enabled ) throws AuthException;
  
  public String getToken( );
  public void setToken( String token ) throws AuthException;
  public String resetToken( ) throws AuthException;
  
  public String getConfirmationCode( );
  public void setConfirmationCode( String code ) throws AuthException;
  public void createConfirmationCode( ) throws AuthException;
    
  public String getPassword( );  
  public void setPassword( String password ) throws AuthException;
  public void createPassword( ) throws AuthException;
  
  public Long getPasswordExpires( );
  public void setPasswordExpires( Long time ) throws AuthException;
  
  public String getInfo( String key ) throws AuthException;
  public Map<String, String> getInfo( ) throws AuthException;
  public void setInfo( String key, String value ) throws AuthException;  
  public void setInfo( Map<String, String> newInfo ) throws AuthException;
  public void removeInfo( String key ) throws AuthException;
  
  public List<AccessKey> getKeys( ) throws AuthException;
  public AccessKey getKey( String keyId ) throws AuthException;
//  public AccessKey addKey( String key ) throws AuthException;
  public void removeKey( String keyId ) throws AuthException;
  public AccessKey createKey( ) throws AuthException;
  
  public List<Certificate> getCertificates( ) throws AuthException;
  public Certificate getCertificate( String certificateId ) throws AuthException;
  public Certificate addCertificate( X509Certificate certificate ) throws AuthException;
  public void removeCertificate( String certficateId ) throws AuthException;
  
  public List<Group> getGroups( ) throws AuthException;
  
  public boolean isSystemAdmin( );
  
  public boolean isAccountAdmin( );

}
