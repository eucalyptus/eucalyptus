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

package com.eucalyptus.compute.identifier

import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers
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
