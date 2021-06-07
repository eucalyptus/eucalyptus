/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.util

import spock.lang.Specification

import java.util.regex.Pattern

import static com.eucalyptus.util.Strings.concat
import static com.eucalyptus.util.Strings.regexReplace
import static com.eucalyptus.util.Strings.substringAfter
import static com.eucalyptus.util.Strings.substringBefore

/**
 *
 */
class StringsSpecification extends Specification {

  def 'should support preceding substring extraction'() {
    expect: 'substring before match'
    substringBefore( match, text ) == substring

    where:
    text         | match     | substring
    '0.0.0.0/0'  | '/'       | '0.0.0.0'
    '0.0.0.0/0'  | '0'       | ''
    '0.0.0.0/0'  | '0/0'     | '0.0.0.'
    'aabbccdde'  | 'e'       | 'aabbccdd'
    'aabbccdde'  | ''        | ''
  }

  def 'should support following substring extraction'() {
    expect: 'substring after match'
    substringAfter( match, text ) == substring

    where:
    text         | match     | substring
    '0.0.0.0/0'  | '/'       | '0'
    '0.0.0.0/0'  | '0'       | '.0.0.0/0'
    '0.0.0.0/0'  | '0/0'     | ''
    'aabbccdde'  | 'a'       | 'abbccdde'
    'aabbccdde'  | ''        | 'aabbccdde'
  }

  def 'should result in empty substring with no match'() {
    expect: 'empty result'
    substringBefore( match, text ) == ''
    substringAfter( match, text ) == ''

    where:
    text         | match
    '0.0.0.0/0'  | 'x'
    '0.0.0.0/0'  | ' '
  }

  def 'should support regular expression replacement'() {
    expect: 'replacement or default text'
    regexReplace( Pattern.compile( regex), replacement, defaultValue ).apply( text ) == result

    where:
    text      | regex         | replacement | defaultValue | result
    'a=b'     | 'a=([a-z]+)'  | '$1'        | ''           | 'b'
    'a=b'     | 'a=([a-z]+)'  | '\\$1'      | ''           | '$1'
    'a='      | 'a=([a-z]+)'  | '$1'        | 'def'        | 'def'
    null      | 'a=([a-z]+)'  | '$1'        | 'def'        | 'def'
    null      | 'a=([a-z]+)'  | '$1'        | null         | null
    'aaa'     | 'a(.*)'       | 'b$1'       | null         | 'baa'
    'a\nb\nc' | '(?s).*(b).*' | '$1'        | null         | 'b'
  }

  def 'should support char sequence concatenation'() {
    expect: 'concatenated character sequence'
    concat( sequences ).toString( ) == result

    where:
    result       | sequences
    ''           | [ '' ]
    'foo'        | [ 'foo' ]
    'foobar'     | [ 'foo', 'bar' ]
    'foobarbaz'  | [ 'foo', 'bar', 'baz' ]

  }

  def 'should support char sequence concatenation with range'() {
    expect: 'concatenated character sequence'
    concat( sequences, start, end ).toString( ) == result

    where:
    result       | start | end | sequences
    ''           |     0 |   0 | [ '' ]
    'fo'         |     0 |   2 | [ 'foo' ]
    'fo'         |     0 |   2 | [ 'foo', 'bar' ]
    'obarb'      |     2 |   7 | [ 'foo', 'bar', 'baz' ]
  }

}
