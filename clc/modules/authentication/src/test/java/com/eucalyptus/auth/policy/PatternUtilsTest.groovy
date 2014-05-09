/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth.policy

import static org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

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
  void testEscapedMatch( ) {
    assertMatches "a-z", "a-z"
    assertMatches "a/c", "a/c"
    assertMatches "a?-", "ab-"
    assertMatches "a^b", "a^b"
    assertMatches "<>,./;:'\"\\|}{][+=_-)(&^%\$#@!~`", "<>,./;:'\"\\|}{][+=_-)(&^%\$#@!~`"
    assertNotMatches ".", "\\a"
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
