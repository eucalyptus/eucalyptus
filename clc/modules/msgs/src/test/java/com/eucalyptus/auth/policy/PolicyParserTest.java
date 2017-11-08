/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy;

import java.io.File;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.NullConditionOp;
import com.eucalyptus.auth.policy.condition.NumericGreaterThan;
import com.eucalyptus.auth.policy.condition.StringEquals;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareErnBuilder;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.RegisteredKeyProvider;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Principal;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;



public class PolicyParserTest {

  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      System.err.println( "Requires input policy file" );
      System.exit( 1 );
    }

    String policy = Files.toString( new File( args[0] ), Charsets.UTF_8 );

    PolicyPolicy parsed = PolicyParser.getInstance( ).parse( policy );

    printPolicy( parsed );
  }

  private static void printPolicy( PolicyPolicy parsed ) {
    System.out.println( "Version = " + parsed.getPolicyVersion( ) );
    for ( Authorization auth : parsed.getAuthorizations() ) {
      System.out.println( "Authorization: " + auth );
      for ( Condition cond : auth.getConditions( ) ) {
        System.out.println( "Condition: " + cond );
      }
    }
  }

  @BeforeClass
  public static void beforeClass( ) {
    Ern.registerServiceErnBuilder( new EuareErnBuilder( ) );
    Conditions.registerCondition( Conditions.STRINGEQUALS, StringEquals.class, true );
    Conditions.registerCondition( Conditions.NUMERICGREATERTHAN, NumericGreaterThan.class, true );
    Conditions.registerCondition( "Null", NullConditionOp.class, false );
    Keys.registerKeyProvider( new RegisteredKeyProvider( ) );
    Keys.registerKey( "test:key", TestKey.class );
  }

  @Test
  public void testParseUserPolicy() throws Exception {
    String policyJson =
        "{\n" +
            "   \"Statement\":[{\n" +
            "      \"Effect\":\"Allow\",\n" +
            "      \"Action\":\"autoscaling:*\",\n" +
            "      \"Resource\":\"*\"\n" +
            "   }]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations() );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNull( "Authorization principal", authorization.getPrincipal( ) );
    assertEquals( "Authorization actions", Sets.newHashSet( "autoscaling:*" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 1, authorization.getResources().size() );
    assertEquals( "Authorization resources", Sets.newHashSet("*"), authorization.getResources() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
  }

  @Test
  public void testParseSingleStatementPolicy( ) throws Exception {
    String policyJson =
        "{\n" +
        "  \"Statement\": {\n" +
        "    \"Effect\": \"Allow\",\n" +
        "    \"Action\": \"*\",\n" +
        "    \"Resource\": \"*\"\n" +
        "  }\n" +
        "}";

    PolicyPolicy policy = PolicyParser.getInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations() );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNull( "Authorization principal", authorization.getPrincipal( ) );
    assertEquals( "Authorization actions", Sets.newHashSet( "*" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 1, authorization.getResources().size() );
    assertEquals( "Authorization resources", Sets.newHashSet("*"), authorization.getResources() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
  }

  @Test
  public void testNormalizeSingleStatementPolicy( ) throws Exception {
    String policyJson =
        "{\n" +
            "  \"Statement\": {\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Action\": \"*\",\n" +
            "    \"Resource\": \"*\"\n" +
            "  }\n" +
            "}";

    String normalizedPolicyJson = PolicyParser.getInstance().normalize( policyJson );
    assertNotNull( "Policy null", normalizedPolicyJson );
    assertEquals( "Normalized policy",
        "{\"Version\":\"2008-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}",
        normalizedPolicyJson );
  }

  @Test
  public void testNormalizePolicy( ) throws Exception {
    String policyJson =
        "{\n" +
            "  \"Statement\": [{\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Action\": \"*\",\n" +
            "    \"Resource\": \"*\"\n" +
            "  }]\n" +
            "}";

    String normalizedPolicyJson = PolicyParser.getInstance().normalize( policyJson );
    assertNotNull( "Policy null", normalizedPolicyJson );
    assertEquals( "Normalized policy",
        "{\"Version\":\"2008-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}",
        normalizedPolicyJson );
  }

  @Test(expected = PolicyParseException.class )
  public void testParseUserPolicyMissingResource() throws Exception {
    String policyJson =
        "{\n" +
        "   \"Statement\":[{\n" +
        "      \"Effect\":\"Allow\",\n" +
        "      \"Action\":\"autoscaling:*\"\n" +
        "   }]\n" +
        "}";

    PolicyParser.getInstance().parse( policyJson );
  }

  @Test(expected = PolicyParseException.class )
  public void testParseAssumeRolePolicyWithEmptyResource() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\n" +
            "         \"Service\": [ \"ec2.amazonaws.com\" ]\n" +
            "      },\n" +
            "      \"Resource\": []\n" +
            "      \"Action\": [ \"sts:AssumeRole\" ]\n" +
            "    } ]\n" +
            "}";
    PolicyParser.getResourceInstance().parse( policyJson );
  }

  @Test
  public void testParseAssumeRolePolicyWithResource() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\n" +
            "         \"Service\": [ \"ec2.amazonaws.com\" ]\n" +
            "      },\n" +
            "      \"Resource\": \"*\",\n" +
            "      \"Action\": [ \"sts:AssumeRole\" ]\n" +
            "    } ]\n" +
            "}";
    PolicyParser.getResourceInstance().parse( policyJson );
  }

  @Test
  public void testParseAssumeRolePolicyWithResourceArray() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\n" +
            "         \"Service\": [ \"ec2.amazonaws.com\" ]\n" +
            "      },\n" +
            "      \"Resource\": [\"*\"],\n" +
            "      \"Action\": [ \"sts:AssumeRole\" ]\n" +
            "    } ]\n" +
            "}";
    PolicyParser.getResourceInstance().parse( policyJson );
  }

  @Test
  public void testParseAssumeRolePolicy() throws Exception {
    String policyJson =
        "{\n" +
        "    \"Statement\": [ {\n" +
        "      \"Effect\": \"Allow\",\n" +
        "      \"Principal\": {\n" +
        "         \"Service\": [ \"ec2.amazonaws.com\" ]\n" +
        "      },\n" +
        "      \"Action\": [ \"sts:AssumeRole\" ]\n" +
        "    } ]\n" +
        "}";

    PolicyPolicy policy = PolicyParser.getResourceInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Statement authorizations", policy.getAuthorizations() );
    assertEquals( "Statement authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization principal", authorization.getPrincipal() );
    Principal principal = authorization.getPrincipal();
    assertEquals( "Principal type", Principal.PrincipalType.Service, principal.getType() );
    assertEquals( "Principal values", Sets.newHashSet( "ec2.amazonaws.com" ), principal.getValues() );
    assertEquals( "Principal not", false, principal.isNotPrincipal() );
    assertNotNull( "Authorization null", authorization );
    assertEquals( "Authorization actions", Sets.newHashSet( "sts:assumerole" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 0, authorization.getResources().size() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
  }

  @Test
  public void testParseFederatedAssumeRolePolicy() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\n" +
            "         \"Federated\": [ \"https://auth.globus.org\" ]\n" +
            "      },\n" +
            "      \"Action\": [ \"sts:AssumeRole\" ]\n" +
            "    } ]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getResourceInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Statement authorizations", policy.getAuthorizations() );
    assertEquals( "Statement authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization principal", authorization.getPrincipal() );
    Principal principal = authorization.getPrincipal();
    assertEquals( "Principal type", Principal.PrincipalType.Federated, principal.getType() );
    assertEquals( "Principal values", Sets.newHashSet( "https://auth.globus.org" ), principal.getValues() );
    assertEquals( "Principal not", false, principal.isNotPrincipal() );
    assertNotNull( "Authorization null", authorization );
    assertEquals( "Authorization actions", Sets.newHashSet( "sts:assumerole" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 0, authorization.getResources().size() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
  }

  @Test(expected = PolicyParseException.class )
  public void testParseAssumeRolePolicyMissingPrincipal() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Action\": [ \"sts:AssumeRole\" ]\n" +
            "    } ]\n" +
            "}";

    PolicyParser.getResourceInstance().parse( policyJson );
  }

  @Test
  public void testParseResourcePolicyWithWildcardPrincipal() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": \"*\",\n" +
            "      \"Action\": \"*\"\n" +
            "    } ]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getResourceInstance( ).parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations( ) );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations( ).size( ) );
    Authorization authorization = policy.getAuthorizations( ).get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNotNull( "Authorization principal", authorization.getPrincipal( ) );
    Principal principal = authorization.getPrincipal();
    assertEquals( "Principal type", Principal.PrincipalType.AWS, principal.getType() );
    assertEquals( "Principal values", Sets.newHashSet( "*" ), principal.getValues() );
    assertEquals( "Principal not", false, principal.isNotPrincipal() );
    assertEquals( "Authorization actions", Sets.newHashSet( "*" ), authorization.getActions( ) );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect( ) );
    assertNotNull( "Authorization resources", authorization.getResources( ) );
    assertEquals( "Authorization resource count", 0, authorization.getResources( ).size( ) );
    assertNotNull( "Authorization conditions", authorization.getConditions( ) );
    assertEquals( "Authorization condition count", 0, authorization.getConditions( ).size( ) );
    assertNotNull( "Authorization policy variables", authorization.getPolicyVariables( ) );
    assertEquals( "Authorization policy variable count", 0, authorization.getPolicyVariables( ).size( ) );
  }

  @Test
  public void testParseResourcePolicyWithWildcardAWSPrincipal() throws Exception {
    String policyJson =
        "{\n" +
            "    \"Statement\": [ {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"NotPrincipal\": {\n" +
            "         \"AWS\": [ \"*\" ]\n" +
            "      },\n" +
            "      \"Action\": [ \"*\" ]\n" +
            "    } ]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getResourceInstance( ).parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations( ) );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations( ).size( ) );
    Authorization authorization = policy.getAuthorizations( ).get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNotNull( "Authorization principal", authorization.getPrincipal( ) );
    Principal principal = authorization.getPrincipal();
    assertEquals( "Principal type", Principal.PrincipalType.AWS, principal.getType() );
    assertEquals( "Principal values", Sets.newHashSet( "*" ), principal.getValues() );
    assertEquals( "Principal not", true, principal.isNotPrincipal() );
    assertEquals( "Authorization actions", Sets.newHashSet( "*" ), authorization.getActions( ) );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect( ) );
    assertNotNull( "Authorization resources", authorization.getResources( ) );
    assertEquals( "Authorization resource count", 0, authorization.getResources( ).size( ) );
    assertNotNull( "Authorization conditions", authorization.getConditions( ) );
    assertEquals( "Authorization condition count", 0, authorization.getConditions( ).size( ) );
    assertNotNull( "Authorization policy variables", authorization.getPolicyVariables( ) );
    assertEquals( "Authorization policy variable count", 0, authorization.getPolicyVariables( ).size( ) );
  }

  @Test
  public void testParsePolicyWithVariables( ) throws Exception {
    String policyJson =
            "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Statement\": [{\n" +
            "    \"Action\": [\"iam:*AccessKey*\"],\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Resource\": [\"arn:aws:iam::012345678912:user/${aws:username}\"]\n" +
            "  }]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations() );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNull( "Authorization principal", authorization.getPrincipal( ) );
    assertEquals( "Authorization actions", Sets.newHashSet( "iam:*accesskey*" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 1, authorization.getResources().size() );
    assertEquals( "Authorization resources", Sets.newHashSet("/${aws:username}"), authorization.getResources() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
    assertNotNull( "Authorization policy variables", authorization.getPolicyVariables( ) );
    assertEquals( "Authorization policy variable count", 1, authorization.getPolicyVariables().size() );
    assertEquals( "Authorization policy variable[0]", "${aws:username}", authorization.getPolicyVariables().iterator().next() );
  }

  @Test
  public void testParsePolicyWithPredefinedVariables( ) throws Exception {
    String policyJson =
        "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Statement\": [{\n" +
            "    \"Action\": [\"iam:*AccessKey*\"],\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Resource\": [\"arn:aws:iam::012345678912:user/${*}${?}${$}\"]\n" +
            "  }]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations() );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNull( "Authorization principal", authorization.getPrincipal( ) );
    assertEquals( "Authorization actions", Sets.newHashSet( "iam:*accesskey*" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 1, authorization.getResources().size() );
    assertEquals( "Authorization resources", Sets.newHashSet("/${*}${?}${$}"), authorization.getResources() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
    assertNotNull( "Authorization policy variables", authorization.getPolicyVariables( ) );
    assertEquals( "Authorization policy variable count", 3, authorization.getPolicyVariables().size() );
    assertEquals( "Authorization policy variables", Sets.newHashSet( "${*}", "${?}", "${$}" ), authorization.getPolicyVariables() );
  }

  @Test
  public void testParsePolicyNoVariableVersion( ) throws Exception {
    String policyJson =
            "{\n" +
            "  \"Statement\": [{\n" +
            "    \"Action\": [\"iam:*AccessKey*\"],\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Resource\": [\"arn:aws:iam::012345678912:user/${aws:username}\"]\n" +
            "  }]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations() );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNull( "Authorization principal", authorization.getPrincipal( ) );
    assertEquals( "Authorization actions", Sets.newHashSet( "iam:*accesskey*" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 1, authorization.getResources().size() );
    assertEquals( "Authorization resources", Sets.newHashSet("/${aws:username}"), authorization.getResources() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 0, authorization.getConditions().size() );
    assertNotNull( "Authorization policy variables", authorization.getPolicyVariables( ) );
    assertEquals( "Authorization policy variable count", 0, authorization.getPolicyVariables().size() );
  }

  @Test(expected = PolicyParseException.class )
  public void testParsePolicyWithNonStringAction() throws Exception {
    String policyJson =
        "{\n" +
            "  \"Statement\": [{\n" +
            "    \"Action\": 5,\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Resource\": \"*\"\n" +
            "  }]\n" +
            "}";
    PolicyParser.getInstance().parse( policyJson );
  }

  @Test(expected = PolicyParseException.class )
  public void testParsePolicyWithNonStringActionArray() throws Exception {
    String policyJson =
        "{\n" +
            "  \"Statement\": [{\n" +
            "    \"Action\": [\"Describe*\",5],\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Resource\": \"*\"\n" +
            "  }]\n" +
            "}";
    PolicyParser.getInstance().parse( policyJson );
  }

  @Test
  public void testParsePolicyWithBooleanCondition() throws Exception {
    String policyJson =
        "{\n"+
            "        \"Statement\":[ {\n"+
            "          \"Effect\": \"Allow\",\n"+
            "          \"Action\": \"*\",\n"+
            "          \"Resource\": \"*\",\n"+
            "          \"Condition\": {\n"+
            "            \"Null\": {\n"+
            "              \"test:key\": true\n"+
            "            }\n"+
            "          }\n"+
            "        } ]\n"+
            "      }";
    PolicyParser.getInstance().parse( policyJson );
  }

  @Test
  public void testParsePolicyWithNumericCondition() throws Exception {
    String policyJson =
        "{\n"+
            "        \"Statement\":[ {\n"+
            "          \"Effect\": \"Allow\",\n"+
            "          \"Action\": \"*\",\n"+
            "          \"Resource\": \"*\",\n"+
            "          \"Condition\": {\n"+
            "            \"NumericGreaterThan\": {\n"+
            "              \"test:key\": 0\n"+
            "            }\n"+
            "          }\n"+
            "        } ]\n"+
            "      }";
    PolicyParser.getInstance().parse( policyJson );
  }

  @Test
  public void testParsePolicyWithMixedTypeCondition() throws Exception {
    String policyJson =
        "{\n"+
            "        \"Statement\":[ {\n"+
            "          \"Effect\": \"Allow\",\n"+
            "          \"Action\": \"*\",\n"+
            "          \"Resource\": \"*\",\n"+
            "          \"Condition\": {\n"+
            "            \"NumericGreaterThan\": {\n"+
            "              \"test:key\": [ 0, \"1\", true ]\n"+
            "            }\n"+
            "          }\n"+
            "        } ]\n"+
            "      }";
    PolicyParser.getInstance().parse( policyJson );
  }

  @PolicyKey("test:key")
  public static class TestKey implements Key {
    @Override public String value( ) { return "value"; }
    @Override public void validateConditionType( Class<? extends ConditionOp> conditionClass ) { }
    @Override public boolean canApply( String action) { return true; }
  }
}
