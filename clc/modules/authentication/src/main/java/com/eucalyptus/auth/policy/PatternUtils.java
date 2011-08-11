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
