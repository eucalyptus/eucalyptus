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
import com.eucalyptus.auth.policy.ern.AddressUtil;
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
import com.eucalyptus.auth.principal.User.RegistrationStatus;
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
  
  private static interface Matcher {
    boolean match( String pattern, String instance );
  }
  
  private static final Matcher PATTERN_MATCHER = new Matcher( ) {
    @Override
    public boolean match( String pattern, String instance ) {
      pattern = PatternUtils.toJavaPattern( pattern );
      if ( pattern == null ) {
        return false;
      }
      return Pattern.matches( pattern, instance );
    }
  };
  
  private static final Matcher ADDRESS_MATCHER = new Matcher( ) {
    @Override
    public boolean match( String pattern, String instance ) {
      if ( pattern == null ) {
        return false;
      }
      return AddressUtil.addressRangeMatch( pattern, instance );
    }
  };
  
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
  public void evaluateAuthorization( String resourceType, String resourceName, Account resourceAccount, String action, User requestUser, Map<Contract.Type, Contract> contracts ) throws AuthException {
    try {
      ContractKeyEvaluator contractEval = new ContractKeyEvaluator( contracts );
      CachedKeyEvaluator keyEval = new CachedKeyEvaluator( );
      
      // Case insensitive
      resourceName = resourceName.toLowerCase( );
      action = action.toLowerCase( );

      // System admin can do everything
      if ( !requestUser.isSystemAdmin( ) ) {
        // Disabled user can't do anything
        if ( !requestUser.isEnabled( ) || !RegistrationStatus.CONFIRMED.equals( requestUser.getRegistrationStatus( ) ) ) {
          LOG.debug( "Request user is rejected because he/she is not enabled or confirmed yet" );
          throw new AuthException( AuthException.ACCESS_DENIED );
        }
        Account account = requestUser.getAccount( );
        
        // Check global (inter-account) authorizations first
        Decision decision = processAuthorizations( lookupGlobalAuthorizations( resourceType, account ), action, resourceName, keyEval, contractEval );
        if ( ( decision == Decision.DENY )
            || ( decision == Decision.DEFAULT && resourceAccount != null && resourceAccount.getAccountNumber( ) != null && !resourceAccount.getAccountNumber( ).equals( account.getAccountNumber( ) ) ) ) {
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
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      LOG.debug( e, e );
      throw e;
    } catch ( Exception e ) {
      LOG.debug( e, e );
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
  public void evaluateQuota( String resourceType, String resourceName, String action, User requestUser, Long quantity) throws AuthException {
    try {
      // System admins are not restricted by quota limits.
      if ( !requestUser.isSystemAdmin( ) ) {
        List<Authorization> quotas = lookupQuotas( resourceType, requestUser, requestUser.getAccount( ), requestUser.isAccountAdmin( ) );
        processQuotas( quotas, action, resourceType, resourceName, quantity );
      }
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( Exception e ) {
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
      if ( !matchActions( auth, action ) ) {
        continue;
      }
      if ( !matchResources( auth, resource ) ) {
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
  
  private boolean matchActions( Authorization auth, String action ) throws AuthException {
    return evaluateElement( matchOne( auth.getActions( ), action, PATTERN_MATCHER ), auth.isNotAction( ) );
  }
  
  private boolean matchResources( Authorization auth, String resource ) throws AuthException {
    if ( PolicySpec.EC2_RESOURCE_ADDRESS.equals( auth.getType( ) ) ) {
      return evaluateElement( matchOne( auth.getResources( ), resource, ADDRESS_MATCHER ), auth.isNotResource( ) );
    } else {
      return evaluateElement( matchOne( auth.getResources( ), resource, PATTERN_MATCHER ), auth.isNotResource( ) );
    }
  }
  
  private static boolean matchOne( Set<String> patterns, String instance, Matcher matcher ) {
    for ( String pattern : patterns ) {
      if ( matcher.match( pattern, instance ) ) {
        return true;
      }
    }
    return false;
  }
  
  private boolean evaluateElement( boolean patternMatched, boolean isNot ) {
    return ( ( patternMatched && !isNot ) || ( !patternMatched && isNot ) );
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
    if ( !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
      results.addAll( account.lookupAccountGlobalAuthorizations( PolicySpec.ALL_RESOURCE ) );
    }
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
    if ( !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
      results.addAll( user.lookupAuthorizations( PolicySpec.ALL_RESOURCE ) );
    }
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
    if ( !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
      results.addAll( account.lookupAccountGlobalQuotas( PolicySpec.ALL_RESOURCE ) );
    }
    if ( !isAccountAdmin ) {
      results.addAll( user.lookupQuotas( resourceType ) );
      if ( !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
        results.addAll( user.lookupQuotas( PolicySpec.ALL_RESOURCE ) );
      }
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
  private void processQuotas( List<Authorization> quotas, String action, String resourceType, String resourceName, Long quantity ) throws AuthException {
    NumericGreaterThan ngt = new NumericGreaterThan( );
    for ( Authorization auth : quotas ) {
      if ( !matchActions( auth, action ) ) {
        LOG.debug( "Action " + action + " not matching" );
        continue;
      }
      if ( !matchResources( auth, resourceName ) ) {
        LOG.debug( "Resource " + resourceName + " not matching" );
        continue;
      }
      QuotaKey.Scope scope = getAuthorizationScope( auth );
      String principalId = getAuthorizationPrincipalId( auth, scope );
      for ( Condition cond : auth.getConditions( ) ) {
        Key key = Keys.getKeyInstance( Keys.getKeyClass( cond.getKey( ) ) );
        if ( !( key instanceof QuotaKey ) ) {
          LOG.debug( "Key " + cond.getKey( ) + " is not a quota" );
          continue;
        }
        QuotaKey quotaKey = ( QuotaKey ) key;
        if ( !key.canApply( action, resourceType ) ) {
          LOG.debug( "Key " + cond.getKey( ) + " can not apply for action=" + action + ", resourceType=" + resourceType );
          continue;
        }
        String usageValue = quotaKey.value( scope, principalId, resourceName, quantity );
        if ( QuotaKey.NOT_SUPPORTED.equals( usageValue ) ) {
          LOG.debug( "Key " + cond.getKey( ) + " is not supported for scope=" + scope );
          continue;
        }
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
        return group.getAccount( ).getAccountNumber( );
      case GROUP:
        return group.getGroupId( );
      case USER:
        return group.getUsers( ).get( 0 ).getUserId( );
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
