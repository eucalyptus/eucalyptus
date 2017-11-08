/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.policy

import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.api.PolicyEngine
import com.eucalyptus.auth.policy.ern.Ern
import com.eucalyptus.auth.policy.ern.EuareErnBuilder
import com.eucalyptus.auth.policy.ern.ResourceNameSupport
import com.eucalyptus.auth.policy.ern.ServiceErnBuilder
import com.eucalyptus.auth.principal.PolicyScope
import com.eucalyptus.auth.principal.PolicyVersion
import com.eucalyptus.crypto.Digest
import com.eucalyptus.crypto.util.B64
import com.eucalyptus.util.Strings
import com.google.common.base.Suppliers
import com.google.common.base.Supplier
import net.sf.json.JSONException
import org.junit.Before
import org.junit.BeforeClass

import java.nio.charset.StandardCharsets

import static com.eucalyptus.auth.api.PolicyEngine.AuthorizationMatch.All

import com.eucalyptus.auth.principal.Authorization
import com.eucalyptus.auth.principal.Condition
import com.eucalyptus.auth.principal.Principal
import groovy.transform.TupleConstructor
import groovy.transform.TypeChecked
import org.junit.Test
import com.eucalyptus.auth.principal.User
import com.eucalyptus.auth.principal.TestUser
import com.google.common.base.Function

/**
 *
 */
@TypeChecked
class PolicyEngineTest {

  @BeforeClass
  static void beforeClass( ){
    Ern.registerServiceErnBuilder( new EuareErnBuilder( ) )
    Ern.registerServiceErnBuilder( new ServiceErnBuilder( [ 'testservice' ] ) {
      @Override
      Ern build( String ern, String service, String region, String account, String resource ) throws JSONException {
        return new ResourceNameSupport( service, region, account, Strings.substringBefore('/').apply(resource), Strings.substringAfter('/').apply(resource) ) { }
      }
    } )
  }

  @Test
  public void testServiceMatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "*"
          ],
          "Resource": "arn:aws:testservice:::resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test( expected = AuthException.class )
  public void testServiceMismatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "*"
          ],
          "Resource": "arn:aws:testservice:::resourcetype/*"
        } ]
      }
      """.stripIndent(), "othertestservice:resourcetype", "othertestservice:Foo", "010101010101", "" )
  }

  @Test
  public void testRegionMatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:region-1:eucalyptus:resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test
  public void testRegionNotSpecifiedForResource( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice::eucalyptus:resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test( expected = AuthException.class )
  public void testRegionMismatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:region-2:eucalyptus:resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test
  public void testAccountMatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:*:eucalyptus:resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test
  public void testAccountNotSpecifiedForResource( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:*::resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test( expected = AuthException.class )
  public void testAccountMismatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:*:111111111111:resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test
  public void testResourceTypeMatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:::resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:resourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test( expected = AuthException.class )
  public void testResourceTypeMismatch( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": [
            "testservice:*"
          ],
          "Resource": "arn:aws:testservice:::resourcetype/*"
        } ]
      }
      """.stripIndent(), "testservice:otherresourcetype", "testservice:Foo", "010101010101", "" )
  }

  @Test
  void testPersonaRolePolicyAccountCreate(  ) {
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
  void testPersonaRolePolicyAccountDelete(  ) {
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
  void testPersonaRolePolicyListAccounts(  ) {
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
  void testPersonaRolePolicyEucalyptusListUsers(  ) {
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
  void testPersonaRolePolicyOtherListUsers(  ) {
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
    PolicyEngine engine = new PolicyEngineImpl( accountResolver( ), Suppliers.ofInstance( Boolean.FALSE ), { 'region-1' } as Supplier<String> )
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
    new TestUser( name: "test", accountNumber: "111111111111" ).activate( )
  }

  private Function<String, String> accountResolver( ) {
    // 010101010101 is the accountNumber for eucalyptus account in this test
    { String account -> 'eucalyptus'==account ? "010101010101" : account } as Function<String, String>
  }

}
