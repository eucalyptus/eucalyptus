/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
