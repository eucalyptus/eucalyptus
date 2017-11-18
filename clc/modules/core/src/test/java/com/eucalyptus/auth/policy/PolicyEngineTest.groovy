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

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.api.PolicyEngine
import com.eucalyptus.auth.policy.condition.ConditionOp
import com.eucalyptus.auth.policy.condition.Conditions
import com.eucalyptus.auth.policy.condition.NullConditionOp
import com.eucalyptus.auth.policy.condition.NumericGreaterThan
import com.eucalyptus.auth.policy.condition.StringEquals
import com.eucalyptus.auth.policy.ern.Ern
import com.eucalyptus.auth.policy.ern.EuareErnBuilder
import com.eucalyptus.auth.policy.ern.ResourceNameSupport
import com.eucalyptus.auth.policy.ern.ServiceErnBuilder
import com.eucalyptus.auth.policy.key.Key
import com.eucalyptus.auth.policy.key.Keys
import com.eucalyptus.auth.policy.key.PolicyKey
import com.eucalyptus.auth.policy.key.RegisteredKeyProvider
import com.eucalyptus.auth.principal.PolicyScope
import com.eucalyptus.auth.principal.PolicyVersion
import com.eucalyptus.auth.principal.Principal
import com.eucalyptus.auth.principal.TypedPrincipal
import com.eucalyptus.crypto.Digest
import com.eucalyptus.crypto.util.B64
import com.eucalyptus.util.Strings
import com.google.common.base.Suppliers
import com.google.common.base.Supplier
import com.google.common.collect.ImmutableSet
import net.sf.json.JSONException
import org.junit.BeforeClass

import java.nio.charset.StandardCharsets

import static com.eucalyptus.auth.api.PolicyEngine.AuthorizationMatch.All

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
    Conditions.registerCondition( Conditions.STRINGEQUALS, StringEquals, true )
    Conditions.registerCondition( Conditions.NUMERICGREATERTHAN, NumericGreaterThan, true )
    Conditions.registerCondition( 'Null', NullConditionOp, false )
    Keys.registerKeyProvider( new RegisteredKeyProvider( ) )
    Keys.registerKey( 'test:nokey', TestNoKey )
    Keys.registerKey( 'test:key', TestKey )
    Keys.registerKey( 'test:keys', TestKeys )
    Keys.registerKey( 'test:keysize', TestKeySize )
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

  /**
   * Verify that a resource policy can grant access to all users (wildcard principal)
   */
  @Test
  void testResourcePolicyWildcardAuth( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "foo:*",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:*",
              "Principal": "*"
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify that a resource policy by itself is sufficient to grant access to users (user arn) in the same account
   */
  @Test
  void testResourcePolicyUserArnOnlyAuth( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "foo:*",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "arn:aws:iam::111111111111:user/test" }
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify that a resource policy by itself is not sufficient to grant access to users (account arn) in the same account
   */
  @Test( expected = AuthException.class )
  void testResourcePolicyAccountNumberOnlyAuthDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "foo:*",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify that a resource policy does not authorize when principal does not match
   */
  @Test( expected = AuthException.class )
  void testResourcePolicyOnlyPrincipalMismatchAuthDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "foo:*",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "arn:aws:iam::111111111111:user/testsomeotheruser" }
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify that a resource policy by itself is not sufficient to grant access to users in another account
   */
  @Test( expected = AuthException.class )
  void testResourcePolicyOnlyAuthDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "foo:*",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "arn:aws:iam::111111111111:user/test" }
            } ]
          }
        """.stripIndent(),
        '010101010101', false, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify that a resource policy and account iam policy (user principal) is sufficient to grant access to users in another account
   */
  @Test
  void testResourcePolicyAndUserPolicyAuth( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "arn:aws:iam::111111111111:user/test" }
            } ]
          }
        """.stripIndent(),
        '010101010101', false, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify that a resource policy and account iam policy (account principal arn) is sufficient to grant access to users in another account
   */
  @Test
  void testResourcePolicyAndAccountArnPolicyAuth( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "arn:aws:iam::111111111111:root" }
            } ]
          }
        """.stripIndent(),
        '010101010101', false, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify that a resource policy and account iam policy (account principal arn) is sufficient to grant access to users in another account
   */
  @Test
  void testResourcePolicyAndAccountNumberPolicyAuth( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '010101010101', false, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify that a user policy is sufficient to grant access when a resource policy is present
   */
  @Test
  void testResourcePolicyDefaultAndUserPolicyAuth( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "s3:*",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify that a user policy does not grant access when denied by a resource policy (user principal)
   */
  @Test( expected = AuthException.class )
  void testResourcePolicyUserArnDenyAndUserPolicyAuthDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Deny",
              "Action": "ec2:*",
              "Principal": { "AWS": "arn:aws:iam::111111111111:user/test" }
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify that a user policy does not grant access when denied by a resource policy (account principal)
   */
  @Test( expected = AuthException.class )
  void testResourcePolicyAccountNumberDenyAndUserPolicyAuthDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Deny",
              "Action": "ec2:*",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '111111111111', false, "ec2:image", "ec2:DescribeImages", "111111111111", "emi-00000000" )
  }

  /**
   * Verify access is denied when there is a resource policy but also an external auth mechanism such as an s3 acl which does not permit
   */
  @Test( expected = AuthException.class )
  void testExternalResourcePermissionDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "s3:*",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '010101010101', false, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify access is allowed when there is a resource policy but also an external auth mechanism such as an s3 acl
   */
  @Test
  void testExternalResourcePermission( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "s3:*",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '010101010101', true, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify that a resource policy with ownership other than the resource or caller cannot authorize
   */
  @Test( expected = AuthException.class )
  void testResourcePolicyDistinctOwnerAuthDenied( ) {
    evaluateAuthorization(
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Resource": "*"
            } ]
          }
        """.stripIndent(),
        """\
          {
            "Statement":[ {
              "Effect": "Allow",
              "Action": "ec2:DescribeImages",
              "Principal": { "AWS": "111111111111" }
            } ]
          }
        """.stripIndent(),
        '000000111111', false, "ec2:image", "ec2:DescribeImages", "010101010101", "emi-00000000" )
  }

  /**
   * Verify that a resource policy with ownership other than the resource or caller can deny access
   */
  @Test
  void testResourcePolicyDistinctOwnerAuthDeny( ) {
  }

  @Test
  void testStringEqualsCondition( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "StringEquals": {
              "test:Key": [ "value", "wrong value" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testStringEqualsConditionAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "StringEquals": {
              "test:Key": [ "wrong value" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testStringEqualsIfExistsCondition( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "StringEqualsIfExists": {
              "test:Key": [ "value" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testStringEqualsIfExistsConditionAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "StringEqualsIfExists": {
              "test:Key": [ "wrong value" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testStringEqualsIfExistsConditionNotExists( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "StringEqualsIfExists": {
              "test:NoKey": [ "wrong value" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testStringEqualsConditionForAllValues( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "ForAllValues:StringEquals": {
              "test:Keys": [ "value1", "value2", "value3" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testStringEqualsConditionForAllValuesAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "ForAllValues:StringEquals": {
              "test:Keys": [ "value1", "value2" ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testStringEqualsConditionForAnyValue( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "ForAnyValue:StringEquals": {
              "test:Keys": [ "value1"  ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testStringEqualsConditionForAnyValueAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "ForAnyValue:StringEquals": {
              "test:Keys": [ "wrong value"  ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNullConditionStringValue( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "Null": {
              "test:NoKey": "true"
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testNullConditionStringValueAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "Null": {
              "test:Key": "true"
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNullCondition( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "Null": {
              "test:NoKey": true
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNotNullCondition( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "Null": {
              "test:Key": false
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNumericGreaterThanCondition( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:KeySize": 0
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNumericGreaterThanConditionMultipleValues( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:KeySize": [ 0, 0, -1, -19231 ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNumericGreaterThanConditionMultipleValuesMixed( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:KeySize": [ 0, "0", -1, "-19231.9", 0.99 ]
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNumericGreaterThanConditionDecimal( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:KeySize": 0.1
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testNumericGreaterThanConditionAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:KeySize": 1
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNumericGreaterThanConditionStringValue( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:KeySize": "0"
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test( expected = AuthException.class )
  void testNumericGreaterThanConditionNoValueAuthDenied( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThan": {
              "test:NoKey": "0"
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
  }

  @Test
  void testNumericGreaterThanIfExistsCondition( ) {
    evaluateAuthorization( """\
      {
        "Statement":[ {
          "Effect": "Allow",
          "Action": "*",
          "Resource": "*",
          "Condition": {
            "NumericGreaterThanIfExists": {
              "test:NoKey": "0"
            }
          }
        } ]
      }
    """.stripIndent(), "iam:account", "iam:ListAccounts", "123456789012", "/admin" )
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

  private void evaluateAuthorization( String policy,
                                      String resourcePolicy,
                                      String resourcePolicyAccountNumber,
                                      boolean requestAccountDefaultAllow,
                                      String resourceType,
                                      String requestAction,
                                      String resourceAccountNumber,
                                      String resourceName ) {
    PolicyEngine engine = new PolicyEngineImpl( accountResolver( ), Suppliers.ofInstance( Boolean.FALSE ), { 'region-1' } as Supplier<String> )
    User user = user( )
    PolicyEngineImpl.AuthEvaluationContextImpl context = new PolicyEngineImpl.AuthEvaluationContextImpl( resourceType, requestAction, user, [:] as Map<String,String>, [ new PolicyVersion(){
      @Override String getPolicyVersionId( ) { '1234567890' }
      @Override String getPolicyName( ) { 'test' }
      @Override PolicyScope getPolicyScope() { PolicyScope.User }
      @Override String getPolicy( ) { policy }
      @Override String getPolicyHash() { B64.standard.encString( Digest.SHA256.digestBinary( getPolicy( ).getBytes( StandardCharsets.UTF_8 ) ) ) }
    } ] as List<PolicyVersion>,
        ImmutableSet.of(
            TypedPrincipal.of( Principal.PrincipalType.AWS, Accounts.getUserArn( user ) ),
            TypedPrincipal.of( Principal.PrincipalType.AWS, Accounts.getAccountArn( user.getAccountNumber( ) ) )
        )
    ){
      @Override boolean isSystemUser() { true }
    }
    PolicyVersion resourcePolicyVersion = new PolicyVersion(){
      @Override String getPolicyVersionId( ) { 'res1234567890' }
      @Override String getPolicyName( ) { 'resource policy' }
      @Override PolicyScope getPolicyScope() { PolicyScope.Resource }
      @Override String getPolicy( ) { resourcePolicy }
      @Override String getPolicyHash() { B64.standard.encString( Digest.SHA256.digestBinary( getPolicy( ).getBytes( StandardCharsets.UTF_8 ) ) ) }
    }
    engine.evaluateAuthorization( context, requestAccountDefaultAllow, resourcePolicyVersion, resourcePolicyAccountNumber, resourceAccountNumber, resourceName, [:] )
  }
  private User user() {
    new TestUser( name: 'test', accountNumber: '111111111111', path: '/' ).activate( )
  }

  private Function<String, String> accountResolver( ) {
    // 010101010101 is the accountNumber for eucalyptus account in this test
    { String account -> 'eucalyptus'==account ? "010101010101" : account } as Function<String, String>
  }

  @PolicyKey('test:nokey')
  static class TestNoKey implements Key {
    @Override String value( ) { null }
    @Override void validateConditionType( Class<? extends ConditionOp> conditionClass ) { }
    @Override boolean canApply( String action) { true }
  }

  @PolicyKey('test:key')
  static class TestKey implements Key {
    @Override String value( ) { 'value' }
    @Override void validateConditionType( Class<? extends ConditionOp> conditionClass ) { }
    @Override boolean canApply( String action) { true }
  }

  @PolicyKey('test:keys')
  static class TestKeys implements Key {
    @Override String value( ) { 'value1,value2,value3' }
    @Override Set<String> values( ) { ['value1','value2','value3'] as Set<String> }
    @Override void validateConditionType( Class<? extends ConditionOp> conditionClass ) { }
    @Override boolean canApply( String action) { true }
  }

  @PolicyKey('test:keysize')
  static class TestKeySize implements Key {
    @Override String value( ) { '1' }
    @Override void validateConditionType( Class<? extends ConditionOp> conditionClass ) { }
    @Override boolean canApply( String action) { true }
  }

}
