/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package edu.ucsb.eucalyptus.msgs

import spock.lang.Specification

import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRegexValue.*

/**
 *
 */
class VpcMessageValidationSpecification extends Specification {

  def 'regexes should match expected values'( ) {
    expect: 'regex matching as expected'
    regex.pattern( ).matcher( text ).matches( ) == matches

    where:
    regex        | text                 | matches
    CIDR         | '0.0.0.0/0'          | true
    CIDR         | '255.255.255.255/32' | true
    CIDR         | '255.255.255.255/33' | false
    CIDR         | '-1.255.255.255/32'  | false
    CIDR         | '0.0.0.0./0'         | false
    CIDR         | '0.0.0.0//0'         | false
    CIDR         | '0.0.0./0/0'         | false
    CIDR         | '0.0.0.0/0 '         | false
    CIDR         | ' 0.0.0.0/0'         | false
    CIDR         | 'a.0.0.0/0'          | false
    CIDR         | '256.0.0.0/0'        | false
    CIDR         | '999.0.0.0/0'        | false
    IP_ADDRESS   | '0.0.0.0'            | true
    IP_ADDRESS   | '255.255.255.255'    | true
    IP_ADDRESS   | '100.100.100.100'    | true
    IP_ADDRESS   | '1.10.100.200'       | true
    IP_ADDRESS   | '1.10.100.'          | false
    IP_ADDRESS   | '1.10.100'           | false
    IP_ADDRESS   | '1.10.100.-1'        | false
    IP_ADDRESS   | '1.10.100.-1.'       | false
    IP_ADDRESS   | 'a.0.0.0'            | false
    IP_ADDRESS   | ''                   | false
    IP_ADDRESS   | 'foo'                | false
    EC2_PROTOCOL | 'icmp'               | true
    EC2_PROTOCOL | 'tcp'                | true
    EC2_PROTOCOL | 'udp'                | true
    EC2_PROTOCOL | '-1'                 | true
    EC2_PROTOCOL | '0'                  | true
    EC2_PROTOCOL | '100'                | true
    EC2_PROTOCOL | '255'                | true
    EC2_PROTOCOL | 'foo'                | false
    EC2_PROTOCOL | '-2'                 | false
    EC2_PROTOCOL | '256'                | false
  }
}
