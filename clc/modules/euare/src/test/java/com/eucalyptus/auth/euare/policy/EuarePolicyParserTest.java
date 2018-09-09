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
package com.eucalyptus.auth.euare.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.policy.PolicyPolicy;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.StringEquals;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.RegisteredKeyProvider;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Principal;
import com.google.common.collect.Sets;

/**
 *
 */
public class EuarePolicyParserTest {

  @BeforeClass
  public static void beforeClass( ) {
    Conditions.registerCondition( "StringEquals", StringEquals.class, true );
    Keys.registerKeyProvider( new RegisteredKeyProvider( ) );
    Keys.registerKeyProvider( new OpenIDConnectKeyProvider( ) );
  }

  @Test
  public void testParseUserPolicy( ) throws Exception {
    String policyJson =
        "{\n" +
            "  \"Statement\": [{\n" +
            "    \"Effect\": \"Allow\",\n" +
            "    \"Principal\": {\"Federated\": \"auth.globus.org\"},\n" +
            "    \"Action\": \"sts:AssumeRoleWithWebIdentity\",\n" +
            "    \"Condition\": {\n" +
            "      \"StringEquals\": {\"auth.globus.org:aud\": \"659067ec-9698-44a8-88ea-db31e071447a\"}\n" +
            "    }\n" +
            "  }]\n" +
            "}";

    PolicyPolicy policy = PolicyParser.getResourceInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Policy authorizations", policy.getAuthorizations() );
    assertEquals( "Policy authorization count", 1, policy.getAuthorizations().size() );
    Authorization authorization = policy.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertNotNull( "Authorization principal", authorization.getPrincipal( ) );
    assertEquals( "Authorization principal type", Principal.PrincipalType.Federated, authorization.getPrincipal( ).getType( ) );
    assertEquals( "Authorization principal values", Sets.newHashSet( "auth.globus.org" ), authorization.getPrincipal( ).getValues( ) );
    assertFalse( "Authorization notprincipal", authorization.getPrincipal( ).isNotPrincipal( ) );
    assertEquals( "Authorization actions", Sets.newHashSet( "sts:assumerolewithwebidentity" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 0, authorization.getResources().size() );
    assertNotNull( "Authorization conditions", authorization.getConditions() );
    assertEquals( "Authorization condition count", 1, authorization.getConditions().size() );
    assertEquals( "Authorization condition 0 ", "StringEquals", authorization.getConditions( ).get( 0 ).getType( ) );
    assertEquals( "Authorization condition 0 ", "auth.globus.org:aud", authorization.getConditions( ).get( 0 ).getKey( ) );
    assertEquals( "Authorization condition 0 ", Sets.newHashSet( "659067ec-9698-44a8-88ea-db31e071447a" ), authorization.getConditions( ).get( 0 ).getValues( ) );
  }
}
