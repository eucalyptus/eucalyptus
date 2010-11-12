package com.eucalyptus.auth;

import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.principal.Condition;

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

}
