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
    Conditions.registerCondition( "StringEquals", StringEquals.class, false );
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
