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
  
  public static final Map<String, Class<? extends ConditionOp>> CONDITION_MAP = Maps.newHashMap( );
  
  static {
    CONDITION_MAP.put( STRINGEQUALS, StringEquals.class );
    CONDITION_MAP.put( STRINGEQUALS_S, StringEquals.class );
    CONDITION_MAP.put( STRINGNOTEQUALS, StringNotEquals.class );
    CONDITION_MAP.put( STRINGNOTEQUALS_S, StringNotEquals.class );
    CONDITION_MAP.put( STRINGEQUALSIGNORECASE, StringEqualsIgnoreCase.class );
    CONDITION_MAP.put( STRINGEQUALSIGNORECASE_S, StringEqualsIgnoreCase.class );
    CONDITION_MAP.put( STRINGNOTEQUALSIGNORECASE, StringNotEqualsIgnoreCase.class );
    CONDITION_MAP.put( STRINGNOTEQUALSIGNORECASE_S, StringNotEqualsIgnoreCase.class );
    CONDITION_MAP.put( STRINGLIKE, StringLike.class );
    CONDITION_MAP.put( STRINGLIKE_S, StringLike.class );
    CONDITION_MAP.put( STRINGNOTLIKE, StringNotLike.class );
    CONDITION_MAP.put( STRINGNOTLIKE_S, StringNotLike.class );
    CONDITION_MAP.put( NUMERICEQUALS, NumericEquals.class );
    CONDITION_MAP.put( NUMERICEQUALS_S, NumericEquals.class );
    CONDITION_MAP.put( NUMERICNOTEQUALS, NumericNotEquals.class );
    CONDITION_MAP.put( NUMERICNOTEQUALS_S, NumericNotEquals.class );
    CONDITION_MAP.put( NUMERICLESSTHAN, NumericLessThan.class );
    CONDITION_MAP.put( NUMERICLESSTHAN_S, NumericLessThan.class );
    CONDITION_MAP.put( NUMERICLESSTHANEQUALS, NumericLessThanEquals.class );
    CONDITION_MAP.put( NUMERICLESSTHANEQUALS_S, NumericLessThanEquals.class );
    CONDITION_MAP.put( NUMERICGREATERTHAN, NumericGreaterThan.class );
    CONDITION_MAP.put( NUMERICGREATERTHAN_S, NumericGreaterThan.class );
    CONDITION_MAP.put( NUMERICGREATERTHANEQUALS, NumericGreaterThanEquals.class );
    CONDITION_MAP.put( NUMERICGREATERTHANEQUALS_S, NumericGreaterThanEquals.class );
    CONDITION_MAP.put( DATEEQUALS, DateEquals.class );
    CONDITION_MAP.put( DATEEQUALS_S, DateEquals.class );
    CONDITION_MAP.put( DATENOTEQUALS, DateNotEquals.class );
    CONDITION_MAP.put( DATENOTEQUALS_S, DateNotEquals.class );
    CONDITION_MAP.put( DATELESSTHAN, DateLessThan.class );
    CONDITION_MAP.put( DATELESSTHAN_S, DateLessThan.class );
    CONDITION_MAP.put( DATELESSTHANEQUALS, DateLessThanEquals.class );
    CONDITION_MAP.put( DATELESSTHANEQUALS_S, DateLessThanEquals.class );
    CONDITION_MAP.put( DATEGREATERTHAN, DateGreaterThan.class );
    CONDITION_MAP.put( DATEGREATERTHAN_S, DateGreaterThan.class );
    CONDITION_MAP.put( DATEGREATERTHANEQUALS, DateGreaterThanEquals.class );
    CONDITION_MAP.put( DATEGREATERTHANEQUALS_S, DateGreaterThanEquals.class );
    CONDITION_MAP.put( BOOL, Bool.class );
    CONDITION_MAP.put( IPADDRESS, IpAddress.class );
    CONDITION_MAP.put( NOTIPADDRESS, NotIpAddress.class );
  }
  
  public static ConditionOp getOpInstance( Class<? extends ConditionOp> opClass ) {
    try {
      ConditionOp op = opClass.newInstance( );
      return op;
    } catch ( IllegalAccessException e ) {
      throw new RuntimeException( "Can not find condition type " + opClass.getName( ), e );
    } catch ( InstantiationException e ) {
      throw new RuntimeException( "Can not find condition type " + opClass.getName( ), e );
    } catch ( ExceptionInInitializerError e ) {
      throw new RuntimeException( "Can not find condition type " + opClass.getName( ), e );
    } catch ( SecurityException e ) {
      throw new RuntimeException( "Can not find condition type " + opClass.getName( ), e );
    }
  }
  
}
