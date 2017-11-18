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

package com.eucalyptus.auth;

import static com.eucalyptus.auth.api.PolicyEngine.AuthorizationMatch;
import static com.google.common.collect.Maps.newHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.TypedPrincipal;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Maps;

public class Permissions {

	private static Logger LOG = Logger.getLogger( Permissions.class );

	private static PolicyEngine policyEngine;

	public static void setPolicyEngine( PolicyEngine engine ) {
		synchronized( Permissions.class ) {
			LOG.info( "Setting the policy engine to: " + engine.getClass( ) );
			policyEngine = engine;
		}
	}

	public static AuthContext createAuthContext(
			final UserPrincipal requestUser,
			final Map<String, String> evaluatedKeys,
			final Set<TypedPrincipal> principals
	) throws AuthException {
		return new AuthContext( requestUser, principals, requestUser.getPrincipalPolicies( ), evaluatedKeys );
	}

	public static AuthContextSupplier createAuthContextSupplier(
			final UserPrincipal requestUser,
			final Map<String, String> evaluatedKeys
	) {
		return () -> createAuthContext( requestUser, evaluatedKeys, Principals.typedSet( requestUser ) );
	}

	public static AuthEvaluationContext createEvaluationContext(
			final String vendor,
			final String resourceType,
			final String action,
			final User requestUser,
			final Iterable<PolicyVersion> policies,
			final Map<String,String> evaluatedKeys,
			final Set<TypedPrincipal> principals
	) {
		return policyEngine.createEvaluationContext(
				PolicySpec.qualifiedName( vendor, resourceType ),
				PolicySpec.qualifiedName( vendor, action ),
				requestUser, evaluatedKeys, policies, principals );
	}


	public static boolean isAuthorized(
			@Nonnull  final String vendor,
			@Nonnull  final String resourceType,
			@Nonnull  final String resourceName,
			@Nullable final AccountFullName resourceAccount,
			@Nonnull  final String action,
			@Nonnull  final AuthContextSupplier requestUser
	) {
		final String resourceAccountNumber = resourceAccount==null ? null : resourceAccount.getAccountNumber( );
		try {
			return isAuthorized( requestUser.get().evaluationContext( vendor, resourceType, action ), resourceAccountNumber, resourceName );
		} catch ( AuthException e ) {
			LOG.error( "Exception in resource access to " + resourceType + ":" + resourceName, e );
			return false;
		}
	}

	public static boolean isAuthorized(
		@Nonnull  final String vendor,
		@Nonnull  final String resourceType,
		@Nonnull  final String resourceName,
		@Nullable final AccountIdentifiers resourceAccount,
		@Nonnull  final String action,
		@Nonnull  final AuthContext requestUser
	) {
		final String resourceAccountNumber = resourceAccount==null ? null : resourceAccount.getAccountNumber( );
		try {
			return isAuthorized( requestUser.evaluationContext( vendor, resourceType, action ), resourceAccountNumber, resourceName );
		} catch ( AuthException e ) {
			LOG.error( "Exception in resource access to " + resourceType + ":" + resourceName, e );
			return false;
		}
	}

	public static boolean isAuthorized(
		@Nonnull  final String vendor,
		@Nonnull  final String resourceType,
		@Nonnull  final String resourceName,
		@Nullable final AccountFullName resourceAccount,
		@Nonnull  final String action,
		@Nonnull  final AuthContext requestUser
	) {
		final String resourceAccountNumber = resourceAccount==null ? null : resourceAccount.getAccountNumber( );
		try {
			return isAuthorized( requestUser.evaluationContext( vendor, resourceType, action ), resourceAccountNumber, resourceName );
		} catch ( AuthException e ) {
			LOG.error( "Exception in resource access to " + resourceType + ":" + resourceName, e );
			return false;
		}
	}

	public static boolean isAuthorized(
			@Nonnull  final AuthEvaluationContext context,
			@Nullable final String resourceAccountNumber,
			@Nonnull  final String resourceName
	) {
		try {
			// If we are not in a request context, e.g. the UI, use a dummy contract map.
			// TODO(wenye): we should consider how to handle this if we allow the EC2 operations in the UI.
			final Map<Contract.Type, Contract> contracts = newHashMap();
			policyEngine.evaluateAuthorization( context, AuthorizationMatch.All, resourceAccountNumber, resourceName, contracts );
			pushToContext(contracts);
			return true;
		} catch ( AuthException e ) {
			LOG.debug( "Denied resource access to " + context.describe( resourceAccountNumber, resourceName ) + ": " + e.getMessage() );
		} catch ( Exception e ) {
			LOG.error( "Exception in resource access to " + context.describe( resourceAccountNumber, resourceName ), e );
		}
		return false;
	}

	public static boolean isAuthorized(
		final Set<TypedPrincipal> principals,
		final PolicyVersion resourcePolicy,
		final AccountFullName resourcePolicyAccount,
		final String resourceType,
		final String resourceName,
		final AccountFullName resourceAccount,
		final String action,
		final User requestUser,
		final Iterable<PolicyVersion> policies,
		final Map<String,String> evaluatedKeys
	) {
		final String resourceAccountNumber =
				resourceAccount==null ? null : resourceAccount.getAccountNumber( );
		final String resourcePolicyAccountNumber =
				resourcePolicyAccount==null ? null : resourcePolicyAccount.getAccountNumber( );
		final AuthEvaluationContext context = policyEngine.createEvaluationContext( resourceType, action, requestUser, evaluatedKeys, policies, principals );
		try {
			final Map<Contract.Type, Contract> contracts = newHashMap();
			policyEngine.evaluateAuthorization( context, false, resourcePolicy, resourcePolicyAccountNumber, resourceAccountNumber, resourceName, contracts );
			pushToContext( contracts );
			return true;
		} catch ( AuthException e ) {
			LOG.debug( "Denied resource access to " + context.describe( resourceAccountNumber, resourceName ) + ": " + e.getMessage() );
		} catch ( Exception e ) {
			LOG.error( "Exception in resource access to " + context.describe( resourceAccountNumber, resourceName ), e );
		}
		return false;
	}

	public static boolean isAuthorized(
			@Nonnull  final String vendor,
			@Nonnull  final String resourceType,
			@Nonnull  final String resourceName,
			@Nullable final AccountFullName resourceAccount,
			@Nullable final PolicyVersion resourcePolicy,
			@Nullable final AccountFullName resourcePolicyAccount,
			@Nonnull  final String action,
			@Nonnull  final AuthContextSupplier requestUser,
			          final boolean requestAccountDefaultAllow // request account authorized via non-iam mechanism
	) {
		final String resourceAccountNumber =
				resourceAccount==null ? null : resourceAccount.getAccountNumber( );
		final String resourcePolicyAccountNumber =
				resourcePolicyAccount==null ? null : resourcePolicyAccount.getAccountNumber( );
		final AuthEvaluationContext context;
		try {
			context = requestUser.get( ).evaluationContext( vendor, resourceType, action );
		} catch ( AuthException e ) {
			LOG.error( "Exception in resource access to " + resourceType + ":" + resourceName, e );
			return false;
		}
		try {
			final Map<Contract.Type, Contract> contracts = newHashMap();
			policyEngine.evaluateAuthorization( context, requestAccountDefaultAllow, resourcePolicy, resourcePolicyAccountNumber, resourceAccountNumber, resourceName, contracts );
			pushToContext( contracts );
			return true;
		} catch ( AuthException e ) {
			LOG.debug( "Denied resource access to " + context.describe( resourceAccountNumber, resourceName ) + ": " + e.getMessage() );
		} catch ( Exception e ) {
			LOG.error( "Exception in resource access to " + context.describe( resourceAccountNumber, resourceName ), e );
		}
		return false;
	}

	/**
	 * Test if perhaps authorized to perform the given action.
	 *
	 * <p>WARNING! This will not check conditions or evaluate authorization for a
	 * specific resource. This check is suitable for determining if a user does
	 * not have permission for an action but MUST NOT be used to authorize access
	 * to a specific resource.</p>
	 *
	 * @param vendor The vendor.
	 * @param action The action.
	 * @param requestUser The context for the requesting user.
	 * @return True if perhaps authorized.
	 */
	public static boolean perhapsAuthorized(
		@Nonnull  final String vendor,
		@Nonnull  final String action,
		@Nonnull  final AuthContextSupplier requestUser
	) {
		try {
			// If we are not in a request context, e.g. the UI, use a dummy contract map.
			final Map<Contract.Type, Contract> contracts = newHashMap();
			policyEngine.evaluateAuthorization( requestUser.get( ).evaluationContext( vendor, null, action ), AuthorizationMatch.Unconditional, null, "", contracts );
			return true;
		} catch ( AuthException e ) {
			LOG.debug( "Denied access for action " + action + ": " + e.getMessage() );
		} catch ( Exception e ) {
			LOG.error( "Exception in access for action " + action, e );
		}
		return false;
	}

	public static boolean canAllocate( String vendor, String resourceType, String resourceName, String action, AuthContext requestUser, Long quantity ) {
		try {
			return canAllocate( requestUser.evaluationContext( vendor, resourceType, action ), resourceName, quantity );
		} catch ( AuthException e ) {
			LOG.error( "Exception in resource access to " + resourceType + ":" + resourceName, e );
			return false;
		}
	}

	public static boolean canAllocate( String vendor, String resourceType, String resourceName, String action, AuthContextSupplier requestUser, Long quantity ) {
		try {
			return canAllocate( requestUser.get().evaluationContext( vendor, resourceType, action ), resourceName, quantity );
		} catch ( AuthException e ) {
			LOG.error( "Exception in resource allocation for " + resourceType + ":" + resourceName, e );
			return false;
		}
	}

	public static boolean canAllocate( AuthEvaluationContext context, String resourceName, Long quantity ) {
		try {
			policyEngine.evaluateQuota( context, resourceName, quantity );
			return true;
		} catch ( AuthException e ) {
			LOG.debug( "Denied resource allocation of " + context.describe( resourceName, quantity ) );
		}
		return false;
	}

	public static Map<String,String> evaluateHostKeys( ) throws AuthException {
		try {
			return Maps.transformValues(
					Keys.getKeyInstances( Key.EvaluationConstraint.ReceivingHost ),
					Keys.value( )
			);
		} catch ( RuntimeException e ) {
			throw Exceptions.rethrow( e, AuthException.class );
		}
	}

	private static void pushToContext( final Map<Contract.Type, Contract> contracts ) {
		try {
			Contexts.lookup().setContracts( contracts );
		} catch ( IllegalContextAccessException e ) {
			LOG.debug( "Not in a request context, contracts not exported to context" );
		}
	}
}
