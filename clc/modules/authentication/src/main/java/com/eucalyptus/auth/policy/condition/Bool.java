package com.eucalyptus.auth.policy.condition;

public class Bool implements ConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return Boolean.valueOf( key ) == Boolean.valueOf( value );
  }
  
}
