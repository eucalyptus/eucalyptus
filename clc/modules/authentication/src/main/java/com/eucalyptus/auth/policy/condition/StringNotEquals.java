package com.eucalyptus.auth.policy.condition;

public class StringNotEquals implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return !key.equals( value );
  }
  
}
