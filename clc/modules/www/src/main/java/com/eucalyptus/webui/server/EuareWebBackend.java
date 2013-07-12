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

package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.login.CredentialExpiredException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PasswordAuthentication;
import com.eucalyptus.auth.Privileged;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.User.RegistrationStatus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.service.LoginUserProfile.LoginAction;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.eucalyptus.webui.shared.query.QueryType;
import com.eucalyptus.webui.shared.query.QueryValue;
import com.eucalyptus.webui.shared.query.SearchQuery;
import com.eucalyptus.webui.shared.query.SearchQuery.Matcher;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EuareWebBackend {

  private static final Logger LOG = Logger.getLogger( EuareWebBackend.class );

  // Field names
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String PATH = "path";
  public static final String USERS = "users";
  public static final String GROUPS = "groups";
  public static final String POLICIES = "policies";
  public static final String ARN = "arn";
  public static final String ACCOUNT = "account";
  public static final String USER = "user";
  public static final String GROUP = "group";
  public static final String PASSWORD = "password";
  public static final String KEY = "key";
  public static final String CERT = "cert";
  public static final String VERSION = "version";
  public static final String TEXT = "text";
  public static final String OWNER = "owner";
  public static final String ENABLED = "enabled";
  public static final String REGISTRATION = "registration";
  public static final String EXPIRATION = "expiration";
  public static final String STATUS = "status";
  public static final String CREATION = "creation";
  public static final String PEM = "pem";
  public static final String ACCOUNTID = "accountid";
  public static final String GROUPID = "groupid";
  public static final String USERID = "userid";
  public static final String OWNERID = "ownerid";
  public static final String SECRETKEY = "secretkey";
  public static final String CONFIRMATIONCODE = "confirmationcode";
  
  public static final String ACTION_CHANGE = "modify";
    
  public static final ArrayList<SearchResultFieldDesc> ACCOUNT_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( REGISTRATION, "Registration status", true, "65%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USERS, "Member users", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUPS, "Member groups", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    ACCOUNT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> GROUP_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PATH, "Path", true, "35%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", true, "30%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARN, "ARN", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNTID, "Owner account", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USERS, "Member users", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    GROUP_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> USER_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PATH, "Path", true, "25%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", true, "15%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ENABLED, "Enabled", true, "10%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( REGISTRATION, "Registration status", true, "15%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ARN, "ARN", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNTID, "Owner account", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUPS, "Membership groups", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PASSWORD, "Password", false, "0px", TableDisplay.NONE, Type.ACTION, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( EXPIRATION, "Password expires on", false, "0px", TableDisplay.NONE, Type.DATE, true, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( KEY, "Access keys", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    USER_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CERT, "X509 certificates", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> POLICY_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( VERSION, "Version", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUP, "Owner group", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USER, "Owner user", false, "35%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNERID, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    POLICY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( TEXT, "Policy text", false, "0px", TableDisplay.NONE, Type.ARTICLE, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> KEY_COMMON_FIELD_DESCS = Lists.newArrayList( );

  static {
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( SECRETKEY, "Secret key", false, "0px", TableDisplay.NONE, Type.REVEALING, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( STATUS, "Active", false, "10%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USER, "Owner user", false, "55%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CREATION, "Creation date", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    KEY_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNERID, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }

  public static final ArrayList<SearchResultFieldDesc> CERT_COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "25%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( STATUS, "Active", false, "10%", TableDisplay.MANDATORY, Type.BOOLEAN, true, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ACCOUNT, "Owner account", false, "10%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USER, "Owner user", false, "55%", TableDisplay.MANDATORY, Type.TEXT, false, true ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( CREATION, "Creation date", false, "0px", TableDisplay.NONE, Type.TEXT, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( OWNERID, "Owner", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    CERT_COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( PEM, "PEM", false, "0px", TableDisplay.NONE, Type.ARTICLE, false, false ) );
  }
  
  private static boolean authenticateWithLdap( User user ) {
    return PasswordAuthentication.authenticateWithLdap( user );
  }
  
  public static User getUser( String userName, String accountName ) throws EucalyptusServiceException {
    User user = null;
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      user = account.lookupUserByName( userName );
    } catch ( Exception e ) {
      LOG.error( "Failed to find user " + userName + "@" + accountName, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to find user " + userName + "@" + accountName + ": " + e.getMessage( ) );
    }
    return getUser( user.getUserId( ) );
  }
  
  public static User getUser( String userId ) throws EucalyptusServiceException {
    if ( userId == null ) {
      throw new EucalyptusServiceException( "Empty user ID" );
    }
    try {
      User user = Accounts.lookupUserById( userId );
      if ( !user.isEnabled( ) || !user.getRegistrationStatus( ).equals( RegistrationStatus.CONFIRMED ) ) {
        throw new EucalyptusServiceException( "User is not enabled or confirmed" );
      }
      return user;
    } catch ( EucalyptusServiceException e ) {
      LOG.debug( e, e );
      throw e;
    } catch ( Exception e ) {
      LOG.error( "Failed to verify user " + userId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to verify user " + userId + ": " + e.getMessage( ) );
    }
  }
  
  public static LoginUserProfile getLoginUserProfile( User user ) throws EucalyptusServiceException {
    try {
      String userProfileSearch = QueryBuilder.get( ).start( QueryType.user ).add( EuareWebBackend.ID, user.getUserId( ) ).query( );
      String userKeySearch = QueryBuilder.get( ).start( QueryType.key ).add( EuareWebBackend.USERID, user.getUserId( ) ).query( );
      LoginAction action = null;
      if ( !authenticateWithLdap( user ) ) {
	      if ( Crypto.verifyPassword( user.getName( ), user.getPassword( ) ) || Strings.isNullOrEmpty( user.getInfo( User.EMAIL ) ) ) {
	        action = LoginAction.FIRSTTIME;
	      } else if ( user.getPasswordExpires( ) < System.currentTimeMillis( ) ) {
	        action = LoginAction.EXPIRATION;
	      }
      }
      Account userAccount = user.getAccount( );
      return new LoginUserProfile( user.getUserId( ), user.getName( ), userAccount.getAccountNumber( ), userAccount.getName( ), userProfileSearch, userKeySearch, action );
    } catch ( Exception e ) {
      LOG.error( "Exception in retrieving user profile", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to retrieve user profile" );
    }
  }
  
  public static void checkPassword( User user, String password ) throws EucalyptusServiceException {
    try {
      PasswordAuthentication.authenticate( user, password );
    } catch ( final AuthException e ) {
      throw new EucalyptusServiceException( "Incorrect password" );
    } catch ( CredentialExpiredException ex ) {
      throw new EucalyptusServiceException( EucalyptusServiceException.EXPIRED_PASSWORD );
    }
  }

  public static User changeUserPasswordAndEmail( final User requestUser,
                                                 final String userId,
                                                 final String oldPass,
                                                 final String newPass,
                                                 final String email ) throws EucalyptusServiceException {
    try {
      final User user = Accounts.lookupUserById( userId );
      if ( authenticateWithLdap( user ) ) {
        throw new EucalyptusServiceException( "Currently authenticating with LDAP. Can not change password." );
      }
      if ( Principals.isSameUser( requestUser, user ) && Strings.nullToEmpty(oldPass).equals( newPass ) ) {
        // If user is changing their own password then the new value must not be the same as the old value
        throw new EucalyptusServiceException( "The new password must not be the same as the old password." );
      }
      // Anyone want to change some other people's password must authenticate himself first
      if ( !Crypto.verifyPassword( oldPass, requestUser.getPassword( ) ) ) {
        throw new EucalyptusServiceException( "You can not be authenticated to change user password" );
      }
      Privileged.changeUserPasswordAndEmail( requestUser, user.getAccount( ), user, newPass, email );
      return user;
    } catch ( EucalyptusServiceException e ) {
      LOG.debug( e, e );
      throw e;
    } catch ( Exception e ) {
      LOG.error( "Failed to change password for user " + userId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to change password for user " + userId + ": " + e.getMessage( ) );      
    }
  }
  
  private static boolean accountMatchQuery( final Account account, SearchQuery query ) throws Exception {
    return query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return account.getName( ) != null && account.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return account.getAccountNumber( ) != null && account.getAccountNumber( ).equals( value.getValue( ) );
      }
    } );
  }
  
  public static List<SearchResultRow> searchAccounts( User requestUser, SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      // Optimization for a single account search
      if ( query.hasOnlySingle( ID ) ) {
        Account account = Accounts.lookupAccountById( query.getSingle( ID ).getValue( ) );
        if ( Privileged.allowReadAccount( requestUser, account ) ) {
          User admin = account.lookupAdmin();
          results.add( serializeAccount( account, admin.getRegistrationStatus( ) ) );
        }
      } else {
        for ( Account account : Accounts.listAllAccounts( ) ) {
          try {
            if ( accountMatchQuery( account, query ) ) {
              if ( Privileged.allowReadAccount( requestUser, account ) ) {
                User admin = account.lookupAdmin();
                results.add( serializeAccount( account, admin.getRegistrationStatus( ) ) );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get accounts", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get accounts for query " + query + ": " + e.getMessage( ) );
    }
    return results;
  }

  private static SearchResultRow serializeAccount( Account account, RegistrationStatus registrationStatus ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( account.getAccountNumber( ) );
    result.addField( account.getName( ) );
    result.addField( registrationStatus.name( ) );
    // Search links for account fields: users, groups and policies
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ACCOUNTID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.group ).add( ACCOUNTID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.policy ).add( ACCOUNTID, account.getAccountNumber( ) ).url( ) );
    return result;
  }
  
  private static List<Account> getAccounts( SearchQuery query ) throws Exception {
    List<Account> accounts = Lists.newArrayList( );
    for ( final Account account : Accounts.listAllAccounts( ) ) {
      if ( query.match( ACCOUNT, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return account.getName( ) != null && account.getName( ).contains( value.getValue( ) );
        }
      } ) && query.match( ACCOUNTID, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return account.getAccountNumber( ) != null && account.getAccountNumber( ).equals( value.getValue( ) );
        }        
      } ) ) {
        accounts.add( account );
      }
    }
    return accounts;
  }

  private static List<User> getUsers( Account account, SearchQuery query ) throws Exception {
    List<User> users = Lists.newArrayList( );
    for ( final User user : account.getUsers( ) ) {
      if ( query.match( USER, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return user.getName( ) != null && user.getName( ).contains( value.getValue( ) );
        }
      } ) && query.match( USERID, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return user.getUserId( ) != null && user.getUserId( ).equals( value.getValue( ) );
        }        
      } ) ) {
        users.add( user );
      }
    }
    return users;
  }
  
  private static List<Group> getGroups( Account account, SearchQuery query ) throws Exception {
    List<Group> groups = Lists.newArrayList( );
    for ( final Group group : account.getGroups( ) ) {
      if ( query.match( GROUP, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return group.getName( ) != null && group.getName( ).contains( value.getValue( ) );
        }
      } ) && query.match( GROUPID, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return group.getGroupId( ) != null && group.getGroupId( ).equals( value.getValue( ) );
        }        
      } ) ) {
        groups.add( group );
      }
    }
    return groups;
  }
  
  private static boolean groupMatchQuery( final Group group, SearchQuery query ) throws Exception {
    return query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return group.getName( ) != null && group.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return group.getGroupId( ) != null && group.getGroupId( ).equals( value.getValue( ) );
      }
    } ) && query.match( PATH, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return group.getPath( ) != null && group.getPath( ).contains( value.getValue( ) );
      }      
    } ) && query.match( USER, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        try {
          return group.hasUser( value.getValue( ) );
        } catch ( Exception e ) {
          LOG.error( e, e );
          return false;
        }
      }      
    } );
  }
  
  public static List<SearchResultRow> searchGroups( User requestUser, SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( ID ) ) {
        // Optimization for a single group search
        Group group = Accounts.lookupGroupById( query.getSingle( ID ).getValue( ) );
        Account account = group.getAccount( );
        if ( Privileged.allowListGroup( requestUser, account, group ) ) {
          results.add( serializeGroup( account, group ) );
        }
      } else if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for groups of a user
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( Group group : Privileged.listGroupsForUser( requestUser, account, user ) ) {
          results.add( serializeGroup( account, group ) );
        }
      } else if ( query.hasOnlySingle( ACCOUNTID ) ) {
        // Optimization for groups of an account
        Account account = Accounts.lookupAccountById( query.getSingle( ACCOUNTID ).getValue( ) );
        for ( Group group : account.getGroups( ) ) {
          try {
            if ( !group.isUserGroup( ) ) {
              if ( Privileged.allowListGroup( requestUser, account, group ) ) {
                results.add( serializeGroup( account, group ) );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      } else {
        for ( Account account : getAccounts( query ) ) {
          try {
            for ( Group group : account.getGroups( ) ) {
              try {
                if ( !group.isUserGroup( ) && groupMatchQuery( group, query ) ) {
                  if ( Privileged.allowListGroup( requestUser, account, group ) ) {
                    results.add( serializeGroup( account, group ) );
                  }
                }
              } catch ( Exception e ) {
                LOG.error( e, e );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get groups", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get groups for query " + query + ": " + e.getMessage( ) );
    }
    return results;    
  }

  private static SearchResultRow serializeGroup( Account account, Group group ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( group.getGroupId( ) );
    result.addField( group.getName( ) );
    result.addField( group.getPath( ) );
    result.addField( account.getName( ) );
    result.addField( ( new EuareResourceName( account.getAccountNumber( ), PolicySpec.IAM_RESOURCE_GROUP, group.getPath( ), group.getName( ) ) ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.account ).add( ID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( GROUPID, group.getGroupId( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.policy ).add( GROUPID, group.getGroupId( ) ).url( ) );
    return result;
  }
  
  private static boolean userMatchQuery( final User user, SearchQuery query ) throws Exception {
    if ( !( query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getName( ) != null && user.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getUserId( ) != null && user.getUserId( ).equals( value.getValue( ) );
      }
    } ) && query.match( PATH, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getPath( ) != null && user.getPath( ).contains( value.getValue( ) );
      }
    } ) && query.match( ENABLED, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.isEnabled( ) != null && ( user.isEnabled( ).booleanValue( ) == "true".equalsIgnoreCase( value.getValue( ) ) );
      }
    } ) && query.match( REGISTRATION, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return user.getRegistrationStatus( ) != null && user.getRegistrationStatus( ).name( ).equalsIgnoreCase( value.getValue( ) );
      }
    } ) ) ) {
      return false;
    }
    
    for ( final Map.Entry<String, String> entry : user.getInfo( ).entrySet( ) ) {
      if ( !query.match( entry.getKey( ), new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          return entry.getValue( ) != null && entry.getValue( ).equalsIgnoreCase( value.getValue( ) );
        }
      } ) ) {
        return false;
      }
    }
    
    if ( query.has( GROUP ) ) {
      final Set<String> userGroups = Sets.newHashSet( );
      for ( Group g : user.getGroups( ) ) {
        userGroups.add( g.getName( ) );
      }
      if ( !query.match( GROUP, new Matcher( ) {
        @Override
        public boolean match( QueryValue value ) {
          try {
            return userGroups.contains( value.getValue( ) );
          } catch ( Exception e ) {
            LOG.error( e, e );
            return false;
          }
        }      
      } ) ) {
        return false;
      }
    }
    
    return true;
  }
    
  public static List<SearchResultRow> searchUsers( User requestUser, SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( ID ) ) {
        // Optimization for a single user search
        User user = Accounts.lookupUserById( query.getSingle( ID ).getValue( ) );
        Account account = user.getAccount( );
        if ( Privileged.allowListAndReadUser( requestUser, account, user ) ) {
          results.add( serializeUser( account, user ) );
        }
      } else if ( query.hasOnlySingle( GROUPID ) ) {
        // Optimization for users of a single group
        Group group = Accounts.lookupGroupById( query.getSingle( GROUPID ).getValue( ) );
        Account account = group.getAccount( );
        for ( User user : group.getUsers( ) ) {
          try {
            if ( Privileged.allowListAndReadUser( requestUser, account, user ) ) {
              results.add( serializeUser( account, user ) );
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      } else if ( query.hasOnlySingle( ACCOUNTID ) ) {
        // Optimization for users of a single account
        Account account = Accounts.lookupAccountById( query.getSingle( ACCOUNTID ).getValue( ) );
        for ( User user : account.getUsers( ) ) {
          try {
            if ( Privileged.allowListAndReadUser( requestUser, account, user ) ) {
              results.add( serializeUser( account, user ) );
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }        
      } else {
        for ( Account account : getAccounts( query ) ) {
          try {
            for ( User user : account.getUsers( ) ) {
              try {
                if ( userMatchQuery( user, query ) ) {
                  if ( Privileged.allowListAndReadUser( requestUser, account, user ) ) {
                    results.add( serializeUser( account, user ) );
                  }
                }
              } catch ( Exception e ) {
                LOG.error( e, e );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get users", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get users for query " + query + ": " + e.getMessage( ) );
    }
    return results;    
    
  }

  private static SearchResultRow serializeUser( Account account, User user ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( user.getUserId( ) );
    result.addField( user.getName( ) );
    result.addField( user.getPath( ) );
    result.addField( account.getName( ) );
    result.addField( user.isEnabled( ).toString( ) );
    result.addField( user.getRegistrationStatus( ).name( ) );
    result.addField( ( new EuareResourceName( account.getAccountNumber( ), PolicySpec.IAM_RESOURCE_USER, user.getPath( ), user.getName( ) ) ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.account ).add( ID, account.getAccountNumber( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.group ).add( USERID, user.getUserId( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.policy ).add( USERID, user.getUserId( ) ).url( ) );
    result.addField( ACTION_CHANGE );
    result.addField( user.getPasswordExpires( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.key ).add( USERID, user.getUserId( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.cert ).add( USERID, user.getUserId( ) ).url( ) );
    // Now the info fields
    for ( Map.Entry<String, String> entry : user.getInfo( ).entrySet( ) ) {
      result.addExtraFieldDesc( new SearchResultFieldDesc( entry.getKey( ), entry.getKey( ), false, "0px", TableDisplay.NONE, Type.KEYVAL, true, false ) );
      result.addField( entry.getValue( ) );
    }
    result.addExtraFieldDesc( new SearchResultFieldDesc( "", "Type new info here", false, "0px", TableDisplay.NONE, Type.NEWKEYVAL, true, false ) );
    result.addField( "" );
    return result;
  }
  
  private static boolean policyMatchQuery( final Policy policy, SearchQuery query ) throws Exception {
    return query.match( NAME, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getName( ) != null && policy.getName( ).contains( value.getValue( ) );
      }
    } ) && query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getPolicyId( ) != null && policy.getPolicyId( ).equals( value.getValue( ) );
      }
    } ) && query.match( VERSION, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getVersion( ) != null && policy.getVersion( ).contains( value.getValue( ) );
      }
    } ) && query.match( TEXT, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return policy.getText( ) != null && policy.getText( ).contains( value.getValue( ) );
      }
    } );
  }
  
  public static List<SearchResultRow> searchPolicies( User requestUser, SearchQuery query ) throws EucalyptusServiceException {
    if ( ( query.has( USER ) || query.has( USERID ) ) && ( query.has( GROUP ) && query.has( GROUPID ) ) ) {
      throw new EucalyptusServiceException( "Invalid policy search: can not have both user and group conditions." );
    }
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for a single user's policies
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        if ( Privileged.allowListAndReadUserPolicy( requestUser, account, user ) ) {
          for ( Policy policy : user.getPolicies( ) ) {
            results.add( serializePolicy( policy, account, null, user ) );
          }
        }
      } else if ( query.hasOnlySingle( GROUPID ) ) {
        // Optimization for a single group's policies
        Group group = Accounts.lookupGroupById( query.getSingle( GROUPID ).getValue( ) );
        Account account = group.getAccount( );
        if ( Privileged.allowReadGroupPolicy( requestUser, account, group ) ) {
          for ( Policy policy : Privileged.listGroupPolicies( requestUser, account, group ) ) {
            results.add( serializePolicy( policy, account, group, null ) );
          }
        }
      } else {
        for ( Account account : getAccounts( query ) ) {
          try {
            for ( User user : getUsers( account, query ) ) {
              try {
                if ( Privileged.allowListAndReadUserPolicy( requestUser, account, user ) ) {
                  for ( Policy policy : user.getPolicies( ) ) {
                    if ( policyMatchQuery( policy, query ) ) {
                      results.add( serializePolicy( policy, account, null, user ) );
                    }
                  }
                }
              } catch ( Exception e ) {
                LOG.error( e, e );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
          try {
            for ( Group group : getGroups( account, query ) ) {
              try {
                if ( Privileged.allowReadGroupPolicy( requestUser, account, group ) ) {
                  for ( Policy policy : Privileged.listGroupPolicies( requestUser, account, group ) ) {
                    if ( policyMatchQuery( policy, query ) ) {
                      results.add( serializePolicy( policy, account, group, null ) );
                    }
                  }
                }
              } catch ( Exception e ) {
                LOG.error( e, e );
              }
            }          
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get policies", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get policies for query " + query + ": " + e.getMessage( ) );      
    }    
    return results;
  }

  private static SearchResultRow serializePolicy( Policy policy, Account account, Group group, User user ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( policy.getPolicyId( ) );
    result.addField( policy.getName( ) );
    result.addField( policy.getVersion( ) );
    result.addField( account != null ? account.getName( ) : "" );
    result.addField( group != null ? group.getName( ) : "" );
    result.addField( user != null ? user.getName( ) : "" );
    if ( user != null ) {
      result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ID, user.getUserId( ) ).url( ) );
    } else if ( group != null ) {
      result.addField( QueryBuilder.get( ).start( QueryType.group ).add( ID, group.getGroupId( ) ).url( ) );
    } else if ( account != null ){
      result.addField( QueryBuilder.get( ).start( QueryType.account ).add( ID, account.getAccountNumber( ) ).url( ) );
    } else {
      // impossible, just to be safe
      result.addField( "" );
    }
    result.addField( policy.getText( ) );
    return result;
  }

  private static boolean certMatchQuery( final Certificate cert, SearchQuery query ) {
    return query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return cert.getCertificateId( ) != null && cert.getCertificateId( ).equals( value.getValue( ) );
      }
    } ) && query.match( STATUS, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        boolean val = "Active".equalsIgnoreCase( value.getValue( ) );
        return cert.isActive( ) != null && ( cert.isActive( ).booleanValue( ) == val );
      }
    } );
  }
  
  public static List<SearchResultRow> searchCerts( User requestUser, SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for a single user's certs
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( Certificate cert : Privileged.listSigningCertificates( requestUser, account, user ) ) {
          results.add( serializeCert( cert, account, user ) );
        }        
      } else {
        for ( Account account : getAccounts( query ) ) {
          try {
            for ( User user : getUsers( account, query ) ) {
              try {
                for ( Certificate cert : Privileged.listSigningCertificates( requestUser, account, user ) ) {
                  if ( certMatchQuery( cert, query ) ) {
                    results.add( serializeCert( cert, account, user ) );
                  }
                }
              } catch ( Exception e ) {
                LOG.error( e, e );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get certs", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get certs for query " + query + ": " + e.getMessage( ) );      
    }
    return results;
  }

  private static SearchResultRow serializeCert( Certificate cert, Account account, User user ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( cert.getCertificateId( ) );
    result.addField( cert.isActive( ).toString( ) );
    result.addField( account.getName( ) );
    result.addField( user.getName( ) );
    result.addField( cert.getCreateDate( ) == null ? "" : cert.getCreateDate( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ID, user.getUserId( ) ).url( ) );
    result.addField( B64.url.decString( cert.getPem( ) ) );
    return result;
  }

  private static boolean keyMatchQuery( final AccessKey key, SearchQuery query ) {
    return query.match( ID, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        return key.getAccessKey( ) != null && key.getAccessKey( ).equals( value.getValue( ) );
      }
    } ) && query.match( STATUS, new Matcher( ) {
      @Override
      public boolean match( QueryValue value ) {
        boolean val = "Active".equalsIgnoreCase( value.getValue( ) );
        return key.isActive( ) != null && ( key.isActive( ).booleanValue( ) == val );
      }
    } );
  }
  
  public static List<SearchResultRow> searchKeys( User requestUser, SearchQuery query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      if ( query.hasOnlySingle( USERID ) ) {
        // Optimization for a single user's keys
        User user = Accounts.lookupUserById( query.getSingle( USERID ).getValue( ) );
        Account account = user.getAccount( );
        for ( AccessKey key : Privileged.listAccessKeys( requestUser, account, user ) ) {
          results.add( serializeKey( key, account, user ) );
        }
      } else {
        for ( Account account : getAccounts( query ) ) {
          try {
            for ( User user : getUsers( account, query ) ) {
              try {
                for ( AccessKey key : Privileged.listAccessKeys( requestUser, account, user ) ) {
                  if ( keyMatchQuery( key, query ) ) {
                    results.add( serializeKey( key, account, user ) );
                  }
                }
              } catch ( Exception e ) {
                LOG.error( e, e );
              }
            }
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get keys", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get keys for query " + query + ": " + e.getMessage( ) );      
    }    
    return results;    
  }

  private static SearchResultRow serializeKey( AccessKey key, Account account, User user ) {
    SearchResultRow result = new SearchResultRow( );
    result.addField( key.getAccessKey( ) );
    result.addField( key.getSecretKey( ) );
    result.addField( key.isActive( ).toString( ) );
    result.addField( account.getName( ) );
    result.addField( user.getName( ) );
    result.addField( key.getCreateDate( ) == null ? "" : key.getCreateDate( ).toString( ) );
    result.addField( QueryBuilder.get( ).start( QueryType.user ).add( ID, user.getUserId( ) ).url( ) );
    return result;
  }

  public static String createAccount( User requestUser, String accountName, String password ) throws EucalyptusServiceException {
    try {
      Account account = Privileged.createAccount( requestUser.isSystemAdmin( ), accountName, password, null/*email*/, true/*skipRegistration*/ );
      return account.getAccountNumber( );
    } catch ( Exception e ) {
      LOG.error( "Failed to create account " + accountName, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to create account " + accountName + ": " + e.getMessage( ) );
    }
  }
  
  public static User signupAccount( String accountName, String password, String email ) throws EucalyptusServiceException {
    try {
      Account account = Privileged.createAccount( true, accountName, password, email, false/*skipRegistration*/ );
      return account.lookupAdmin();
    } catch ( Exception e ) {
      LOG.error( "Failed to signup account " + accountName, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to signup account " + accountName );
    }
  }

  public static void deleteAccounts( User requestUser, ArrayList<String> ids ) throws EucalyptusServiceException {
    // For better error message
    if ( !requestUser.isSystemAdmin( ) ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
    boolean hasError = false;
    for ( String id : ids ) {
      try { 
        Account account = Accounts.lookupAccountById( id );
        Privileged.deleteAccount( requestUser.isSystemAdmin( ), account.getName( ), true );
      } catch ( Exception e ) {
        LOG.error( "Failed to delete account " + id, e );
        LOG.debug( e, e );
        hasError = true;
      }
    }
    if ( hasError ) {
      throw new EucalyptusServiceException( "Failed to delete some accounts" );
    }
  }

  public static void modifyAccount( User requestUser, ArrayList<String> values ) throws EucalyptusServiceException {
    try {
      // deserialize
      int i = 0;
      String accountId = values.get( i++ );
      String newName = values.get( i++ );
      
      Account account = Accounts.lookupAccountById( accountId );
      Privileged.modifyAccount( requestUser, account, newName );
    } catch ( Exception e ) {
      LOG.error( "Failed to modify account " + values, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to modify account " + values + ": " + e.getMessage( ) );
    }
  }

  public static String createUser( User requestUser, String accountId, String name, String path ) {
    try {
      Account account = Accounts.lookupAccountById( accountId );
      User user = Privileged.createUser( requestUser, account, name, path );
      return user.getName( );
    } catch ( Exception e ) {
      LOG.error( "Failed to create user " + name + " in " + accountId );
      LOG.debug( e, e );
    }
    return null;
  }

  public static String createGroup( User requestUser, String accountId, String name, String path ) {
    try {
      Account account = Accounts.lookupAccountById( accountId );
      Group group = Privileged.createGroup( requestUser, account, name, path );
      return group.getName( );
    } catch ( Exception e ) {
      LOG.error( "Failed to create group " + name + " in " + accountId );
      LOG.debug( e, e );
    }
    return null;
  }
  
  public static void deleteGroups( User requestUser, ArrayList<String> ids ) throws EucalyptusServiceException {
    boolean hasError = false;
    for ( String id : ids ) {
      try { 
        Group group = Accounts.lookupGroupById( id );
        Account account = group.getAccount( );
        Privileged.deleteGroup( requestUser, account, group, true );
      } catch ( Exception e ) {
        LOG.error( "Failed to delete group " + id, e );
        LOG.debug( e, e );
        hasError = true;
      }
    }
    if ( hasError ) {
      throw new EucalyptusServiceException( "Failed to delete some groups" );
    }
  }

  public static void deleteUsers( User requestUser, ArrayList<String> ids ) throws EucalyptusServiceException {
    boolean hasError = false;
    for ( String id : ids ) {
      try { 
        User user = Accounts.lookupUserById( id );
        Privileged.deleteUser( requestUser, user.getAccount( ), user, true );
      } catch ( Exception e ) {
        LOG.error( "Failed to delete user " + id, e );
        LOG.debug( e, e );
        hasError = true;
      }
    }
    if ( hasError ) {
      throw new EucalyptusServiceException( "Failed to delete some users" );
    }    
  }

  public static void addAccountPolicy( User requestUser, String accountId, String name, String document ) throws EucalyptusServiceException {
    try {
      Account account = Accounts.lookupAccountById( accountId );
      Privileged.putAccountPolicy( requestUser.isSystemAdmin( ), account, name, document );
    } catch ( Exception e ) {
      LOG.error( "Failed to add new policy " + name + " to account " + accountId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to add policy " + name + " to account " + accountId + ": " + e.getMessage( ) );
    }
  }

  public static void addUserPolicy( User requestUser, String userId, String name, String document ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserById( userId );
      Privileged.putUserPolicy( requestUser, user.getAccount( ), user, name, document );
    } catch ( Exception e ) {
      LOG.error( "Failed to add new policy " + name + " to user " + userId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to add policy " + name + " to user " + userId + ": " + e.getMessage( ) );
    }
  }

  public static void addGroupPolicy( User requestUser, String groupId, String name, String document ) throws EucalyptusServiceException {
    try {
      Group group = Accounts.lookupGroupById( groupId );
      Privileged.putGroupPolicy( requestUser, group.getAccount( ), group, name, document );
    } catch ( Exception e ) {
      LOG.error( "Failed to add new policy " + name + " to group " + groupId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to add policy " + name + " to group " + groupId + ": " + e.getMessage( ) );
    }
  }

  public static void deletePolicy( User requestUser, SearchResultRow policySerialized ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      i++;//ID
      String policyName = policySerialized.getField( i++ );
      i++;//Version
      String accountName = policySerialized.getField( i++ );
      String groupName = policySerialized.getField( i++ );
      String userName = policySerialized.getField( i++ );
      Account account = Accounts.lookupAccountByName( accountName );
      if ( !Strings.isNullOrEmpty( userName ) ) {
        // delete user policy
        User user = account.lookupUserByName( userName );
        Privileged.deleteUserPolicy( requestUser, account, user, policyName );
      } else if ( !Strings.isNullOrEmpty( groupName ) ) {
        // delete group policy
        Group group = account.lookupGroupByName( groupName );
        Privileged.deleteGroupPolicy( requestUser, account, group, policyName );
      } else {
        // delete account policy
        Privileged.deleteAccountPolicy( requestUser.isSystemAdmin( ), account, policyName );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to delete policy " + policySerialized, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to delete policy " + policySerialized + ": " + e.getMessage( ) );      
    }
  }

  public static void deleteAccessKey( User requestUser, SearchResultRow keySerialized ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      String keyId = keySerialized.getField( i++ );
      i++;//Secret key
      i++;//Active
      String accountName = keySerialized.getField( i++ );
      String userName = keySerialized.getField( i++ );
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      Privileged.deleteAccessKey( requestUser, account, user, keyId );
    } catch ( Exception e ) {
      LOG.error( "Failed to delete key " + keySerialized, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to delete key " + keySerialized + ": " + e.getMessage( ) );      
    }
  }

  public static void deleteCertificate( User requestUser, SearchResultRow certSerialized ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      String certId = certSerialized.getField( i++ );
      i++;//Active
      String accountName = certSerialized.getField( i++ );
      String userName = certSerialized.getField( i++ );
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      Privileged.deleteSigningCertificate( requestUser, account, user, certId );
    } catch ( Exception e ) {
      LOG.error( "Failed to delete cert " + certSerialized, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to delete cert " + certSerialized + ": " + e.getMessage( ) );      
    }
  }

  public static void addUserToGroupByName( User requestUser, String userName, String groupId ) throws EucalyptusServiceException {
    try {
      Group group = Accounts.lookupGroupById( groupId );
      Account account = group.getAccount( );
      User user = account.lookupUserByName( userName );
      Privileged.addUserToGroup( requestUser, account, user, group );
    } catch ( Exception e ) {
      LOG.error( "Failed to add user " + userName + " to group " + groupId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to add user" + userName + ": " + e.getMessage( ) + ". Operation interrupted." );
    }    
  }

  public static void addUserToGroupById( User requestUser, String userId, String groupName ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserById( userId );
      Account account = user.getAccount( );
      Group group = account.lookupGroupByName( groupName );
      Privileged.addUserToGroup( requestUser, account, user, group );
    } catch ( Exception e ) {
      LOG.error( "Failed to add user " + userId + " to group " + groupName, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to add user" + userId + ": " + e.getMessage( ) + ". Operation interrupted." );
    }    
  }

  public static void removeUserFromGroupByName( User requestUser, String userName, String groupId ) throws EucalyptusServiceException {
    try {
      Group group = Accounts.lookupGroupById( groupId );
      Account account = group.getAccount( );
      User user = account.lookupUserByName( userName );
      Privileged.removeUserFromGroup( requestUser, account, user, group );
    } catch ( Exception e ) {
      LOG.error( "Failed to remove user " + userName + " from group " + groupId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to remove user" + userName + ": " + e.getMessage( ) + ". Operation interrupted." );
    }
  }

  public static void removeUserFromGroupById( User requestUser, String userId, String groupName ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserById( userId );
      Account account = user.getAccount( );
      Group group = account.lookupGroupByName( groupName );
      Privileged.removeUserFromGroup( requestUser, account, user, group );
    } catch ( Exception e ) {
      LOG.error( "Failed to remove user " + userId + " from group " + groupName, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to remove user" + userId + ": " + e.getMessage( ) + ". Operation interrupted." );
    }
  }

  public static void addAccessKey( User requestUser, String userId ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserById( userId );
      Privileged.createAccessKey( requestUser, user.getAccount( ), user );
    } catch ( Exception e ) {
      LOG.error( "Failed to create key for user " + userId, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to create key for user " + userId + ": " + e.getMessage( ) );
    }
  }

  public static void addCertificate( User requestUser, String userId, String pem ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserById( userId );
      Privileged.uploadSigningCertificate( requestUser, user.getAccount( ), user, pem );
    } catch ( Exception e ) {
      LOG.error( "Failed to add certificate to user " + userId + ": " + pem, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to add certificate to user " + userId );
    }
  }

  public static void modifyCertificate( User requestUser, ArrayList<String> values ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      String certId = values.get( i++ );
      String status = values.get( i++ );
      String accountName = values.get( i++ );
      String userName = values.get( i++ );
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      Privileged.modifySigningCertificate( requestUser, account, user, certId, "true".equalsIgnoreCase( status ) ? "Active" : "Inactive" );
    } catch ( Exception e ) {
      LOG.error( "Failed to modify cert " + values, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to modify cert " + values + ": " + e.getMessage( ) );      
    }
  }

  public static void modifyAccessKey( User requestUser, ArrayList<String> values ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      String keyId = values.get( i++ );
      i++; // secret key
      String status = values.get( i++ );
      String accountName = values.get( i++ );
      String userName = values.get( i++ );
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      Privileged.modifyAccessKey( requestUser, account, user, keyId, "true".equalsIgnoreCase( status ) ? "Active" : "Inactive" );
    } catch ( Exception e ) {
      LOG.error( "Failed to modify key " + values, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to modify key " + values + ": " + e.getMessage( ) );      
    }
  }

  public static void modifyGroup( User requestUser, ArrayList<String> values ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      String groupId = values.get( i++ );
      String groupName = values.get( i++ );
      String path = values.get( i++ );
      
      Group group = Accounts.lookupGroupById( groupId );
      String newName = group.getName( ).equals( groupName ) ? null : groupName;
      String newPath = group.getPath( ) != null && group.getPath( ).equals( path ) ? null : path;
      Privileged.modifyGroup( requestUser, group.getAccount( ), group, newName, newPath );
    } catch ( Exception e ) {
      LOG.error( "Failed to modify group " + values, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to modify group " + values + ": " + e.getMessage( ) );      
    }
  }

  public static void modifyUser( User requestUser, ArrayList<String> keys, ArrayList<String> values ) throws EucalyptusServiceException {
    try {
      // Deserialize
      int i = 0;
      String userId = values.get( i++ );
      String userName = values.get( i++ );
      String path = values.get( i++ );
      i++;//Account
      String enabled = values.get( i++ );
      i++;//Reg
      i++;//Arn
      i++;//AccountID
      i++;//Groups
      i++;//Policies
      i++;//Password
      Long expiration = Long.parseLong( values.get( i++ ) );
      i++;//Keys
      i++;//Certs
      Map<String, String> newInfo = Maps.newHashMap( );
      for ( int k = i; k < values.size( ); k++ ) {
        String key = keys.get( k );
        String value = values.get( k );
        if ( !Strings.isNullOrEmpty( key ) ) {
          newInfo.put( key, value );
        }
      }
      
      User user = Accounts.lookupUserById( userId );
      String newName = user.getName( ).equals( userName ) ? null : userName;
      String newPath = user.getPath( ) != null && user.getPath( ).equals( path ) ? null : path;
      Boolean newEnabled = user.isEnabled( ).toString( ).equalsIgnoreCase( enabled ) ? null : !user.isEnabled( );
      Long newExpiration = user.getPasswordExpires( ).equals( expiration ) ? null : expiration;
      Privileged.modifyUser( requestUser, user.getAccount( ), user, newName, newPath, newEnabled, newExpiration, newInfo );
    } catch ( Exception e ) {
      LOG.error( "Failed to modify user " + keys + " = " + values, e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to modify user " + keys + " = " + values + ": " + e.getMessage( ) );      
    }
  }

  private static String getSystemAdminEmail( ) {
    try {
      User admin = Accounts.lookupSystemAdmin( );
      return admin.getInfo( User.EMAIL );
    } catch ( Exception e ) {
      LOG.error( "Failed to get system admin", e );
      LOG.debug( e, e );
    }
    return null;
  }

  private static String getAccountAdminEmail( Account account ) {
    try {
      User admin = account.lookupAdmin();
      return admin.getInfo( User.EMAIL );
    } catch ( Exception e ) {
      LOG.error( "Failed to get account admin", e );
      LOG.debug( e, e );
    }
    return null;
  }
  
  public static void notifyAccountRegistration( User user, String accountName, String email, String backendUrl ) {
    try {
      String adminEmail = EuareWebBackend.getSystemAdminEmail( );
      if ( adminEmail == null ) {
        throw new IllegalArgumentException( "Can not find signup notification email address" );
      }
      String subject = WebProperties.getProperty( WebProperties.ACCOUNT_SIGNUP_SUBJECT, WebProperties.ACCOUNT_SIGNUP_SUBJECT_DEFAULT );
      String approveUrl = QueryBuilder.get( ).start( QueryType.approve ).add( ACCOUNT, accountName ).url( backendUrl );
      String rejectUrl = QueryBuilder.get( ).start( QueryType.reject ).add( ACCOUNT, accountName ).url( backendUrl );
      String emailMessage =
        user.getName( ) + " has requested an account on the Eucalyptus system\n" +
        "\n   Account name:  " + accountName +
        "\n   Email address: " + email +
        "\n\n" +
        "To APPROVE this request, click on the following link:\n\n   " +
        approveUrl +
        "\n\n" +
        "To REJECT this request, click on the following link:\n\n   " +
        rejectUrl +
        "\n\n";
      ServletUtils.sendMail( adminEmail, adminEmail, subject + " (" + accountName + ", " + email + ")", emailMessage);    
    } catch ( Exception e ) {
      LOG.error( "Failed to send account signup email", e );
      LOG.debug( e, e );
    }
  }

  public static ArrayList<String> processAccountSignups( User requestUser, ArrayList<String> accountNames, boolean approve, String backendUrl ) throws EucalyptusServiceException {
    if ( !requestUser.isSystemAdmin( ) ) {
      throw new EucalyptusServiceException( "Operation is not authorized" );
    }
    ArrayList<String> success = Lists.newArrayList( );
    for ( String accountName : accountNames ) {
      try {
        Account account = Accounts.lookupAccountByName( accountName );
        User admin = account.lookupAdmin();
        if ( admin.getRegistrationStatus( ).equals( RegistrationStatus.REGISTERED ) ) {
          if ( approve ) {
            admin.setRegistrationStatus( RegistrationStatus.APPROVED );
            notifyAccountApproval( admin, accountName, backendUrl );
          } else {
            notiftyAccountRejection( admin, accountName, backendUrl );
            Accounts.deleteAccount( accountName, false, true );
          }
          success.add( accountName );
        } else {
          throw new IllegalArgumentException( "Account " + accountName + " can not be approved or rejected." );
        }
      } catch ( Exception e ) {
        LOG.error( "Failed to " + ( approve ? "approve" : "reject" ) + " account " + accountName, e );
        LOG.debug( e, e );
      }
    }
    return success;
  }

  private static void notiftyAccountRejection( User admin, String accountName, String backendUrl ) throws Exception {
    String userEmail = admin.getInfo( User.EMAIL );
    if ( userEmail == null ) {
      throw new IllegalArgumentException( "Can not find email to send approval notification for account " + accountName );
    }
    String subject = WebProperties.getProperty( WebProperties.ACCOUNT_REJECTION_SUBJECT, WebProperties.ACCOUNT_REJECTION_SUBJECT_DEFAULT );
    String message = WebProperties.getProperty( WebProperties.ACCOUNT_REJECTION_MESSAGE, WebProperties.ACCOUNT_REJECTION_MESSAGE_DEFAULT );
    ServletUtils.sendMail( userEmail, userEmail, subject, message );
  }

  private static void notifyAccountApproval( User admin, String accountName, String backendUrl ) throws Exception {
    String userEmail = admin.getInfo( User.EMAIL );
    if ( userEmail == null ) {
      throw new IllegalArgumentException( "Can not find email to send approval notification for account " + accountName );
    }
    String confirmLink = QueryBuilder.get( ).start( QueryType.confirm ).add( CONFIRMATIONCODE, admin.getConfirmationCode( ) ).url( backendUrl );
    String emailMessage = "Your account '" + accountName + "' application was approved. Click the following link to login and confirm your account:" +
                          "\n\n" +
                          confirmLink +
                          "\n\n" +
                          "After you confirm your account you can login into it with 'admin' user and the password that you provided during account's application." +
                          "\n\n" +
                          "However, if you never requested a Eucalyptus account then, please, disregard this message.";
    String subject = WebProperties.getProperty( WebProperties.ACCOUNT_APPROVAL_SUBJECT, WebProperties.ACCOUNT_APPROVAL_SUBJECT_DEFAULT );
    ServletUtils.sendMail( userEmail, userEmail, subject, emailMessage );
  }

  public static ArrayList<String> processUserSignups( User requestUser, ArrayList<String> userIds, boolean approve, String backendUrl ) throws EucalyptusServiceException {
    ArrayList<String> success = Lists.newArrayList( );
    for ( String userId : userIds ) {
      try {
        User user = Accounts.lookupUserById( userId );
        if ( Privileged.allowProcessUserSignup( requestUser, user ) ) {
          if ( user.getRegistrationStatus( ).equals( RegistrationStatus.REGISTERED ) ) {
            if ( approve ) {
              user.setRegistrationStatus( RegistrationStatus.APPROVED );
              notifyUserApproval( user, backendUrl );
            } else {
              notifyUserRejection( user, backendUrl );
              Account account = user.getAccount( );
              account.deleteUser( user.getName( ), false, true );
            }
            success.add( userId );
          } else {
            throw new IllegalArgumentException( "User " + user + " can not be approved or rejected." );
          }
        }
      } catch ( Exception e ) {
        LOG.error( "Failed to " + ( approve ? "approve" : "reject" ) + " user " + userId, e );
        LOG.debug( e, e );
      }
    }
    return success;
  }

  private static void notifyUserRejection( User user, String backendUrl ) throws Exception {
    String userEmail = user.getInfo( User.EMAIL );
    if ( userEmail == null ) {
      throw new IllegalArgumentException( "Can not find email to send approval notification for user " + user );
    }
    String subject = WebProperties.getProperty( WebProperties.USER_REJECTION_SUBJECT, WebProperties.USER_REJECTION_SUBJECT_DEFAULT );
    String message = WebProperties.getProperty( WebProperties.USER_REJECTION_MESSAGE, WebProperties.USER_REJECTION_MESSAGE_DEFAULT );
    ServletUtils.sendMail( userEmail, userEmail, subject, message );
  }

  private static void notifyUserApproval( User user, String backendUrl ) throws Exception {
    String userEmail = user.getInfo( User.EMAIL );
    if ( userEmail == null ) {
      throw new IllegalArgumentException( "Can not find email to send approval notification for user " + user );
    }
    String confirmLink = QueryBuilder.get( ).start( QueryType.confirm ).add( CONFIRMATIONCODE, user.getConfirmationCode( ) ).url( backendUrl );
    String emailMessage = "You user application was approved. Click the following link to login and confirm your user account:" + 
                          "\n\n" +
                          confirmLink +
                          "\n\n" +
                          "However, if you never requested a Eucalyptus user account then, please, disregard this message.";
    String subject = WebProperties.getProperty( WebProperties.USER_APPROVAL_SUBJECT, WebProperties.USER_APPROVAL_SUBJECT_DEFAULT );
    ServletUtils.sendMail( userEmail, userEmail, subject, emailMessage );
  }

  public static User createUser( String userName, String accountName, String password, String email ) {
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      Map<String, String> info = Maps.newHashMap( );
      info.put( User.EMAIL, email );
      User user = account.addUser( userName, "/", false/*skipRegistration*/, true/*enabled*/, info );
      user.resetToken( );
      user.createConfirmationCode( );
      user.setPassword( Crypto.generateEncryptedPassword( password ) );
      user.setPasswordExpires( System.currentTimeMillis( ) + User.PASSWORD_LIFETIME );
      return user;
    } catch ( Exception e ) {
      LOG.error( "Failed to create user " + userName + " in " + accountName, e );
      LOG.debug( e, e );
      return null;
    }
  }

  public static void notifyUserRegistration( User user, String accountName, String email, String backendUrl ) {
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      String adminEmail = EuareWebBackend.getAccountAdminEmail( account );
      if ( adminEmail == null ) {
        throw new IllegalArgumentException( "Can not find signup notification email address" );
      }
      String subject = WebProperties.getProperty( WebProperties.USER_SIGNUP_SUBJECT, WebProperties.USER_SIGNUP_SUBJECT_DEFAULT );
      String approveUrl = QueryBuilder.get( ).start( QueryType.approve ).add( USERID, user.getUserId( ) ).url( backendUrl );
      String rejectUrl = QueryBuilder.get( ).start( QueryType.reject ).add( USERID, user.getUserId( ) ).url( backendUrl );
      String emailMessage =
        user.getName( ) + " has requested a user account in " + accountName + " on the Eucalyptus system\n" +
        "\n   User name:  " + user.getName( ) +
        "\n   Email address: " + email +
        "\n\n" +
        "To APPROVE this request, click on the following link:\n\n   " +
        approveUrl +
        "\n\n" +
        "To REJECT this request, click on the following link:\n\n   " +
        rejectUrl +
        "\n\n";
      ServletUtils.sendMail( adminEmail, adminEmail, subject + " (" + user.getName( ) + ", " + email + ")", emailMessage);    
    } catch ( Exception e ) {
      LOG.error( "Failed to send user signup email", e );
      LOG.debug( e, e );
    }
  }

  public static void confirmUser( String confirmationCode ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserByConfirmationCode( confirmationCode );
      if ( RegistrationStatus.APPROVED.equals( user.getRegistrationStatus( ) ) ) {
        user.setRegistrationStatus( RegistrationStatus.CONFIRMED );
        user.setConfirmationCode( null );
      } else if ( RegistrationStatus.REGISTERED.equals( user.getRegistrationStatus( ) ) ) {
        throw new IllegalArgumentException( "User " + user + " is not approved" );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to confirm user signup", e );
      LOG.debug( e , e );
      throw new EucalyptusServiceException( "Failed to confirm user or account signup" );
    }
  }

  public static void requestPasswordRecovery( String userName, String accountName, String email, String backendUrl ) {
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      if ( !user.isEnabled( ) || !RegistrationStatus.CONFIRMED.equals( user.getRegistrationStatus( ) ) ) {
        throw new IllegalArgumentException( "User is in invalid state" );
      }
      if ( email != null && email.equals( user.getInfo( User.EMAIL ) ) ) {
        long expires = System.currentTimeMillis() + User.RECOVERY_EXPIRATION;
        user.setConfirmationCode( String.format( "%015d", expires ) + Crypto.generateSessionToken() );
        // Need to make sure the confirmation code is saved
        notifyUserPasswordReset( account.lookupUserByName( userName ), backendUrl );
      } else {
        throw new IllegalArgumentException( "Invalid user email address" );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to initiate password reset for " + userName + " in " + accountName, e );
      LOG.debug( e , e );
    }
    
  }

  private static void notifyUserPasswordReset( User user, String backendUrl ) {
    try {
      String userEmail = user.getInfo( User.EMAIL );
      if ( userEmail == null ) {
        throw new IllegalArgumentException( "Empty user email address for" );
      }
      String confirmUrl = QueryBuilder.get( ).start( QueryType.reset ).add( CONFIRMATIONCODE, user.getConfirmationCode( ) ).url( backendUrl );
      String subject = WebProperties.getProperty( WebProperties.PASSWORD_RESET_SUBJECT, WebProperties.PASSWORD_RESET_SUBJECT_DEFAULT );
      String mainMessage = WebProperties.getProperty( WebProperties.PASSWORD_RESET_MESSAGE, WebProperties.PASSWORD_RESET_MESSAGE_DEFAULT );
      String emailMessage = mainMessage + 
                             "\n\n" +
                             confirmUrl +
                             "\n";

      ServletUtils.sendMail( userEmail, userEmail, subject, emailMessage );
    } catch (Exception e) {
      LOG.error( "Failed to send password reset notification for " + user, e );
      LOG.debug( e , e );
    }
  }

  public static User resetPassword( String confirmationCode, String password ) throws EucalyptusServiceException {
    try {
      User user = Accounts.lookupUserByConfirmationCode( confirmationCode );
      long expires = Long.parseLong( confirmationCode.substring( 0, 15 ) );
      long now = System.currentTimeMillis( );
      if (now > expires) {
        throw new IllegalArgumentException( "Recovery attempt expired" );
      }
      user.setConfirmationCode( null );
      user.setPassword( Crypto.generateEncryptedPassword( password ) );
      user.setPasswordExpires( System.currentTimeMillis( ) + User.PASSWORD_LIFETIME );
      return user;
    } catch ( Exception e ) {
      LOG.error( "Failed to reset password", e );
      LOG.debug( e , e );
      throw new EucalyptusServiceException( "Failed to reset password" );
    }
  }

  public static String getUserToken( User requestUser ) throws EucalyptusServiceException {
    try {
      return requestUser.getToken( );
    } catch ( Exception e ) {
      LOG.error( "Failed to get user security code", e );
      LOG.debug( e , e );
      throw new EucalyptusServiceException( "Failed to get user security code" );
    }
  }

}
