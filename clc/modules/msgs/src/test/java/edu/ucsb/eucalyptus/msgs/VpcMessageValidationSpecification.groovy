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
package edu.ucsb.eucalyptus.msgs

import spock.lang.Specification

import static edu.ucsb.eucalyptus.msgs.VpcMessageValidation.FieldRegexValue.*

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
