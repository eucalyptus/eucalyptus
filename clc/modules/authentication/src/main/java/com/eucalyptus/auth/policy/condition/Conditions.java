package com.eucalyptus.auth.policy.condition;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * IAM condition constants
 * 
 * @author wenye
 *
 */
public class Conditions {

  public static final String STRINGEQUALS = "StringEquals";
  public static final String STRINGEQUALS_S = "streq";
  
  public static final String STRINGNOTEQUALS = "StringNotEquals";
  public static final String STRINGNOTEQUALS_S = "strneq";
  
  public static final String STRINGEQUALSIGNORECASE = "StringEqualsIgnoreCase";
  public static final String STRINGEQUALSIGNORECASE_S = "streqi";
  
  public static final String STRINGNOTEQUALSIGNORECASE = "StringNotEqualsIgnoreCase";
  public static final String STRINGNOTEQUALSIGNORECASE_S = "strneqi";
  
  public static final String STRINGLIKE = "StringLike";
  public static final String STRINGLIKE_S = "strl";
  
  public static final String STRINGNOTLIKE = "StringNotLike";
  public static final String STRINGNOTLIKE_S = "strnl";
  
  public static final String NUMERICEQUALS = "NumericEquals";
  public static final String NUMERICEQUALS_S = "numeq";
  
  public static final String NUMERICNOTEQUALS = "NumericNotEquals";
  public static final String NUMERICNOTEQUALS_S = "numneq";
  
  public static final String NUMERICLESSTHAN = "NumericLessThan";
  public static final String NUMERICLESSTHAN_S = "numlt";
  
  public static final String NUMERICLESSTHANEQUALS = "NumericLessThanEquals";
  public static final String NUMERICLESSTHANEQUALS_S = "numlteq";
  
  public static final String NUMERICGREATERTHAN = "NumericGreaterThan";
  public static final String NUMERICGREATERTHAN_S = "numgt";
  
  public static final String NUMERICGREATERTHANEQUALS = "NumericGreaterThanEquals";
  public static final String NUMERICGREATERTHANEQUALS_S = "numgteq";
  
  public static final String DATEEQUALS = "DateEquals";
  public static final String DATEEQUALS_S = "dateeq";
  
  public static final String DATENOTEQUALS = "DateNotEquals";
  public static final String DATENOTEQUALS_S = "dateneq";
  
  public static final String DATELESSTHAN = "DateLessThan";
  public static final String DATELESSTHAN_S = "datelt";
  
  public static final String DATELESSTHANEQUALS = "DateLessThanEquals";
  public static final String DATELESSTHANEQUALS_S = "datelteq";
  
  public static final String DATEGREATERTHAN = "DateGreaterThan";
  public static final String DATEGREATERTHAN_S = "dategt";
  
  public static final String DATEGREATERTHANEQUALS = "DateGreaterThanEquals";
  public static final String DATEGREATERTHANEQUALS_S = "dategteq";
  
  public static final String BOOL = "Bool";
  
  public static final String IPADDRESS = "IpAddress";
  
  public static final String NOTIPADDRESS = "NotIpAddress";
  
  // Eucalyptus extensions
  public static final String QUOTA = "Quota";
  
  public static final Map<String, ConditionOp> CONDITION_MAP = Maps.newHashMap( );
  
  static {
    ConditionOp op = new StringEquals( );
    CONDITION_MAP.put( STRINGEQUALS, op );
    CONDITION_MAP.put( STRINGEQUALS_S, op );
    op = new StringNotEquals( );
    CONDITION_MAP.put( STRINGNOTEQUALS, op );
    CONDITION_MAP.put( STRINGNOTEQUALS_S, op );
    op = new StringEqualsIgnoreCase( );
    CONDITION_MAP.put( STRINGEQUALSIGNORECASE, op );
    CONDITION_MAP.put( STRINGEQUALSIGNORECASE_S, op );
    op = new StringNotEqualsIgnoreCase( );
    CONDITION_MAP.put( STRINGNOTEQUALSIGNORECASE, op );
    CONDITION_MAP.put( STRINGNOTEQUALSIGNORECASE_S, op );
    op = new StringLike( );
    CONDITION_MAP.put( STRINGLIKE, op );
    CONDITION_MAP.put( STRINGLIKE_S, op );
    op = new StringNotLike( );
    CONDITION_MAP.put( STRINGNOTLIKE, op );
    CONDITION_MAP.put( STRINGNOTLIKE_S, op );
    op = new NumericEquals( );
    CONDITION_MAP.put( NUMERICEQUALS, op );
    CONDITION_MAP.put( NUMERICEQUALS_S, op );
    op = new NumericNotEquals( );
    CONDITION_MAP.put( NUMERICNOTEQUALS, op );
    CONDITION_MAP.put( NUMERICNOTEQUALS_S, op );
    op = new NumericLessThan( );
    CONDITION_MAP.put( NUMERICLESSTHAN, op );
    CONDITION_MAP.put( NUMERICLESSTHAN_S, op );
    op = new NumericLessThanEquals( );
    CONDITION_MAP.put( NUMERICLESSTHANEQUALS, op );
    CONDITION_MAP.put( NUMERICLESSTHANEQUALS_S, op );
    op = new NumericGreaterThan( );
    CONDITION_MAP.put( NUMERICGREATERTHAN, op );
    CONDITION_MAP.put( NUMERICGREATERTHAN_S, op );
    op = new NumericGreaterThanEquals( );
    CONDITION_MAP.put( NUMERICGREATERTHANEQUALS, op );
    CONDITION_MAP.put( NUMERICGREATERTHANEQUALS_S, op );
    op = new DateEquals( );
    CONDITION_MAP.put( DATEEQUALS, op );
    CONDITION_MAP.put( DATEEQUALS_S, op );
    op = new DateNotEquals( );
    CONDITION_MAP.put( DATENOTEQUALS, op );
    CONDITION_MAP.put( DATENOTEQUALS_S, op );
    op = new DateLessThan( );
    CONDITION_MAP.put( DATELESSTHAN, op );
    CONDITION_MAP.put( DATELESSTHAN_S, op );
    op = new DateLessThanEquals( );
    CONDITION_MAP.put( DATELESSTHANEQUALS, op );
    CONDITION_MAP.put( DATELESSTHANEQUALS_S, op );
    op = new DateGreaterThan( );
    CONDITION_MAP.put( DATEGREATERTHAN, op );
    CONDITION_MAP.put( DATEGREATERTHAN_S, op );
    op = new DateGreaterThanEquals( );
    CONDITION_MAP.put( DATEGREATERTHANEQUALS, op );
    CONDITION_MAP.put( DATEGREATERTHANEQUALS_S, op );
      
    CONDITION_MAP.put( BOOL, new Bool( ) );
      
    CONDITION_MAP.put( IPADDRESS, new IpAddress( ) );
      
    CONDITION_MAP.put( NOTIPADDRESS, new NotIpAddress( ) );
    
    CONDITION_MAP.put( QUOTA, new Quota( ) );
  }
  
}
