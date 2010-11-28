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
  
  public PolicyEngineImpl( ) {
  }
  
  @Override
  public <T> Map<String, Contract> evaluateAuthorization( Class<T> resourceClass, String resourceName ) throws AuthException {
    try {
      User requestUser = RequestContext.getRequestUser( );
      Class<? extends BaseMessage> requestMessageClass = RequestContext.getRequestMessageClass( );
      String userId = requestUser.getUserId( );
      String accountId = requestUser.getAccount( ).getAccountId( );
      String resourceType = getResourceType( resourceClass );
      String action = getAction( requestMessageClass );
      List<Authorization> matchedAuths = lookupMatchedAuthorizations( resourceType, userId, accountId );
      return processAuthorizations( matchedAuths, action, resourceName );
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
  
  private Map<String, Contract> processAuthorizations( List<Authorization> authorizations, String action, String resource ) throws AuthException {
    CachedKeyEvaluator keyEval = new CachedKeyEvaluator( );
    ContractKeyEvaluator contractEval = new ContractKeyEvaluator( );
    // Default deny
    boolean allowed = false;
    for ( Authorization auth : authorizations ) {
      LOG.debug( AuthTest.MARK + "Processing authorization: " + auth );
      if ( !evaluatePatterns( auth.getActions( ), auth.isNotAction( ), action ) ) {
        LOG.debug( AuthTest.MARK + "Action not matched: " + action );
        continue;
      }
      //YE TODO: special case for ec2:address with IP range.
      if ( !evaluatePatterns( auth.getResources( ), auth.isNotResource( ), resource ) ) {
        LOG.debug( AuthTest.MARK + "Resource not matched: " + resource );
        continue;
      }
      if ( !evaluateConditions( auth.getConditions( ), action, auth.getType( ), keyEval, contractEval ) ) {
        LOG.debug( AuthTest.MARK + "condition not matched" );
        continue;
      }
      if ( auth.getEffect( ) == EffectType.Deny ) {
        LOG.debug( AuthTest.MARK + "Explicit deny." );
        // Explicit deny
        throw new AuthException( AuthException.ACCESS_DENIED );
      } else {
        allowed = true;
      }      
    }
    if ( allowed ) {
      LOG.debug( AuthTest.MARK + "Approved" );
      return contractEval.getContracts( );
    }
    LOG.debug( AuthTest.MARK + "Default deny." );
    throw new AuthException( AuthException.ACCESS_DENIED );
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
  
  private List<Authorization> lookupMatchedAuthorizations( String resourceType, String userId, String accountId ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( Policies.lookupAccountGlobalAuthorizations( resourceType, accountId ) );
    results.addAll( Policies.lookupAccountGlobalAuthorizations( PolicySpecConstants.ALL_RESOURCE, accountId ) );
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
      Class<? extends BaseMessage> requestMessageClass = RequestContext.getRequestMessageClass( );
      String userId = requestUser.getUserId( );
      String accountId = requestUser.getAccount( ).getAccountId( );
      String resourceType = getResourceType( resourceClass );
      String action = getAction( requestMessageClass );
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
