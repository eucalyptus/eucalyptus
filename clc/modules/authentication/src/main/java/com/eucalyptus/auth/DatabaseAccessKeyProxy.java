package com.eucalyptus.auth;

import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseAccessKeyProxy implements AccessKey {

  private static final long serialVersionUID = 1L;
  
  private static final Logger LOG = Logger.getLogger( DatabaseAccessKeyProxy.class );
  
  private AccessKeyEntity delegate;
  
  public DatabaseAccessKeyProxy( AccessKeyEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String getId( ) {
    return this.delegate.getId( );
  }
  
  @Override
  public Boolean isActive( ) {
    return this.delegate.isActive( );
  }
  
  @Override
  public void setActive( final Boolean active ) throws AuthException {
    try {
      Transactions.one( AccessKeyEntity.class, this.delegate.getId( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) throws Throwable {
          t.setActive( active );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setActive for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public String getKey( ) {
    return this.delegate.getKey( );
  }
  
  @Override
  public void setKey( final String key ) throws AuthException {
    try {
      Transactions.one( AccessKeyEntity.class, this.delegate.getId( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) throws Throwable {
          t.setKey( key );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setKey for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public Date getCreateDate( ) {
    return this.delegate.getCreateDate( );
  }
  
  @Override
  public void setCreateDate( final Date createDate ) throws AuthException {
    try {
      Transactions.one( AccessKeyEntity.class, this.delegate.getId( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) throws Throwable {
          t.setCreateDate( createDate );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setCreateDate for " + this.delegate );
      throw new AuthException( e );
    } 
  }
  
  @Override
  public User getUser( ) throws AuthException {
    final List<User> results = Lists.newArrayList( );
    try {
      Transactions.one( AccessKeyEntity.class, this.delegate.getId( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) throws Throwable {
          results.add( new DatabaseUserProxy( t.getUser( ) ) );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getUser for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }
  
}
