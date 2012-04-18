package com.eucalyptus.auth.policy.condition;

import java.util.regex.Pattern;
import com.eucalyptus.auth.policy.PatternUtils;

@PolicyCondition( { Conditions.STRINGNOTLIKE, Conditions.STRINGNOTLIKE_S } )
public class StringNotLike implements StringConditionOp {
  
  @Override
  public boolean check( String key, String value ) {
    String pattern = PatternUtils.toJavaPattern( value.toLowerCase( ) );
    return !Pattern.matches( pattern, key );
  }
  
}
