package com.eucalyptus.auth;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Hmacs;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DatabaseUserProxy implements User {

  private static final long serialVersionUID = 1L;
  
  private static Logger LOG = Logger.getLogger( DatabaseUserProxy.class );
  
  private UserEntity delegate;
  
  public DatabaseUserProxy( UserEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }

  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }

  @Override
  public String getId( ) {
    return this.delegate.getId( );
  }

  @Override
  public void setName( String name ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      user.setName( name );
      for ( GroupEntity g : user.getGroups( ) ) {
        if ( g.isUserGroup( ) ) {
          g.setName( DatabaseAuthUtils.getUserGroupName( name ) );
          break;
        }
      }
      db.commit( );
    } catch ( Throwable e ) {
      Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
      db.rollback( );
      throw new AuthException( e );
    }
  }

  @Override
  public String getPath( ) {
    return this.delegate.getPath( );
  }

  @Override
  public void setPath( final String path ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setPath( path );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setPath for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public RegistrationStatus getRegistrationStatus( ) {
    return this.delegate.getRegistrationStatus( );
  }

  @Override
  public void setRegistrationStatus( final RegistrationStatus stat ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setRegistrationStatus( stat );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setRegistrationStatus for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public Boolean isEnabled( ) {
    return this.delegate.isEnabled( );
  }

  @Override
  public void setEnabled( final Boolean enabled ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setEnabled( enabled );
        }
      } );
    } catch ( TransactionException e ) {
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
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setToken( token );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setToken for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void createToken( ) throws AuthException {
    this.setToken( Crypto.generateSessionToken( this.delegate.getName( ) ) );
  }

  @Override
  public String getConfirmationCode( ) {
    return this.delegate.getConfirmationCode( );
  }

  @Override
  public void setConfirmationCode( final String code ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setConfirmationCode( code );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setConfirmationCode for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void createConfirmationCode( ) throws AuthException {
    this.setConfirmationCode( Crypto.generateSessionToken( this.delegate.getName( ) ) );
  }

  @Override
  public String getPassword( ) {
    return this.delegate.getPassword( );
  }

  @Override
  public void setPassword( final String password ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setPassword( password );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setPassword for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void createPassword( ) throws AuthException {
    this.setPassword( Crypto.generateHashedPassword( this.delegate.getName( ) ) );
  }
  
  @Override
  public Long getPasswordExpires( ) {
    return this.delegate.getPasswordExpires( );
  }

  @Override
  public void setPasswordExpires( final Long time ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setPasswordExpires( time );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setPasswordExpires for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public String getInfo( final String key ) throws AuthException {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.getInfo( ).get( key ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getInfo for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }

  @Override
  public Map<String, String> getInfo( ) throws AuthException {
    final Map<String, String> results = Maps.newHashMap( );
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.putAll( t.getInfo( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getInfo for " + this.delegate );
      throw new AuthException( e );
    }
    return results;
  }

  @Override
  public void setInfo( final String key, final String value ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.getInfo( ).put( key, value );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setInfo for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public void setInfo( final Map<String, String> newInfo ) throws AuthException {
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.getInfo( ).clear( );
          t.getInfo( ).putAll( newInfo );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setInfo for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public List<AccessKey> getKeys( ) throws AuthException {
    final List<AccessKey> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          for ( AccessKeyEntity k : t.getKeys( ) ) {
            results.add( new DatabaseAccessKeyProxy( k ) );
          }
        }
      } );      
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getKeys for " + this.delegate );
      throw new AuthException( e );      
    }
    return results;
  }
  
  @Override
  public AccessKey getKey( final String keyId ) throws AuthException {
    EntityWrapper<AccessKeyEntity> db = EntityWrapper.get( AccessKeyEntity.class );
    try {
      AccessKeyEntity key = db.getUnique( AccessKeyEntity.newInstanceWithId( keyId ) );
      db.commit( );
      return new DatabaseAccessKeyProxy( key );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get access key " + keyId );
      throw new AuthException( e );
    }
  }

  @Override
  public void removeKey( final String keyId ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      AccessKeyEntity keyEntity = db.recast(AccessKeyEntity.class).getUnique( AccessKeyEntity.newInstanceWithId( keyId ) );
      user.getKeys( ).remove( keyEntity );
      db.recast( AccessKeyEntity.class ).delete( keyEntity );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get delete key " + keyId );
      throw new AuthException( e );
    }
  }

  @Override
  public AccessKey createKey( ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      AccessKeyEntity keyEntity = new AccessKeyEntity( user );
      keyEntity.setActive( true );
      db.recast( AccessKeyEntity.class ).add( keyEntity );
      db.commit( );
      return new DatabaseAccessKeyProxy( keyEntity );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get create new access key: " + e.getMessage( ) );
      throw new AuthException( e );
    }
  }
  
  @Override
  public List<Certificate> getCertificates( ) throws AuthException {
    final List<Certificate> results = Lists.newArrayList( );
    try {
      final UserEntity search = UserEntity.newInstanceWithId( this.delegate.getId( ) );
      Transactions.one( search, new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          for ( CertificateEntity c : t.getCertificates( ) ) {
            results.add( new DatabaseCertificateProxy( c ) );
          }
        }
      } );      
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getCertificates for " + this.delegate );
      throw new AuthException( e );      
    }
    return results;
  }
  

  @Override
  public Certificate getCertificate( final String certificateId ) throws AuthException {
    EntityWrapper<CertificateEntity> db = EntityWrapper.get( CertificateEntity.class );
    try {
      CertificateEntity cert = db.getUnique( CertificateEntity.newInstanceWithId( certificateId ) );
      db.commit( );
      return new DatabaseCertificateProxy( cert );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get signing certificate " + certificateId );
      throw new AuthException( e );
    }
  }

  @Override
  public Certificate addCertificate( X509Certificate cert ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      CertificateEntity certEntity = new CertificateEntity( X509CertHelper.fromCertificate( cert ) );
      certEntity.setActive( true );
      certEntity.setRevoked( false );
      db.recast( CertificateEntity.class ).add( certEntity );
      certEntity.setUser( user );
      db.commit( );
      return new DatabaseCertificateProxy( certEntity );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get add certificate " + cert );
      throw new AuthException( e );
    }
  }
  
  @Override
  public void removeCertificate( final String certficateId ) throws AuthException {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      CertificateEntity certificateEntity = db.recast(CertificateEntity.class).getUnique( CertificateEntity.newInstanceWithId( certficateId ) );
      user.getCertificates( ).remove( certificateEntity );
      db.recast( CertificateEntity.class ).delete( certificateEntity );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get delete certificate " + certficateId );
      throw new AuthException( e );
    }
  }
  
  @Override
  public List<Group> getGroups( ) throws AuthException {
    final List<Group> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          for ( GroupEntity g : t.getGroups( ) ) {
            results.add( new DatabaseGroupProxy( g ) );
          }
        }
      } );      
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getGroups for " + this.delegate );
      throw new AuthException( e );      
    }
    return results;
  }

  @Override
  public Account getAccount( ) throws AuthException {
    final List<Account> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.newInstanceWithId( this.delegate.getId( ) ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          if ( t.getGroups( ).size( ) < 1 ) {
            throw new RuntimeException( "Unexpected group number of the user" );
          }
          results.add( new DatabaseAccountProxy( t.getGroups( ).get( 0 ).getAccount( ) ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getAccount for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }

  @Override
  public boolean isSystemAdmin( ) {
    try {
      return DatabaseAuthUtils.isSystemAccount( this.getAccount( ).getName( ) );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      return false;
    }
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
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      GroupEntity group = getUserGroupEntity( user );
      if ( group == null ) {
        throw new RuntimeException( "Can't find user group for user " + this.delegate.getName( ) );
      }
      for ( PolicyEntity p : group.getPolicies( ) ) {
        results.add( new DatabasePolicyProxy( p ) );
      }
      db.commit( );
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to get policies for " + this.delegate );
      throw new AuthException( "Failed to get policies", e );
    }
  }
  
  @Override
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    PolicyEntity parsedPolicy = PolicyParser.getInstance( ).parse( policy );
    parsedPolicy.setName( name );
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    try {
      UserEntity userEntity = db.recast(UserEntity.class).getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      GroupEntity groupEntity = getUserGroupEntity( userEntity );
      if ( groupEntity == null ) {
        throw new RuntimeException( "Can't find user group for user " + this.delegate.getName( ) );
      }
      db.recast( PolicyEntity.class ).add( parsedPolicy );
      parsedPolicy.setGroup( groupEntity );
      for ( StatementEntity statement : parsedPolicy.getStatements( ) ) {
        db.recast( StatementEntity.class ).add( statement );
        statement.setPolicy( parsedPolicy );
        for ( AuthorizationEntity auth : statement.getAuthorizations( ) ) {
          db.recast( AuthorizationEntity.class ).add( auth );
          auth.setStatement( statement );
        }
        for ( ConditionEntity cond : statement.getConditions( ) ) {
          db.recast( ConditionEntity.class ).add( cond );
          cond.setStatement( statement );
        }
      }
      db.commit( );
      return new DatabasePolicyProxy( parsedPolicy );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to attach policy for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to attach policy", e );
    }
  }
  
  @Override
  public void removePolicy( String name ) throws AuthException {
    if ( name == null ) {
      throw new AuthException( "Empty policy ID" );
    }
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    try {
      UserEntity user = db.getUnique( UserEntity.newInstanceWithId( this.delegate.getId( ) ) );
      GroupEntity group = getUserGroupEntity( user );
      if ( group == null ) {
        throw new RuntimeException( "Can't find user group for user " + this.delegate.getName( ) );
      }
      PolicyEntity policy = DatabaseAuthUtils.removeGroupPolicy( group, name );
      if ( policy != null ) {
        db.recast( PolicyEntity.class ).delete( policy );
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to remove policy " + name + " in " + this.delegate );
      throw new AuthException( "Failed to remove policy", e );
    }
  }
  
  @Override
  public List<Authorization> lookupAuthorizations( String resourceType ) throws AuthException {
    String userId = this.delegate.getId( );
    if ( resourceType == null ) {
      throw new AuthException( "Empty resource type" );
    }
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) db
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.or( 
                      Restrictions.eq( "effect", EffectType.Allow ),
                      Restrictions.eq( "effect", EffectType.Deny ) ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true )
          .createCriteria( "users" ).setCacheable( true ).add(Restrictions.idEq( userId ) )
          .list( );
      db.commit( );
      List<Authorization> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup authorization for user with ID " + userId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup auth", e );
    }
  }
  
  @Override
  public List<Authorization> lookupQuotas( String resourceType ) throws AuthException {
    String userId = this.delegate.getId( );
    EntityWrapper<AuthorizationEntity> db = EntityWrapper.get( AuthorizationEntity.class );
    try {
      @SuppressWarnings( "unchecked" )
      List<AuthorizationEntity> authorizations = ( List<AuthorizationEntity> ) db
          .createCriteria( AuthorizationEntity.class ).setCacheable( true ).add(
              Restrictions.and(
                  Restrictions.eq( "type", resourceType ),
                  Restrictions.eq( "effect", EffectType.Limit ) ) )
          .createCriteria( "statement" ).setCacheable( true )
          .createCriteria( "policy" ).setCacheable( true )
          .createCriteria( "group" ).setCacheable( true )
          .createCriteria( "users" ).add(Restrictions.idEq( userId ) )
          .list( );
      db.commit( );
      List<Authorization> results = Lists.newArrayList( );
      for ( AuthorizationEntity auth : authorizations ) {
        results.add( new DatabaseAuthorizationProxy( auth ) );
      }
      return results;
    } catch ( Throwable e ) {
      db.rollback( );
      Debugging.logError( LOG, e, "Failed to lookup quotas for user with ID " + userId + ", type=" + resourceType);
      throw new AuthException( "Failed to lookup quota", e );
    }
  }

  @Override
  public boolean isSystemInternal( ) {
    try {
      return DatabaseAuthUtils.isSystemAccount( this.getAccount( ).getName( ) );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      return false;
    }
  }

}
