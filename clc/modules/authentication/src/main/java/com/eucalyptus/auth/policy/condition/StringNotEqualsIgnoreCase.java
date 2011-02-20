package com.eucalyptus.auth.policy.condition;

@PolicyCondition( { Conditions.STRINGNOTEQUALSIGNORECASE, Conditions.STRINGNOTEQUALSIGNORECASE_S } )
public class StringNotEqualsIgnoreCase implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return !key.equalsIgnoreCase( value );
  }
  
}
