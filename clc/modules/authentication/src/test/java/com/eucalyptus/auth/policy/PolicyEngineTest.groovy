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
package com.eucalyptus.auth.policy

import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.api.PolicyEngine
import com.eucalyptus.auth.policy.ern.Ern
import com.eucalyptus.auth.policy.ern.EuareErnBuilder
import com.eucalyptus.auth.principal.PolicyScope
import com.eucalyptus.auth.principal.PolicyVersion
import com.eucalyptus.crypto.Digest
import com.eucalyptus.crypto.util.B64
import com.google.common.base.Suppliers
import org.junit.Before
import org.junit.BeforeClass

import java.nio.charset.StandardCharsets

import static com.eucalyptus.auth.api.PolicyEngine.AuthorizationMatch.All

import com.eucalyptus.auth.principal.Authorization
import com.eucalyptus.auth.principal.Condition
import com.eucalyptus.auth.principal.Group
import com.eucalyptus.auth.principal.Principal
import groovy.transform.TupleConstructor
import groovy.transform.TypeChecked
import org.junit.Test
import com.eucalyptus.auth.principal.User
import com.eucalyptus.auth.principal.TestUser
import com.eucalyptus.auth.principal.TestAccount
import com.google.common.base.Function

/**
 *
 */
@TypeChecked
class PolicyEngineTest {

  @BeforeClass
  public static void beforeClass( ){
    Ern.registerServiceErnBuilder( new EuareErnBuilder( ) )
  }

  @Test
  public void testPersonaRolePolicyAccountCreate(  ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "iam:*"
          ],
          "NotResource": "arn:aws:iam::eucalyptus:*"
        } ]
      }
      """.stripIndent(), "iam:account", "iam:CreateAccount", "123456789012", "" )
  }

  @Test
  public void testPersonaRolePolicyAccountDelete(  ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "iam:*"
          ],
          "NotResource": "arn:aws:iam::eucalyptus:*"
        } ]
      }
    """.stripIndent(), "iam:account", "iam:DeleteAccount", "123456789012", "/test" )
  }

  @Test
  public void testPersonaRolePolicyListAccounts(  ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "iam:*"
          ],
          "NotResource": "arn:aws:iam::eucalyptus:*"
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  public void testPersonaRolePolicyEucalyptusListUsers(  ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "iam:*"
          ],
          "NotResource": "arn:aws:iam::eucalyptus:*"
        } ]
      }
    """.stripIndent(), "iam:user", "iam:ListUsers", "010101010101", "/admin" )
  }

  @Test
  public void testPersonaRolePolicyOtherListUsers(  ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "iam:*"
          ],
          "NotResource": "arn:aws:iam::eucalyptus:*"
        } ]
      }
    """.stripIndent(), "iam:user", "iam:ListUsers", "123456789012", "/admin" )
  }

  private void evaluateAuthorization( String policy,
                                      String resourceType,
                                      String requestAction,
                                      String resourceAccountNumber,
                                      String resourceName ) {
    PolicyEngine engine = new PolicyEngineImpl( accountResolver( ), Suppliers.ofInstance( Boolean.FALSE ) )
    PolicyEngineImpl.AuthEvaluationContextImpl context = new PolicyEngineImpl.AuthEvaluationContextImpl( resourceType, requestAction, user(), [:] as Map<String,String>, [ new PolicyVersion(){
      @Override String getPolicyVersionId( ) { '1234567890' }
      @Override String getPolicyName( ) { 'test' }
      @Override PolicyScope getPolicyScope() { PolicyScope.User }
      @Override String getPolicy( ) { policy }
      @Override String getPolicyHash() { B64.standard.encString( Digest.SHA256.digestBinary( getPolicy( ).getBytes( StandardCharsets.UTF_8 ) ) ) }
    } ] as List<PolicyVersion> ){
      @Override boolean isSystemUser() { true }
    }
    engine.evaluateAuthorization( context, All, resourceAccountNumber, resourceName, [:] )
  }

  private User user() {
    new TestUser( name: "test", accountNumber: "111111111111", account: new TestAccount( accountNumber: "111111111111", name: "test" ) ).activate( )
  }

  private Function<String, String> accountResolver( ) {
    // 010101010101 is the accountNumber for eucalyptus account in this test
    { String account -> "010101010101" } as Function<String, String>
  }

}
