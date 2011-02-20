package com.eucalyptus.auth.policy.condition;

@PolicyCondition( { Conditions.STRINGNOTEQUALS, Conditions.STRINGNOTEQUALS_S } )
public class StringNotEquals implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    return !key.equals( value );
  }
  
}
