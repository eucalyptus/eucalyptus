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
  
  private static final Map<String, Class<? extends ConditionOp>> CONDITION_MAP = Maps.newHashMap( );
  
  public synchronized static boolean registerCondition( String op, Class<? extends ConditionOp> conditionClass ) {
    if ( CONDITION_MAP.containsKey( op ) ) {
      return false;
    }
    CONDITION_MAP.put( op, conditionClass );
    return true;
  }

  public static Class<? extends ConditionOp> getConditionOpClass( String op ) {
    return CONDITION_MAP.get( op );
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
