package com.eucalyptus.auth.policy.condition;

public interface ConditionOp {

  public boolean check( String key, String value );
  
}
