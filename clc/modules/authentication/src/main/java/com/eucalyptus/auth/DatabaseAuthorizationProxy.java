package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseAuthorizationProxy implements Authorization {
  
  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseAuthorizationProxy.class );

  private AuthorizationEntity delegate;
  
  public DatabaseAuthorizationProxy( AuthorizationEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public EffectType getEffect( ) {
    return this.delegate.getEffect( );
  }

  @Override
  public String getActionPattern( ) {
    return this.delegate.getActionPattern( );
  }

  @Override
  public String getResourceType( ) {
    return this.delegate.getResourceType( );
  }

  @Override
  public String getResourcePattern( ) {
    return this.delegate.getResourcePattern( );
  }

  @Override
  public List<? extends Condition> getConditions( ) {
    final List<DatabaseConditionProxy> results = Lists.newArrayList( );
    try {
      Transactions.one( AuthorizationEntity.class, this.delegate.getId( ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) throws Throwable {
          for ( Condition c : t.getConditions( ) ) {
            results.add( new DatabaseConditionProxy( ( ConditionEntity ) c ) );
          }
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to getConditions for " + this.delegate );
    }
    return results;
  }

  @Override
  public String toString( ) {
    return this.delegate.toString( );
  }

  @Override
  public Boolean isNotAction( ) {
    return this.delegate.isNotAction( );
  }

  @Override
  public Boolean isNotResource( ) {
    return this.delegate.isNotResource( );
  }
  
}
