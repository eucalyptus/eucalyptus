package com.eucalyptus.auth.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthTest;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.Policies;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.NumericLessThan;
import com.eucalyptus.auth.policy.condition.NumericLessThanEquals;
import com.eucalyptus.auth.policy.key.CachedKeyEvaluator;
import com.eucalyptus.auth.policy.key.ContractKey;
import com.eucalyptus.auth.policy.key.ContractKeyEvaluator;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.policy.key.QuotaKeyEvaluator;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.context.IllegalContextAccessException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * The implementation of policy engine, which evaluates a request against specified policies.
 * 
 * @author wenye
 *
 */
public class PolicyEngineImpl implements PolicyEngine {
  
  private static final Logger LOG = Logger.getLogger( PolicyEngineImpl.class );
  
  private static enum Decision {
    DEFAULT, // no match
    DENY,    // explicit deny
    ALLOW,   // explicit allow
  }
  
  public PolicyEngineImpl( ) {
  }
  
  /**
   * The authorization evaluation algorithm is a combination of AWS IAM policy evaluation logic and
   * AWS inter-account permission checking logic (including EC2 image and snapshot permission, and
   * S3 bucket ACL and bucket policy). The algorithm is described in the following:
   * 
   * 1. If request user is system admin, access is GRANTED.
   * 2. Otherwise, check global (inter-account) authorizations, which are attached to account admin.
   *    If explicitly denied, access is DENIED.
   *    If explicitly allowed, continue.
   *    If no matching authorization, check request user's account ID and resource's account ID:
   *       If not match, access is DENIED.
   *       If match, continue.
   * 3. If request user is account admin, access is GRANTED.
   * 4. Otherwise, check local (intra-account) authorizations.
   *    If explicitly or default denied, access is DENIED.
   *    If explicitly allowed, access is GRANTED.
   *    
   * @see com.eucalyptus.auth.api.PolicyEngine#evaluateAuthorization(java.lang.Class, java.lang.String, java.lang.String)
   */
  @Override
  public <T> void evaluateAuthorization( Class<T> resourceClass, String resourceName, String resourceAccountId ) throws AuthException {
    try {
      User requestUser = RequestContext.getRequestUser( );
      // System admin can do everything
      if ( requestUser.isSystemAdmin( ) ) {
        return;
      }
      
      BaseMessage request = RequestContext.getRequest( );
      Map<String, Contract> contracts = RequestContext.getContracts( );
      String userId = requestUser.getUserId( );
      String accountId = requestUser.getAccount( ).getAccountId( );
      String resourceType = getResourceType( resourceClass );
      String action = getAction( request.getClass( ) );
      
      CachedKeyEvaluator keyEval = new CachedKeyEvaluator( );
      ContractKeyEvaluator contractEval = new ContractKeyEvaluator( contracts );
      
      // Check global (inter-account) authorizations first
      Decision decision = processAuthorizations( lookupGlobalAuthorizations( resourceType, accountId ), action, resourceName, keyEval, contractEval );
      if ( ( decision == Decision.DENY )
          || ( decision == Decision.DEFAULT && !resourceAccountId.equals( accountId ) ) ) {
        LOG.debug( request + " is rejected by global authorization check, due to decision " + decision );
        throw new AuthException( AuthException.ACCESS_DENIED ); 
      }
      // Account admin can do everything within the account
      if ( requestUser.isAccountAdmin( ) ) {
        return;
      }
      // If not denied by global authorizations, check local (intra-account) authorizations.
      decision = processAuthorizations( lookupLocalAuthorizations( resourceType, userId ), action, resourceName, keyEval, contractEval );
      // Denied by explicit or default deny
      if ( decision == Decision.DENY || decision == Decision.DEFAULT ) {
        LOG.debug( request + " is rejected by local authorization check, due to decision " + decision );
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      // Allowed
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( IllegalContextAccessException e ) {
      //this would happen if Contexts.lookup() is invoked outside of mule.
      throw new AuthException( "Cannot invoke without a corresponding service context available.", e );
    } catch ( Throwable e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource access", e );
    }
  }
  
  private Decision processAuthorizations( List<Authorization> authorizations, String action, String resource, CachedKeyEvaluator keyEval, ContractKeyEvaluator contractEval ) throws AuthException {
    Decision result = Decision.DEFAULT; 
    for ( Authorization auth : authorizations ) {
      if ( !evaluatePatterns( auth.getActions( ), auth.isNotAction( ), action ) ) {
        continue;
      }
      //YE TODO: special case for ec2:address with IP range.
      if ( !evaluatePatterns( auth.getResources( ), auth.isNotResource( ), resource ) ) {
        continue;
      }
      if ( !evaluateConditions( auth.getConditions( ), action, auth.getType( ), keyEval, contractEval ) ) {
        continue;
      }
      if ( auth.getEffect( ) == EffectType.Deny ) {
        // Explicit deny
        return Decision.DENY;
      } else {
        result = Decision.ALLOW;
      }      
    }
    return result;
  }
  
  private boolean evaluatePatterns( Set<String> patterns, boolean isNot, String instance ) {
    boolean matched = false;
    for ( String pattern : patterns ) {
      if ( Pattern.matches( PatternUtils.toJavaPattern( pattern ), instance ) ) {
        matched = true;
        break;
      }
    }
    if ( ( matched && !isNot ) || ( !matched && isNot ) ) {
      return true;
    }
    return false;
  }
  
  private boolean evaluateConditions( List<? extends Condition> conditions, String action, String resourceType, CachedKeyEvaluator keyEval, ContractKeyEvaluator contractEval ) throws AuthException {
    for ( Condition cond : conditions ) {
      ConditionOp op = Conditions.getOpInstance( Conditions.CONDITION_MAP.get( cond.getType( ) ) );
      Key key = Keys.getKeyInstance( Keys.KEY_MAP.get( cond.getKey( ) ) );
      if ( !key.canApply( action, resourceType ) ) {
        continue;
      }
      if ( key instanceof ContractKey ) {
        contractEval.addContract( ( ContractKey ) key, cond.getValues( ) );
        continue;
      }
      boolean condValue = false;
      for ( String value : cond.getValues( ) ) {
        if ( op.check( keyEval.getValue( key ), value ) ) {
          condValue = true;
          break;
        }
      }
      if ( condValue != true ) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Lookup global (inter-accounts) authorizations.
   * 
   * @param resourceType Type of the resource
   * @param accountId The ID of the account of the request user
   * @return The list of global authorizations apply to the request user
   * @throws AuthException for any error
   */
  private List<Authorization> lookupGlobalAuthorizations( String resourceType, String accountId ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( Policies.lookupAccountGlobalAuthorizations( resourceType, accountId ) );
    results.addAll( Policies.lookupAccountGlobalAuthorizations( PolicySpecConstants.ALL_RESOURCE, accountId ) );
    return results;
  }
  
  /**
   * Lookup local (intra-accounts) authorizations.
   * 
   * @param resourceType Type of the resource
   * @param userId The ID of the request user
   * @return The list of local authorization apply to the request user
   * @throws AuthException for any error
   */
  private List<Authorization> lookupLocalAuthorizations( String resourceType, String userId ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( Policies.lookupAuthorizations( resourceType, userId ) );
    results.addAll( Policies.lookupAuthorizations( PolicySpecConstants.ALL_RESOURCE, userId ) );
    return results;
  }
  
  private String getAction( Class<? extends BaseMessage> messageClass ) throws AuthException {
    String action = PolicySpecConstants.MESSAGE_CLASS_TO_ACTION.get( messageClass );
    if ( action == null ) {
      throw new AuthException( "The message class does not map to a supported action: " + messageClass.getName( ) );
    }
    return action;
  }
  
  private <T> String getResourceType( Class<T> resourceClass ) throws AuthException {
    String resource = PolicySpecConstants.RESOURCE_CLASS_TO_STRING.get( resourceClass );
    if ( resource == null ) {
      throw new AuthException( "Can not translate resource type: " + resourceClass.getName( ) );
    }
    return resource;
  }

  @Override
  public <T> void evaluateQuota( Class<T> resourceClass, String resourceName ) throws AuthException {
    try {
      User requestUser = RequestContext.getRequestUser( );
      BaseMessage request = RequestContext.getRequest( );
      String userId = requestUser.getUserId( );
      String accountId = requestUser.getAccount( ).getAccountId( );
      String resourceType = getResourceType( resourceClass );
      String action = getAction( request.getClass( ) );
      Map<Class<? extends QuotaKey>, String> matchedQuotas = lookupMatchedQuotas( action, resourceType, resourceName, userId, accountId );
      processQuotas( matchedQuotas );
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( IllegalContextAccessException e ) {
      //this would happen if Contexts.lookup() is invoked outside of mule.
      throw new AuthException( "Cannot invoke without a corresponding service context available.", e );
    } catch ( Throwable e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource allocation.", e );
    }
  }

  private void processQuotas( Map<Class<? extends QuotaKey>, String> matchedQuotas ) throws AuthException {
    NumericLessThanEquals nlte = new NumericLessThanEquals( );
    for ( Map.Entry<Class<? extends QuotaKey>, String> entry : matchedQuotas.entrySet( ) ) {
      Key key = Keys.getKeyInstance( entry.getKey( ) );
      if ( !nlte.check( key.value( ), entry.getValue( ) ) ) {
        LOG.error( "Quota " + key.getClass( ).getName( ) + " is exceeded." );
        throw new AuthException( AuthException.QUOTA_EXCEEDED );
      }
    }
  }

  private Map<Class<? extends QuotaKey>, String> lookupMatchedQuotas( String action, String resourceType, String resourceName, String userId, String accountId ) throws AuthException {
    QuotaKeyEvaluator quotaEval = new QuotaKeyEvaluator( );
    processQuotaAuthorizations( QuotaKeyEvaluator.Level.ACCOUNT, Policies.lookupAccountGlobalAuthorizations( resourceType, accountId ), action, resourceName, quotaEval );
    processQuotaAuthorizations( QuotaKeyEvaluator.Level.GROUP, Policies.lookupGroupQuotas( resourceType, userId ), action, resourceName, quotaEval );
    processQuotaAuthorizations( QuotaKeyEvaluator.Level.USER, Policies.lookupUserQuotas( resourceType, userId ), action, resourceName, quotaEval );
    return quotaEval.getQuotas( );
  }

  private void processQuotaAuthorizations( QuotaKeyEvaluator.Level level, List<? extends Authorization> auths, String action, String resourceName, QuotaKeyEvaluator quotaEval ) throws AuthException {
    for ( Authorization auth : auths ) {
      if ( !evaluatePatterns( auth.getActions( ), auth.isNotAction( ), action ) ) {
        continue;
      }
      if ( !evaluatePatterns( auth.getResources( ), auth.isNotResource( ), resourceName ) ) {
        continue;
      }
      for ( Condition cond : auth.getConditions( ) ) {
        Key key = Keys.getKeyInstance( Keys.KEY_MAP.get( cond.getKey( ) ) );
        if ( !( key instanceof QuotaKey ) ) {
          continue;
        }
        if ( !key.canApply( null, auth.getType( ) ) ) {
          continue;
        }
        quotaEval.addLevelQuota( level, ( ( QuotaKey ) key ).getClass( ), cond.getValues( ).toArray( new String[0] )[0] );
      }
    }
  }
  
}
