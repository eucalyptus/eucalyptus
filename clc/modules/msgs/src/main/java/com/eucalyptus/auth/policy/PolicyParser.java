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

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.auth.principal.Principal.PrincipalType;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthenticationLimitProvider;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.json.JsonUtils;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.NullConditionOp;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.Parameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
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

  private static final Logger LOG = Logger.getLogger( PolicyParser.class );

  private static final Pattern VARIABLE_MATCHER = Pattern.compile( "\\$\\{(?:[a-zA-Z][a-zA-Z0-9]*:[a-zA-Z]+|[*]|[?]|[$])}" );

  private enum PolicyAttachmentType {
    Identity( true/*requireResource*/, false/*requirePrincipal*/ ),
    Resource( false/*requireResource*/, true/*requirePrincipal*/ );

    private final boolean requireResource;
    private final boolean requirePrincipal;

    PolicyAttachmentType( final boolean requireResource,
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

  private static final class PolicyParseContext {
    private final String version;

    private PolicyParseContext( final String version ) {
      this.version = version;
    }

    public String getVersion() {
      return version;
    }
  }

  private final PolicyAttachmentType attachmentType;

  private final boolean validating; // true for json validation

  public static PolicyParser getLaxInstance( ) {
    return new PolicyParser( PolicyAttachmentType.Identity, false );
  }

  public static PolicyParser getLaxResourceInstance( ) {
    return new PolicyParser( PolicyAttachmentType.Resource, false );
  }

  public static PolicyParser getInstance( ) {
    return new PolicyParser( PolicyAttachmentType.Identity, true );
  }

  public static PolicyParser getResourceInstance( ) {
    return new PolicyParser( PolicyAttachmentType.Resource, true );
  }

  private PolicyParser(
      final PolicyAttachmentType attachmentType,
      final boolean validating
  ) {
    this.attachmentType = attachmentType;
    this.validating = validating;
  }

  /**
   * Parse the input policy text and returns an PolicyEntity object that
   * represents the policy internally.
   *
   * @param policy The input policy text.
   * @return The parsed the policy entity.
   * @throws PolicyParseException for policy syntax error.
   */
  public PolicyPolicy parse( String policy ) throws PolicyParseException {
    return parse( policy, null );
  }

  /**
   * Parse the input policy text and returns an PolicyEntity object that
   * represents the policy internally.
   *
   * @param policy The input policy text.
   * @param minimumVersion The required (minimum) policy version
   * @return The parsed the policy entity.
   * @throws PolicyParseException for policy syntax error.
   */
  public PolicyPolicy parse( String policy, String minimumVersion ) throws PolicyParseException {
    if ( policy == null ) {
      throw new PolicyParseException( PolicyParseException.EMPTY_POLICY );
    }
    if ( policy.length( ) > AuthenticationLimitProvider.Values.getPolicySizeLimit( ) ) {
      throw new PolicyParseException( PolicyParseException.SIZE_TOO_LARGE );
    }
    if ( validating && AuthenticationLimitProvider.Values.getUseValidatingPolicyParser( ) ) {
      try { // parser that ensures policy is valid json
        Json.parseObject( policy );
      } catch ( IOException e ) {
        Debugging.logError( LOG, e, "Syntax error in input policy" );
        throw new PolicyParseException( e.getMessage( ), e );
      }
    }
    try {
      JSONObject policyJsonObj = JSONObject.fromObject( policy );
      String version = JsonUtils.getByType( String.class, policyJsonObj, PolicySpec.VERSION );
      if ( minimumVersion != null && ( version == null || minimumVersion.compareTo( version ) < 0 ) ) {
        throw new PolicyParseException( "Version must be at least " + minimumVersion );
      }
      // Policy statements
      List<PolicyAuthorization> authorizations = parseStatements( new PolicyParseContext( version ), policyJsonObj );
      return PolicyUtils.intern( new PolicyPolicy( version, authorizations ) );
    } catch ( JSONException e ) {
      Debugging.logError( LOG, e, "Syntax error in input policy" );
      throw new PolicyParseException( e );
    }
  }

  /**
   * Normalize the given policy.
   *
   * Normalization requires a valid (JSON) policy so should NOT be performed
   * for existing policies, only user input.
   *
   * The current implementation removes meaningless whitespace, adds a version,
   * and ensure statements are a list not a single value.
   */
  public String normalize( final String policy ) throws PolicyParseException {
    try {
      final ObjectNode jsonPolicy = Json.parseObject( policy );
      final ObjectNode normalizedPolicy = jsonPolicy.objectNode( );
      property( normalizedPolicy, jsonPolicy, PolicySpec.VERSION, "2008-10-17" );
      propertyArray( normalizedPolicy, jsonPolicy, PolicySpec.STATEMENT );
      return Json.writeObjectAsString( normalizedPolicy );
    } catch ( IOException e ) {
      throw new PolicyParseException( e.getMessage( ), e );
    }
  }

  private void property(
      final ObjectNode target,
      final ObjectNode source,
      final String name,
      final String defaultValue
  ) {
    JsonNode propertyNode = source.get( name );
    if ( propertyNode == null && defaultValue != null ) {
      propertyNode = target.textNode( defaultValue );
    }
    if ( propertyNode != null ) {
      target.set( name, propertyNode );
    }
  }

  private void propertyArray(
      final ObjectNode target,
      final ObjectNode source,
      final String name
  ) {
    JsonNode propertyNode = source.get( name );
    if ( propertyNode != null && !propertyNode.isArray( ) ) {
      ArrayNode arrayPropertyNode = target.arrayNode( );
      arrayPropertyNode.add( propertyNode );
      propertyNode = arrayPropertyNode;
    }
    if ( propertyNode != null ) {
      target.set( name, propertyNode );
    }
  }
  /**
   * Parse all statements.
   *
   * @param policy Input policy text.
   * @return A list of statement entities from the input policy.
   * @throws JSONException for syntax error.
   */
  private List<PolicyAuthorization> parseStatements(
      final PolicyParseContext context,
      final JSONObject policy
  ) throws JSONException {
    List<JSONObject> objs;
    if ( policy.get( PolicySpec.STATEMENT ) instanceof JSONObject ) {
      objs = Lists.newArrayList( JsonUtils.getRequiredByType( JSONObject.class, policy, PolicySpec.STATEMENT ) );
    } else {
      objs = JsonUtils.getRequiredArrayByType( JSONObject.class, policy, PolicySpec.STATEMENT );
    }
    List<PolicyAuthorization> authorizations = Lists.newArrayList( );
    for ( JSONObject o : objs ) {
      authorizations.addAll( parseStatement( context, o ) );
    }
    return authorizations;
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
  private List<PolicyAuthorization> parseStatement(
      final PolicyParseContext context,
      final JSONObject statement
  ) throws JSONException {
    // statement ID
    String sid = JsonUtils.getByType( String.class, statement, PolicySpec.SID );
    // effect
    JsonUtils.checkRequired( statement, PolicySpec.EFFECT );
    String effect = JsonUtils.getByType( String.class, statement, PolicySpec.EFFECT );
    checkEffect( effect );
    // principal
    PolicyPrincipal principal = parsePrincipal( statement );
    // conditions
    List<PolicyCondition> conditions = parseConditions( statement, effect );
    return parseAuthorizations( context, statement, sid, effect, principal, conditions );
  }

  /**
   * Parse the principal part of a statement.
   *
   * @param statement The input statement in JSON object.
   * @return The optional principal entity entities.
   * @throws JSONException for syntax error.
   */
  private PolicyPrincipal parsePrincipal(
      final JSONObject statement
  ) {
    final String principalElement =
        JsonUtils.checkBinaryOption( statement, PolicySpec.PRINCIPAL, PolicySpec.NOTPRINCIPAL, attachmentType.isPrincipalRequired() );
    final boolean notPrincipal = PolicySpec.NOTPRINCIPAL.equals( principalElement );
    if ( PolicySpec.ALL_PRINCIPALS.equals( statement.get( principalElement ) ) ) {
      return new PolicyPrincipal( notPrincipal, PrincipalType.AWS, Sets.newHashSet( PolicySpec.ALL_PRINCIPALS ) );
    }
    final JSONObject principal = JsonUtils.getByType( JSONObject.class, statement, principalElement );
    if ( principal == null ) return null;

    String principalType = null;
    for ( final PrincipalType type : PrincipalType.values( ) ) {
      if ( principal.containsKey( type.name( ) ) ) {
        if ( principalType != null ) {
          throw new JSONException( "Element " + principalType + " and " + type.name( ) + " can not be both present" );
        }
        principalType = type.name( );
      }
    }
    if ( principalType == null ) {
      throw new JSONException( "One of element " + ( Joiner.on( " or " ).join( PrincipalType.values( ) ) ) + " is required" );
    }
    final List<String> values = JsonUtils.parseStringOrStringList( principal, principalType );
    if ( values.size( ) < 1 && attachmentType.isPrincipalRequired() ) {
      throw new JSONException( "Empty principal values" );
    }
    if ( values.size( ) > 0 && !attachmentType.isPrincipalRequired() ) {
      throw new JSONException( "Policy document should not specify a principal." );
    }
    return new PolicyPrincipal( notPrincipal, PrincipalType.valueOf( principalType ), Sets.newHashSet( values ) );
  }

  /**
   * Parse the authorization part of a statement.
   *
   * @param statement The input statement in JSON object.
   * @param effect The effect of the statement
   * @return A list of authorization entities.
   * @throws JSONException for syntax error.
   */
  private List<PolicyAuthorization> parseAuthorizations(
      final PolicyParseContext context,
      final JSONObject statement,
      final String sid,
      final String effect,
      final PolicyPrincipal principal,
      final List<PolicyCondition> conditions ) throws JSONException {
    // actions
    String actionElement = JsonUtils.checkBinaryOption( statement, PolicySpec.ACTION, PolicySpec.NOTACTION );
    List<String> actions = JsonUtils.parseStringOrStringList( statement, actionElement );
    if ( actions.size( ) < 1 ) {
      throw new JSONException( "Empty action values" );
    }
    // resources
    String resourceElement = JsonUtils.checkBinaryOption( statement, PolicySpec.RESOURCE, PolicySpec.NOTRESOURCE, attachmentType.isResourceRequired() );
    List<String> resources = JsonUtils.parseStringOrStringList( statement, resourceElement );
    if ( resources.size( ) < 1 && ( attachmentType.isResourceRequired() || PolicySpec.RESOURCE.equals( resourceElement ) ) ) {
      throw new JSONException( "Empty resource values" );
    }
    // decompose actions and resources and re-combine them into a list of authorizations
    return decomposeStatement( context, effect, sid, actionElement, actions, resourceElement, resources, principal, conditions );
  }

  /**
   * The algorithm of decomposing the actions and resources of a statement into authorizations:
   * 1. Group actions into different vendors.
   * 2. Group resources into different resource types.
   * 3. Permute all combinations of action groups and resource groups, matching them by the same
   *    vendors.
   */
  private List<PolicyAuthorization> decomposeStatement(
      final PolicyParseContext context,
      final String effect,
      final String sid,
      final String actionElement,
      final List<String> actions,
      final String resourceElement,
      final List<String> resources,
      final PolicyPrincipal principal,
      final List<PolicyCondition> conditions
  ) {
    // Group actions by vendor
    final SetMultimap<String, String> actionMap = HashMultimap.create( );
    for ( String action : actions ) {
      action = normalizeString( action );
      final String vendor = checkAction( action );
      actionMap.put( vendor, action );
    }
    // Group resources by type, key is a pair of (optional) account + resource type
    final SetMultimap<PolicyResourceSetKey, String> resourceMap = HashMultimap.create( );
    for ( final String resource : resources ) {
      for ( final Ern ern : Ern.parse( resource ).explode( ) ) {
        resourceMap.put(
            key( ern.getRegion( ), ern.getAccount( ), ern.getResourceType( ) ),
            ern.getResourceName( ) );
      }
    }
    final boolean notAction = PolicySpec.NOTACTION.equals( actionElement );
    final boolean notResource = PolicySpec.NOTRESOURCE.equals( resourceElement );
    // Permute action and resource groups and construct authorizations.
    final List<PolicyAuthorization> results = Lists.newArrayList( );
    for ( final Map.Entry<String, Collection<String>> actionSetEntry : actionMap.asMap( ).entrySet() ) {
      final String vendor = actionSetEntry.getKey( );
      final Set<String> actionSet = (Set<String>) actionSetEntry.getValue( );
      boolean added = false;
      for ( final Map.Entry<PolicyResourceSetKey, Collection<String>> resourceSetEntry : resourceMap.asMap().entrySet() ) {
        final Optional<String> region = Optional.fromNullable( resourceSetEntry.getKey( ).region );
        final Optional<String> accountIdOrName = Optional.fromNullable( resourceSetEntry.getKey( ).account );
        final String type = resourceSetEntry.getKey( ).type;
        final Set<String> resourceSet = (Set<String>) resourceSetEntry.getValue( );
        if ( PolicySpec.ALL_ACTION.equals( vendor )
            || PolicySpec.ALL_RESOURCE.equals( type )
            || PolicySpec.isPermittedResourceVendor( vendor, PolicySpec.vendor( type ) ) ) {
          results.add( new PolicyAuthorization(
              sid,
              EffectType.valueOf( effect ),
              region.orNull( ),
              accountIdOrName.orNull( ),
              type,
              principal,
              conditions,
              actionSet,
              notAction,
              resourceSet,
              notResource,
              variableSet( context, conditions, resourceSet ) ) );
          added = true;
        }
      }
      if ( !added ) {
        results.add( new PolicyAuthorization(
            sid, EffectType.valueOf( effect ), principal, conditions, actionSet, notAction, variableSet( context, conditions ) ) );
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
  private List<PolicyCondition> parseConditions(
      final JSONObject statement,
      final String effect
  ) throws JSONException {
    JSONObject condsObj = JsonUtils.getByType( JSONObject.class, statement, PolicySpec.CONDITION );
    boolean isQuota = EffectType.Limit.name( ).equals( effect );
    List<PolicyCondition> results = Lists.newArrayList( );
    if ( condsObj != null ) {
      for ( Object t : condsObj.keySet( ) ) {
        String type = ( String ) t;
        Class<? extends ConditionOp> typeClass = checkConditionType( type );
        JSONObject paramsObj = JsonUtils.getByType( JSONObject.class, condsObj, type );
        for ( Object k : paramsObj.keySet( ) ) {
          String key = ( String ) k;
          Set<String> values = Sets.newHashSet( );
          values.addAll( JsonUtils.parseStringOrStringList(
              Sets.newHashSet( String.class, Boolean.class, Integer.class, Double.class ), paramsObj, key ) );
          key = normalizeString( key );
          checkConditionKeyAndValues( key, values, typeClass, isQuota );
          results.add( new PolicyCondition( type, key, values ) );
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
    final Key keyObj = Keys.getKeyByName( key );
    if ( keyObj == null ) {
      throw new JSONException( "Condition key '" + key + "' is not supported" );
    }
    if ( isQuota && !(keyObj instanceof QuotaKey)) {
      throw new JSONException( "Quota statement can only use quota keys.'" + key + "' is invalid." );
    }
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

  private String normalizeString( String value ) {
    return ( value != null ) ? value.trim( ).toLowerCase( ) : null;
  }

  private Set<String> variableSet(
      final PolicyParseContext context,
      final List<PolicyCondition> conditions
  ) {
    return variableSet( context, conditions, Collections.<String>emptySet( ) );
  }

  private static boolean supportsPolicyVariables( final PolicyParseContext context ) {
    return context.getVersion( ) != null && "2012-10-17".compareTo( context.getVersion( ) ) <= 0;
  }

  private Set<String> variableSet(
      final PolicyParseContext context,
      final List<PolicyCondition> conditions,
      final Set<String> resources
  ) {
    if ( !supportsPolicyVariables( context ) ) {
      return Collections.emptySet( );
    }

    final Set<String> variables = Sets.newHashSet( );

    for ( final Condition condition : conditions ) {
      for ( final String value : condition.getValues( ) ) {
        addVariablesFrom( variables, value );
      }
    }

    for ( final String resource : resources ) {
      try {
        addVariablesFrom( variables, resource );
      } catch ( final Exception e ) {
        Logs.exhaust( ).error( e, e );
      }
    }

    return variables;
  }

  private void addVariablesFrom( final Set<String> variables, final String text ) {
    final Matcher matcher = VARIABLE_MATCHER.matcher( text );
    while ( matcher.find( ) ) {
      variables.add( matcher.group( ) );
    }
  }

  private static PolicyResourceSetKey key( final String region, final String account, final String type ) {
    return new PolicyResourceSetKey( Strings.emptyToNull( region ), Strings.emptyToNull( account ), type );
  }

  private static final class PolicyResourceSetKey {
    @Nullable
    private final String region;
    @Nullable
    private final String account;
    @Nonnull
    private final String type;

    public PolicyResourceSetKey(
        @Nullable final String region,
        @Nullable final String account,
        @Nonnull  final String type ) {
      Parameters.checkParam( "type", type, notNullValue( ) );
      this.region = region;
      this.account = account;
      this.type = type;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final PolicyResourceSetKey that = (PolicyResourceSetKey) o;
      return Objects.equals( region, that.region ) &&
          Objects.equals( account, that.account ) &&
          Objects.equals( type, that.type );
    }

    @Override
    public int hashCode() {
      return Objects.hash( region, account, type );
    }
  }
}
