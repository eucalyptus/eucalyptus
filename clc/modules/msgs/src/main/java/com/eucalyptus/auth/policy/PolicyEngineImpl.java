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
import static com.eucalyptus.util.Parameters.checkParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.eucalyptus.auth.policy.key.ContractKey;
import com.eucalyptus.auth.policy.key.ContractKeyEvaluator;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.policy.variable.PolicyVariables;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.auth.principal.TypedPrincipal;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * The implementation of policy engine, which evaluates a request against specified policies.
 */
public class PolicyEngineImpl implements PolicyEngine {

  private static final Logger LOG = Logger.getLogger( PolicyEngineImpl.class );

  private static final Cache<String,ImmutableList<Authorization>> authorizationCache = CacheBuilder
      .<String,ImmutableList<Authorization>>newBuilder()
      .maximumSize( 10_000 )
      .expireAfterWrite( 1, TimeUnit.HOURS )
      .build( );

  @Nonnull
  private final Function<String,String> accountResolver;

  @Nonnull
  private final Supplier<Boolean> enableSystemQuotas;

  @Nonnull
  private final Supplier<String> region;

  private enum Decision {
    DEFAULT, // no match
    DENY,    // explicit deny
    ALLOW,   // explicit allow
  }

  private interface Matcher {
    boolean match( String pattern, String instance );
  }

  private static final Matcher PATTERN_MATCHER = new Matcher( ) {
    @Override
    public boolean match( String pattern, String instance ) {
      pattern = PolicyUtils.toJavaPattern( pattern );
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

  public PolicyEngineImpl(
      @Nonnull final Supplier<Boolean> enableSystemQuotas,
      @Nonnull final Supplier<String> region
  ) {
    this( DefaultAccountResolver.INSTANCE, enableSystemQuotas, region );
  }

  public PolicyEngineImpl(
      @Nonnull final Function<String,String> accountResolver,
      @Nonnull final Supplier<Boolean> enableSystemQuotas,
      @Nonnull final Supplier<String> region
  ) {
    this.accountResolver = checkParam( "accountResolver", accountResolver, notNullValue( ) );
    this.enableSystemQuotas = checkParam( "enableSystemQuotas", enableSystemQuotas, notNullValue( ) );
    this.region = checkParam( "region", region, notNullValue( ) );
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
      final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;
      if ( Decision.ALLOW != evaluateResourceAuthorization(
          evaluationContext,
          authorizationMatch,
          !evaluationContext.isSystemUser( ),
          resourceAccountNumber,
          resourceName,
          contracts ) ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      }
      // Allowed
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw new AuthException( "An error occurred while trying to evaluate policy for resource access", e );
    }
  }

  @Override
  public void evaluateAuthorization( @Nonnull  final AuthEvaluationContext context,
                                               final boolean requestAccountDefaultAllow,
                                     @Nullable final PolicyVersion resourcePolicy,
                                     @Nullable final String resourcePolicyAccountNumber,
                                     @Nullable final String resourceAccountNumber,
                                     @Nonnull  final String resourceName,
                                     @Nonnull  final Map<Contract.Type, Contract> contracts ) throws AuthException {
    try {
      final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;
      final ContractKeyEvaluator contractEval = new ContractKeyEvaluator( contracts );
      final CachedKeyEvaluator keyEval = new CachedKeyEvaluator( context.getEvaluatedKeys( ) );
      final String action = evaluationContext.getAction( );

      // System admin can do everything
      if ( !evaluationContext.isSystemAdmin() ) {
        final boolean sameAccount = context.getRequestUser( ).getAccountNumber( ).equals( resourceAccountNumber );
        final boolean sameResourceAccount = resourceAccountNumber != null && resourceAccountNumber.equals( resourcePolicyAccountNumber );
        // Check resource authorizations, ignore authorizations for own account
        final Decision resourceDecision = resourcePolicy == null ?
                Decision.DEFAULT :
                processAuthorizations( AuthEvaluationContextImpl.authorizations( resourcePolicy, true ), AuthorizationMatch.All, action, resourceAccountNumber, evaluationContext.getResourceType( ), resourceName, evaluationContext.getPrincipals( ), isAccountPrincipal( resourceAccountNumber ), keyEval, contractEval );
        // Denied by explicit or default deny
        if ( ( resourceDecision == Decision.DENY ) ||
            ( !requestAccountDefaultAllow && !sameAccount && sameResourceAccount && resourceDecision != Decision.ALLOW ) ) {
          LOG.debug( "Request is rejected by resource authorization check, due to decision " + resourceDecision );
          throw new AuthException( AuthException.ACCESS_DENIED );
        } else {
          final Decision decision = evaluateResourceAuthorization( evaluationContext, AuthorizationMatch.All, !requestAccountDefaultAllow && (resourcePolicy == null || !sameResourceAccount), resourceAccountNumber, resourceName, contracts );
          if ( Decision.DENY == decision ||
              ( !sameAccount && decision != Decision.ALLOW ) ||
              ( (!(sameResourceAccount || requestAccountDefaultAllow) || resourceDecision != Decision.ALLOW) && decision != Decision.ALLOW ) ) {
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
  public void evaluateQuota( @Nonnull final AuthEvaluationContext context,
                             @Nonnull       String resourceName,
                             @Nonnull final Long quantity ) throws AuthException {
    try {
      final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;

      String resourceType = context.getResourceType();
      resourceName = PolicySpec.canonicalizeResourceName( resourceType, resourceName );
      String action = context.getAction().toLowerCase();

      // Quotas can be disabled for system users
      if ( !evaluationContext.isSystemUser( ) || enableSystemQuotas.get( ) ) {
        List<Pair<PolicyVersion,Authorization>> quotas = evaluationContext.lookupQuotas( );
        processQuotas(
            quotas,
            evaluationContext.getRequestAccountNumber( ),
            evaluationContext.getRequestUser( ).getUserId( ),
            action,
            resourceType,
            resourceName,
            quantity );
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
                                                        final Map<String,String> evaluatedKeys,
                                                        final Iterable<PolicyVersion> policies ) {
    return new AuthEvaluationContextImpl( resourceType, action, requestUser, evaluatedKeys, policies );
  }

  @Override
  public AuthEvaluationContext createEvaluationContext( final String resourceType,
                                                        final String action,
                                                        final User requestUser,
                                                        final Map<String,String> evaluatedKeys,
                                                        final Iterable<PolicyVersion> policies,
                                                        final Set<TypedPrincipal> principals ) {
    return new AuthEvaluationContextImpl( resourceType, action, requestUser, evaluatedKeys, policies, principals );
  }

  private Decision evaluateResourceAuthorization( @Nonnull  final AuthEvaluationContext context,
                                                  @Nonnull  final AuthorizationMatch authorizationMatch,
                                                            final boolean accountOnly,
                                                  @Nullable final String resourceAccountNumber,
                                                  @Nonnull  String resourceName,
                                                  @Nonnull  final Map<Contract.Type, Contract> contracts ) throws AuthException {
    final AuthEvaluationContextImpl evaluationContext = (AuthEvaluationContextImpl)context;
    final ContractKeyEvaluator contractEval = new ContractKeyEvaluator( contracts );
    final CachedKeyEvaluator keyEval = new CachedKeyEvaluator( context.getEvaluatedKeys( ) );
    final String action = evaluationContext.getAction( );
    final String resourceType = evaluationContext.getResourceType( );
    resourceName = PolicySpec.canonicalizeResourceName( resourceType, resourceName );

    // System admin can do everything
    if ( evaluationContext.isSystemAdmin( ) ) {
      return Decision.ALLOW;
    }

    final String accountNumber = evaluationContext.getRequestAccountNumber( );
    if ( accountOnly && resourceAccountNumber != null && !resourceAccountNumber.equals( accountNumber ) ) {
      LOG.debug( "Request is rejected due to resource account mismatch with identity account" );
      return Decision.DEFAULT;
    }

    final Decision decision = processAuthorizations(
        evaluationContext.lookupAuthorizations( ),
        authorizationMatch,
        action,
        resourceAccountNumber,
        resourceType,
        resourceName,
        evaluationContext.getPrincipals( ),
        Predicates.alwaysFalse( ),
        keyEval,
        contractEval );
    if ( decision == Decision.DENY || decision == Decision.DEFAULT ) {
      LOG.debug( "Request is rejected by authorization check, due to decision " + decision );
    }
    return decision;
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
                                          @Nullable final String resourceType,
                                          @Nullable final String resource,
                                          @Nullable final Set<TypedPrincipal> principals,
                                          @Nullable final Predicate<TypedPrincipal> denyOnlyPrincipal,
                                          @Nonnull  final CachedKeyEvaluator keyEval,
                                          @Nonnull  final ContractKeyEvaluator contractEval ) throws AuthException {
    Decision result = Decision.DEFAULT;
    final String region = PolicyEngineImpl.this.region.get( );
    for ( Authorization auth : authorizations ) {
      boolean denyOnly = false;
      if ( auth.getEffect( ) == EffectType.Limit ) continue;

      if ( !matchActions( auth, action ) ) {
        continue;
      }
      if ( !matchPrincipal( auth.getPrincipal(), filter( principals, Predicates.not( denyOnlyPrincipal ) ) ) ) {
        if ( !matchPrincipal( auth.getPrincipal(), filter( principals, denyOnlyPrincipal ) ) ) {
          continue;
        } else {
          denyOnly = true;
        }
      }
      if ( authorizationMatch == AuthorizationMatch.Unconditional && auth.getEffect( ) == EffectType.Allow ) {
        return Decision.ALLOW; // Cannot deny reliably with unconditional matching
      }
      if ( !matchResources( auth, region, resourceAccountNumber, resourceType, resource ) ) {
        continue;
      }
      if ( !evaluateConditions( auth.getPolicyVariables(), auth.getConditions( ), action, keyEval, contractEval ) ) {
        continue;
      }
      if ( auth.getEffect( ) == EffectType.Deny ) {
        // Explicit deny
        return Decision.DENY;
      } else if ( !denyOnly ) {
        result = Decision.ALLOW;
      }
    }
    return result;
  }

  private boolean matchActions( Authorization auth, String action ) throws AuthException {
    return evaluateElement( matchOne( auth.getActions( ), action, PATTERN_MATCHER ), auth.isNotAction( ) );
  }

  private boolean matchPrincipal( @Nullable Principal principal, @Nullable Set<TypedPrincipal> principals ) throws AuthException {
    if ( principal == null ) {
      return true;
    } else if ( principals != null ) {
      boolean anyMatch = false;
      for( final TypedPrincipal typedPrincipal : principals ) {
        if ( typedPrincipal.getType( ) == principal.getType( ) ) {
          if ( evaluateElement(
              matchOne(
                  typedPrincipal.getType( ).convertForUserMatching( principal.getValues( ) ),
                  typedPrincipal.getName( ),
                  PATTERN_MATCHER ),
              false ) ) {
            anyMatch = true;
          }
        }
      }
      return (anyMatch && !principal.isNotPrincipal( )) || (!anyMatch && principal.isNotPrincipal( ));
    }
    return principal.isNotPrincipal( );
  }

  private boolean matchResources( Authorization auth, String resourceType, String resource ) throws AuthException {
    return matchResources( auth, null, null, resourceType, resource );
  }

  private boolean matchResources( @Nonnull  Authorization auth,
                                  @Nullable String region,
                                  @Nullable String resourceAccountNumber,
                                  @Nullable String resourceType,
                                  @Nullable String resource ) throws AuthException {
    if ( resource == null ) {
      return true;
    } else if ( auth.getRegion() != null && region != null && !auth.getRegion().equals( region ) ) {
      return auth.isNotResource( );
    } else if ( auth.getAccount() != null && resourceAccountNumber != null && !resolveAccount(auth.getAccount()).equals( resourceAccountNumber ) ) {
      return auth.isNotResource( );
    } else if ( auth.getType( ) != null && !matchOne( Collections.singleton( auth.getType( ) ), resourceType, PATTERN_MATCHER ) ) {
      return auth.isNotResource( );
    } else  if ( PolicySpec.EC2_RESOURCE_ADDRESS.equals( auth.getType( ) ) ) {
      return evaluateElement( matchOne( auth.getResources( ), resource, ADDRESS_MATCHER ), auth.isNotResource( ) );
    } else if ( String.format("%s:%s", PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_SERVER_CERTIFICATE).equals ( auth.getType( ))){
      return evaluateElement( matchOne( auth.getResources( ), resource, SERVER_CERTIFICATE_MATCHER ), auth.isNotResource( ) );
    }else {
      return evaluateElement( matchOneOrEmpty( auth.getPolicyVariables( ), auth.getResources( ), resource, PATTERN_MATCHER ), auth.isNotResource( ) );
    }
  }

  private static boolean matchOne( Set<String> patterns, String instance, Matcher matcher ) throws AuthException {
    return matchOne( Collections.emptySet( ), patterns, instance, matcher );
  }

  private static boolean matchOne( Set<String> variables, Set<String> patterns, String instance, Matcher matcher ) throws AuthException {
    for ( String pattern : patterns ) {
      if ( matcher.match( variableExplode( variables, pattern ) , instance ) ) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchOneOrEmpty( Set<String> variables, Set<String> patterns, String instance, Matcher matcher ) throws AuthException {
    return patterns.isEmpty( ) ||
        matchOne( variables, patterns, instance, matcher );
  }

  private static String variableExplode( Set<String> variables, String text ) throws AuthException {
    if ( variables.isEmpty( ) ) return text;

    String result = text;
    for ( final String variable : variables ) {
      final String variableValue = PolicyVariables.getPolicyVariable( variable ).evaluate( );
      //TODO: variable values cannot currently contain ? or *, if they could we would need
      //TODO: to escape the values when they were used in regex matches
      result = result.replace( variable, variableValue );
    }

    return result;
  }

  private <T> Set<T> filter( final Iterable<T> iterable, final Predicate<? super T> matching ) {
    if ( iterable == null ) {
      return null;
    }
    return ImmutableSet.copyOf( Iterables.filter( iterable, matching ) );
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
  private boolean evaluateConditions(
      final Set<String> policyVariables,
      final List<? extends Condition> conditions,
      final String action,
      final CachedKeyEvaluator keyEval,
      final ContractKeyEvaluator contractEval
  ) throws AuthException {
    for ( Condition cond : conditions ) {
      ConditionOp op = Conditions.getOpInstance( cond.getType( ) );
      Key key = Keys.getKeyByName( cond.getKey( ) );
      final boolean applies = key.canApply( action );
      if ( key instanceof ContractKey ) {
        if ( applies ) contractEval.addContract( ( ContractKey ) key, cond.getValues( ) );
        continue;
      }
      boolean condValue = false;
      final Set<String> expandedValues = Sets.newLinkedHashSet( );
      for ( String value : cond.getValues( ) ) {
        expandedValues.add( variableExplode( policyVariables, value ) );
      }
      condValue = op.check( applies ? keyEval.getValues( key ) : Collections.singleton( null ), expandedValues );
      if ( !condValue ) {
        return false;
      }
    }
    return true;
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
  private void processQuotas( final List<Pair<PolicyVersion,Authorization>> quotas,
                              final String accountId,
                              final String userId,
                              final String action,
                              final String resourceType,
                              final String resourceName,
                              final Long quantity ) throws AuthException {
    NumericGreaterThan ngt = new NumericGreaterThan( );
    for ( Pair<PolicyVersion,Authorization> quota : quotas ) {
      final PolicyVersion policy = quota.getLeft();
      final Authorization auth = quota.getRight( );
      if ( !matchActions( auth, action ) ) {
        LOG.debug( "Action " + action + " not matching" );
        continue;
      }
      if ( !matchResources( auth, resourceType, resourceName ) ) {
        LOG.debug( "Resource " + resourceName + " not matching" );
        continue;
      }
      PolicyScope scope = policy.getPolicyScope();
      String principalId = getAuthorizationPrincipalId( scope, accountId, userId );
      for ( Condition cond : auth.getConditions( ) ) {
        Key key = Keys.getKeyByName( cond.getKey( ) );
        if ( !( key instanceof QuotaKey ) ) {
          LOG.debug( "Key " + cond.getKey( ) + " is not a quota" );
          continue;
        }
        QuotaKey quotaKey = ( QuotaKey ) key;
        if ( !quotaKey.canApply( action , resourceType ) ) {
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
   * @param scope The scope
   * @return The principal ID (account, group or user)
   * @throws AuthException for any error
   */
  private String getAuthorizationPrincipalId( PolicyScope scope, String accountId, String userId ) throws AuthException {
    switch ( scope )  {
      case Account:
        return accountId;
      case User:
      case Group:
      case Role:
        return userId;
    }
    throw new RuntimeException( "Should not reach here: unrecognized scope." );
  }

  @SuppressWarnings( "Guava" )
  private Predicate<TypedPrincipal> isAccountPrincipal( final String accountNumber ) throws AuthException {
    if ( accountNumber == null ) {
      return Predicates.alwaysFalse( );
    } else {
      final TypedPrincipal match = TypedPrincipal.of( Principal.PrincipalType.AWS, Accounts.getAccountArn( accountNumber ) );
      return Predicates.equalTo( match );
    }
  }

  static class AuthEvaluationContextImpl implements AuthEvaluationContext {
    @Nullable
    private final String resourceType;
    private final String action;
    private final User requestUser;
    @Nullable
    private final Set<TypedPrincipal> principals;
    private Boolean systemAdmin;
    private Boolean systemUser;
    private Map<String,String> evaluatedKeys;
    private List<Authorization> authorizations;
    private List<Pair<PolicyVersion,Authorization>> quotaAuthorizations;
    private final List<PolicyVersion> policies;

    AuthEvaluationContextImpl( @Nullable final String resourceType,
                               final String action,
                               final User requestUser,
                               final Map<String, String> evaluatedKeys,
                               final Iterable<PolicyVersion> policies ) {
      this( resourceType, action, requestUser, evaluatedKeys, policies, null );
    }

    AuthEvaluationContextImpl( @Nullable final String resourceType,
                               final String action,
                               final User requestUser,
                               final Map<String, String> evaluatedKeys,
                               final Iterable<PolicyVersion> policies,
                               @Nullable final Set<TypedPrincipal> principals ) {
      this.resourceType = resourceType;
      this.action = action.toLowerCase();
      this.evaluatedKeys = ImmutableMap.copyOf( evaluatedKeys.entrySet( ).stream( )
          .filter( entry -> entry.getValue( ) != null )
          .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
      this.policies = ImmutableList.copyOf( policies );
      this.requestUser = requestUser;
      this.principals = principals==null ? null : ImmutableSet.copyOf( principals );
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
    public Set<TypedPrincipal> getPrincipals() {
      return principals;
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
      Principal.PrincipalType principalType = null;
      String principalName = null;
      if ( principals != null && !principals.isEmpty( ) ) {
        final TypedPrincipal primaryPrincipal = Iterables.get( principals, 0 );
        principalType = primaryPrincipal.getType( );
        principalName = primaryPrincipal.getName( );
      }
      return principalType != null ?
        principalType + ":" + principalName + " / " + requestUser :
        String.valueOf( requestUser );
    }

    String getRequestAccountNumber() throws AuthException {
      return requestUser.getAccountNumber( );
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

    public List<Authorization> lookupAuthorizations( ) throws AuthException {
      if ( authorizations == null ) {
        final List<Pair<PolicyVersion,Authorization>> authorizations = authorizations( policies, false );
        this.authorizations = ImmutableList.copyOf( Iterables.filter(
            Iterables.transform( authorizations, Pair.<PolicyVersion,Authorization>right( ) ),
            resourceType == null ?
                AuthorizationPredicates.ALLOW_EFFECT :
                Predicates.not( AuthorizationPredicates.LIMIT_EFFECT )
        ) );
      }
      return authorizations;
    }

    static List<Authorization> authorizations( final PolicyVersion policy, final boolean resourcePolicy ) throws AuthException {
      try {
        return authorizationCache.get( policy.getPolicyHash( ), new Callable<ImmutableList<Authorization>>() {
          @Override
          public ImmutableList<Authorization> call() throws Exception {
            return ImmutableList.copyOf(  ( resourcePolicy ? PolicyParser.getLaxResourceInstance( ) : PolicyParser.getLaxInstance( ) ).parse( policy.getPolicy( ) ).getAuthorizations( ) );
          }
        } );
      } catch ( final ExecutionException e ) {
        throw new AuthException( "Invalid policy", e.getCause( ) );
      }
    }

    static List<Pair<PolicyVersion,Authorization>> authorizations( final List<PolicyVersion> policies, final boolean resourcePolicy ) throws AuthException {
      final List<Pair<PolicyVersion,Authorization>> authorizations = Lists.newArrayList( );
      for ( final PolicyVersion policy : policies ) {
        Iterables.addAll( authorizations, Iterables.transform( authorizations( policy, resourcePolicy ), Pair.<PolicyVersion,Authorization>pair( ).apply( policy ) ) );
      }
      return authorizations;
    }

    public List<Pair<PolicyVersion,Authorization>> lookupQuotas( ) throws AuthException {
      if ( quotaAuthorizations == null ) {
        this.quotaAuthorizations = ImmutableList.copyOf( Iterables.filter(
            authorizations( policies, false ),
            Predicates.compose( AuthorizationPredicates.LIMIT_EFFECT, Pair.<PolicyVersion,Authorization>right( ) )
        ) );
      }
      return quotaAuthorizations;
    }
  }

  private enum AuthorizationPredicates implements Predicate<Authorization> {
    ALLOW_EFFECT {
      @Override
      public boolean apply( @Nullable final Authorization authorization ) {
        return authorization != null && authorization.getEffect( ) == EffectType.Allow;
      }
    },
    ALL_RESOURCE {
      @Override
      public boolean apply( @Nullable final Authorization authorization ) {
        return authorization != null && authorization.getResources( ).contains( PolicySpec.ALL_RESOURCE );
      }
    },
    LIMIT_EFFECT {
      @Override
      public boolean apply( @Nullable final Authorization authorization ) {
        return authorization != null && authorization.getEffect( ) == EffectType.Limit;
      }
    },
  }

  private enum EucalyptusAccountNumberSupplier implements Supplier<String> {
    INSTANCE;

    @Override
    public String get() {
      try {
        return Accounts.lookupAccountIdByAlias( AccountIdentifiers.SYSTEM_ACCOUNT );
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
      if ( AccountIdentifiers.SYSTEM_ACCOUNT.equals( accountNumberOrAlias ) ) {
        return eucalyptusAccountNumberSupplier.get( );
      } else {
        return accountNumberOrAlias;
      }
    }
  }
}
