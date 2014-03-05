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
package com.eucalyptus.auth.policy

import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.api.PolicyEngine
import static com.eucalyptus.auth.api.PolicyEngine.AuthorizationMatch.All
import com.eucalyptus.auth.entities.AuthorizationEntity
import com.eucalyptus.auth.entities.PolicyEntity
import com.eucalyptus.auth.entities.StatementEntity
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
                                      String requestType,
                                      String requestAction,
                                      String resourceAccountNumber,
                                      String resourceName ) {
    List<Authorization> authorizations = authorizations( PolicyParser.instance.parse( policy ) )
    PolicyEngine engine = new PolicyEngineImpl( accountResolver( ) )
    PolicyEngineImpl.AuthEvaluationContextImpl context = new PolicyEngineImpl.AuthEvaluationContextImpl( requestType, requestAction, user(), [:] as Map<String,String> ){
      @Override boolean isSystemUser() { true }
      @Override List<Authorization> lookupGlobalAuthorizations() { [] }
      @Override List<Authorization> lookupLocalAuthorizations() { authorizations }
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

  private List<Authorization> authorizations( PolicyEntity policy ) {
    policy.statements.collect{ StatementEntity statement ->
      statement.authorizations.collect { AuthorizationEntity authorization ->
        new AuthorizationEntityAsAuthorization( authorization )
    } }.flatten() as List<Authorization>
  }

  @TupleConstructor private static class AuthorizationEntityAsAuthorization implements Authorization {
    @Delegate AuthorizationEntity entity
    @Override List<Condition> getConditions() { [] } //TODO:Conditions from statement
    @Override Group getGroup() { null }
    @Override Principal getPrincipal() { null }
  }
}
