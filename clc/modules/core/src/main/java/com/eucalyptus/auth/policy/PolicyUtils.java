/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.auth.policy;

import java.util.regex.Pattern;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.google.common.base.Function;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public class PolicyUtils {

  private static final Pattern ESCAPE_PATTERN = Pattern.compile( "([^a-zA-Z0-9*?])" );
  private static final Pattern WILDCARD_MULTIPLE_PATTERN = Pattern.compile( "([*])" );
  private static final Pattern WILDCARD_SINGLE_PATTERN = Pattern.compile( "([?])" );

  private static final Interner<String> stringInterner = Interners.newWeakInterner( );
  private static final Interner<Authorization> authorizationInterner = Interners.newWeakInterner( );
  private static final Interner<PolicyPrincipal> principalInterner = Interners.newWeakInterner( );
  private static final Interner<Condition> conditionInterner = Interners.newWeakInterner( );
  private static final Interner<PolicyPolicy> policyInterner = Interners.newWeakInterner( );

  private static final Function<String,String> stringInternFunction = Interners.asFunction( stringInterner );
  private static final Function<Authorization,Authorization> authorizationInternFunction = Interners.asFunction( authorizationInterner );
  private static final Function<Condition,Condition> conditionInternFunction = Interners.asFunction( conditionInterner );

  /**
   * Convert an IAM policy pattern (action pattern or resource pattern with * and ?)
   * to a canonical Java regex Pattern.
   */
  public static String toJavaPattern( String pattern ) {
    String result = pattern;
    
    if ( pattern == null ) {
      return null;
    }
    result = ESCAPE_PATTERN.matcher( result ).replaceAll( "\\\\$1" );
    result = WILDCARD_SINGLE_PATTERN.matcher( result ).replaceAll( "." );
    result = WILDCARD_MULTIPLE_PATTERN.matcher( result ).replaceAll( ".*" );
    
    return result;
  }

  static String intern( final String string ) {
    return string == null ? null : stringInterner.intern( string );
  }

  static PolicyPrincipal intern( final PolicyPrincipal principal ) {
    return principal == null ? null : principalInterner.intern( principal );
  }

  static PolicyPolicy intern( final PolicyPolicy policy ) {
    return policy == null ? null : policyInterner.intern( policy );
  }

  public static <T> T checkParam( final T value,
                                  final Matcher<? super T> matcher ) {
    return checkParam( "", value, matcher );
  }

  public static <T> T checkParam( final String reason,
                                  final T value,
                                  final Matcher<? super T> matcher ) {
    if ( !matcher.matches( value ) ) {
      final Description description = new StringDescription();
      description.appendText( reason )
          .appendText( "\nExpected: " )
          .appendDescriptionOf( matcher )
          .appendText( "\n     but: " );
      matcher.describeMismatch( value, description );

      throw new IllegalArgumentException( description.toString() );
    }

    return value;
  }

  static Function<String,String> internString( ) {
    return stringInternFunction;
  }

  static Function<Authorization,Authorization> internAuthorization( ) {
    return authorizationInternFunction;
  }

  static Function<Condition,Condition> internCondition( ) {
    return conditionInternFunction;
  }
}
