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

package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.AccountProvider;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.RoleUser;
import com.eucalyptus.auth.principal.User;
import com.google.common.base.Function;

/**
 * <h2>Eucalyptus/AWS IDs & Access Keys:</h2>
 * <p>
 * <strong>NOTE:IMPORTANT: It SHOULD NOT be the @Id of the underlying entity as this value is not
 * guaranteed to be fixed in the future (e.g., disrupted by upgrade, version changes,
 * etc.).</strong>
 * </p>
 * <ol>
 * <li>- AWS Account Number: Public ID for an ACCOUNT.</li>
 * <ul>
 * <li>- "globally" unique 12-digit number associated with the Eucalyptus account.</li>
 * <li>- is a shared value; other users may need it or discover it during normal operation of the
 * system</li>
 * <li>- _MUST_ be a 12-digit number. User commands require this value as input in certain cases and
 * enforce the length of the ID.</li>
 * </ul>
 * </li>
 * <li>AWS Access Key: Identifier value corresponding to the AWS Secret Access Key used to sign
 * requests.</li>
 * <ul>
 * <li>- "globally" unique 20 alpha-numeric characters
 * <li>
 * <li>- is a shared value; other users may need it or discover it during normal operation of the
 * system
 * <li>
 * <li>- _MUST_ be 20-alphanum characters; per the specification (e.g.,
 * s3.amazonaws.com/awsdocs/ImportExport/latest/AWSImportExport-dg.pdf). User commands require this
 * value as input in certain cases and enforce the length of the ID.
 * <li>
 * </ul>
 * </ol>
 */
public class Accounts {
  private static final Logger LOG = Logger.getLogger( Accounts.class );
  
  private static AccountProvider accounts;
  
  public static void setAccountProvider( AccountProvider provider ) {
    synchronized ( Accounts.class ) {
      LOG.info( "Setting the account provider to: " + provider.getClass( ) );
      accounts = provider;
    }
  }
  
  public static AccountProvider getAccountProvider( ) {
    return accounts;
  }
  
  public static Account lookupAccountByName( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountByName( accountName );
  }
  
  public static Account lookupAccountById( String accountId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountById( accountId );
  }

  public static Account lookupAccountByCanonicalId(String canonicalId) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccountByCanonicalId(canonicalId);
  }

  public static Account addAccount( String accountName ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( accountName );
  }
  
  public static void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    Accounts.getAccountProvider( ).deleteAccount( accountName, forceDeleteSystem, recursive );
  }
  
  public static List<Account> listAllAccounts( ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllAccounts( );
  }
  
  public static Account addSystemAccount( ) throws AuthException {
    return Accounts.getAccountProvider( ).addAccount( Account.SYSTEM_ACCOUNT );
  }
  
  public static Set<String> resolveAccountNumbersForName( final String accountNameLike ) throws AuthException {
    return Accounts.getAccountProvider().resolveAccountNumbersForName( accountNameLike );    
  }
  
  public static List<User> listAllUsers( ) throws AuthException {
    return Accounts.getAccountProvider( ).listAllUsers( );
  }
  
  public static boolean shareSameAccount( String userId1, String userId2 ) {
    return Accounts.getAccountProvider( ).shareSameAccount( userId1, userId2 );
  }
  
  @Deprecated
  public static User lookupUserByName( String userName ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByName( userName );
  }

  public static User lookupUserById( String userId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserById( userId );
  }

  public static User lookupUserByAccessKeyId( String keyId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByAccessKeyId( keyId );
  }
  
  public static User lookupUserByCertificate( X509Certificate cert ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByCertificate( cert );
  }
  
  public static Group lookupGroupById( String groupId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupGroupById( groupId );
  }

  public static Role lookupRoleById( String roleId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupRoleById( roleId );
  }

  public static Certificate lookupCertificate( X509Certificate cert ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupCertificate( cert );
  }
  
  public static AccessKey lookupAccessKeyById( String keyId ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupAccessKeyById( keyId );
  }
  
  public static User lookupSystemAdmin( ) throws AuthException {
    Account system = Accounts.getAccountProvider( ).lookupAccountByName( Account.SYSTEM_ACCOUNT );
    return system.lookupAdmin();
  }
  
  public static String getFirstActiveAccessKeyId( User user ) throws AuthException {
    for ( AccessKey k : user.getKeys( ) ) {
      if ( k.isActive( ) ) {
        return k.getAccessKey( );
      }
    }
    throw new AuthException( "No active access key for " + user );
  }
  
  public static User lookupUserByConfirmationCode( String code ) throws AuthException {
    return Accounts.getAccountProvider( ).lookupUserByConfirmationCode( code );
  }

    public static User lookupUserByEmailAddress( String email ) throws AuthException {
        return Accounts.getAccountProvider().lookupUserByEmailAddress( email );
    }

  public static User roleAsUser( final Role role ) throws AuthException {
    return new RoleUser( role, role.getAccount().lookupAdmin() );
  }

  public static String getUserFullName( User user ) {
    if ( "/".equals( user.getPath( ) ) ) {
      return "/" + user.getName( );
    } else {
      return user.getPath( ) + "/" + user.getName( );
    }
  }
  
  public static String getGroupFullName( Group group ) {
    if ( "/".equals( group.getPath( ) ) ) {
      return "/" + group.getName( );
    } else {
      return group.getPath( ) + "/" + group.getName( );
    }
  }

  public static String getRoleFullName( Role role ) {
    if ( "/".equals( role.getPath( ) ) ) {
      return "/" + role.getName( );
    } else {
      return role.getPath( ) + "/" + role.getName( );
    }
  }

  public static String getInstanceProfileFullName( InstanceProfile instanceProfile ) {
    if ( "/".equals( instanceProfile.getPath( ) ) ) {
      return "/" + instanceProfile.getName( );
    } else {
      return instanceProfile.getPath( ) + "/" + instanceProfile.getName( );
    }
  }

  public static String getUserArn( final User user ) throws AuthException {
    return buildArn( user.getAccount(), PolicySpec.IAM_RESOURCE_USER, user.getPath(), user.getName() );
  }

  public static String getGroupArn( final Group group ) throws AuthException {
    return buildArn( group.getAccount(), PolicySpec.IAM_RESOURCE_GROUP, group.getPath(), group.getName() );
  }

  public static String getRoleArn( final Role role ) throws AuthException {
    return buildArn( role.getAccount(), PolicySpec.IAM_RESOURCE_ROLE, role.getPath(), role.getName() );
  }

  public static String getInstanceProfileArn( final InstanceProfile instanceProfile ) throws AuthException {
    return buildArn( instanceProfile.getAccount(), PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE, instanceProfile.getPath(), instanceProfile.getName() );
  }

  private static String buildArn( final Account account,
                                  final String type,
                                  final String path,
                                  final String name ) throws AuthException {
    return new EuareResourceName( account.getAccountNumber(), type, path, name ).toString();
  }

  public static void normalizeUserInfo( ) throws AuthException {
    for ( User user : listAllUsers( ) ) {
      try {
        // In old code the info key is case sensitive
        // In new code User.setInfo(Map<String,String) converts all keys to lower case
        user.setInfo( user.getInfo( ) );
      } catch ( AuthException e ) {
        LOG.error( e, e );
        continue;
      }
    }
  }

  public static Function<Account,String> toAccountNumber() {
    return AccountStringProperties.ACCOUNT_NUMBER;
  }

  public static Function<User,String> toUserId() {
    return UserStringProperties.USER_ID;
  }

  private enum AccountStringProperties implements Function<Account,String> {
    ACCOUNT_NUMBER {
      @Override
      public String apply( final Account account ) {
        return account.getAccountNumber();
      }
    }
  }

  private enum UserStringProperties implements Function<User,String> {
    USER_ID {
      @Override
      public String apply( final User user ) {
        return user.getUserId();
      }
    }
  }
}
