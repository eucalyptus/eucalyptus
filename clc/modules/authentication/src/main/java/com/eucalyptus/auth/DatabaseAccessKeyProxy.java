package com.eucalyptus.auth;

import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AccessKeyEntity;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
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
  public Boolean isActive( ) {
    return this.delegate.isActive( );
  }
  
  @Override
  public void setActive( final Boolean active ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( AccessKeyEntity.class, "accessKey", this.delegate.getAccessKey( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) {
          t.setActive( active );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setActive for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public String getSecretKey( ) {
    return this.delegate.getSecretKey( );
  }
  
//  @Override
  public void setSecretKey( final String key ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( AccessKeyEntity.class, "accessKey", this.delegate.getAccessKey( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) {
          t.setSecretKey( key );
        }
      } );
    } catch ( ExecutionException e ) {
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
      DatabaseAuthUtils.invokeUnique( AccessKeyEntity.class, "accessKey", this.delegate.getAccessKey( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) {
          t.setCreateDate( createDate );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setCreateDate for " + this.delegate );
      throw new AuthException( e );
    } 
  }
  
  @Override
  public User getUser( ) throws AuthException {
    final List<User> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( AccessKeyEntity.class, "accessKey", this.delegate.getAccessKey( ), new Tx<AccessKeyEntity>( ) {
        public void fire( AccessKeyEntity t ) {
          results.add( new DatabaseUserProxy( t.getUser( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getUser for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }

  @Override
  public String getAccessKey( ) {
    return this.delegate.getAccessKey( );
  }
  
}
