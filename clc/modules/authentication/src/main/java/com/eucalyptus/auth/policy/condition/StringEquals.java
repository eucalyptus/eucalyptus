package com.eucalyptus.auth.policy.condition;

@PolicyCondition( { Conditions.STRINGEQUALS, Conditions.STRINGEQUALS_S } )
public class StringEquals implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return key.equals( value );
  }
  
}
