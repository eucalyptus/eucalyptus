package com.eucalyptus.auth.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.condition.NumericGreaterThan;
import com.eucalyptus.auth.policy.key.CachedKeyEvaluator;
import com.eucalyptus.auth.policy.key.ContractKey;
import com.eucalyptus.auth.policy.key.ContractKeyEvaluator;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.google.common.collect.Lists;

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
  

  /*
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
   * (non-Javadoc)
   * @see com.eucalyptus.auth.api.PolicyEngine#evaluateAuthorization(java.lang.Class, java.lang.String, java.lang.String)
   */
  @Override
  public Map<String, Contract> evaluateAuthorization( String resourceType, String resourceName, Account resourceAccount, String action, User requestUser ) throws AuthException {
    try {
      ContractKeyEvaluator contractEval = new ContractKeyEvaluator( );
      CachedKeyEvaluator keyEval = new CachedKeyEvaluator( );

      // System admin can do everything
      if ( !requestUser.isSystemAdmin( ) ) {
        String userId = requestUser.getId( );
        Account account = requestUser.getAccount( );
        
        // Check global (inter-account) authorizations first
        Decision decision = processAuthorizations( lookupGlobalAuthorizations( resourceType, account ), action, resourceName, keyEval, contractEval );
        if ( ( decision == Decision.DENY )
            || ( decision == Decision.DEFAULT && resourceAccount.getId( ) != null && !resourceAccount.getId( ).equals( account.getId( ) ) ) ) {
          LOG.debug( "Request is rejected by global authorization check, due to decision " + decision );
          throw new AuthException( AuthException.ACCESS_DENIED ); 
        }
        // Account admin can do everything within the account
        if ( !requestUser.isAccountAdmin( ) ) {
          // If not denied by global authorizations, check local (intra-account) authorizations.
          decision = processAuthorizations( lookupLocalAuthorizations( resourceType, requestUser ), action, resourceName, keyEval, contractEval );
          // Denied by explicit or default deny
          if ( decision == Decision.DENY || decision == Decision.DEFAULT ) {
            LOG.debug( "Request is rejected by local authorization check, due to decision " + decision );
            throw new AuthException( AuthException.ACCESS_DENIED );
          }
        }
      }
      // Allowed
      return contractEval.getContracts( );
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( Throwable e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource access", e );
    }    
  }

  /*
   * Quota evaluation algorithm is very simple: going through all quotas that can be applied to the request (by user
   * and resource), at all levels (account, group and user), if any of the quota is exceeded, reject the request.
   * 
   * (non-Javadoc)
   * @see com.eucalyptus.auth.api.PolicyEngine#evaluateQuota(java.lang.Integer, java.lang.Class, java.lang.String)
   */
  @Override
  public void evaluateQuota( String resourceType, String resourceName, String action, User requestUser, Integer quantity) throws AuthException {
    try {
      // System admins are not restricted by quota limits.
      if ( !requestUser.isSystemAdmin( ) ) {
        List<Authorization> quotas = lookupQuotas( resourceType, requestUser, requestUser.getAccount( ), requestUser.isAccountAdmin( ) );
        processQuotas( quotas, action, resourceType, resourceName, quantity );
      }
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( Throwable e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource allocation.", e );
    }
  }

  /**
   * Process a list of authorizations against the current request. Collecting contracts from matching authorizations.
   * 
   * @param authorizations The list of authorizations to process
   * @param action The request action
   * @param resource The requested resource
   * @param keyEval The key cache for condition evaluation (optimization purpose)
   * @param contractEval The contract evaluator and collector.
   * @return The final decision: DEFAULT - no matching authorization, DENY - explicit deny, ALLOW = explicit allow
   * @throws AuthException
   */
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
  
  /**
   * Match action or resource patterns with an action or resource name instance.
   * 
   * @param patterns The list of input patterns
   * @param isNot If the action list or resource list is a negated list.
   * @param instance The action or resource name instance
   * @return true if matched, false otherwise.
   */
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
  
  /**
   * Evaluate conditions for an authorization.
   * 
   * @param conditions
   * @param action
   * @param resourceType
   * @param keyEval
   * @param contractEval
   * @return
   * @throws AuthException
   */
  private boolean evaluateConditions( List<? extends Condition> conditions, String action, String resourceType, CachedKeyEvaluator keyEval, ContractKeyEvaluator contractEval ) throws AuthException {
    for ( Condition cond : conditions ) {
      ConditionOp op = Conditions.getOpInstance( Conditions.getConditionOpClass( cond.getType( ) ) );
      Key key = Keys.getKeyInstance( Keys.getKeyClass( cond.getKey( ) ) );
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
  private List<Authorization> lookupGlobalAuthorizations( String resourceType, Account account ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( account.lookupAccountGlobalAuthorizations( resourceType ) );
    results.addAll( account.lookupAccountGlobalAuthorizations( PolicySpec.ALL_RESOURCE ) );
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
  private List<Authorization> lookupLocalAuthorizations( String resourceType, User user ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( user.lookupAuthorizations( resourceType ) );
    results.addAll( user.lookupAuthorizations( PolicySpec.ALL_RESOURCE ) );
    return results;
  }
  
  /**
   * Find all quotas that can be applied for the request, by resource type, user and account.
   * 
   * @param resourceType The resource type to allocate.
   * @param userId The request user ID.
   * @param account The request user account.
   * @param isAccountAdmin If the request user is account admin.
   * @return The list of authorizations (quotas) that match.
   * @throws AuthException for any error.
   */
  private List<Authorization> lookupQuotas( String resourceType, User user, Account account, boolean isAccountAdmin ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( account.lookupAccountGlobalQuotas( resourceType ) );
    results.addAll( account.lookupAccountGlobalQuotas( PolicySpec.ALL_RESOURCE ) );
    if ( !isAccountAdmin ) {
      results.addAll( user.lookupQuotas( resourceType ) );
      results.addAll( user.lookupQuotas( PolicySpec.ALL_RESOURCE ) );
    }
    return results;    
  }
  
  /**
   * Process each of the quota authorizations. If any of them is exceeded, deny access.
   * 
   * @param quotas The quota authorizations
   * @param action The request action.
   * @param resourceType The resource type for allocation
   * @param resourceName The resource associated with the allocation
   * @param quantity The quantity to allocate.
   * @throws AuthException for any error.
   */
  private void processQuotas( List<Authorization> quotas, String action, String resourceType, String resourceName, Integer quantity ) throws AuthException {
    NumericGreaterThan ngt = new NumericGreaterThan( );
    for ( Authorization auth : quotas ) {
      LOG.debug( "YE " + "evaluate quota " + auth );
      if ( !evaluatePatterns( auth.getActions( ), auth.isNotAction( ), action ) ) {
        LOG.debug( " YE " + "action " + action + " not matching" );
        continue;
      }
      if ( !evaluatePatterns( auth.getResources( ), auth.isNotResource( ), resourceName ) ) {
        LOG.debug( " YE " + "resource " + resourceName + " not matching" );
        continue;
      }
      QuotaKey.Scope scope = getAuthorizationScope( auth );
      String principalId = getAuthorizationPrincipalId( auth, scope );
      for ( Condition cond : auth.getConditions( ) ) {
        Key key = Keys.getKeyInstance( Keys.getKeyClass( cond.getKey( ) ) );
        if ( !( key instanceof QuotaKey ) ) {
          LOG.debug( " YE " + "not quota key" );
          continue;
        }
        QuotaKey quotaKey = ( QuotaKey ) key;
        if ( !key.canApply( action, resourceType ) ) {
          LOG.debug( " YE " + "can not apply key" );
          continue;
        }
        String usageValue = quotaKey.value( scope, principalId, resourceName, quantity );
        String quotaValue = cond.getValues( ).toArray( new String[0] )[0];
        if ( ngt.check( usageValue, quotaValue ) ) {
          LOG.error( "Quota " + key.getClass( ).getName( ) + " is exceeded: quota=" + quotaValue + ", usage=" + usageValue );
          throw new AuthException( AuthException.QUOTA_EXCEEDED );
        }
      }
    }
  }
  
  /**
   * Get the principal ID for an authorization based on scope.
   * 
   * @param auth The authorization
   * @param scope The scope of the authorization
   * @return The principal ID (account, group or user)
   * @throws AuthException for any error
   */
  private String getAuthorizationPrincipalId( Authorization auth, QuotaKey.Scope scope ) throws AuthException {
    Group group = auth.getGroup( );
    switch ( scope ) {
      case ACCOUNT:
        return group.getAccount( ).getId( );
      case GROUP:
        return group.getId( );
      case USER:
        return group.getUsers( ).get( 0 ).getId( );
    }
    throw new RuntimeException( "Should not reach here: unrecognized scope." );
  }
  
  /**
   * Found out the scope of the authorization.
   * 
   * @param auth The authorization to be inspected.
   * @return The scope of the authorization, ACCOUNT, GROUP or USER.
   * @throws AuthException for any error.
   */
  private QuotaKey.Scope getAuthorizationScope( Authorization auth ) throws AuthException {
    Group group = auth.getGroup( );
    if ( !group.isUserGroup( ) ) {
      return QuotaKey.Scope.GROUP;
    }
    User user = group.getUsers( ).get( 0 );
    if ( user == null ) {
      throw new RuntimeException( "Empty user group " + group.getName( ) );
    }
    if ( user.isAccountAdmin( ) ) {
      return QuotaKey.Scope.ACCOUNT;
    }
    return QuotaKey.Scope.USER;
  }
  
}
