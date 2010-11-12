package com.eucalyptus.auth;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AccountEntity;
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
  public X509Certificate getX509Certificate( String id ) {
    return this.delegate.getX509Certificate( id );
  }
  
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    return this.delegate.getAllX509Certificates( );
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
      Debugging.logError( LOG, e, "Failed to setX509Certificate for " + this.delegate );
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
  public String getSecretKey( String id ) {
    return this.delegate.getSecretKey( id );
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
  public String getInfo( String key ) {
    return this.delegate.getInfo( key );
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
      Debugging.logError( LOG, e, "Failed to change user name for " + this.delegate.getName( ) );
      db.rollback( );
    }
  }

  @Override
  public Map<String, String> getInfoMap( ) {
    return this.delegate.getInfoMap( );
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
    List<DatabaseGroupProxy> groups = Lists.newArrayList( );
    for ( Group g : this.delegate.getGroups( ) ) {
      GroupEntity ge = ( GroupEntity ) g;
      if ( !ge.isUserGroup( ) ) {
        groups.add( new DatabaseGroupProxy( ge ) );
      }
    }
    return groups;
  }
  
  @Override
  public Account getAccount( ) {
    return new DatabaseAccountProxy( ( AccountEntity ) this.delegate.getAccount( ) );
  }

  @Override
  public String lookupX509Certificate( X509Certificate cert ) {
    return this.delegate.lookupX509Certificate( cert );
  }

  @Override
  public String lookupSecretKeyId( String key ) {
    return this.delegate.lookupSecretKeyId( key );
  }

  @Override
  public boolean isSystemAdmin( ) {
    return this.delegate.isSystemAdmin( );
  }

  @Override
  public String getFirstActiveSecretKeyId( ) {
    return this.delegate.getFirstActiveSecretKeyId( );
  }
  
}
