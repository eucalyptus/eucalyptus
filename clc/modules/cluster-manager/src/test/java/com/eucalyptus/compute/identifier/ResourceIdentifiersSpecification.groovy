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

package com.eucalyptus.compute.identifier

import spock.lang.Specification

/**
 * Specification for EC2 identifiers (e.g. i-abcdef01)
 */
class ResourceIdentifiersSpecification extends Specification {
  
  def 'should allow allow arbitrary prefix length and case'() {
    expect: 'identifier round trip when input is normalized'
    ResourceIdentifiers.parse( prefix, identifier ).identifier == identifier
    
    where:
    prefix  | identifier
    'i'     | 'i-00000000'
    'vol'   | 'vol-00000000'
    'XXXXX' | 'XXXXX-00000000'
    'XX-XX' | 'XX-XX-00000000'
  }
  
  def 'prefix must not be empty'() {
    when: 'parsing an invalid identifier'
    ResourceIdentifiers.parse( prefix, identifier )
    
    then: 'it fails'
    thrown( InvalidResourceIdentifier )

    where:
    prefix | identifier
    ''     | '-00000000'
    null   | '-00000000'
    ''     | '00000000'
    null   | '00000000'
  }

  def 'prefix must match expected prefix'() {
    when: 'parsing an identifier that does not match the expected prefix'
    ResourceIdentifiers.parse( prefix, identifier )

    then: 'it fails'
    thrown( InvalidResourceIdentifier )

    where:
    prefix | identifier
    'i'    | 'I-00000000'
    'I'    | 'i-00000000'
    'Vol'  | 'vol-00000000'
    'vol'  | 'vo-00000000'
    'vol'  | 'voll-00000000'
    ''     | '-00000000'
  }

  def 'hex length is eight'() {
    when: 'parsing input with invalid length'
    ResourceIdentifiers.parse( prefix, identifier )

    then: 'it fails'
    thrown( InvalidResourceIdentifier )

    where:
    prefix | identifier
    'i'    | 'i-AAAAAAA'
    'vol'  | 'vol-fffffffff'
    'snap' | 'snap-a'
    'emi'  | 'emi-'
    'ami'  | 'ami'
  }

  def 'identifier hex lower case'() {
    expect: 'round tripped identifier has hex in lower case'
    ResourceIdentifiers.parse( prefix, identifier ).identifier == normalized

    where:
    prefix  | identifier       | normalized
    'i'     | 'i-A0000000'     | 'i-a0000000'
    'vol'   | 'vol-00F00000'   | 'vol-00f00000'
    'XXXXX' | 'XXXXX-ABCDEF00' | 'XXXXX-abcdef00'
    'XX-XX' | 'XX-XX-A0000000' | 'XX-XX-a0000000'
  }

  def 'string value is identifier'() {
    expect: 'toString is identifier'
    ResourceIdentifiers.parse( prefix, identifier ).toString() == identifier

    where:
    prefix  | identifier       
    'i'     | 'i-a0000000'     
    'vol'   | 'vol-00f0000a'   
  }

  def 'any prefix allowed if not given'() {
    expect: 'parsing is successful'
    ResourceIdentifiers.parse( identifier ).identifier == identifier

    where:
    identifier << [ 'i-a0000000', 'vol-00f0000a' ]
  }

}
