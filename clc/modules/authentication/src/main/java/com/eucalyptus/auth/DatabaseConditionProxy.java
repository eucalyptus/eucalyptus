package com.eucalyptus.auth;

import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

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
  public Set<String> getValues( ) {
    return this.delegate.getValues( );
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( ConditionEntity.class, this.delegate.getId( ), new Tx<ConditionEntity>( ) {
        public void fire( ConditionEntity t ) throws Throwable {
          sb.append( "Condition(" );
          sb.append( "ID=" ).append( t.getId( ) ).append( ", " );
          sb.append( "type=" ).append( t.getType( ) ).append( ", " );
          sb.append( "key=" ).append( t.getKey( ) ).append( ", " );
          sb.append( "values=" ).append( t.getValues( ) );
          sb.append( ")" );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }
  
}
