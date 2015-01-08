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
 ************************************************************************/
package com.eucalyptus.tokens;

import static com.eucalyptus.auth.login.AccountUsernamePasswordCredentials.AccountUsername;
import static com.eucalyptus.auth.login.HmacCredentials.QueryIdCredential;
import static com.eucalyptus.auth.policy.PolicySpec.IAM_RESOURCE_USER;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_STS;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.auth.tokens.SecurityTokenValidationException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.tokens.policy.ExternalIdContext;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * Service component for temporary access tokens
 */
@SuppressWarnings( "UnusedDeclaration" )
public class TokensService {

  public GetSessionTokenResponseType getSessionToken( final GetSessionTokenType request ) throws EucalyptusCloudException {
    final GetSessionTokenResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final Subject subject = ctx.getSubject();
    final User requestUser = ctx.getUser( );

    final Set<QueryIdCredential> queryIdCreds = subject == null ?
        Collections.<QueryIdCredential>emptySet( ) :
        subject.getPublicCredentials( QueryIdCredential.class );
    if ( queryIdCreds.isEmpty( ) ) {
      throw new TokensException( TokensException.Code.MissingAuthenticationToken, "Missing credential." );
    }

    final String queryId = Iterables.getOnlyElement( queryIdCreds ).getQueryId( );
    final AccessKey accessKey;
    try {
      accessKey = Accounts.lookupAccessKeyById( queryId );
    } catch ( final AuthException e ) {
      throw new TokensException( TokensException.Code.MissingAuthenticationToken, "Invalid credential: " + queryId );
    }

    try {
      final int durationSeconds =
          Objects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) );
      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          requestUser,
          accessKey,
          requestUser.isAccountAdmin( ) ? (int)TimeUnit.HOURS.toSeconds( 1 ) : 0,
          durationSeconds );

      reply.setResult( GetSessionTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  public AssumeRoleResponseType assumeRole( final AssumeRoleType request ) throws EucalyptusCloudException {
    final AssumeRoleResponseType reply = request.getReply( );
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );

    // Verify that access is not via a role or password credentials.
    //
    // It is not currently safe to allow access via a role (see EUCA-8416).
    // Other forms of temporary credential are not forbidden by the pipeline at
    // the time of authentication.
    final Context ctx = Contexts.lookup( );
    final Subject subject = ctx.getSubject( );
    final Set<QueryIdCredential> queryIdCreds = subject == null ?
        Collections.<QueryIdCredential>emptySet( ) :
        subject.getPublicCredentials( QueryIdCredential.class );
    if ( queryIdCreds.size( ) == 1 && Iterables.get( queryIdCreds, 0 ).getType( ).isPresent( ) ) {
      throw new TokensException( TokensException.Code.MissingAuthenticationToken, "Temporary credential not permitted." );
    }
    rejectPasswordCredentials( );

    final Role role = lookupRole( request.getRoleArn() );

    //TODO Should we fail if a policy is supplied? (since we ignore it)
    try {
      ExternalIdContext.doWithExternalId(
          request.getExternalId(),
          EucalyptusCloudException.class,
          new Callable<Role>() {
            @Override
            public Role call() throws EucalyptusCloudException {
              try {
                return RestrictedTypes.doPrivilegedWithoutOwner(
                    Accounts.getRoleFullName( role ),
                    new RoleResolver( role ) );
              } catch ( final AuthException e ) {
                throw new EucalyptusCloudException( e.getMessage(), e );
              }
            }
          } );

      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          role,
          Objects.firstNonNull( request.getDurationSeconds(), (int)TimeUnit.HOURS.toSeconds(1)) );
      reply.getAssumeRoleResult().setCredentials( new CredentialsType(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
      reply.getAssumeRoleResult().setAssumedRoleUser( new AssumedRoleUserType(
          role.getRoleId() + ":" + request.getRoleSessionName(),
          assumedRoleArn( role, request.getRoleSessionName() )
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  public GetAccessTokenResponseType getAccessToken( final GetAccessTokenType request ) throws EucalyptusCloudException {
    final GetAccessTokenResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final Subject subject = ctx.getSubject();
    final User requestUser = ctx.getUser( );

    final AccountUsername accountUsername = subject == null ?
        null :
        Iterables.getFirst( subject.getPublicCredentials( AccountUsername.class ), null );
    if ( accountUsername == null ||
        !accountUsername.getAccount( ).equals( ctx.getAccount( ).getName( ) ) ||
        !accountUsername.getUsername( ).equals( requestUser.getName( ) ) ) {
      throw new EucalyptusCloudException( "Invalid authentication" );
    }

    try {
      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          requestUser,
          requestUser.isAccountAdmin( ) ? (int)TimeUnit.DAYS.toSeconds( 1 ) : 0,
          Objects.firstNonNull( request.getDurationSeconds(), (int)TimeUnit.HOURS.toSeconds(12)));
      reply.setResult( GetAccessTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  public GetImpersonationTokenResponseType getImpersonationToken( final GetImpersonationTokenType request ) throws EucalyptusCloudException {
    final GetImpersonationTokenResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();

    // Verify that access is not via password credentials.
    rejectPasswordCredentials( );

    final User impersonated;
    final Account impersonatedAccount;
    try {
      if ( !Strings.isNullOrEmpty( request.getImpersonatedUserId( ) ) ) {
        impersonated = Accounts.lookupUserById( request.getImpersonatedUserId( ) );
      } else {
        Account account = Accounts.lookupAccountByName( request.getAccountAlias( ) );
        impersonated = account.lookupUserByName( request.getUserName( ) );
      }
      impersonatedAccount = impersonated.getAccount( );
    } catch ( AuthException e ) {
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    }

    try {
      if ( !ctx.isAdministrator() || !Permissions.isAuthorized(
          VENDOR_STS,
          IAM_RESOURCE_USER,
          Accounts.getUserFullName( impersonated ),
          impersonatedAccount,
          PolicySpec.STS_GETIMPERSONATIONTOKEN,
          ctx.getAuthContext( ) ) ) {
        throw new EucalyptusCloudException( "Permission denied" );
      }

      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          impersonated,
          impersonated.isAccountAdmin( ) ? (int)TimeUnit.DAYS.toSeconds( 1 ) : 0,
          Objects.firstNonNull( request.getDurationSeconds(), (int)TimeUnit.HOURS.toSeconds(12)));
      reply.setResult( GetImpersonationTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  private static void rejectPasswordCredentials( ) throws TokensException {
    final Context context = Contexts.lookup( );
    final Subject subject = context.getSubject( );
    final AccountUsername accountUsername = subject == null ?
        null :
        Iterables.getFirst( subject.getPublicCredentials( AccountUsername.class ), null );
    if ( accountUsername != null ) {
      throw new TokensException( TokensException.Code.MissingAuthenticationToken, "Missing credential." );
    }
  }

  private static String assumedRoleArn( final Role role,
                                        final String roleSessionName ) throws AuthException {
    return "arn:aws:sts::"+role.getAccount().getAccountNumber()+":assumed-role"+Accounts.getRoleFullName( role )+"/"+roleSessionName;
  }

  private static Role lookupRole( final String roleArnString ) throws TokensException {
    try {
      final Ern roleArn = Ern.parse( roleArnString );
      if ( !(roleArn instanceof EuareResourceName) ||
          !PolicySpec.IAM_RESOURCE_ROLE.equals(((EuareResourceName) roleArn).getUserOrGroup()) ) throw new IllegalArgumentException();
      final String roleAccountId = roleArn.getNamespace();
      final String roleName = ((EuareResourceName) roleArn).getName();

      final Account account = Account.SYSTEM_ACCOUNT.equals( roleAccountId ) ?
          Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT ) :
          Accounts.lookupAccountById( roleAccountId );
      return account.lookupRoleByName( roleName );
    } catch ( Exception e ) {
      throw new TokensException( TokensException.Code.InvalidParameterValue, "Invalid role: " + roleArnString );
    }
  }

  private static class RoleResolver implements Function<String,Role> {
    private final Role role;

    private RoleResolver( final Role role ) {
      this.role = role;
    }

    @Override
    public Role apply( @Nullable final String roleFullName ) {
      return role;
    }
  }
}
