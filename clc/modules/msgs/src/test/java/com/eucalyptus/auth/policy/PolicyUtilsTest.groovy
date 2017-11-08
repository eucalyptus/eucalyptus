/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.auth.policy

import static org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

class PolicyUtilsTest {

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
    Pattern.matches( PolicyUtils.toJavaPattern( pattern ), value )
  }
}
