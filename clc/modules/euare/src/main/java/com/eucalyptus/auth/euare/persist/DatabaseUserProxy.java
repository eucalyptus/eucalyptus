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

package com.eucalyptus.auth.euare.persist;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.euare.checker.InvalidValueException;
import com.eucalyptus.auth.euare.checker.ValueChecker;
import com.eucalyptus.auth.euare.checker.ValueCheckerFactory;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity_;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity_;
import com.eucalyptus.auth.euare.persist.entities.GroupEntity;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity_;
import com.eucalyptus.auth.euare.principal.EuareAccessKey;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareCertificate;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.policy.PolicyPolicy;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Tx;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DatabaseUserProxy implements EuareUser {

  private static final long serialVersionUID = 1L;
  
  private static Logger LOG = Logger.getLogger( DatabaseUserProxy.class );
  
  private static final ValueChecker NAME_CHECKER = ValueCheckerFactory.createUserNameChecker( );
  private static final ValueChecker PATH_CHECKER = ValueCheckerFactory.createPathChecker( );
  private static final ValueChecker POLICY_NAME_CHECKER = ValueCheckerFactory.createPolicyNameChecker( );
  
  private UserEntity delegate;
  private transient Supplier<String> accountNumberSupplier =
      DatabaseAuthUtils.getAccountNumberSupplier( this );
  private transient Supplier<Map<String,String>> userInfoSupplier =
      getUserInfoSupplier();
  private transient Boolean isSystemAdmin = null;

  public DatabaseUserProxy( UserEntity delegate ) {
    this.delegate = delegate;
  }

  public DatabaseUserProxy( UserEntity delegate,
                            String accountNumber ) {
    this.delegate = delegate;
    accountNumberSupplier = Suppliers.ofInstance( accountNumber );
  }

  public DatabaseUserProxy( UserEntity delegate,
                            String accountNumber,
                            Map<String,String> info ) {
    this.delegate = delegate;
    accountNumberSupplier = Suppliers.ofInstance( accountNumber );
    userInfoSupplier = Suppliers.ofInstance( info );
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString();
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }

  @Override
  public String getUserId( ) {
    return this.delegate.getUserId();
  }

  @Override
  public void setName( String name ) throws AuthException {
    try {
      NAME_CHECKER.check( name );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid user name " + name );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    try {
      // try looking up the user with same name
      this.getAccount( ).lookupUserByName( name );
    } catch ( AuthException e ) {
      // not found
      try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
        UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
        user.setName( name );
        for ( GroupEntity g : user.getGroups( ) ) {
          if ( g.isUserGroup( ) ) {
            g.setName( DatabaseAuthUtils.getUserGroupName( name ) );
            break;
          }
        }
        db.commit( );
      } catch ( Exception t ) {
        Debugging.logError( LOG, t, "Failed to setName for " + this.delegate );
        throw new AuthException( t );
      }
      return;
    }
    // found
    throw new AuthException( AuthException.USER_ALREADY_EXISTS );
  }

  @Override
  public String getPath( ) {
    return this.delegate.getPath();
  }

  @Override
  public void setPath( final String path ) throws AuthException {
    try {
      PATH_CHECKER.check( path );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid path " + path );
      throw new AuthException( AuthException.INVALID_PATH, e );
    }
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.setPath( path );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setPath for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public Date getCreateDate() {
    return this.delegate.getCreationTimestamp( );
  }

  @Override
  public boolean isEnabled( ) {
    return this.delegate.isEnabled( );
  }

  @Override
  public void setEnabled( final boolean enabled ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.setEnabled( enabled );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setEnabled for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public String getToken( ) {
    return this.delegate.getToken( );
  }

  @Override
  public void setToken( final String token ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.setToken( token );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setToken for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public String resetToken( ) throws AuthException {
    String original = this.delegate.getToken( );
    this.setToken( Crypto.generateSessionToken() );
    return original;
  }

  @Override
  public String getPassword( ) {
    return this.delegate.getPassword( );
  }

  @Override
  public void setPassword( final String password ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.setPassword( password );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setPassword for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public Long getPasswordExpires( ) {
    return this.delegate.getPasswordExpires( );
  }

  @Override
  public void setPasswordExpires( final Long time ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.setPasswordExpires( time );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setPasswordExpires for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public String getInfo( final String key ) throws AuthException {
    return DatabaseAuthUtils.extract( userInfoSupplier ).get( key.toLowerCase( ) );
  }

  @Override
  public Map<String, String> getInfo( ) throws AuthException {
    return DatabaseAuthUtils.extract( userInfoSupplier );
  }

  @Override
  public void setInfo( final String key, final String value ) throws AuthException {
    if ( Strings.isNullOrEmpty( key ) ) {
      throw new AuthException( "Empty key" );
    }
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.getInfo( ).put( key.toLowerCase( ), value );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setInfo for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void removeInfo( final String key ) throws AuthException {
    if ( Strings.isNullOrEmpty( key ) ) {
      throw new AuthException( "Empty key" );
    }
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.getInfo( ).remove( key.toLowerCase( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to removeInfo for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void setInfo( final Map<String, String> newInfo ) throws AuthException {
    if ( newInfo == null ) {
      throw new AuthException( "Empty user info map" );
    }
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          t.getInfo( ).clear( );
          for ( Map.Entry<String, String> entry : newInfo.entrySet( ) ) {
            t.getInfo( ).put( entry.getKey( ).toLowerCase( ), entry.getValue( ) );
          }
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setInfo for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public List<AccessKey> getKeys( ) throws AuthException {
    final List<AccessKey> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          for ( AccessKeyEntity k : t.getKeys( ) ) {
            results.add( new DatabaseAccessKeyProxy( k ) );
          }
        }
      } );      
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getKeys for " + this.delegate );
      throw new AuthException( e );      
    }
    return results;
  }
  
  @Override
  public EuareAccessKey getKey( final String keyId ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( AccessKeyEntity.class ) ) {
      AccessKeyEntity key = DatabaseAuthUtils.getUnique( AccessKeyEntity.class, AccessKeyEntity_.accessKey, keyId );
      checkKeyOwner( key );
      db.commit( );
      return new DatabaseAccessKeyProxy( key );
    } catch ( NoSuchElementException e ) {
      throw new AuthException( AuthException.NO_SUCH_KEY );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get access key " + keyId );
      throw new AuthException( AuthException.NO_SUCH_KEY );
    }
  }

  @Override
  public void removeKey( final String keyId ) throws AuthException {
    if ( Strings.isNullOrEmpty( keyId ) ) {
      throw new AuthException( AuthException.EMPTY_KEY_ID );
    }
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
      AccessKeyEntity keyEntity = DatabaseAuthUtils.getUnique( AccessKeyEntity.class, AccessKeyEntity_.accessKey, keyId );
      checkKeyOwner( keyEntity );
      if ( !user.getKeys( ).remove( keyEntity ) ) {
        throw new AuthException( AuthException.NO_SUCH_KEY );
      }
      Entities.delete( keyEntity );
      db.commit( );
    } catch ( AuthException e ) {
      throw e;
    } catch ( NoSuchElementException e ) {
      throw new AuthException( AuthException.NO_SUCH_KEY, e );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get delete key " + keyId );
      throw new AuthException( e );
    }
  }

  @Override
  public EuareAccessKey createKey( ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
      AccessKeyEntity keyEntity = new AccessKeyEntity( user );
      keyEntity.setActive( true );
      Entities.persist( keyEntity );
      user.getKeys( ).add( keyEntity );
      db.commit( );
      return new DatabaseAccessKeyProxy( keyEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get create new access key: " + e.getMessage( ) );
      throw new AuthException( e );
    }
  }
  
  @Override
  public List<Certificate> getCertificates( ) throws AuthException {
    final List<Certificate> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          for ( CertificateEntity c : t.getCertificates( ) ) {
            results.add( new DatabaseCertificateProxy( c ) );
          }
        }
      } );      
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getCertificates for " + this.delegate );
      throw new AuthException( e );      
    }
    return results;
  }
  

  @Override
  public EuareCertificate getCertificate( final String certificateId ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( CertificateEntity.class ) ) {
      CertificateEntity cert = DatabaseAuthUtils.getUnique( CertificateEntity.class, CertificateEntity_.certificateId, certificateId );
      checkCertOwner( cert );
      db.commit( );
      return new DatabaseCertificateProxy( cert );
    } catch ( NoSuchElementException e ) {
      throw new AuthException( AuthException.NO_SUCH_CERTIFICATE );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get signing certificate " + certificateId );
      throw new AuthException( AuthException.NO_SUCH_CERTIFICATE );
    }
  }

  @Override
  public EuareCertificate addCertificate( String certificateId, X509Certificate cert ) throws AuthException {
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
      CertificateEntity certEntity = new CertificateEntity( certificateId, cert );
      certEntity.setActive( true );
      Entities.persist( certEntity );
      certEntity.setUser( user );
      user.getCertificates( ).add( certEntity );
      db.commit( );
      return new DatabaseCertificateProxy( certEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get add certificate " + cert );
      throw new AuthException( e );
    }
  }
  
  @Override
  public void removeCertificate( final String certificateId ) throws AuthException {
    if ( Strings.isNullOrEmpty( certificateId ) ) {
      throw new AuthException( AuthException.EMPTY_CERT_ID );
    }
    try ( final TransactionResource db = Entities.transactionFor( CertificateEntity.class ) ) {
      CertificateEntity certificateEntity =
          DatabaseAuthUtils.getUnique( CertificateEntity.class, CertificateEntity_.certificateId, certificateId );
      checkCertOwner( certificateEntity );
      Entities.delete( certificateEntity );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get delete certificate " + certificateId );
      throw new AuthException( e );
    }
  }
  
  @Override
  public List<EuareGroup> getGroups( ) throws AuthException {
    final List<EuareGroup> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          for ( GroupEntity g : t.getGroups( ) ) {
            results.add( new DatabaseGroupProxy( g, accountNumberSupplier ) );
          }
        }
      } );      
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getGroups for " + this.delegate );
      throw new AuthException( e );      
    }
    return results;
  }

  @Override
  public String getAccountNumber( ) throws AuthException {
    return DatabaseAuthUtils.extract( accountNumberSupplier );
  }

  @Override
  public EuareAccount getAccount( ) throws AuthException {
    final List<EuareAccount> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) {
          if ( t.getGroups( ).size( ) < 1 ) {
            throw new RuntimeException( "Unexpected group number of the user" );
          }
          final AccountEntity account =  t.getGroups( ).get( 0 ).getAccount( );
          Entities.initialize( account );
          results.add( new DatabaseAccountProxy( account ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getAccount for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }

  @Override
  public boolean isSystemAdmin( ) {
    if ( isSystemAdmin != null ) {
      // Safe to cache as the account alias cannot be changed for
      // system accounts
      return isSystemAdmin;
    }
    try {
      final EuareAccount account = this.getAccount( );
      accountNumberSupplier = Suppliers.ofInstance( account.getAccountNumber( ) );
      return isSystemAdmin = Accounts.isSystemAccount( account.getName() );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      return false;
    }
  }

  @Override
  public boolean isSystemUser() {
    return isSystemAdmin( );
  }

  @Override
  public boolean isAccountAdmin( ) {
    return DatabaseAuthUtils.isAccountAdmin( this.getName( ) );
  }

  private GroupEntity getUserGroupEntity( UserEntity userEntity ) {
    GroupEntity groupEntity = null;
    for ( GroupEntity g : userEntity.getGroups( ) ) {
      if ( g.isUserGroup( ) ) { 
        groupEntity = g;
        break;
      }
    }
    return groupEntity;
  }
  
  @Override
  public List<Policy> getPolicies( ) throws AuthException {
    List<Policy> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
      GroupEntity group = getUserGroupEntity( user );
      if ( group == null ) {
        throw new RuntimeException( "Can't find user group for user " + this.delegate.getName( ) );
      }
      for ( PolicyEntity p : group.getPolicies( ) ) {
        results.add( new DatabasePolicyProxy( p ) );
      }
      db.commit( );
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get policies for " + this.delegate );
      throw new AuthException( "Failed to get policies", e );
    }
  }

  @Override
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    return storePolicy( name, policy, /*allowUpdate*/ false );
  }

  @Override
  public Policy putPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    return storePolicy( name, policy, /*allowUpdate*/ true );
  }

  private Policy storePolicy( String name, String policy, boolean allowUpdate ) throws AuthException, PolicyParseException {
    try {
      POLICY_NAME_CHECKER.check( name );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, "Invalid policy name " + name );
      throw new AuthException( AuthException.INVALID_NAME, e );
    }
    if ( DatabaseAuthUtils.policyNameinList( name, this.getPolicies( ) ) && !allowUpdate ) {
      Debugging.logError( LOG, null, "Policy name already used: " + name );
      throw new AuthException( AuthException.INVALID_NAME );
    }
    final PolicyPolicy policyPolicy = PolicyParser.getInstance().parse( policy );
    final PolicyEntity parsedPolicy = PolicyEntity.create( name, policyPolicy.getPolicyVersion( ), policy );
    if ( this.isAccountAdmin( ) && containsAllowEffect( policyPolicy ) ) {
      throw new PolicyParseException( "Policy with Allow effect can not be assigned to account/account admin" );
    }
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity userEntity = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
      final GroupEntity groupEntity = getUserGroupEntity( userEntity );
      if ( groupEntity == null ) {
        throw new RuntimeException( "Can't find user group for user " + this.delegate.getName( ) );
      }
      final PolicyEntity remove = DatabaseAuthUtils.removeGroupPolicy( groupEntity, name );
      if ( remove != null ) {
        Entities.delete( remove );
      }
      Entities.persist( parsedPolicy );
      parsedPolicy.setGroup( groupEntity );
      groupEntity.getPolicies( ).add( parsedPolicy );
      db.commit( );
      return new DatabasePolicyProxy( parsedPolicy );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to attach policy for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to attach policy", e );
    }
  }
  
  @Override
  public void removePolicy( String name ) throws AuthException {
    if ( Strings.isNullOrEmpty( name ) ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( UserEntity.class ) ) {
      UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, this.delegate.getUserId( ) );
      GroupEntity group = getUserGroupEntity( user );
      if ( group == null ) {
        throw new RuntimeException( "Can't find user group for user " + this.delegate.getName( ) );
      }
      PolicyEntity policy = DatabaseAuthUtils.removeGroupPolicy( group, name );
      if ( policy != null ) {
        Entities.delete( policy );
      }
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to remove policy " + name + " in " + this.delegate );
      throw new AuthException( "Failed to remove policy", e );
    }
  }

    /**
     * @return true if the policy contains Effect is "Allow".
     */
  private boolean containsAllowEffect( final PolicyPolicy policy ) {
    for ( final Authorization authorization : policy.getAuthorizations( ) ) {
      if ( authorization.getEffect( ) == Authorization.EffectType.Allow ) {
        return true;
      }
    }
    return false;
  }

  private Supplier<Map<String,String>> getUserInfoSupplier( ) {
    return new Supplier<Map<String, String>>() {
      @Override
      public Map<String, String> get() {
        final Map<String, String> results = Maps.newHashMap( );
        try {
          DatabaseAuthUtils.invokeUnique( UserEntity.class, UserEntity_.userId, DatabaseUserProxy.this.delegate.getUserId( ), new Tx<UserEntity>( ) {
            public void fire( UserEntity t ) {
              results.putAll( t.getInfo( ) );
            }
          } );
        } catch ( ExecutionException e ) {
          Debugging.logError( LOG, e, "Failed to getInfo for " + DatabaseUserProxy.this.delegate );
          throw new RuntimeException( new AuthException( e ).fillInStackTrace( ) );
        }
        return results;
      }
    };
  }

  private void checkKeyOwner( final AccessKeyEntity keyEnt ) throws AuthException {
    checkOwner( keyEnt.getUser( ), AuthException.NO_SUCH_KEY, "key" );
  }

  private void checkCertOwner( final CertificateEntity certEnt ) throws AuthException {
    checkOwner( certEnt.getUser( ), AuthException.NO_SUCH_CERTIFICATE, "certificate" );
  }

  private void checkOwner( final UserEntity user, final String error, final String type ) throws AuthException {
    if ( !this.delegate.getUserId( ).equals( user.getUserId( ) ) ) {
      AuthException ae = new AuthException( error );
      Debugging.logError( LOG, ae, this.delegate.getName( ) + " is not owner of provided " + type );
      throw ae;
    }
  }

  private void readObject( ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject( );
    this.accountNumberSupplier = DatabaseAuthUtils.getAccountNumberSupplier( this );
    this.userInfoSupplier = getUserInfoSupplier( );
  }
}
