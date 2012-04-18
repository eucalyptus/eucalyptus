package com.eucalyptus.auth.policy;

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
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.json.JsonUtils;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.NumericLessThanEquals;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The IAM policy parser.
 * 
 * @author wenye
 *
 */
public class PolicyParser {

  public static final int MAX_POLICY_SIZE = 16 * 1024; // 16KB, much larger than AWS IAM specified
  
  private static final Logger LOG = Logger.getLogger( PolicyParser.class );
  
  private static PolicyParser instance = null;
  
  public static PolicyParser getInstance( ) {
    if ( instance == null ) {
      instance = new PolicyParser( );
    }
    return instance;
  }
  
  public PolicyParser( ) {
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
      PolicyEntity policyEntity = new PolicyEntity( version, policy, statements );
      return policyEntity;
    } catch ( JSONException e ) {
      Debugging.logError( LOG, e, "Syntax error in input policy" );
      throw new PolicyParseException( PolicyParseException.SYNTAX_ERROR, e );
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
    List<JSONObject> objs = JsonUtils.getArrayByType( JSONObject.class, policy, PolicySpec.STATEMENT );
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
    // authorizations: action + resource
    List<AuthorizationEntity> authorizations = parseAuthorizations( statement, effect );
    // conditions
    List<ConditionEntity> conditions = parseConditions( statement, effect );
    // Construct the statement: a list of authorizations and a list of conditions
    StatementEntity statementEntity = new StatementEntity( sid );
    statementEntity.setAuthorizations( authorizations );
    statementEntity.setConditions( conditions );
    return statementEntity;
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
    String resourceElement = JsonUtils.checkBinaryOption( statement, PolicySpec.RESOURCE, PolicySpec.NOTRESOURCE );
    List<String> resources = JsonUtils.parseStringOrStringList( statement, resourceElement );
    if ( resources.size( ) < 1 ) {
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
   *    
   * @param effect
   * @param actionElement
   * @param actions
   * @param resourceElement
   * @param resources
   * @return
   */
  private List<AuthorizationEntity> decomposeStatement( String effect, String actionElement, List<String> actions, String resourceElement, List<String> resources ) {
    // Group actions by vendor
    Map<String, Set<String>> actionMap = Maps.newHashMap( );
    for ( String action : actions ) {
      action = normalize( action );
      String vendor = checkAction( action );
      addToSetMap( actionMap, vendor, action );
    }
    // Group resources by type
    Map<String, Set<String>> resourceMap = Maps.newHashMap( );
    for ( String resource : resources ) {
      Ern ern = Ern.parse( resource );
      addToSetMap( resourceMap, ern.getResourceType( ), ern.getResourceName( ) );
    }
    boolean notAction = Boolean.valueOf( PolicySpec.NOTACTION.equals( actionElement ) );
    boolean notResource = Boolean.valueOf( PolicySpec.NOTRESOURCE.equals( resourceElement ) );
    // Permute action and resource groups and construct authorizations.
    List<AuthorizationEntity> results = Lists.newArrayList( );
    for ( Map.Entry<String, Set<String>> actionSetEntry : actionMap.entrySet( ) ) {
      String vendor = actionSetEntry.getKey( );
      Set<String> actionSet = actionSetEntry.getValue( );
      for ( Map.Entry<String, Set<String>> resourceSetEntry : resourceMap.entrySet( ) ) {
        String type = resourceSetEntry.getKey( );
        Set<String> resourceSet = resourceSetEntry.getValue( );
        if ( PolicySpec.ALL_ACTION.equals( vendor )
            || PolicySpec.ALL_RESOURCE.equals( type )
            || type.startsWith( vendor ) ) {
          results.add( new AuthorizationEntity( EffectType.valueOf( effect ), type, actionSet, notAction, resourceSet, notResource ) );
        }
      }
    }
    return results;
  }
  
  /**
   * Add a value to a map of sets.
   * 
   * @param map
   * @param key
   * @param value
   */
  private void addToSetMap( Map<String, Set<String>> map, String key, String value ) {
    Set<String> set = map.get( key );
    if ( set == null ) {
      set = Sets.newHashSet( );
      map.put( key, set );
    }
    set.add( value );
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
    Matcher matcher = PolicySpec.ACTION_PATTERN.matcher( action );
    if ( !matcher.matches( ) ) {
      throw new JSONException( "'" + action + "' is not a valid action" );
    }
    if ( PolicySpec.ALL_ACTION.equals( action ) ) {
      return PolicySpec.ALL_ACTION;
    }
    String prefix = matcher.group( 1 ); // vendor
    String pattern = matcher.group( 2 ); // action pattern
    for ( String defined : PolicySpec.VENDOR_ACTIONS.get( prefix ) ) {
      if ( Pattern.matches( PatternUtils.toJavaPattern( pattern ), defined ) ) {
        return prefix;
      }
    }
    throw new JSONException( "'" + pattern + "' does not match any defined action" );
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
  private void checkConditionKeyAndValues( String key, Set<String> values, Class<? extends ConditionOp> typeClass, boolean isQuota ) throws JSONException {
    if ( key == null ) {
      throw new JSONException( "Empty key name" );
    }
    Class<? extends Key> keyClass = Keys.getKeyClass( key );
    if ( keyClass == null ) {
      throw new JSONException( "Condition key '" + key + "' is not supported" );
    }
    if ( isQuota && !QuotaKey.class.isAssignableFrom( keyClass ) ) {
      throw new JSONException( "Quota statement can only use quota keys.'" + key + "' is invalid." );
    }
    Key keyObj = Keys.getKeyInstance( keyClass );
    keyObj.validateConditionType( typeClass );
    if ( values.size( ) < 1 ) {
      throw new JSONException( "No value for key '" + key + "'" );
    }
    if ( isQuota && values.size( ) > 1 ) {
      throw new JSONException( "Quota key '" + key + "' can only have one value" );
    }
    for ( String v : values ) {
      keyObj.validateValueType( v );
    }
  }
  
  /**
   * Check the validity of effect.
   * 
   * @param effect
   * @throws JSONException
   */
  private void checkEffect( String effect ) throws JSONException {
    if ( effect == null ) {
      throw new JSONException( "Effect can not be empty" );
    }
    if ( effect != null && !PolicySpec.EFFECTS.contains( effect ) ) {
      throw new JSONException( "Invalid Effect value: " + effect );
    }
  }
  
  private String normalize( String value ) {
    return ( value != null ) ? value.trim( ).toLowerCase( ) : null;
  }

}
