/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy.condition;

import java.util.Map;
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
