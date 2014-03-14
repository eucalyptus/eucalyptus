/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.auth.principal.Principal.PrincipalType;
import static com.eucalyptus.util.Parameters.checkParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthEvaluationContext;
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
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.auth.principal.User.RegistrationStatus;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * The implementation of policy engine, which evaluates a request against specified policies.
 */
public class PolicyEngineImpl implements PolicyEngine {
  
  private static final Logger LOG = Logger.getLogger( PolicyEngineImpl.class );

  @Nonnull
  private final Function<String,String> accountResolver;

  private enum Decision {
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
  
  private static final Matcher SERVER_CERTIFICATE_MATCHER = new Matcher( ) {
    @Override
    public boolean match( String pattern, String instance ) {
      if(pattern==null)
        return false;
      // instance is in full ARN form while pattern is /{cert_name};
      if(! instance.startsWith("arn:aws:iam::"))
        return false;
      
      int idx = instance.indexOf(":server-certificate");
      if(idx<0)
        return false;
      idx = idx + ":server-certificate".length();
      if(idx>=instance.length())
        return false;

      final String certPathAndName = instance.substring(idx);
      return Pattern.matches( pattern, certPathAndName );
    }
  };
  
  public PolicyEngineImpl( ) {
    this( DefaultAccountResolver.INSTANCE );
  }

  public PolicyEngineImpl( @Nonnull final Function<String,String> accountResolver ) {
    this.accountResolver = checkParam( "accountResolver", accountResolver, notNullValue( ) );
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
  public void evaluateAuthorization( @Nonnull  final AuthEvaluationContext context,
                                     @Nonnull  final AuthorizationMatch authorizationMatch,
                                     @Nullable final String resourceAccountNumber,
                                     @Nonnull  final String resourceName,
                                     @Nonnull  final Map<Contract.Type, Contract> contracts ) throws AuthException {
    try {
      if ( Decision.ALLOW != evaluateResourceAuthorization( context, authorizationMatch, resourceAccountNumber, resourceName, contracts ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      // Allowed
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource access", e );
    }    
  }

  @Override
  public void evaluateAuthorization( @Nonnull  final AuthEvaluationContext context,
                                     @Nullable final Policy resourcePolicy,
                                     @Nullable final String resourceAccountNumber,
                                     @Nonnull  final String resourceName,
                                     @Nonnull  final Map<Contract.Type, Contract> contracts ) throws AuthException {
    try {
      final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;
      final ContractKeyEvaluator contractEval = new ContractKeyEvaluator( contracts );
      final CachedKeyEvaluator keyEval = new CachedKeyEvaluator( context.getEvaluatedKeys( ) );
      final String action = evaluationContext.getAction( );
      final User requestUser = evaluationContext.getRequestUser( );

      // System admin can do everything
      if ( !evaluationContext.isSystemAdmin() ) {
        // Disabled user can't do anything
        verifyUser( requestUser );
        final Account account = evaluationContext.getRequestAccount();

        // Account admin can do everything within the account
        if ( !requestUser.isAccountAdmin( ) ||
             resourceAccountNumber == null ||
             !resourceAccountNumber.equals( account.getAccountNumber( ) ) ) {
          // Check resource authorizations
          Decision decision = resourcePolicy == null ?
              Decision.ALLOW :
              processAuthorizations( resourcePolicy.getAuthorizations(), AuthorizationMatch.All, action, null, null, evaluationContext.getPrincipalType(), evaluationContext.getPrincipalName(), keyEval, contractEval );
          // Denied by explicit or default deny
          if ( decision != Decision.ALLOW ) {
            LOG.debug( "Request is rejected by resource authorization check, due to decision " + decision );
            throw new AuthException( AuthException.ACCESS_DENIED );
          } else {
            decision = evaluateResourceAuthorization( evaluationContext, AuthorizationMatch.All, resourceAccountNumber, resourceName, contracts );
            if ( Decision.DENY == decision || ( Decision.ALLOW != decision && resourcePolicy == null ) ) {
              throw new AuthException( AuthException.ACCESS_DENIED );
            }
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
  public void evaluateQuota( @Nonnull final AuthEvaluationContext context,
                             @Nonnull       String resourceName,
                             @Nonnull final Long quantity ) throws AuthException {
    try {
      final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;

      User requestUser = context.getRequestUser();
      String resourceType = context.getResourceType();
      // Case insensitive
      resourceName = PolicySpec.canonicalizeResourceName( resourceType, resourceName );
      String action = context.getAction().toLowerCase();

      // System users are not restricted by quota limits.
      if ( !evaluationContext.isSystemUser() ) {
        List<Authorization> quotas = lookupQuotas( resourceType, requestUser, evaluationContext.getRequestAccount(), requestUser.isAccountAdmin( ) );
        processQuotas( quotas, action, resourceType, resourceName, quantity );
      }
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource allocation.", e );
    }
  }

  @Override
  public AuthEvaluationContext createEvaluationContext( final String resourceType,
                                                    final String action,
                                                    final User requestUser,
                                                    final Map<String,String> evaluatedKeys ) {
    return new AuthEvaluationContextImpl( resourceType, action, requestUser, evaluatedKeys );
  }

  @Override
  public AuthEvaluationContext createEvaluationContext( final String resourceType,
                                                    final String action,
                                                    final User requestUser,
                                                    final Map<String,String> evaluatedKeys,
                                                    final PrincipalType principalType,
                                                    final String principalName ) {
    return new AuthEvaluationContextImpl( resourceType, action, requestUser, evaluatedKeys, principalType, principalName );
  }

  private Decision evaluateResourceAuthorization( @Nonnull  final AuthEvaluationContext context,
                                                  @Nonnull  final AuthorizationMatch authorizationMatch,
                                                  @Nullable final String resourceAccountNumber,
                                                  @Nonnull  String resourceName,
                                                  @Nonnull  final Map<Contract.Type, Contract> contracts ) throws AuthException {
    final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;
    final ContractKeyEvaluator contractEval = new ContractKeyEvaluator( contracts );
    final CachedKeyEvaluator keyEval = new CachedKeyEvaluator( context.getEvaluatedKeys( ) );
    final String action = evaluationContext.getAction( );
    final User requestUser = evaluationContext.getRequestUser( );
    final String resourceType = evaluationContext.getResourceType( );
    resourceName = PolicySpec.canonicalizeResourceName( resourceType, resourceName );

    // System admin can do everything
    if ( evaluationContext.isSystemAdmin( ) ) {
      return Decision.ALLOW;
    }

    // Disabled user can't do anything
    verifyUser( requestUser );
    final Account account = evaluationContext.getRequestAccount( );

    // Check global (inter-account) authorizations first //TODO:STEVE: Should these apply for roles?
    Decision decision = processAuthorizations( evaluationContext.lookupGlobalAuthorizations( ), authorizationMatch, action, resourceAccountNumber, resourceName, keyEval, contractEval );
    if ( decision == Decision.DENY ) {
      LOG.debug( "Request is rejected by global authorization check, due to decision " + decision );
      return decision;
    }

    if ( resourceAccountNumber != null && !resourceAccountNumber.equals( account.getAccountNumber( ) ) && !evaluationContext.isSystemUser() ) {
      decision = processAuthorizations( evaluationContext.lookupLocalAuthorizations( ), authorizationMatch ,action, resourceAccountNumber ,resourceName, keyEval, contractEval );
      if ( decision == Decision.DENY ) {
        LOG.debug( "Request is rejected by local authorization check, due to decision " + decision );
      }
      // Local authorizations may DENY access to a resource in another account but cannot grant it
      return decision == Decision.ALLOW ? Decision.DEFAULT : decision;
    } else if ( requestUser.isAccountAdmin( ) ) { // Account admin can do everything within their account
      return Decision.ALLOW;
    } else {
      // If not denied by global authorizations, check local (intra-account) authorizations.
      decision = processAuthorizations( evaluationContext.lookupLocalAuthorizations(), authorizationMatch, action, resourceAccountNumber, resourceName, keyEval, contractEval );
      // Denied by explicit or default deny
      if ( decision == Decision.DENY || decision == Decision.DEFAULT ) {
        LOG.debug( "Request is rejected by local authorization check, due to decision " + decision );
      }
      return decision;
    }
  }

  private void verifyUser( final User requestUser ) throws AuthException {
    if ( !requestUser.isEnabled( ) || !RegistrationStatus.CONFIRMED.equals( requestUser.getRegistrationStatus( ) ) ) {
      LOG.debug( "Request user is rejected because he/she is not enabled or confirmed yet" );
      throw new AuthException( AuthException.ACCESS_DENIED );
    }
  }

  private Decision processAuthorizations( List<Authorization> authorizations,
                                          AuthorizationMatch authorizationMatch,
                                          String action,
                                          String resourceAccountNumber,
                                          String resource,
                                          CachedKeyEvaluator keyEval,
                                          ContractKeyEvaluator contractEval ) throws AuthException {
    return  processAuthorizations( authorizations, authorizationMatch ,action, resourceAccountNumber, resource, null, null, keyEval, contractEval );
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
  private Decision processAuthorizations( @Nonnull  final List<Authorization> authorizations,
                                          @Nonnull  AuthorizationMatch authorizationMatch,
                                          @Nonnull  final String action,
                                          @Nullable final String resourceAccountNumber,
                                          @Nullable final String resource,
                                          @Nullable final PrincipalType principalType,
                                          @Nullable final String principalName,
                                          @Nonnull  final CachedKeyEvaluator keyEval,
                                          @Nonnull  final ContractKeyEvaluator contractEval ) throws AuthException {
    Decision result = Decision.DEFAULT;
    for ( Authorization auth : authorizations ) {
      if ( !matchActions( auth, action ) ) {
        continue;
      }
      if ( !matchPrincipal( auth.getPrincipal(), principalType, principalName ) ) {
        continue;
      }
      if ( authorizationMatch == AuthorizationMatch.Unconditional && auth.getEffect( ) == EffectType.Allow ) {
        return Decision.ALLOW; // Cannot deny reliably with unconditional matching
      }
      if ( !matchResources( auth, resourceAccountNumber, resource ) ) {
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

  private boolean matchPrincipal( Principal principal, PrincipalType principalType, String principalName ) throws AuthException {
    return principalName == null || (
        principal != null &&
        principal.getType() == principalType &&
        evaluateElement( matchOne( principalType.convertForUserMatching( principal.getValues() ), principalName, PATTERN_MATCHER ), principal.isNotPrincipal() ) );
  }

  private boolean matchResources( Authorization auth, String resource ) throws AuthException {
    return matchResources( auth, null, resource );
  }

  private boolean matchResources( @Nonnull  Authorization auth,
                                  @Nullable String resourceAccountNumber,
                                  @Nullable String resource ) throws AuthException {
    
    if ( resource == null ) {
      return true;
    } else if ( auth.getAccount() != null && resourceAccountNumber != null && !resolveAccount(auth.getAccount()).equals( resourceAccountNumber ) ) {
      return auth.isNotResource( );
    } else  if ( PolicySpec.EC2_RESOURCE_ADDRESS.equals( auth.getType( ) ) ) {
      return evaluateElement( matchOne( auth.getResources( ), resource, ADDRESS_MATCHER ), auth.isNotResource( ) );
    } else if ( String.format("%s:%s", PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_SERVER_CERTIFICATE).equals ( auth.getType( ))){
      return evaluateElement( matchOne( auth.getResources( ), resource, SERVER_CERTIFICATE_MATCHER), auth.isNotResource() );
    }else {
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

  private String resolveAccount( final String accountNumberOrAlias ) {
    return accountResolver.apply( accountNumberOrAlias );
  }

  private boolean evaluateElement( boolean patternMatched, boolean isNot ) {
    return ( ( patternMatched && !isNot ) || ( !patternMatched && isNot ) );
  }
  
  /**
   * Evaluate conditions for an authorization.
   */
  private boolean evaluateConditions( List<? extends Condition> conditions, String action, String resourceType, CachedKeyEvaluator keyEval, ContractKeyEvaluator contractEval ) throws AuthException {
    for ( Condition cond : conditions ) {
      ConditionOp op = Conditions.getOpInstance( cond.getType( ) );
      Key key = Keys.getKeyInstance( Keys.getKeyClass( cond.getKey( ) ) );
      final boolean applies = key.canApply( action, resourceType );
      if ( key instanceof ContractKey ) {
        if ( applies ) contractEval.addContract( ( ContractKey ) key, cond.getValues( ) );
        continue;
      }
      boolean condValue = false;
      for ( String value : cond.getValues( ) ) {
        if ( op.check( applies ? keyEval.getValue( key ) : null, value ) ) {
          condValue = true;
          break;
        }
      }
      if ( !condValue ) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Lookup global (inter-accounts) authorizations.
   * 
   * @param resourceType Type of the resource
   * @param account The account of the request user
   * @return The list of global authorizations apply to the request user
   * @throws AuthException for any error
   */
  private static List<Authorization> lookupGlobalAuthorizations( String resourceType, Account account ) throws AuthException {
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
   * @param user The request user
   * @return The list of local authorization apply to the request user
   * @throws AuthException for any error
   */
  private static List<Authorization> lookupLocalAuthorizations( String resourceType, User user ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( user.lookupAuthorizations( resourceType ) );
    if ( resourceType != null && resourceType.contains( ":" ) && !resourceType.endsWith( ":*" ) ) {
      results.addAll( user.lookupAuthorizations( resourceType.substring( 0, resourceType.indexOf( ':' ) ) + ":*" ) );
    }
    if ( resourceType != null && !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
      results.addAll( user.lookupAuthorizations( PolicySpec.ALL_RESOURCE ) );
    }
    return results;
  }
  
  /**
   * Find all quotas that can be applied for the request, by resource type, user and account.
   * 
   * @param resourceType The resource type to allocate.
   * @param user The request user.
   * @param account The request user account.
   * @param isAccountAdmin If the request user is account admin.
   * @return The list of authorizations (quotas) that match.
   * @throws AuthException for any error.
   */
  private static List<Authorization> lookupQuotas( String resourceType, User user, Account account, boolean isAccountAdmin ) throws AuthException {
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
        String quotaValue = Iterables.getFirst( cond.getValues(), null );
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
  
  static class AuthEvaluationContextImpl implements AuthEvaluationContext {
    @Nullable
    private final String resourceType;
    private final String action;
    private final User requestUser;
    @Nullable
    private final PrincipalType principalType;
    @Nullable
    private final String principalName;
    private Account requestAccount;
    private Boolean systemAdmin;
    private Boolean systemUser;
    private Map<String,String> evaluatedKeys;
    private List<Authorization> globalAuthorizations;
    private List<Authorization> localAuthorizations;

    AuthEvaluationContextImpl( @Nullable final String resourceType,
                               final String action,
                               final User requestUser,
                               final Map<String, String> evaluatedKeys ) {
      this( resourceType, action, requestUser, evaluatedKeys, null, null );
    }

    AuthEvaluationContextImpl( @Nullable final String resourceType,
                               final String action,
                               final User requestUser,
                               final Map<String, String> evaluatedKeys,
                               @Nullable final PrincipalType principalType,
                               @Nullable final String principalName ) {
      this.resourceType = resourceType;
      this.action = action.toLowerCase();
      this.evaluatedKeys = ImmutableMap.copyOf( evaluatedKeys );
      this.requestUser = requestUser;
      this.principalType = principalType;
      this.principalName = principalName;
    }

    @Nullable
    @Override
    public String getResourceType() {
      return resourceType;
    }

    @Override
    public String getAction() {
      return action;
    }

    @Override
    public User getRequestUser() {
      return requestUser;
    }

    public Map<String, String> getEvaluatedKeys( ) {
      return evaluatedKeys;
    }

    @Override
    @Nullable
    public PrincipalType getPrincipalType() {
      return principalType;
    }

    @Override
    @Nullable
    public String getPrincipalName() {
      return principalName;
    }

    @Override
    public String describe( String resourceAccountNumber, String resourceName ) {
      return String.valueOf(resourceType) + ":" + resourceName + (resourceAccountNumber==null ? "" : " of " + resourceAccountNumber) + " for " + describePrincipal( );
    }

    @Override
    public String describe( final String resourceName, final Long quantity ) {
      return String.valueOf(resourceType) + ":" + resourceName + " by " + quantity + " for " + describePrincipal( );
    }

    private String describePrincipal( ) {
      return principalType != null && principalName != null ?
        principalType + ":" + principalName + " / " + requestUser :
        String.valueOf( requestUser );
    }

    Account getRequestAccount() throws AuthException {
      if ( requestAccount == null ) {
        requestAccount = requestUser.getAccount();        
      }
      return requestAccount;
    }

    boolean isSystemAdmin() {
      if ( systemAdmin == null ) {
        systemAdmin = requestUser.isSystemAdmin();
      }
      return systemAdmin;
    }

    boolean isSystemUser() {
      if ( systemUser == null ) {
        systemUser = requestUser.isSystemUser();
      }
      return systemUser;
    }

    public List<Authorization> lookupGlobalAuthorizations( ) throws AuthException {
      if ( globalAuthorizations == null ) {
        if ( resourceType != null ) {
          globalAuthorizations = cached( PolicyEngineImpl.lookupGlobalAuthorizations( resourceType, getRequestAccount() ) );
        } else {
          globalAuthorizations = Collections.emptyList();
        }
      }
      return globalAuthorizations;
    }

    public List<Authorization> lookupLocalAuthorizations( ) throws AuthException {
      if ( localAuthorizations == null ) {
        localAuthorizations = cached( PolicyEngineImpl.lookupLocalAuthorizations( resourceType, getRequestUser( ) ) );
      }
      return localAuthorizations;
    }

    private List<Authorization> cached( final List<Authorization> authorizations ) {
      return ImmutableList.copyOf( Iterables.transform( authorizations, CachedAuthorizationTransform.INSTANCE ) );
    }
  }

  private static final class CachedDelegatingAuthorization implements Authorization {
    private static final long serialVersionUID = 1L;
    private final Authorization delegate;
    private Set<String> actions;
    private List<Condition> conditions;
    private Set<String> resource;
    private Principal principal;

    private CachedDelegatingAuthorization( final Authorization delegate ) {
      this.delegate = delegate;
    }

    @Override
    public Set<String> getActions( ) throws AuthException {
      if ( actions == null ) {
        actions = ImmutableSet.copyOf( delegate.getActions() );
      }
      return actions;
    }

    @Override
    public List<Condition> getConditions( ) throws AuthException {
      if ( conditions == null ) {
        conditions = ImmutableList.copyOf(
            Iterables.transform( delegate.getConditions( ), CachedConditionTransform.INSTANCE ) );
      }
      return conditions;
    }

    @Override
    public Set<String> getResources( ) throws AuthException {
      if ( resource == null ) {
        resource = ImmutableSet.copyOf( delegate.getResources() );
      }
      return resource;
    }

    @Override
    public String getAccount() {
      return delegate.getAccount( );
    }

    @Override
    public String getType( ) {
      return delegate.getType( );
    }

    @Override
    public Principal getPrincipal() throws AuthException {
      if ( principal == null ) {
        principal = delegate.getPrincipal( );
      }
      return principal;
    }

    @Override
    public Boolean isNotResource() {
      return delegate.isNotResource();
    }

    @Override
    public Boolean isNotAction() {
      return delegate.isNotAction();
    }

    @Override
    public Group getGroup() throws AuthException {
      return delegate.getGroup();
    }

    @Override
    public EffectType getEffect() {
      return delegate.getEffect();
    }
  }

  private static class CachedDelegatingCondition implements Condition {
    private static final long serialVersionUID = 1L;
    private final Condition delegate;
    private Set<String> values;

    private CachedDelegatingCondition( final Condition delegate ) {
      this.delegate = delegate;
    }

    @Override
    public String getKey() {
      return delegate.getKey();
    }

    @Override
    public String getType() {
      return delegate.getType();
    }

    @Override
    public Set<String> getValues() throws AuthException {
      if ( values == null ) {
        values = delegate.getValues();
      }
      return values;
    }
  }

  private enum CachedAuthorizationTransform implements Function<Authorization,Authorization> {
    INSTANCE;

    @Override
    public Authorization apply( final Authorization authorization ) {
      return new CachedDelegatingAuthorization( authorization );
    }
  }

  private enum CachedConditionTransform implements Function<Condition,Condition> {
    INSTANCE;

    @Override
    public Condition apply( final Condition condition ) {
      return new CachedDelegatingCondition( condition );
    }
  }

  private enum EucalyptusAccountNumberSupplier implements Supplier<String> {
    INSTANCE;

    @Override
    public String get() {
      try {
        return Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT ).getAccountNumber( );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  private enum DefaultAccountResolver implements Function<String,String> {
    INSTANCE;

    private static final Supplier<String> eucalyptusAccountNumberSupplier =
        Suppliers.memoize( EucalyptusAccountNumberSupplier.INSTANCE );

    @Override
    public String apply( final String accountNumberOrAlias ) {
      if ( Account.SYSTEM_ACCOUNT.equals( accountNumberOrAlias ) ) {
        return eucalyptusAccountNumberSupplier.get( );
      } else {
        return accountNumberOrAlias;
      }
    }
  }
}
