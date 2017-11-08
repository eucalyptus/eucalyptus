/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
