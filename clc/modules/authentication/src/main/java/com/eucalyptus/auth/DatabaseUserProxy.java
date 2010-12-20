package com.eucalyptus.auth;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
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
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String getUserId( ) {
    return this.delegate.getUserId( );
  }
  
  @Override
  public X509Certificate getX509Certificate( final String id ) {
    final List<X509Certificate> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.getX509Certificate( id ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getX509Certificate for " + this.delegate );
    }
    return results.get( 0 );
  }
  
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    final List<X509Certificate> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.addAll( t.getAllX509Certificates( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getAllX509Certificates for " + this.delegate );
    }
    return results;
  }
  
  @Override
  public void addX509Certificate( final X509Certificate cert ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.addX509Certificate( cert );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to addX509Certificate for " + this.delegate );
    }
  }
  
  @Override
  public void activateX509Certificate( final String id ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.activateX509Certificate( id );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to activateX509Certificate for " + this.delegate );
    }
  }
  
  @Override
  public void deactivateX509Certificate( final String id ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.deactivateX509Certificate( id );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to deactivateX509Certificate for " + this.delegate );
    }
  }
  
  @Override
  public void revokeX509Certificate( final String id ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.revokeX509Certificate( id );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to revokeX509Certificate for " + this.delegate );
    }
  }
  
  @Override
  public BigInteger getNumber( ) {
    return this.delegate.getNumber( );
  }
  
  @Override
  public String getSecretKey( final String id ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.getSecretKey( id ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getSecretKey for " + this.delegate );
    }
    return results.get( 0 );
  }
  
  @Override
  public void addSecretKey( final String key ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.addSecretKey( key );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to addSecretKey for " + this.delegate );
    }
  }
  
  @Override
  public void activateSecretKey( final String id ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.activateSecretKey( id );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to activateSecretKey for " + this.delegate );
    }
  }
  
  @Override
  public void deactivateSecretKey( final String id ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.deactivateSecretKey( id );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to deactivateSecretKey for " + this.delegate );
    }
  }
  
  @Override
  public void revokeSecretKey( final String id ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.revokeSecretKey( id );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to revokeSecretKey for " + this.delegate );
    }
  }
  
  @Override
  public User getDelegate( ) {
    return this.delegate;
  }
  
  @Override
  public String getPath( ) {
    return this.delegate.getPath( );
  }
  
  @Override
  public RegistrationStatus getRegistrationStatus( ) {
    return this.delegate.getRegistrationStatus( );
  }
  
  @Override
  public void setRegistrationStatus( final RegistrationStatus stat ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setRegistrationStatus( stat );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setRegistrationStatus for " + this.delegate );
    }
  }
  
  @Override
  public Boolean isEnabled( ) {
    return this.delegate.isEnabled( );
  }
  
  @Override
  public void setEnabled( final Boolean enabled ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setEnabled( enabled );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setEnabled for " + this.delegate );
    }
  }
  
  @Override
  public boolean checkToken( String testToken ) {
    return this.delegate.checkToken( testToken );
  }
  
  @Override
  public String getConfirmationCode( ) {
    return this.delegate.getConfirmationCode( );
  }
  
  @Override
  public String getPassword( ) {
    return this.delegate.getPassword( );
  }
  
  @Override
  public Long getPasswordExpires( ) {
    return this.delegate.getPasswordExpires( );
  }
  
  @Override
  public void setPasswordExpires( final Long time ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setPasswordExpires( time );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setPasswordExpires for " + this.delegate );
    }
  }
  
  @Override
  public void setPassword( final String password ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setPassword( password );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setPassword for " + this.delegate );
    }
  }
  
  @Override
  public String getInfo( final String key ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.getInfo( key ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getInfo for " + this.delegate );
    }
    return results.get( 0 );
  }
  
  @Override
  public void setInfo( final String key, final String value ) {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setInfo( key, value );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setInfo for " + this.delegate );
    }
  }

  @Override
  public void setName( String name ) {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    EntityManager em = db.getEntityManager( );
    try {
      UserEntity user = em.find( UserEntity.class, this.delegate.getId( ) );
      user.setName( name );
      for ( Group g : user.getGroups( ) ) {
        GroupEntity ge = ( GroupEntity ) g;
        if ( ge.isUserGroup( ) ) {
          ge.setName( DatabaseAuthProvider.getUserGroupName( name ) );
          break;
        }
      }
      db.commit( );
    } catch ( Throwable e ) {
      Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
      db.rollback( );
    }
  }

  @Override
  public Map<String, String> getInfoMap( ) {
    final Map<String, String> results = Maps.newHashMap( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.putAll( t.getInfoMap( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getInfoMap for " + this.delegate );
    }
    return results;
  }

  @Override
  public void setInfo( final Map<String, String> newInfo ) throws AuthException {
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          t.setInfo( newInfo );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setInfo for " + this.delegate );
    }
  }
  
  @Override
  public List<? extends Group> getGroups( ) {
    final List<DatabaseGroupProxy> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          for ( Group g : t.getGroups( ) ) {
            GroupEntity ge = ( GroupEntity ) g;
            if ( !ge.isUserGroup( ) ) {
              results.add( new DatabaseGroupProxy( ge ) );
            }
          }
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getGroups for " + this.delegate );
    }
    return results;
  }
  
  @Override
  public Account getAccount( ) {
    final List<DatabaseAccountProxy> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( new DatabaseAccountProxy( ( AccountEntity) t.getAccount( ) ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getAccount for " + this.delegate );
    }
    return results.get( 0 );
  }

  @Override
  public String lookupX509Certificate( final X509Certificate cert ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.lookupX509Certificate( cert ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to lookupX509Certificate for " + this.delegate );
    }
    return results.get( 0 );
  }

  @Override
  public String lookupSecretKeyId( final String key ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.lookupSecretKeyId( key ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to lookupSecretKeyId for " + this.delegate );
    }
    return results.get( 0 );
  }

  @Override
  public boolean isSystemAdmin( ) {
    return SYSTEM_ADMIN_ACCOUNT_NAME.equals( this.getAccount( ).getName( ) );
  }

  @Override
  public String getFirstActiveSecretKeyId( ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.add( t.getFirstActiveSecretKeyId( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getFirstActiveSecretKeyId for " + this.delegate );
    }
    return results.get( 0 );
  }
  
  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
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
  public List<String> getActiveX509CertificateIds( ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.addAll( t.getActiveX509CertificateIds( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getActiveX509CertificateIds for " + this.delegate );
    }
    return results;
  }

  @Override
  public List<String> getInactiveX509CertificateIds( ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.addAll( t.getInactiveX509CertificateIds( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getInactiveX509CertificateIds for " + this.delegate );
    }
    return results;
  }

  @Override
  public List<String> getActiveSecretKeyIds( ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.addAll( t.getActiveSecretKeyIds( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getActiveSecretKeyIds for " + this.delegate );
    }
    return results;
  }

  @Override
  public List<String> getInactiveSecretKeyIds( ) {
    final List<String> results = Lists.newArrayList( );
    try {
      Transactions.one( UserEntity.class, this.delegate.getId( ), new Tx<UserEntity>( ) {
        public void fire( UserEntity t ) throws Throwable {
          results.addAll( t.getInactiveSecretKeyIds( ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getInactiveSecretKeyIds for " + this.delegate );
    }
    return results;
  }

  @Override
  public boolean isAccountAdmin( ) {
    return this.delegate.isAccountAdmin( );
  }
  
}
