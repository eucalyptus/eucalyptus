/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy.condition;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.Strings;
import com.google.common.collect.Maps;

/**
 * IAM condition constants
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

  public static final String IF_EXISTS_SUFFIX = "IfExists";

  public static final String FOR_ALL_VALUES_PREFIX = "ForAllValues:";

  public static final String FOR_ANY_VALUE_PREFIX = "ForAnyValue:";

  private static final Map<String, Class<? extends ConditionOp>> CONDITION_MAP = Maps.newHashMap( );
  
  public synchronized static boolean registerCondition(
      @Nonnull final String op,
      @Nonnull final Class<? extends ConditionOp> conditionClass,
               final boolean addIfExists
  ) {
    if ( CONDITION_MAP.containsKey( op ) ) {
      return false;
    }
    CONDITION_MAP.put( op, conditionClass );
    if ( addIfExists ) {
      CONDITION_MAP.put( op + IF_EXISTS_SUFFIX, conditionClass );
    }
    return true;
  }

  public static Class<? extends ConditionOp> getConditionOpClass( String op ) {
    return CONDITION_MAP.get( stripPrefix( op ) );
  }
  
  public static ConditionOp getOpInstance( final String op ) {
    final Class<? extends ConditionOp> opClass = getConditionOpClass( op );
    try {
      return set( op, conditional( op, opClass.newInstance( ) ) );
    } catch ( final
        IllegalAccessException |
        InstantiationException |
        SecurityException |
        ExceptionInInitializerError e ) {
      throw new RuntimeException( "Can not find condition type " + opClass.getName( ), e );
    }
  }

  private static ConditionOp conditional( final String name,
                                          final ConditionOp conditionOp ) {
    if ( name.endsWith( IF_EXISTS_SUFFIX ) ) {
      return new IfExistsDelegatingConditionOp( conditionOp );
    } else {
      return conditionOp;
    }
  }

  private static ConditionOp set( final String name,
                                  final ConditionOp conditionOp ) {

    if ( name.startsWith( FOR_ANY_VALUE_PREFIX ) ) {
      return new ForAnyValueDelegatingConditionOp( conditionOp );
    } else if ( name.startsWith( FOR_ALL_VALUES_PREFIX ) ) {
      return new ForAllValuesDelegatingConditionOp( conditionOp );
    } else {
      return conditionOp;
    }
  }

  private static String stripPrefix( final String op ) {
    return Strings.trimPrefix(
        FOR_ANY_VALUE_PREFIX,
        Strings.trimPrefix( FOR_ALL_VALUES_PREFIX, op ) );
  }

  private static class IfExistsDelegatingConditionOp implements ConditionOp {
    private final ConditionOp delegate;

    private IfExistsDelegatingConditionOp( final ConditionOp conditionOp ) {
      this.delegate = conditionOp;
    }

    @Override
    public boolean check( @Nullable final String key, final String value ) {
      return key == null || delegate.check( key, value );
    }
  }

  private static class ForAnyValueDelegatingConditionOp implements ConditionOp {
    private final ConditionOp delegate;

    private ForAnyValueDelegatingConditionOp( final ConditionOp conditionOp ) {
      this.delegate = conditionOp;
    }

    @Override
    public boolean check( @Nullable final String key, final String value ) {
      return delegate.check( key, value );
    }

    @Override
    public boolean check( final Set<String> keys, final Set<String> values ) {
      boolean success = false;
      for ( final String key : keys ) {
        for ( final String value : values ) {
          success = check( key, value );
          if ( success ) break;
        }
        if ( success ) break;
      }
      return success;
    }
  }

  private static class ForAllValuesDelegatingConditionOp implements ConditionOp {
    private final ConditionOp delegate;

    private ForAllValuesDelegatingConditionOp( final ConditionOp conditionOp ) {
      this.delegate = conditionOp;
    }

    @Override
    public boolean check( @Nullable final String key, final String value ) {
      return delegate.check( key, value );
    }

    @Override
    public boolean check( @Nullable final Set<String> keys, final Set<String> values ) {
      boolean success = true;
      for ( final String key : keys ) {
        for ( final String value : values ) {
          success = check( key, value );
          if ( success ) break;
        }
        if ( !success ) break;
      }
      return success;
    }
  }
}
