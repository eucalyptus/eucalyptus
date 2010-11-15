package com.eucalyptus.auth.policy;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyException;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.json.JsonUtils;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.google.common.collect.Lists;
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
  
  private static PolicyParser parser;
  
  public static PolicyParser getInstance( ) {
    if ( parser == null ) {
      parser = new PolicyParser( );
    }
    return parser;
  }
  
  public PolicyParser( ) {
  }
  
  public PolicyEntity parse( String policy ) throws PolicyException {
    if ( policy == null ) {
      throw new PolicyException( PolicyException.EMPTY_POLICY );
    }
    if ( policy.length( ) > MAX_POLICY_SIZE ) {
      throw new PolicyException( PolicyException.SIZE_TOO_LARGE );
    }
    try {
      JSONObject policyJsonObj = JSONObject.fromObject( policy );
      String version = JsonUtils.getByType( String.class, policyJsonObj, PolicySpecConstants.VERSION );
      List<StatementEntity> statements = parseStatements( policyJsonObj );
      PolicyEntity policyEntity = new PolicyEntity( version, policy, statements );
      return policyEntity;
    } catch ( JSONException e ) {
      Debugging.logError( LOG, e, "Syntax error in input policy" );
      throw new PolicyException( PolicyException.SYNTAX_ERROR, e );
    }
  }
  
  private List<StatementEntity> parseStatements( JSONObject policy ) throws JSONException {
    List<JSONObject> objs = JsonUtils.getArrayByType( JSONObject.class, policy, PolicySpecConstants.STATEMENT );
    List<StatementEntity> statements = Lists.newArrayList( );
    for ( JSONObject o : objs ) {
      statements.add( parseStatement( o ) );
    }
    return statements;
  }
  
  private StatementEntity parseStatement( JSONObject statement ) throws JSONException {
    String sid = JsonUtils.getByType( String.class, statement, PolicySpecConstants.SID );

    JsonUtils.checkRequired( statement, PolicySpecConstants.EFFECT );
    String effect = JsonUtils.getByType( String.class, statement, PolicySpecConstants.EFFECT );
    checkEffect( effect );

    List<AuthorizationEntity> authorizations = parseAuthorizations( statement, effect );
    List<ConditionEntity> conditions = parseConditions( statement, effect );
    
    StatementEntity statementEntity = new StatementEntity( sid );
    statementEntity.setAuthorizations( authorizations );
    statementEntity.setConditions( conditions );
    return statementEntity;
  }
  
  private List<AuthorizationEntity> parseAuthorizations( JSONObject statement, String effect ) throws JSONException {
    String actionElement = JsonUtils.checkBinaryOption( statement, PolicySpecConstants.ACTION, PolicySpecConstants.NOTACTION );
    List<String> actions = JsonUtils.parseStringOrStringList( statement, actionElement );
    if ( EffectType.Limit.name( ).equals( effect ) && PolicySpecConstants.NOTACTION.equals( actionElement ) ) {
      throw new JSONException( "Quota statement does not allow NotAction" );
    }

    String resourceElement = JsonUtils.checkBinaryOption( statement, PolicySpecConstants.RESOURCE, PolicySpecConstants.NOTRESOURCE );
    List<String> resources = JsonUtils.parseStringOrStringList( statement, resourceElement );
    if ( EffectType.Limit.name( ).equals( effect ) && PolicySpecConstants.NOTRESOURCE.equals( resourceElement ) ) {
      throw new JSONException( "Quota statement does not allow NotResource" );
    }
    
    List<AuthorizationEntity> results = Lists.newArrayList( );
    for ( String action : actions ) {
      action = action.toLowerCase( );
      checkAction( action );
      for ( String resource : resources ) {
        String resType = checkResourceType( resource );
        if ( actionMatchesResourceType( action, resType ) ) {
          results.add( new AuthorizationEntity( EffectType.valueOf( effect ),
                                                action,
                                                Boolean.valueOf( PolicySpecConstants.NOTACTION.equals( actionElement ) ),
                                                resType,
                                                resource,
                                                Boolean.valueOf( PolicySpecConstants.NOTRESOURCE.equals( resourceElement ) ) ) );
        }
      }
    }
    return results;
  }
  
  private List<ConditionEntity> parseConditions( JSONObject statement, String effect ) throws JSONException {
    JSONObject condsObj = JsonUtils.getByType( JSONObject.class, statement, PolicySpecConstants.CONDITION );
    boolean isQuota = EffectType.Limit.name( ).equals( effect );
    List<ConditionEntity> results = Lists.newArrayList( );
    if ( condsObj != null ) {    
      for ( Object t : condsObj.keySet( ) ) {
        String type = ( String ) t;
        // Ignore non quota condition if it is a quota statement (i.e. Effect is Limit)
        if ( isQuota && !Conditions.QUOTA.equals( type ) ) {
          continue;
        }
        checkConditionType( type );
        JSONObject paramsObj = JsonUtils.getByType( JSONObject.class, condsObj, type );
        for ( Object k : paramsObj.keySet( ) ) {
          String key = ( String ) k;
          checkConditionKey( key );
          Set<String> values = Sets.newHashSet( );
          values.addAll( JsonUtils.parseStringOrStringList( paramsObj, key ) );
          results.add( new ConditionEntity( type, key, values ) );
        }
      }
    }
    return results;
  }

  private void checkAction( String action ) throws JSONException {
    Matcher matcher = PolicySpecConstants.ACTION_PATTERN.matcher( action );
    if ( !matcher.matches( ) ) {
      throw new JSONException( "'" + action + "' is not a valid action" );
    }
    if ( PolicySpecConstants.ALL_ACTION.equals( action ) ) {
      return;
    }
    String prefix = matcher.group( 1 );
    String pattern = matcher.group( 2 );
    for ( String defined : PolicySpecConstants.VENDOR_ACTIONS.get( prefix ) ) {
      if ( Pattern.matches( PatternUtils.toJavaPattern( pattern ), defined ) ) {
        return;
      }
    }
    throw new JSONException( "'" + pattern + "' does not match any defined action" );
  }
  
  private boolean actionMatchesResourceType( String action, String resourceType ) {
    if ( PolicySpecConstants.ALL_ACTION.equals( action ) || PolicySpecConstants.ALL_RESOURCE.equals( resourceType ) ) {
      return true;
    }
    String actionVendor = getVendor( action );
    String resourceVendor = getVendor( resourceType );
    return actionVendor.equals( resourceVendor );
  }
  
  private String getVendor( String name ) {
    int colon = name.indexOf( ':' );
    return name.substring( 0, colon );
  }
  
  private String checkResourceType( String resource ) throws JSONException {
    Matcher matcher = PolicySpecConstants.ARN_PATTERN.matcher( resource );
    if ( !matcher.matches( ) ) {
      throw new JSONException( "'" + resource + "' is not a valid ARN" );
    }
    String vendor = null;
    String type = null;
    if ( matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_IAM ) != null ) {
      vendor = matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_IAM );
      type = matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_IAM_USERGROUP );
    } else if ( matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_EC2 ) != null ) {
      vendor = matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_EC2 );
      type = matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_EC2_TYPE ).toLowerCase( );
      if ( !PolicySpecConstants.EC2_RESOURCES.contains( type ) ) {
        throw new JSONException( "EC2 type '" + type + "' is not supported" );
      }
    } else if ( matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_S3 ) != null ) {
      vendor = matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_S3 );
      if ( matcher.group( PolicySpecConstants.ARN_PATTERNGROUP_S3_OBJECT ) != null ) {
        type = PolicySpecConstants.S3_RESOURCE_OBJECT;
      } else {
        type = PolicySpecConstants.S3_RESOURCE_BUCKET;
      }
    } else {
      return PolicySpecConstants.ALL_RESOURCE;
    }
    return vendor + ":" + type;
  }
  
  private void checkConditionType( String type ) throws JSONException {
    if ( type == null ) {
      throw new JSONException( "Empty condition type" );
    }
    if ( !Conditions.CONDITION_MAP.containsKey( type ) ) {
      throw new JSONException( "Condition type '" + type + "' is not supported" );
    }
  }
  
  private void checkConditionKey( String key ) throws JSONException {
    if ( key == null ) {
      throw new JSONException( "Empty key name" );
    }
    if ( !Keys.KEY_MAP.containsKey( key.toLowerCase( ) ) ) {
      throw new JSONException( "Condition key '" + key + "' is not supported" );
    }
  }
  
  private void checkEffect( String effect ) throws JSONException {
    if ( effect == null ) {
      throw new JSONException( "Effect can not be empty" );
    }
    if ( effect != null && !PolicySpecConstants.EFFECTS.contains( effect ) ) {
      throw new JSONException( "Invalid Effect value: " + effect );
    }
  }

}
