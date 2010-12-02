package com.eucalyptus.auth.policy.condition;

import java.util.regex.Pattern;
import com.eucalyptus.auth.policy.PatternUtils;

@PolicyCondition( { Conditions.STRINGLIKE, Conditions.STRINGLIKE_S } )
public class StringLike implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    String pattern = PatternUtils.toJavaPattern( value.toLowerCase( ) );
    return Pattern.matches( pattern, key );
  }
  
}
