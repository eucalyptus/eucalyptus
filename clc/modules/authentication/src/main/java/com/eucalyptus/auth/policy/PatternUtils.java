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
 ************************************************************************/

package com.eucalyptus.auth.policy;

import java.util.regex.Pattern;

public class PatternUtils {

  private static final Pattern ESCAPE_PATTERN = Pattern.compile( "([^a-zA-z0-9*?])" );
  private static final Pattern WILDCARD_MULTIPLE_PATTERN = Pattern.compile( "([*])" );
  private static final Pattern WILDCARD_SINGLE_PATTERN = Pattern.compile( "([?])" );
  
  /**
   * Convert an IAM policy pattern (action pattern or resource pattern with * and ?)
   * to a canonical Java regex Pattern.
   * 
   * @param policyPattern
   * @return
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

}
