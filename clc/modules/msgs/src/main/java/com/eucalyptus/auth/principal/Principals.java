/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.auth.principal;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Principals {
  
  private static final String  SYSTEM_ID      = Account.SYSTEM_ACCOUNT;
  private static final String  NOBODY_ID      = Account.NOBODY_ACCOUNT;
  private static final Account NOBODY_ACCOUNT = new Account( ) {
                                                @Override
                                                public String getAccountNumber( ) {
                                                  return String.format( "%012d", NOBODY_ACCOUNT_ID );
                                                }
                                                
                                                @Override
                                                public String getName( ) {
                                                  return NOBODY_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public void setName( String name ) throws AuthException {}
                                                
                                                @Override
                                                public List<User> getUsers( ) throws AuthException {
                                                  return Lists.newArrayList( Principals.nobodyUser( ) );
                                                }
                                                
                                                @Override
                                                public List<Group> getGroups( ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                };
                                                
                                                @Override
                                                public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException {
                                                  throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                }
                                                
                                                @Override
                                                public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {}
                                                
                                                @Override
                                                public Group addGroup( String groupName, String path ) throws AuthException {
                                                  throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                }
                                                
                                                @Override
                                                public void deleteGroup( String groupName, boolean recursive ) throws AuthException {}
                                                
                                                @Override
                                                public Group lookupGroupByName( String groupName ) throws AuthException {
                                                  throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                }
                                                
                                                @Override
                                                public User lookupUserByName( String userName ) throws AuthException {
                                                  if ( Principals.nobodyUser( ).getName( ).equals( userName ) ) {
                                                    return Principals.nobodyUser( );
                                                  } else {
                                                    throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                  }
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                              };
  
  private static final Account SYSTEM_ACCOUNT = new Account( ) {
                                                @Override
                                                public String getAccountNumber( ) {
                                                  return String.format( "%012d", SYSTEM_ACCOUNT_ID );
                                                }
                                                
                                                @Override
                                                public String getName( ) {
                                                  return Account.SYSTEM_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public void setName( String name ) throws AuthException {}
                                                
                                                @Override
                                                public List<User> getUsers( ) throws AuthException {
                                                  return Lists.newArrayList( Principals.systemUser( ) );
                                                }
                                                
                                                @Override
                                                public List<Group> getGroups( ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                };
                                                
                                                @Override
                                                public User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info ) throws AuthException {
                                                  throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                }
                                                
                                                @Override
                                                public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {}
                                                
                                                @Override
                                                public Group addGroup( String groupName, String path ) throws AuthException {
                                                  throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                }
                                                
                                                @Override
                                                public void deleteGroup( String groupName, boolean recursive ) throws AuthException {}
                                                
                                                @Override
                                                public Group lookupGroupByName( String groupName ) throws AuthException {
                                                  throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                }
                                                
                                                @Override
                                                public User lookupUserByName( String userName ) throws AuthException {
                                                  if ( Principals.systemUser( ).getName( ).equals( userName ) ) {
                                                    return Principals.systemUser( );
                                                  } else {
                                                    throw new AuthException( AuthException.SYSTEM_MODIFICATION );
                                                  }
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupAccountGlobalAuthorizations( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupAccountGlobalQuotas( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                              };
  
  private static final User    SYSTEM_USER    = new User( ) {
                                                private final Certificate       cert  = new Certificate( ) {
                                                                                        @Override
                                                                                        public Boolean isActive( ) {
                                                                                          return true;
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setActive( Boolean active ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public Boolean isRevoked( ) {
                                                                                          return false;
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setRevoked( Boolean revoked ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public String getPem( ) {
                                                                                          return B64.url.encString( PEMFiles.getBytes( getX509Certificate( ) ) );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public X509Certificate getX509Certificate( ) {
                                                                                          return SystemCredentials.lookup( Eucalyptus.class ).getCertificate( );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setX509Certificate( X509Certificate x509 ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public Date getCreateDate( ) {
                                                                                          return SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ).getNotBefore( );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setCreateDate( Date createDate ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public User getUser( ) throws AuthException {
                                                                                          return Principals.systemUser( );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public String getCertificateId( ) {
                                                                                          return SYSTEM_ID;
                                                                                        }
                                                                                      };
                                                private final List<Certificate> certs = new ArrayList<Certificate>( ) {
                                                                                        {
                                                                                          add( cert );
                                                                                        }
                                                                                      };
                                                
                                                @Override
                                                public String getUserId( ) {
                                                  return Account.SYSTEM_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public String getName( ) {
                                                  return Account.SYSTEM_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public String getPath( ) {
                                                  return "/";
                                                }
                                                
                                                @Override
                                                public User.RegistrationStatus getRegistrationStatus( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public Boolean isEnabled( ) {
                                                  return true;
                                                }
                                                
                                                @Override
                                                public String getToken( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public String getConfirmationCode( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public String getPassword( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public Long getPasswordExpires( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public String getInfo( String key ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public Map<String, String> getInfo( ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public List<AccessKey> getKeys( ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public AccessKey getKey( String keyId ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public AccessKey createKey( ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public List<Certificate> getCertificates( ) throws AuthException {
                                                  return certs;
                                                }
                                                
                                                @Override
                                                public Certificate getCertificate( String certificateId ) throws AuthException {
                                                  return cert;
                                                }
                                                
                                                @Override
                                                public Certificate addCertificate( X509Certificate certificate ) throws AuthException {
                                                  return cert;
                                                }
                                                
                                                @Override
                                                public List<Group> getGroups( ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public Account getAccount( ) throws AuthException {
                                                  return systemAccount( );
                                                }
                                                
                                                @Override
                                                public boolean isSystemAdmin( ) {
                                                  return true;
                                                }
                                                                                                
                                                @Override
                                                public boolean isAccountAdmin( ) {
                                                  return true;
                                                }
                                                
                                                @Override
                                                public List<Policy> getPolicies( ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupAuthorizations( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupQuotas( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public void setName( String name ) throws AuthException {}
                                                
                                                @Override
                                                public void setPath( String path ) throws AuthException {}
                                                
                                                @Override
                                                public void setRegistrationStatus( User.RegistrationStatus stat ) throws AuthException {}
                                                
                                                @Override
                                                public void setEnabled( Boolean enabled ) throws AuthException {}
                                                
                                                @Override
                                                public void setToken( String token ) throws AuthException {}
                                                
                                                @Override
                                                public String resetToken( ) throws AuthException { return null; }
                                                
                                                @Override
                                                public void setConfirmationCode( String code ) throws AuthException {}
                                                
                                                @Override
                                                public void createConfirmationCode( ) throws AuthException {}
                                                
                                                @Override
                                                public void setPassword( String password ) throws AuthException {}
                                                
                                                @Override
                                                public void createPassword( ) throws AuthException {}
                                                
                                                @Override
                                                public void setPasswordExpires( Long time ) throws AuthException {}
                                                
                                                @Override
                                                public void setInfo( String key, String value ) throws AuthException {}
                                                
                                                @Override
                                                public void setInfo( Map<String, String> newInfo ) throws AuthException {}
                                                
                                                @Override
                                                public void removeKey( String keyId ) throws AuthException {}
                                                
                                                @Override
                                                public void removeCertificate( String certficateId ) throws AuthException {}
                                                
                                                @Override
                                                public void removePolicy( String name ) throws AuthException {}

                                                @Override
                                                public void removeInfo(String key) throws AuthException {}
                                              };
  
  private static final User    NOBODY_USER    = new User( ) {
                                                private final Certificate       cert  = new Certificate( ) {
                                                                                        
                                                                                        @Override
                                                                                        public Boolean isActive( ) {
                                                                                          return true;
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setActive( Boolean active ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public Boolean isRevoked( ) {
                                                                                          return null;
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setRevoked( Boolean revoked ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public String getPem( ) {
                                                                                          return B64.url.encString( PEMFiles.getBytes( getX509Certificate( ) ) );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public X509Certificate getX509Certificate( ) {
                                                                                          return SystemCredentials.lookup( Eucalyptus.class ).getCertificate( );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setX509Certificate( X509Certificate x509 ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public Date getCreateDate( ) {
                                                                                          return null;
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public void setCreateDate( Date createDate ) throws AuthException {}
                                                                                        
                                                                                        @Override
                                                                                        public User getUser( ) throws AuthException {
                                                                                          return Principals.nobodyUser( );
                                                                                        }
                                                                                        
                                                                                        @Override
                                                                                        public String getCertificateId( ) {
                                                                                          return Principals.NOBODY_ID;
                                                                                        }
                                                                                      };
                                                private final List<Certificate> certs = new ArrayList<Certificate>( ) {
                                                                                        {
                                                                                          add( cert );
                                                                                        }
                                                                                      };
                                                
                                                @Override
                                                public String getUserId( ) {
                                                  return Account.NOBODY_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public String getName( ) {
                                                  return Account.NOBODY_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public String getPath( ) {
                                                  return "/";
                                                }
                                                
                                                @Override
                                                public User.RegistrationStatus getRegistrationStatus( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public Boolean isEnabled( ) {
                                                  return true;
                                                }
                                                
                                                @Override
                                                public String getToken( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public String getConfirmationCode( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public String getPassword( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public Long getPasswordExpires( ) {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public String getInfo( String key ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public Map<String, String> getInfo( ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public List<AccessKey> getKeys( ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public AccessKey getKey( String keyId ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public AccessKey createKey( ) throws AuthException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public List<Certificate> getCertificates( ) throws AuthException {
                                                  return certs;
                                                }
                                                
                                                @Override
                                                public Certificate getCertificate( String certificateId ) throws AuthException {
                                                  return cert;
                                                }
                                                
                                                @Override
                                                public Certificate addCertificate( X509Certificate certificate ) throws AuthException {
                                                  return cert;
                                                }
                                                
                                                @Override
                                                public List<Group> getGroups( ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public Account getAccount( ) throws AuthException {
                                                  return NOBODY_ACCOUNT;
                                                }
                                                
                                                @Override
                                                public boolean isSystemAdmin( ) {
                                                  return false;
                                                }
                                                
                                                @Override
                                                public boolean isAccountAdmin( ) {
                                                  return false;
                                                }
                                                
                                                @Override
                                                public List<Policy> getPolicies( ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
                                                  return null;
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupAuthorizations( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public List<Authorization> lookupQuotas( String resourceType ) throws AuthException {
                                                  return Lists.newArrayList( );
                                                }
                                                
                                                @Override
                                                public void setName( String name ) throws AuthException {}
                                                
                                                @Override
                                                public void setPath( String path ) throws AuthException {}
                                                
                                                @Override
                                                public void setRegistrationStatus( User.RegistrationStatus stat ) throws AuthException {}
                                                
                                                @Override
                                                public void setEnabled( Boolean enabled ) throws AuthException {}
                                                
                                                @Override
                                                public void setToken( String token ) throws AuthException {}
                                                
                                                @Override
                                                public String resetToken( ) throws AuthException { return null; }
                                                
                                                @Override
                                                public void setConfirmationCode( String code ) throws AuthException {}
                                                
                                                @Override
                                                public void createConfirmationCode( ) throws AuthException {}
                                                
                                                @Override
                                                public void setPassword( String password ) throws AuthException {}
                                                
                                                @Override
                                                public void createPassword( ) throws AuthException {}
                                                
                                                @Override
                                                public void setPasswordExpires( Long time ) throws AuthException {}
                                                
                                                @Override
                                                public void setInfo( String key, String value ) throws AuthException {}
                                                
                                                @Override
                                                public void setInfo( Map<String, String> newInfo ) throws AuthException {}
                                                
                                                @Override
                                                public void removeKey( String keyId ) throws AuthException {}
                                                
                                                @Override
                                                public void removeCertificate( String certficateId ) throws AuthException {}
                                                
                                                @Override
                                                public void removePolicy( String name ) throws AuthException {}

                                                @Override
                                                public void removeInfo(String key) throws AuthException {}
                                              };
  
  public static User systemUser( ) {
    return SYSTEM_USER;
  }
  
  public static User nobodyUser( ) {
    return NOBODY_USER;
  }
  
  public static Account nobodyAccount( ) {
    return NOBODY_ACCOUNT;
  }
  
  public static Account systemAccount( ) {
    return SYSTEM_ACCOUNT;
  }
  
  private static final UserFullName SYSTEM_USER_ERN = UserFullName.getInstance( systemUser( ) );
  private static final UserFullName NOBODY_USER_ERN = UserFullName.getInstance( nobodyUser( ) );
  
  public static OwnerFullName nobodyFullName( ) {
    return NOBODY_USER_ERN;
  }
  
  public static OwnerFullName systemFullName( ) {
    return SYSTEM_USER_ERN;
  }
  
  public static boolean isFakeIdentify( String id ) {
    return Sets.newHashSet( SYSTEM_USER.getUserId( ), SYSTEM_ACCOUNT.getAccountNumber( ),
                            NOBODY_ACCOUNT.getAccountNumber( ), NOBODY_USER.getUserId( ) ).contains( id );
  }
}
