package com.eucalyptus.auth.policy

import static org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern


/**
 * 
 */
class PatternUtilsTest {

  @Test
  void testBasicMatch( ) {
    assertMatches "a", "a"
    assertMatches "abc", "abc"
    assertNotMatches "a", "b"
    assertNotMatches "a", "aa"
    assertNotMatches "", "a"
  }

  @Test
  void testStarMatch( ) {
    assertMatches "a*", "a"
    assertMatches "*a", "a"
    assertMatches "a*", "ab"
    assertMatches "*a", "ba"
    assertMatches "*a*", "bab"
    assertMatches "*", ""
    assertNotMatches "a*", "b"
    assertNotMatches "a*", "ba"
    assertNotMatches "*a", "ab"
    assertNotMatches "*a", "b"
    assertNotMatches "*a*", "bbb"
  }

  @Test
  void testQuestionMatch( ) {
    assertMatches "?", "a"
    assertMatches "???", "aaa"
    assertMatches "?b", "ab"
    assertMatches "b?", "ba"
    assertNotMatches "??", "a"
  }

  void assertMatches( String pattern, String value ) {
    assertTrue( pattern+"~="+value, matches( pattern, value ) )
  }

  void assertNotMatches( String pattern, String value ) {
    assertFalse( pattern+"!="+value, matches( pattern, value ) )
  }

  boolean matches( String pattern, String value ) {
    Pattern.matches( PatternUtils.toJavaPattern( pattern ), value )
  }
}
