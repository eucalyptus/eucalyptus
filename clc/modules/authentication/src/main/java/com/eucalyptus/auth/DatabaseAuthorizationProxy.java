package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
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
  public Boolean isNegative( ) {
    return this.delegate.isNegative( );
  }

  @Override
  public List<? extends Condition> getConditions( ) {
    List<DatabaseConditionProxy> results = Lists.newArrayList( );
    for ( Condition c : this.delegate.getConditions( ) ) {
      results.add( new DatabaseConditionProxy( ( ConditionEntity ) c ) );
    }
    return results;
  }
  
}
