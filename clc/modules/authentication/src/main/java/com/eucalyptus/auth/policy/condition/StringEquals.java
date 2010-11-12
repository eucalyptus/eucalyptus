package com.eucalyptus.auth.policy.condition;

public class StringEquals implements ConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return key.equals( value );
  }
  
}
