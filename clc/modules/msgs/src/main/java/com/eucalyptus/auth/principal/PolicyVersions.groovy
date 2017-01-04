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
package com.eucalyptus.auth.principal

import com.eucalyptus.crypto.Digest
import com.eucalyptus.crypto.util.B64
import com.google.common.base.Function
import com.google.common.io.BaseEncoding
import groovy.transform.CompileStatic
import groovy.transform.Immutable

import java.nio.charset.StandardCharsets

/**
 *
 */
@CompileStatic
class PolicyVersions {

  private static final String ADMINISTRATOR_POLICY = '''\
    {
      "Version": "2012-10-17",
      "Statement": [{
        "Effect": "Allow",
        "Action": ["*"],
        "Resource": ["*"]
      }]
    }
    '''.stripIndent( );

  private static final PolicyVersion ADMINISTRATOR = new PolicyVersionImpl(
      'arn:aws:iam::eucalyptus:policy/Administrator,1',
      'Administrator',
      PolicyScope.User,
      ADMINISTRATOR_POLICY,
      hash( ADMINISTRATOR_POLICY )
  );

  static PolicyVersion getAdministratorPolicy( ) {
    return ADMINISTRATOR;
  }

  public static Function<Policy,PolicyVersion> policyVersion( final PolicyScope scope, final String scopeArn ) {
    {
      Policy sourcePolicy ->
        new PolicyVersionImpl(
          "${scopeArn}/policy/${sourcePolicy.name},${sourcePolicy.policyVersion}",
          sourcePolicy.name,
          scope,
          sourcePolicy.text,
          hash( sourcePolicy.text )
        )
    } as Function<Policy,PolicyVersion>
  }

  /**
   * Policy version for a managed policy.
   */
  public static PolicyVersion policyVersion( final Policy sourcePolicy, final String policyArn ) {
    new PolicyVersionImpl(
        "${policyArn},${sourcePolicy.policyVersion}",
        sourcePolicy.name,
        PolicyScope.Managed,
        sourcePolicy.text,
        hash( sourcePolicy.text )
    )
  }

  public static String hash( String text ) {
    return BaseEncoding.base64( ).encode( Digest.SHA256.digestBinary( text.getBytes( StandardCharsets.UTF_8 ) ) );
  }


  @Immutable
  private static class PolicyVersionImpl implements PolicyVersion {
    String policyVersionId
    String policyName
    PolicyScope policyScope
    String policy
    String policyHash
  }
}
