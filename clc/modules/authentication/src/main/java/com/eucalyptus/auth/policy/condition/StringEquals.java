package com.eucalyptus.auth.policy.condition;

public class StringEquals implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return key.equals( value );
  }
  
}
