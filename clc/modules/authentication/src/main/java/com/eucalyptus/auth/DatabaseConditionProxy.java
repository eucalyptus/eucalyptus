package com.eucalyptus.auth;

import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Sets;

public class DatabaseConditionProxy implements Condition {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger( DatabaseConditionProxy.class );
  
  private ConditionEntity delegate;
  
  public DatabaseConditionProxy( ConditionEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String getType( ) {
    return this.delegate.getType( );
  }

  @Override
  public String getKey( ) {
    return this.delegate.getKey( );
  }

  @Override
  public Set<String> getValues( ) throws AuthException {
    final Set<String> results = Sets.newHashSet( );
    try {
      Transactions.one( ConditionEntity.newInstanceWithId( this.delegate.getConditionId( ) ), new Tx<ConditionEntity>( ) {
        public void fire( ConditionEntity t ) {
          results.addAll( t.getValues( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getValues for " + this.delegate );
      throw new AuthException( e );
    }
    return results;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( ConditionEntity.newInstanceWithId( this.delegate.getConditionId( ) ), new Tx<ConditionEntity>( ) {
        public void fire( ConditionEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }
  
}
