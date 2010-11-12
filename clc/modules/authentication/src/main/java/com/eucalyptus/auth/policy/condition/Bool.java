package com.eucalyptus.auth.policy.condition;

public class Bool implements ConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return key.equalsIgnoreCase( value );
  }
  
}
