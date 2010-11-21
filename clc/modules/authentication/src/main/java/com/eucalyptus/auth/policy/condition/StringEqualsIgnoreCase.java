package com.eucalyptus.auth.policy.condition;

public class StringEqualsIgnoreCase implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return key.equalsIgnoreCase( value );
  }
  
}
