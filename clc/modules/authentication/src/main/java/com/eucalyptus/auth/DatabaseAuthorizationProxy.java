package com.eucalyptus.auth;

import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
  public List<Condition> getConditions( ) {
    final List<Condition> results = Lists.newArrayList( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          for ( ConditionEntity c : t.getStatement( ).getConditions( ) ) {
            results.add( new DatabaseConditionProxy( c ) );
          }
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getConditions for " + this.delegate );
    }
    return results;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }

  @Override
  public Boolean isNotAction( ) {
    return this.delegate.isNotAction( );
  }

  @Override
  public Boolean isNotResource( ) {
    return this.delegate.isNotResource( );
  }

  @Override
  public String getType( ) {
    return this.delegate.getType( );
  }

  @Override
  public Set<String> getActions( ) {
    final Set<String> results = Sets.newHashSet( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          results.addAll( t.getActions( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getActions for " + this.delegate );
    }
    return results;
  }

  @Override
  public Set<String> getResources( ) {
    final Set<String> results = Sets.newHashSet( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          results.addAll( t.getResources( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getResources for " + this.delegate );
    }
    return results;
  }

  @Override
  public Group getGroup( ) {
    final List<Group> results = Lists.newArrayList( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          results.add( new DatabaseGroupProxy( t.getStatement( ).getPolicy( ).getGroup( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getGroup for " + this.delegate );
    }
    return results.get( 0 );
  }
  
}
