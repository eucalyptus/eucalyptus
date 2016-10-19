/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudformation.policy

import com.eucalyptus.auth.policy.PolicySpec
import com.eucalyptus.auth.policy.ern.Ern
import com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec
import spock.lang.Specification

import static com.eucalyptus.auth.policy.ern.Ern.parse


/**
 *
 */
class CloudFormationErnSpecification extends Specification {

  def setupSpec( ) {
    Ern.registerServiceErnBuilder( new CloudFormationErnBuilder( ) )
  }

  def 'should support reqular and wildcard arns'() {
    expect: 'arn parts are parsed correctly'
    parse( arn ).region == region
    parse( arn ).account == account
    parse( arn ).resourceType == PolicySpec.qualifiedName( CloudFormationPolicySpec.VENDOR_CLOUDFORMATION, type )
    parse( arn ).resourceName == resource

    where:
    region       | account        | type    | resource                                       | arn
    'eucalyptus' | '000070854946' | 'stack' | '*'                                            | 'arn:aws:cloudformation:eucalyptus:000070854946:stack/*'
    null         | '000070854946' | 'stack' | 'mystack/*'                                    | 'arn:aws:cloudformation::000070854946:stack/mystack/*'
    null         | '000070854946' | 'stack' | 'mystack/384f0199-3ccd-479a-ba59-35e9e1d4ad59' | 'arn:aws:cloudformation::000070854946:stack/mystack/384f0199-3ccd-479a-ba59-35e9e1d4ad59'
  }
}
