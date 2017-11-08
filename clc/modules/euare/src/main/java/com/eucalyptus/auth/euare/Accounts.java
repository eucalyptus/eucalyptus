/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.auth.euare;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.euare.principal.GlobalNamespace;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
public class Accounts extends com.eucalyptus.auth.Accounts {

  private static final Logger LOG = Logger.getLogger( Accounts.class );
  private static Supplier<AccountProvider> accounts = serviceLoaderSupplier( AccountProvider.class );

  public static void reserveGlobalName( GlobalNamespace namespace, String name ) throws AuthException {
    if ( GlobalNamespace.Account_Alias != namespace || !isSystemAccount( name ) ) {
      getIdentityProvider( ).reserveGlobalName( namespace.getNamespace( ), name, 90, UUID.randomUUID( ).toString( ) );
    }
  }

  public static List<AccountIdentifiers> resolveAccountNumbersForName( final String accountNameLike ) throws AuthException {
    return getAccountProvider().resolveAccountNumbersForName( accountNameLike );
  }

  public static EuareAccount lookupAccountByName( String accountName ) throws AuthException {
    if ( isAccountNumber( accountName ) ) {
      return (EuareAccount) getAccountProvider( ).lookupAccountById( accountName );
    } else {
      return (EuareAccount) getAccountProvider( ).lookupAccountByName( accountName );
    }
  }

  public static EuareUser lookupUserById( String userId ) throws AuthException {
    return getAccountProvider().lookupUserById( userId );
  }

  public static EuareUser lookupUserByEmailAddress( String email ) throws AuthException {
    return getAccountProvider().lookupUserByEmailAddress( email );
  }

  public static List<EuareUser> listAllUsers( ) throws AuthException {
    return getAccountProvider( ).listAllUsers( );
  }

  public static void normalizeUserInfo( ) throws AuthException {
    for ( EuareUser user : listAllUsers( ) ) {
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

  public static EuareAccount lookupAccountById( String accountId ) throws AuthException {
    return getAccountProvider().lookupAccountById( accountId );
  }

  public static EuareAccount lookupAccountByCanonicalId(String canonicalId) throws AuthException {
    return getAccountProvider().lookupAccountByCanonicalId( canonicalId );
  }

  public static void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    getAccountProvider().deleteAccount( accountName, forceDeleteSystem, recursive );
  }

  public static List<EuareAccount> listAllAccounts( ) throws AuthException {
    return getAccountProvider().listAllAccounts( );
  }

  public static Certificate lookupCertificateByHashId( String certificateId ) throws AuthException {
    return getAccountProvider().lookupCertificateByHashId( certificateId );
  }

  public static Certificate lookupCertificateById( String certificateId ) throws AuthException {
    return getAccountProvider().lookupCertificateById( certificateId );
  }

  public static AccessKey lookupAccessKeyById( String keyId ) throws AuthException {
    return getAccountProvider().lookupAccessKeyById( keyId );
  }

  public static EuareRole lookupRoleById( String roleId ) throws AuthException {
    return getAccountProvider().lookupRoleById( roleId );
  }

  public static UserPrincipal roleAsPrincipal( final EuareRole role, final String sessionName ) throws AuthException {
    return new UserPrincipalImpl( role, sessionName );
  }

  public static boolean isSystemAccount( EuareAccount account ) {
    return isSystemAccount( account == null ? null : account.getName() );
  }

  public static UserPrincipal userAsPrincipal( final EuareUser user  ) throws AuthException {
    return new UserPrincipalImpl( user );
  }

  public static EuareAccount addSystemAccount( ) throws AuthException {
    return getAccountProvider().addAccount( EuareAccount.SYSTEM_ACCOUNT );
  }

  public static EuareAccount addAccount( @Nullable String accountName ) throws AuthException {
    return getAccountProvider().addAccount( accountName );
  }

  /**
   * Add a system account with an admin user.
   *
   * @return The new account or an existing account with the specified name.
   */
  public static EuareAccount addSystemAccountWithAdmin( final String accountName ) throws AuthException {
    final EuareAccount account = getAccountProvider().addSystemAccount( accountName );
    try {
      account.lookupUserByName( User.ACCOUNT_ADMIN );
    } catch ( final AuthException e ) {
      account.addUser( User.ACCOUNT_ADMIN, "/", true, null );
    }
    return account;
  }

  public static void setAccountProvider( AccountProvider provider ) {
    synchronized ( com.eucalyptus.auth.Accounts.class ) {
      LOG.info( "Setting the account provider to: " + provider.getClass( ) );
      accounts = Suppliers.ofInstance( provider );
    }
  }

  protected static AccountProvider getAccountProvider( ) {
    return accounts.get();
  }

  public static String getGroupFullName( EuareGroup group ) {
    if ( group.getPath( ).endsWith( "/" ) ) {
      return group.getPath( ) + group.getName( );
    } else {
      return group.getPath( ) + "/" + group.getName( );
    }
  }

  public static String getGroupArn( final EuareGroup group ) throws AuthException {
    return buildArn( group.getAccountNumber( ), IamPolicySpec.IAM_RESOURCE_GROUP, group.getPath(), group.getName() );
  }

  public static Predicate<EuareGroup> isUserGroup( ) {
    return GroupFilters.USER_GROUP;
  }

  private enum GroupFilters implements Predicate<EuareGroup> {
    USER_GROUP {
      @Override
      public boolean apply( @Nullable final EuareGroup group ) {
        return group != null && group.isUserGroup( );
      }
    }
  }
}
