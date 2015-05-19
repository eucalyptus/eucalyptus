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

package com.eucalyptus.auth.euare.persist;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.AccountIdentifiersImpl;
import com.eucalyptus.entities.Entities;
import org.apache.log4j.Logger;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.euare.AccountProvider;
import com.eucalyptus.auth.euare.checker.InvalidValueException;
import com.eucalyptus.auth.euare.checker.ValueChecker;
import com.eucalyptus.auth.euare.checker.ValueCheckerFactory;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.hibernate.persister.collection.CollectionPropertyNames;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

/**
 * The authorization provider based on database storage. This class includes all the APIs to
 * create/delete/query Eucalyptus authorization entities.
 */
public class DatabaseAuthProvider implements AccountProvider {
  
  private static Logger LOG = Logger.getLogger( DatabaseAuthProvider.class );
  
  private static final ValueChecker ACCOUNT_NAME_CHECKER = ValueCheckerFactory.createAccountNameChecker( );
  
  public DatabaseAuthProvider( ) {
  }

  @Override
  public EuareUser lookupUserById( final String userId ) throws AuthException {
    if ( userId == null ) {
      throw new AuthException( AuthException.EMPTY_USER_ID );
    }
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, "userId", userId );
      db.commit( );
      return new DatabaseUserProxy( user );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to find user by ID " + userId );
      throw new AuthException( AuthException.NO_SUCH_USER, e );
    }
  }

  @Override
  public EuareRole lookupRoleById( final String roleId ) throws AuthException {
    if ( roleId == null ) {
      throw new AuthException( AuthException.EMPTY_ROLE_ID );
    }
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity role = DatabaseAuthUtils.getUnique( RoleEntity.class, "roleId", roleId );
      return new DatabaseRoleProxy( role );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to find role by ID " + roleId );
      throw new AuthException( AuthException.NO_SUCH_ROLE, e );
    }
  }

  /**
   * Add account admin user separately.
   */
  @Override
  public EuareAccount addAccount( @Nullable String accountName ) throws AuthException {
    if ( accountName != null ) {
      try {
        ACCOUNT_NAME_CHECKER.check( accountName );
      } catch ( InvalidValueException e ) {
        Debugging.logError( LOG, e, "Invalid account name " + accountName );
        throw new AuthException( AuthException.INVALID_NAME, e );
      }
      if ( DatabaseAuthUtils.checkAccountExists( accountName ) ) {
        throw new AuthException( AuthException.ACCOUNT_ALREADY_EXISTS );
      }
    }
    return doAddAccount( accountName );
  }

  /**
   *
   */
  @Override
  public EuareAccount addSystemAccount( String accountName ) throws AuthException {
    if ( accountName.startsWith( AccountIdentifiers.SYSTEM_ACCOUNT_PREFIX ) ) {
      try {
        ACCOUNT_NAME_CHECKER.check( accountName.substring( EuareAccount.SYSTEM_ACCOUNT_PREFIX.length( ) ) );
      } catch ( InvalidValueException e ) {
        Debugging.logError( LOG, e, "Invalid account name " + accountName );
        throw new AuthException( AuthException.INVALID_NAME, e );
      }
    } else if ( !AccountIdentifiers.SYSTEM_ACCOUNT.equals( accountName ) ) {
      throw new AuthException( AuthException.INVALID_NAME );
    }

    EuareAccount account = null;
    try {
      account = lookupAccountByName( accountName );
    } catch ( AuthException e ) {
      // create it
    }

    if ( account == null ) {
      account = doAddAccount( accountName );
    }

    return account;
  }

  /**
   *
   */
  private EuareAccount doAddAccount( @Nullable String accountName ) throws AuthException {
    AccountEntity account = new AccountEntity( accountName );
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      Entities.persist( account );
      db.commit( );
      return new DatabaseAccountProxy( account );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to add account " + accountName );
      throw new AuthException( AuthException.ACCOUNT_CREATE_FAILURE, e );
    }
  }

  @Override
  @SuppressWarnings( "unchecked" )
  public void deleteAccount( String accountName, boolean forceDeleteSystem, boolean recursive ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    if ( !forceDeleteSystem && Accounts.isSystemAccount( accountName ) ) {
      throw new AuthException( AuthException.DELETE_SYSTEM_ACCOUNT );
    }
    if ( !(recursive || DatabaseAuthUtils.isAccountEmpty( accountName ) ) ) {
      throw new AuthException( AuthException.ACCOUNT_DELETE_CONFLICT );
    }
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      if ( recursive ) {
        List<UserEntity> users = ( List<UserEntity> ) Entities
            .createCriteria( UserEntity.class ).setCacheable( true )
            .createCriteria( "groups" ).setCacheable( true ).add( Restrictions.eq( "userGroup", true ) )
            .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
            .list( );
        for ( UserEntity u : users ) {
          Entities.delete( u );
        }

        List<InstanceProfileEntity> profiles = ( List<InstanceProfileEntity> ) Entities
            .createCriteria( InstanceProfileEntity.class ).setCacheable( true )
            .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
            .list( );
        for ( InstanceProfileEntity p : profiles ) {
          Entities.delete( p );
        }

        List<RoleEntity> roles = ( List<RoleEntity> ) Entities
            .createCriteria( RoleEntity.class ).setCacheable( true )
            .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
            .list( );
        for ( RoleEntity r : roles ) {
          Entities.delete( r );
        }

        List<GroupEntity> groups = ( List<GroupEntity> ) Entities
            .createCriteria( GroupEntity.class ).setCacheable( true )
            .createCriteria( "account" ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
            .list( );
        for ( GroupEntity g : groups ) {
          Entities.delete( g );
        }
      }
      AccountEntity account = ( AccountEntity ) Entities
          .createCriteria( AccountEntity.class ).setCacheable( true ).add( Restrictions.eq( "name", accountName ) )
          .uniqueResult( );
      if ( account == null ) {
        throw new NoSuchElementException( "Can not find account " + accountName );
      }
      Entities.delete( account );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to delete account " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT, e );
    }
  }

  @Override
  public List<AccountIdentifiers> resolveAccountNumbersForName( final String accountNameLike ) throws AuthException {
    final List<AccountIdentifiers> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      for ( final AccountEntity account : Entities.query( new AccountEntity( accountNameLike ) ) ) {
        results.add( new AccountIdentifiersImpl(
            account.getAccountNumber( ),
            account.getName( ),
            account.getCanonicalId( )
        ) );
      }
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to resolve account numbers" );
      throw new AuthException( "Failed to resolve account numbers", e );
    }
    return results;
  }

  @Override
  public List<EuareUser> listAllUsers( ) throws AuthException {
    List<EuareUser> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      List<UserEntity> users = Entities.query( new UserEntity( ) );
      db.commit( );
      for ( UserEntity u : users ) {
        results.add( new DatabaseUserProxy( u ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get all users" );
      throw new AuthException( "Failed to get all users", e );
    }
  }

  @Override
  public List<EuareAccount> listAllAccounts( ) throws AuthException {
    List<EuareAccount> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      for ( AccountEntity account : Entities.query( new AccountEntity( ), true ) ) {
        results.add( new DatabaseAccountProxy( account ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get accounts" );
      throw new AuthException( "Failed to accounts", e );
    }
  }

  @Override
  public Certificate lookupCertificateByHashId( String certificateId ) throws AuthException {
    if ( certificateId == null ) {
      throw new AuthException( "Certificate identifier required" );
    }
    try ( final TransactionResource db = Entities.transactionFor( CertificateEntity.class ) ) {
      CertificateEntity certEntity = DatabaseAuthUtils.getUnique( CertificateEntity.class, "certificateHashId", certificateId );
      db.commit( );
      return new DatabaseCertificateProxy( certEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to lookup cert " + certificateId );
      throw new AuthException( AuthException.NO_SUCH_CERTIFICATE, e );
    }
  }

  @Override
  public Certificate lookupCertificateById( String certificateId ) throws AuthException {
    if ( certificateId == null ) {
      throw new AuthException( "Certificate identifier required" );
    }
    try ( final TransactionResource db = Entities.transactionFor( CertificateEntity.class ) ) {
      CertificateEntity certEntity = DatabaseAuthUtils.getUnique( CertificateEntity.class, "certificateId", certificateId );
      db.commit( );
      return new DatabaseCertificateProxy( certEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to lookup cert " + certificateId );
      throw new AuthException( AuthException.NO_SUCH_CERTIFICATE, e );
    }
  }

  @Override
  public EuareAccount lookupAccountByName( String accountName ) throws AuthException {
    if ( accountName == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( CertificateEntity.class ) ) {
      @SuppressWarnings( "unchecked" )
      AccountEntity result = ( AccountEntity ) Entities.createCriteria( AccountEntity.class )
          .setCacheable( true )
          .add( Restrictions.eq( "name", accountName ) )
          .setReadOnly( true )
          .uniqueResult( );
      if ( result == null ) {
        throw new AuthException( AuthException.NO_SUCH_ACCOUNT );
      }
      return new DatabaseAccountProxy( result );
    } catch ( AuthException e ) {
      Debugging.logError( LOG, e, "No matching account " + accountName );
      throw e;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to find account " + accountName );
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT, e );
    }
  }

  @Override
  public EuareAccount lookupAccountById( final String accountId ) throws AuthException {
    if ( accountId == null ) {
      throw new AuthException( AuthException.EMPTY_ACCOUNT_ID );
    }
    try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
      AccountEntity account = DatabaseAuthUtils.getUnique( AccountEntity.class, "accountNumber", accountId );
      db.commit( );
      return new DatabaseAccountProxy( account );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to find account " + accountId );
      throw new AuthException( "Failed to find account", e );
    }
  }

    @Override
    public EuareAccount lookupAccountByCanonicalId( final String canonicalId ) throws AuthException {
      if ( canonicalId == null || "".equals(canonicalId) ) {
          throw new AuthException( AuthException.EMPTY_CANONICAL_ID );
      }
      try ( final TransactionResource db = Entities.transactionFor( AccountEntity.class ) ) {
        AccountEntity example = new AccountEntity();
        example.setCanonicalId(canonicalId);
        List<AccountEntity> results = Entities.query(example);
        if (results != null && results.size() > 0) {
          AccountEntity found = results.get(0);
          return new DatabaseAccountProxy(found);
        } else {
          throw new AuthException( AuthException.NO_SUCH_USER );
        }
      } catch ( Exception e ) {
          Debugging.logError( LOG, e, "Error occurred looking for account by canonical ID " + canonicalId );
          throw new AuthException( AuthException.NO_SUCH_USER, e );
      }
    }

    @Override
  public AccessKey lookupAccessKeyById( final String keyId ) throws AuthException {
    if ( keyId == null ) {
      throw new AuthException( "Empty access key ID" );
    }
    try ( final TransactionResource db = Entities.transactionFor( AccessKeyEntity.class ) ) {
      AccessKeyEntity keyEntity = DatabaseAuthUtils.getUnique( AccessKeyEntity.class, "accessKey", keyId );
      db.commit( );
      return new DatabaseAccessKeyProxy( keyEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to find access key with ID " + keyId );
      throw new InvalidAccessKeyAuthException( "Failed to find access key", e );
    }
  }

    public EuareUser lookupUserByEmailAddress( String email ) throws AuthException {
        if (email == null || "".equals(email)) {
            throw new AuthException("Empty email address to search");
        }
        final EntityTransaction tx = Entities.get(UserEntity.class);
        try {
            final UserEntity match = (UserEntity) Entities.createCriteria(UserEntity.class)
            .setCacheable(true)
            .createAlias("info", "i")
            .add(Restrictions.eq("i." + CollectionPropertyNames.COLLECTION_ELEMENTS, email).ignoreCase())
            .setFetchMode("info", FetchMode.JOIN)
            .setReadOnly( true )
            .uniqueResult();
            if (match == null) {
                throw new AuthException(AuthException.NO_SUCH_USER);
            }
            boolean emailMatched = false;
            Map<String,String> info = match.getInfo();
            if ( info != null ) {
                for (Map.Entry<String,String> entry : info.entrySet()) {
                    if (entry.getKey() != null
                            && User.EMAIL.equals( entry.getKey() )
                            && entry.getValue() != null
                            && email.equalsIgnoreCase(entry.getValue())) {
                        emailMatched = true;
                        break;
                    }
                }
            }
            if (! emailMatched) {
                throw new AuthException(AuthException.NO_SUCH_USER);
            }
            return new DatabaseUserProxy(match);
        }
        catch ( AuthException e ) {
            Debugging.logError( LOG, e, "Failed to find user by email address " + email );
            throw e;
        }
        catch ( Exception e ) {
            Debugging.logError( LOG, e, "Failed to find user by email address " + email );
            throw new AuthException( AuthException.NO_SUCH_USER, e );
        }
        finally {
          tx.rollback();
        }
    }
}
