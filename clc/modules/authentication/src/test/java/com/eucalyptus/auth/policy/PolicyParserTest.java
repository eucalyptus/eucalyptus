/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.policy;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.PrincipalEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Principal;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import net.sf.json.JSONException;


public class PolicyParserTest {

  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      System.err.println( "Requires input policy file" );
      System.exit( 1 ); 
    }

    String policy = Files.toString( new File( args[0] ), Charsets.UTF_8 );
        
    PolicyEntity parsed = PolicyParser.getInstance( ).parse( policy );
    
    printPolicy( parsed );
  }
  
  private static void printPolicy( PolicyEntity parsed ) {
    System.out.println( "Policy:\n" + parsed.getText( ) + "\n" + "Version = " + parsed.getPolicyVersion( ) );
    for ( StatementEntity statement : parsed.getStatements( ) ) {
      System.out.println( "Statement: " + statement.getSid( ) );
      for ( AuthorizationEntity auth : statement.getAuthorizations( ) ) {
        System.out.println( "Authorization: " + auth );
      }
      for ( ConditionEntity cond : statement.getConditions( ) ) {
        System.out.println( "Condition: " + cond );
      }
    }
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

    PolicyEntity policy = PolicyParser.getInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Statements null", policy.getStatements() );
    assertEquals( "Statement count", 1, policy.getStatements().size() );
    StatementEntity statement = policy.getStatements().get( 0 );
    assertNotNull( "Statement null", statement );
    assertNull( "Statement principal", statement.getPrincipal() );
    assertNotNull( "Statement authorizations", statement.getAuthorizations() );
    assertEquals( "Statement authorization count", 1, statement.getAuthorizations().size() );
    AuthorizationEntity authorization = statement.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertEquals( "Authorization actions", Sets.newHashSet( "autoscaling:*" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 1, authorization.getResources().size() );
    assertEquals( "Authorization resources", Sets.newHashSet("*"), authorization.getResources() );
    assertNotNull( "Statement conditions", statement.getConditions() );
    assertEquals( "Statement condition count", 0, statement.getConditions().size() );  }

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

    PolicyEntity policy = PolicyParser.getResourceInstance().parse( policyJson );
    assertNotNull( "Policy null", policy );
    assertNotNull( "Statements null", policy.getStatements() );
    assertEquals( "Statement count", 1, policy.getStatements().size() );
    StatementEntity statement = policy.getStatements().get( 0 );
    assertNotNull( "Statement null", statement );
    assertNotNull( "Statement principal", statement.getPrincipal() );
    PrincipalEntity principal = statement.getPrincipal();
    assertEquals( "Principal type", Principal.PrincipalType.Service, principal.getType() );
    assertEquals( "Principal values", Sets.newHashSet( "ec2.amazonaws.com" ), principal.getValues() );
    assertEquals( "Principal not", false, principal.isNotPrincipal() );
    assertNotNull( "Statement authorizations", statement.getAuthorizations() );
    assertEquals( "Statement authorization count", 1, statement.getAuthorizations().size() );
    AuthorizationEntity authorization = statement.getAuthorizations().get( 0 );
    assertNotNull( "Authorization null", authorization );
    assertEquals( "Authorization actions", Sets.newHashSet( "sts:assumerole" ), authorization.getActions() );
    assertEquals( "Authorization effect", Authorization.EffectType.Allow, authorization.getEffect() );
    assertNotNull( "Authorization resources", authorization.getResources() );
    assertEquals( "Authorization resource count", 0, authorization.getResources().size() );
    assertNotNull( "Statement conditions", statement.getConditions() );
    assertEquals( "Statement condition count", 0, statement.getConditions().size() );
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
}
