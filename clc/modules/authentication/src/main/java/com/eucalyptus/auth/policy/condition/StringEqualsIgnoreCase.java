package com.eucalyptus.auth.policy.condition;

@PolicyCondition( { Conditions.STRINGEQUALSIGNORECASE, Conditions.STRINGEQUALSIGNORECASE_S } )
public class StringEqualsIgnoreCase implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return key.equalsIgnoreCase( value );
  }
  
}
