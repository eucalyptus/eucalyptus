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

import static com.eucalyptus.auth.principal.Principal.PrincipalType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.PrincipalEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.json.JsonUtils;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.NullConditionOp;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.util.Pair;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * The IAM policy parser.
 */
public class PolicyParser {

  public static final int MAX_POLICY_SIZE = 16 * 1024; // 16KB, much larger than AWS IAM specified
  
  private static final Logger LOG = Logger.getLogger( PolicyParser.class );
  
  private enum PolicyAttachmentType {
    Identity( true/*requireResource*/, false/*requirePrincipal*/ ),
    Resource( false/*requireResource*/, true/*requirePrincipal*/ );

    private final boolean requireResource;
    private final boolean requirePrincipal;

    private PolicyAttachmentType( final boolean requireResource,
                                  final boolean requirePrincipal ) {
      this.requireResource = requireResource;
      this.requirePrincipal = requirePrincipal;
    }

    public boolean isResourceRequired() {
      return requireResource;
    }

    public boolean isPrincipalRequired() {
      return requirePrincipal;
    }
  }

  private final PolicyAttachmentType attachmentType;
  
  public static PolicyParser getInstance( ) {
    return new PolicyParser( PolicyAttachmentType.Identity );
  }

  public static PolicyParser getResourceInstance( ) {
    return new PolicyParser( PolicyAttachmentType.Resource );
  }

  private PolicyParser( final PolicyAttachmentType attachmentType  ) {
    this.attachmentType = attachmentType;
  }
  
  /**
   * Parse the input policy text and returns an PolicyEntity object that
   * represents the policy internally.
   * 
   * @param policy The input policy text.
   * @return The parsed the policy entity.
   * @throws PolicyParseException for policy syntax error.
   */
  public PolicyEntity parse( String policy ) throws PolicyParseException {
    if ( policy == null ) {
      throw new PolicyParseException( PolicyParseException.EMPTY_POLICY );
    }
    if ( policy.length( ) > MAX_POLICY_SIZE ) {
      throw new PolicyParseException( PolicyParseException.SIZE_TOO_LARGE );
    }
    try {
      JSONObject policyJsonObj = JSONObject.fromObject( policy );
      String version = JsonUtils.getByType( String.class, policyJsonObj, PolicySpec.VERSION );
      // Policy statements
      List<StatementEntity> statements = parseStatements( policyJsonObj );
      return new PolicyEntity( version, policy, statements );
    } catch ( JSONException e ) {
      Debugging.logError( LOG, e, "Syntax error in input policy" );
      throw new PolicyParseException( e );
    }
  }
  
  /**
   * Parse all statements.
   * 
   * @param policy Input policy text.
   * @return A list of statement entities from the input policy.
   * @throws JSONException for syntax error.
   */
  private List<StatementEntity> parseStatements( JSONObject policy ) throws JSONException {
    List<JSONObject> objs = JsonUtils.getRequiredArrayByType( JSONObject.class, policy, PolicySpec.STATEMENT );
    List<StatementEntity> statements = Lists.newArrayList( );
    for ( JSONObject o : objs ) {
      statements.add( parseStatement( o ) );
    }
    return statements;
  }
  
  /**
   * Parse one statement. A statement is internally represented by a list of authorizations
   * and a list of conditions. The action list and the resource list of the statement are
   * parsed into authorizations (which action is allowed on which resource). The condition
   * block is translated into conditions (keys, values and their relationships).
   * 
   * @param statement The JSON object of the statement
   * @return The parsed statement entity
   * @throws JSONException for syntax error
   */
  private StatementEntity parseStatement( JSONObject statement ) throws JSONException {
    // statement ID
    String sid = JsonUtils.getByType( String.class, statement, PolicySpec.SID );
    // effect
    JsonUtils.checkRequired( statement, PolicySpec.EFFECT );
    String effect = JsonUtils.getByType( String.class, statement, PolicySpec.EFFECT );
    checkEffect( effect );
    // principal
    PrincipalEntity principal = parsePrincipal( statement );
    // authorizations: action + resource
    List<AuthorizationEntity> authorizations = parseAuthorizations( statement, effect );
    // conditions
    List<ConditionEntity> conditions = parseConditions( statement, effect );
    // Construct the statement: a list of authorizations and a list of conditions
    return new StatementEntity( sid, principal, authorizations, conditions );
  }

  /**
   * Parse the principal part of a statement.
   *
   * @param statement The input statement in JSON object.
   * @return The optional principal entity entities.
   * @throws JSONException for syntax error.
   */
  private PrincipalEntity parsePrincipal( final JSONObject statement ) {
    final String principalElement =
        JsonUtils.checkBinaryOption( statement, PolicySpec.PRINCIPAL, PolicySpec.NOTPRINCIPAL, attachmentType.isPrincipalRequired() );
    final JSONObject principal = JsonUtils.getByType( JSONObject.class, statement, principalElement );
    if ( principal == null ) return null;

    final String principalType = JsonUtils.checkBinaryOption( principal, PrincipalType.AWS.name(), PrincipalType.Service.name() );
    final List<String> values = JsonUtils.parseStringOrStringList( principal, principalType );
    if ( values.size( ) < 1 && attachmentType.isPrincipalRequired() ) {
      throw new JSONException( "Empty principal values" );
    }
    boolean notPrincipal = PolicySpec.NOTPRINCIPAL.equals( principalElement );
    return new PrincipalEntity( notPrincipal, PrincipalType.valueOf( principalType ), Sets.newHashSet( values ) );
  }

  /**
   * Parse the authorization part of a statement.
   * 
   * @param statement The input statement in JSON object.
   * @param effect The effect of the statement
   * @return A list of authorization entities.
   * @throws JSONException for syntax error.
   */
  private List<AuthorizationEntity> parseAuthorizations( JSONObject statement, String effect ) throws JSONException {
    // actions
    String actionElement = JsonUtils.checkBinaryOption( statement, PolicySpec.ACTION, PolicySpec.NOTACTION );
    List<String> actions = JsonUtils.parseStringOrStringList( statement, actionElement );
    if ( actions.size( ) < 1 ) {
      throw new JSONException( "Empty action values" );
    }
    // resources
    String resourceElement = JsonUtils.checkBinaryOption( statement, PolicySpec.RESOURCE, PolicySpec.NOTRESOURCE, attachmentType.isResourceRequired() );
    List<String> resources = JsonUtils.parseStringOrStringList( statement, resourceElement );
    if ( resources.size( ) < 1 && attachmentType.isResourceRequired() ) {
      throw new JSONException( "Empty resource values" );
    }
    // decompose actions and resources and re-combine them into a list of authorizations
    return decomposeStatement( effect, actionElement, actions, resourceElement, resources );
  }
  
  /**
   * The algorithm of decomposing the actions and resources of a statement into authorizations:
   * 1. Group actions into different vendors.
   * 2. Group resources into different resource types.
   * 3. Permute all combinations of action groups and resource groups, matching them by the same
   *    vendors.
   */
  private List<AuthorizationEntity> decomposeStatement(
      final String effect,
      final String actionElement,
      final List<String> actions,
      final String resourceElement,
      final List<String> resources
  ) {
    // Group actions by vendor
    final SetMultimap<String, String> actionMap = HashMultimap.create( );
    for ( String action : actions ) {
      action = normalize( action );
      final String vendor = checkAction( action );
      actionMap.put( vendor, action );
    }
    // Group resources by type, key is a pair of (optional) account + resource type
    final SetMultimap<Pair<Optional<String>,String>, String> resourceMap = HashMultimap.create( );
    for ( final String resource : resources ) {
      final Ern ern = Ern.parse( resource );
      resourceMap.put(
          Pair.lopair(
              Strings.emptyToNull( ern.getNamespace( ) ) ,
              ern.getResourceType( ) ),
          ern.getResourceName() );
    }
    final boolean notAction = PolicySpec.NOTACTION.equals( actionElement );
    final boolean notResource = PolicySpec.NOTRESOURCE.equals( resourceElement );
    // Permute action and resource groups and construct authorizations.
    final List<AuthorizationEntity> results = Lists.newArrayList( );
    for ( final Map.Entry<String, Collection<String>> actionSetEntry : actionMap.asMap( ).entrySet() ) {
      final String vendor = actionSetEntry.getKey( );
      final Set<String> actionSet = (Set<String>) actionSetEntry.getValue( );
      boolean added = false;
      for ( final Map.Entry<Pair<Optional<String>,String>, Collection<String>> resourceSetEntry : resourceMap.asMap().entrySet() ) {
        final Optional<String> accountIdOrName = resourceSetEntry.getKey( ).getLeft();
        final String type = resourceSetEntry.getKey( ).getRight();
        final Set<String> resourceSet = (Set<String>) resourceSetEntry.getValue( );
        if ( PolicySpec.ALL_ACTION.equals( vendor )
            || PolicySpec.ALL_RESOURCE.equals( type )
            || PolicySpec.isPermittedResourceVendor( vendor, PolicySpec.vendor( type ) ) ) {
          results.add( new AuthorizationEntity(
              EffectType.valueOf( effect ),
              accountIdOrName.orNull( ),
              type,
              actionSet,
              notAction,
              resourceSet,
              notResource ) );
          added = true;
        }
      }
      if ( !added ) {
        results.add( new AuthorizationEntity( EffectType.valueOf( effect ), actionSet, notAction ) );
      }
    }
    return results;
  }

  /**
   * Parse the conditions of a statement
   * 
   * @param statement The JSON object of the statement
   * @param effect The effect of the statement
   * @return A list of parsed condition entity.
   * @throws JSONException for syntax error.
   */
  private List<ConditionEntity> parseConditions( JSONObject statement, String effect ) throws JSONException {
    JSONObject condsObj = JsonUtils.getByType( JSONObject.class, statement, PolicySpec.CONDITION );
    boolean isQuota = EffectType.Limit.name( ).equals( effect );
    List<ConditionEntity> results = Lists.newArrayList( );
    if ( condsObj != null ) {    
      for ( Object t : condsObj.keySet( ) ) {
        String type = ( String ) t;
        Class<? extends ConditionOp> typeClass = checkConditionType( type );
        JSONObject paramsObj = JsonUtils.getByType( JSONObject.class, condsObj, type );
        for ( Object k : paramsObj.keySet( ) ) {
          String key = ( String ) k;
          Set<String> values = Sets.newHashSet( );
          values.addAll( JsonUtils.parseStringOrStringList( paramsObj, key ) );
          key = normalize( key );
          checkConditionKeyAndValues( key, values, typeClass, isQuota );
          results.add( new ConditionEntity( type, key, values ) );
        }
      }
    }
    return results;
  }

  /**
   * Check validity of the action value.
   * 
   * @param action The input action pattern.
   * @return The vendor of the action.
   * @throws JSONException for any error
   */
  private String checkAction( String action ) throws JSONException {
    final Matcher matcher = PolicySpec.ACTION_PATTERN.matcher( action );
    if ( !matcher.matches( ) ) {
      throw new JSONException( "'" + action + "' is not a valid action" );
    }
    if ( PolicySpec.ALL_ACTION.equals( action ) ) {
      return PolicySpec.ALL_ACTION;
    }
    return matcher.group( 1 ); // vendor
  }

  /**
   * Check the validity of a condition type.
   * 
   * @param type The condition type string.
   * @return The class represents the condition type.
   * @throws JSONException for syntax error.
   */
  private Class<? extends ConditionOp> checkConditionType( String type ) throws JSONException {
    if ( type == null ) {
      throw new JSONException( "Empty condition type" );
    }
    Class<? extends ConditionOp> typeClass = Conditions.getConditionOpClass( type );
    if ( typeClass == null ) {
      throw new JSONException( "Condition type '" + type + "' is not supported" );
    }
    return typeClass;
  }
  
  /**
   * Check the condition key and value validity.
   * 
   * @param key Condition key.
   * @param values Condition values.
   * @param typeClass The condition type
   * @param isQuota If it is for a quota statement
   * @throws JSONException for syntax error.
   */
  private void checkConditionKeyAndValues(
      final String key,
      final Set<String> values,
      final Class<? extends ConditionOp> typeClass,
      final boolean isQuota
  ) throws JSONException {
    if ( key == null ) {
      throw new JSONException( "Empty key name" );
    }
    final Class<? extends Key> keyClass = Keys.getKeyClass( key );
    if ( keyClass == null ) {
      throw new JSONException( "Condition key '" + key + "' is not supported" );
    }
    if ( isQuota && !QuotaKey.class.isAssignableFrom( keyClass ) ) {
      throw new JSONException( "Quota statement can only use quota keys.'" + key + "' is invalid." );
    }
    final Key keyObj = Keys.getKeyInstance( keyClass );
    if ( !NullConditionOp.class.equals( typeClass ) ) {
      keyObj.validateConditionType( typeClass );
    }
    if ( values.size( ) < 1 ) {
      throw new JSONException( "No value for key '" + key + "'" );
    }
    if ( isQuota && values.size( ) > 1 ) {
      throw new JSONException( "Quota key '" + key + "' can only have one value" );
    }
    if ( !NullConditionOp.class.equals( typeClass ) ) for ( final String v : values ) {
      keyObj.validateValueType( v );
    }
  }
  
  /**
   * Check the validity of effect.
   */
  private void checkEffect( String effect ) throws JSONException {
    if ( effect == null ) {
      throw new JSONException( "Effect can not be empty" );
    }
    if ( !PolicySpec.EFFECTS.contains( effect ) ) {
      throw new JSONException( "Invalid Effect value: " + effect );
    }
  }
  
  private String normalize( String value ) {
    return ( value != null ) ? value.trim( ).toLowerCase( ) : null;
  }

}
