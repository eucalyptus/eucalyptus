package com.eucalyptus.auth.policy.condition;

@PolicyCondition( { Conditions.BOOL } )
public class Bool implements ConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return Boolean.valueOf( key ) == Boolean.valueOf( value );
  }
  
}
