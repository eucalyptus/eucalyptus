/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
}
