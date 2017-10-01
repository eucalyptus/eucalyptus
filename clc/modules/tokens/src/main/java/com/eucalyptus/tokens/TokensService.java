/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.tokens;

import static com.eucalyptus.auth.AccessKeys.accessKeyIdentifier;
import static com.eucalyptus.auth.login.AccountUsernamePasswordCredentials.AccountUsername;
import com.eucalyptus.auth.principal.AccessKeyCredential;
import static com.eucalyptus.auth.policy.PolicySpec.IAM_RESOURCE_USER;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_STS;
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.euare.common.oidc.OIDCIssuerIdentifier;
import com.eucalyptus.auth.euare.common.oidc.OIDCUtils;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.euare.policy.OpenIDConnectAudKey;
import com.eucalyptus.auth.euare.policy.OpenIDConnectSubKey;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.HasRole;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.auth.principal.Principal.PrincipalType;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.auth.tokens.SecurityTokenValidationException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.tokens.TokensException.Code;
import com.eucalyptus.tokens.common.msgs.AssumeRoleResponseType;
import com.eucalyptus.tokens.common.msgs.AssumeRoleType;
import com.eucalyptus.tokens.common.msgs.AssumeRoleWithWebIdentityResponseType;
import com.eucalyptus.tokens.common.msgs.AssumeRoleWithWebIdentityResultType;
import com.eucalyptus.tokens.common.msgs.AssumeRoleWithWebIdentityType;
import com.eucalyptus.tokens.common.msgs.AssumedRoleUserType;
import com.eucalyptus.tokens.common.msgs.CredentialsType;
import com.eucalyptus.tokens.common.msgs.GetAccessTokenResponseType;
import com.eucalyptus.tokens.common.msgs.GetAccessTokenResultType;
import com.eucalyptus.tokens.common.msgs.GetAccessTokenType;
import com.eucalyptus.tokens.common.msgs.GetCallerIdentityResponseType;
import com.eucalyptus.tokens.common.msgs.GetCallerIdentityResultType;
import com.eucalyptus.tokens.common.msgs.GetCallerIdentityType;
import com.eucalyptus.tokens.common.msgs.GetImpersonationTokenResponseType;
import com.eucalyptus.tokens.common.msgs.GetImpersonationTokenResultType;
import com.eucalyptus.tokens.common.msgs.GetImpersonationTokenType;
import com.eucalyptus.tokens.common.msgs.GetSessionTokenResponseType;
import com.eucalyptus.tokens.common.msgs.GetSessionTokenResultType;
import com.eucalyptus.tokens.common.msgs.GetSessionTokenType;
import com.eucalyptus.tokens.oidc.JsonWebKey;
import com.eucalyptus.tokens.oidc.JsonWebKeySet;
import com.eucalyptus.tokens.oidc.JsonWebSignatureVerifier;
import com.eucalyptus.tokens.oidc.OidcDiscoveryCache;
import com.eucalyptus.tokens.oidc.OidcIdentityToken;
import com.eucalyptus.tokens.oidc.OidcParseException;
import com.eucalyptus.tokens.oidc.OidcProviderConfiguration;
import com.eucalyptus.tokens.policy.ExternalIdKey;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.apache.log4j.Logger;

/**
 * Service component for temporary access tokens
 */
@SuppressWarnings( { "UnusedDeclaration", "Guava", "StaticPseudoFunctionalStyleMethod" } )
@ComponentNamed
public class TokensService {
  private static final Logger LOG = Logger.getLogger( TokensService.class );
  private static final Pattern ROLE_ARN_PATTERN = Pattern.compile( "arn:aws:iam::([0-9\\p{javaLowerCase}-]{1,63}):role/.+" );
  private static final int ROLE_ARN_PATTERN_ACCOUNT_GROUP = 1;
  private static final OidcDiscoveryCache oidcDiscoveryCache = new OidcDiscoveryCache( );

  public GetCallerIdentityResponseType getCallerIdentity(
      final GetCallerIdentityType request
  ) throws EucalyptusCloudException {
    final GetCallerIdentityResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final UserPrincipal user = ctx.getUser( );
    final String arn;
    final String userId = user.getAuthenticatedId( );
    final String account = ctx.getAccountNumber( );
    try {
      final Optional<RoleSecurityTokenAttributes> roleAttributes = RoleSecurityTokenAttributes.forUser( user );
      if ( roleAttributes.isPresent( ) ) {
        arn = assumedRoleArn( ((HasRole) user).getRole( ), roleAttributes.get( ).getSessionName( ) );
      } else {
        arn = Accounts.getUserArn( user );
      }
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage( ), e );
    }
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    reply.setResult( new GetCallerIdentityResultType( arn, userId, account ) );
    return reply;
  }

  public GetSessionTokenResponseType getSessionToken( final GetSessionTokenType request ) throws EucalyptusCloudException {
    final GetSessionTokenResponseType reply = request.getReply();
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );
    final Context ctx = Contexts.lookup();
    final Subject subject = ctx.getSubject();
    final User requestUser = ctx.getUser( );

    final Set<AccessKeyCredential> accessKeyCredentials = subject == null ?
        Collections.emptySet( ) :
        subject.getPublicCredentials( AccessKeyCredential.class );
    if ( accessKeyCredentials.isEmpty( ) ) {
      throw new TokensException( Code.MissingAuthenticationToken, "Missing credential." );
    }

    final String accessKeyId = Iterables.getOnlyElement( accessKeyCredentials ).getAccessKeyId( );
    final AccessKey accessKey;
    try {
      accessKey = Iterables.find( requestUser.getKeys( ), propertyPredicate( accessKeyId, accessKeyIdentifier( ) ) );
    } catch ( final AuthException | NoSuchElementException e ) {
      throw new TokensException( Code.MissingAuthenticationToken, "Invalid credential: " + accessKeyId );
    }

    try {
      final int durationSeconds =
          MoreObjects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) );
      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          requestUser,
          accessKey,
          requestUser.isAccountAdmin() ? (int) TimeUnit.HOURS.toSeconds( 1 ) : 0,
          durationSeconds );

      reply.setResult( GetSessionTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( Code.ValidationError, e.getMessage( ) );
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
    final Set<AccessKeyCredential> accessKeyCredentials = subject == null ?
        Collections.emptySet( ) :
        subject.getPublicCredentials( AccessKeyCredential.class );
    //noinspection OptionalGetWithoutIsPresent
    if ( accessKeyCredentials.size( ) == 1 &&
        Iterables.get( accessKeyCredentials, 0 ).getType( ).isDefined( ) &&
        Iterables.get( accessKeyCredentials, 0 ).getType( ).get( ) != TemporaryAccessKey.TemporaryKeyType.Access ) {
      throw new TokensException( Code.MissingAuthenticationToken, "Temporary credential not permitted." );
    }
    rejectPasswordCredentials( );

    // basic parameter validation
    if ( request.getRoleSessionName( ) == null || !request.getRoleSessionName( ).matches( "[\\w+=,.@-]{2,64}" ) ) {
      throw new TokensException( Code.ValidationError, "Invalid role session name" );
    }

    final BaseRole role = lookupRole( request.getRoleArn( ), "AssumeRole" );

    try {
      // check for ec2 principal (instance profile)
      final PrincipalType principalType;
      final String principalName;
      final RoleSecurityTokenAttributes tokenAttributes;
      if ( request.getRoleSessionName( ).matches( "i-[0-9a-fA-F]{8,32}" ) &&
          Principals.isSameUser( Contexts.lookup( ).getUser( ), Principals.systemUser( ) ) ) {
        principalType = PrincipalType.Service;
        principalName = "ec2.amazon.com";
        tokenAttributes = RoleSecurityTokenAttributes.instance(
            request.getRoleSessionName( ),
            "arn:aws:ec2:" + RegionConfigurations.getRegionName( ).or( "" ) + ":" +
                role.getAccountNumber( ) + ":instance/" + request.getRoleSessionName( )
        );
      } else {
        principalType = null;
        principalName = null;
        tokenAttributes = RoleSecurityTokenAttributes.basic( request.getRoleSessionName( ) );
      }

      try {
        PolicyEvaluationContext.builder( )
            .attrIfNotNull( ExternalIdKey.CONTEXT_KEY, request.getExternalId( ) )
            .attrIfNotNull( RestrictedTypes.principalTypeContextKey, principalType )
            .attrIfNotNull( RestrictedTypes.principalNameContextKey, principalName )
            .build( ).doWithContext( () ->
            RestrictedTypes.doPrivileged(
                Accounts.getRoleFullName( role ),
                new RoleResolver( role ) ) );
      } catch ( final AuthException e ) {
        throw new TokensException( Code.AccessDenied, "Not authorized to perform sts:AssumeRole" );
      } catch ( final Exception e ) {
        throw new TokensException( Code.AccessDenied, e.getMessage( ) );
      }

      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          role,
          tokenAttributes,
          MoreObjects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 1 ) ) );
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
      throw new TokensException( Code.ValidationError, e.getMessage( ) );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  public AssumeRoleWithWebIdentityResponseType assumeRoleWithWebIdentity(
      final AssumeRoleWithWebIdentityType request
  ) throws EucalyptusCloudException {
    final AssumeRoleWithWebIdentityResponseType reply = request.getReply( );
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );

    // verify credentials were not used in the request
    final Context ctx = Contexts.lookup( );
    final Subject subject = ctx.getSubject( );
    final Set<Object> creds = subject == null ? Collections.emptySet( ) : subject.getPublicCredentials( );
    if ( !creds.isEmpty( ) ) {
      throw new TokensException( Code.AccessDenied, "Credentials not acceptable for action" );
    }

    // basic parameter validation
    if ( request.getRoleSessionName( ) == null || !request.getRoleSessionName( ).matches( "[\\w+=,.@-]{2,64}" ) ) {
      throw new TokensException( Code.ValidationError, "Invalid role session name" );
    }
    if ( request.getWebIdentityToken( ) == null ||
        request.getWebIdentityToken( ).length( ) < 4 ||
        request.getWebIdentityToken( ).length( ) > 2048 ) {
      throw new TokensException( Code.ValidationError, "Token invalid" );
    }

    final BaseRole role = lookupRole( request.getRoleArn( ), "AssumeRoleWithWebIdentity" );

    try {
      final String identityToken = request.getWebIdentityToken( );
      // parse JWT
      final String[] jwtParts = identityToken.split( "\\." );
      if ( jwtParts.length != 3 ) {
        throw new TokensException( Code.InvalidIdentityToken,
            "The ID Token provided is not a valid JWT. (You may see this error if you sent an Access Token)" );
      }
      final String issuerUrl;
      final List<String> audList;
      final String sub;
      try {
        final OidcIdentityToken idToken = OidcIdentityToken.parse(
            new String( BaseEncoding.base64Url( ).decode( jwtParts[ 1 ] ), StandardCharsets.UTF_8 ) );
        issuerUrl = idToken.getIss( );
        audList = idToken.getAud( );
        sub = idToken.getSub( );
        if ( Strings.isNullOrEmpty( sub ) ) {
          throw new TokensException( Code.InvalidIdentityToken, "Invalid token subject" );
        }
        final long now = System.currentTimeMillis( );
        final long skew = TimeUnit.SECONDS.toMillis( TokensServiceConfiguration.getWebIdentityTokenTimeSkew( ) );
        final long issued = idToken.getIat( ) * 1000L;
        if ( ( issued - skew ) > now ) {
          throw new TokensException( Code.InvalidIdentityToken, "Token not yet valid" );
        }
        final long notBefore = idToken.getNbf( ).getOrElse( 0L ) * 1000L;
        if ( ( notBefore - skew ) > now ) {
          throw new TokensException( Code.InvalidIdentityToken, "Token not yet valid" );
        }
        final long expiration = idToken.getExp( ) * 1000L;
        if ( ( expiration + skew ) < now ) {
          throw new TokensException( Code.ExpiredTokenException, "Token has expired" );
        }
      } catch ( IllegalArgumentException | OidcParseException e ) {
        throw new TokensException( Code.InvalidIdentityToken, "Token invalid: " + e.getMessage( ) );
      }

      // fetch oidc provider
      final String accountId = role.getAccountNumber( );
      final OIDCIssuerIdentifier issuerIdentifier = OIDCUtils.parseIssuerIdentifier( issuerUrl );
      final OpenIdConnectProvider provider =
          lookupOpenIdConnectProvider( accountId, issuerIdentifier.getHost( ) + issuerIdentifier.getPath( ) );
      final String providerArn = provider.getArn( );

      // verify aud from token
      final String aud = Stream.ofAll( audList ).find( provider.getClientIds( )::contains )
          .getOrElseThrow( ( ) -> new TokensException( Code.InvalidIdentityToken, "Incorrect token audience" ) );

      // oidc discovery
      final String configJson = resolveUrl( OidcProviderConfiguration.buildDiscoveryUrl( provider ) ).getLeft( );
      final OidcProviderConfiguration providerConfiguration;
      try {
        providerConfiguration = OidcProviderConfiguration.parse( configJson );
      } catch ( final OidcParseException e ) {
        LOG.warn( "Error performing discovery for oidc provider: " + e.getMessage( ) );
        throw new TokensException( Code.IDPCommunicationError, "Error discovering OIDC provider configuration" );
      }
      if ( !providerConfiguration.getIssuer( ).equals( OIDCUtils.buildIssuerIdentifier( provider ) ) ) {
        LOG.warn( "OIDC provider discovery error, issuer mismatch: " + providerConfiguration.getIssuer( ) );
        throw new TokensException( Code.IDPCommunicationError, "Error discovering OIDC provider configuration" );
      }

      final Pair<String, Certificate[]> readResult = resolveUrl( providerConfiguration.getJwksUri( ) );
      final byte[] thumbprint = Digest.SHA1.digestBinary( readResult.getRight( )[ 0 ].getEncoded( ) );
      if ( !Stream.ofAll( provider.getThumbprints( ) ).find( providerThumb ->
          MessageDigest.isEqual(
              thumbprint,
              BaseEncoding.base16( ).decode( Ascii.toUpperCase( providerThumb ) ) )
      ).isDefined( ) ) {
        throw new TokensException( Code.ValidationError, "SSL Certificate thumbprint does not match" );
      }
      // verify JWT signature
      final String keysJson = readResult.getLeft( );
      if ( !isSignatureVerified(
          jwtParts,
          keysJson,
          alg -> TokensServiceConfiguration.getWebIdSignatureAlgorithmPattern( ).matcher( alg ).matches( ) )
      ) {
        throw new TokensException( Code.InvalidIdentityToken, "Token signature invalid" );
      }

      // verify assume role policy
      try {
        PolicyEvaluationContext.builder( )
            .attr( RestrictedTypes.principalTypeContextKey, PrincipalType.Federated )
            .attr( RestrictedTypes.principalNameContextKey, providerArn )
            .attr( OpenIDConnectAudKey.CONTEXT_KEY, Pair.pair( provider.getUrl( ), aud ) )
            .attr( OpenIDConnectSubKey.CONTEXT_KEY, Pair.pair( provider.getUrl( ), sub ) )
            .build( ).doWithContext( () ->
            RestrictedTypes.doPrivileged(
                Accounts.getRoleFullName( role ),
                new RoleResolver( role ) ) );
      } catch ( final AuthException e ) {
        throw new TokensException( Code.AccessDenied, "Not authorized to perform sts:AssumeRoleWithWebIdentity" );
      } catch ( final Exception e ) {
        throw new TokensException( Code.AccessDenied, e.getMessage( ) );
      }

      // issue credentials
      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          role,
          RoleSecurityTokenAttributes.webIdentity(
              request.getRoleSessionName( ),
              provider.getUrl( ),
              aud,
              sub
          ),
          MoreObjects.firstNonNull( request.getDurationSeconds( ), (int) TimeUnit.HOURS.toSeconds( 1 ) ) );

      // populate result
      final AssumeRoleWithWebIdentityResultType result = reply.getAssumeRoleWithWebIdentityResult( );
      result.setProvider( providerArn );
      result.setAudience( aud );
      result.setSubjectFromWebIdentityToken( sub );
      result.setCredentials( new CredentialsType(
          token.getAccessKeyId( ),
          token.getSecretKey( ),
          token.getToken( ),
          token.getExpires( )
      ) );
      result.setAssumedRoleUser( new AssumedRoleUserType(
          role.getRoleId( ) + ":" + request.getRoleSessionName( ),
          assumedRoleArn( role, request.getRoleSessionName( ) )
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( Code.ValidationError, e.getMessage( ) );
    } catch ( final IOException e ) {
      LOG.warn( "Error performing discovery for oidc provider: " + e.getMessage( ) );
      Logs.exhaust( ).info( "Error performing discovery for oidc provider: " + e.getMessage( ), e );
      throw new TokensException( Code.IDPCommunicationError, e.getMessage( ) );
    } catch ( GeneralSecurityException e ) {
      LOG.error( "Error assuming role with web identity", e );
      throw new EucalyptusCloudException( e.getMessage(), e );
    } catch ( final AuthException e ) {
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  private static Pair<String, Certificate[]> resolveUrl( final String url ) throws IOException {
    return oidcDiscoveryCache.get(
        TokensServiceConfiguration.webIdentityOidcDiscoveryCache,
        TokensServiceConfiguration.webIdentityOidcDiscoveryRefresh * 1000L,
        System.currentTimeMillis( ),
        url );
  }

  static Boolean isSignatureVerified(
      final String[] jwtParts,
      final String jwkText,
      final Predicate<String> signatureAlgorithmPredicate
      ) throws GeneralSecurityException {
    try {
      final JsonWebKeySet webKeySet = JsonWebKeySet.parse( jwkText );
      return JsonWebSignatureVerifier.isValid(
          jwtParts[ 0 ],
          jwtParts[ 1 ],
          jwtParts[ 2 ],
          new JsonWebSignatureVerifier.KeyResolver( ) {
            @Override
            public <K extends JsonWebKey> Option<K> resolve( final Option<String> kid, final Class<K> keyType ) {
              return webKeySet.findKey( kid, keyType, "sig", "verify" );
            }
          },
          signatureAlgorithmPredicate
      );
    } catch ( GeneralSecurityException e ) {
      throw  e;
    } catch ( Throwable e ) {
      throw new GeneralSecurityException( e.getMessage( ), e );
    }
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

    try {
      if ( accountUsername == null ||
          !accountUsername.getAccount( ).equals( Accounts.lookupAccountAliasById( ctx.getAccountNumber( ) ) ) ||
          !accountUsername.getUsername( ).equals( requestUser.getName( ) ) ) {
        throw new EucalyptusCloudException( "Invalid authentication" );
      }

      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          requestUser,
          requestUser.isAccountAdmin() ? (int) TimeUnit.DAYS.toSeconds( 1 ) : 0,
          MoreObjects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) ) );
      reply.setResult( GetAccessTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( Code.ValidationError, e.getMessage( ) );
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
    final AccountFullName impersonatedAccount;
    try {
      if ( !Strings.isNullOrEmpty( request.getImpersonatedUserId( ) ) ) {
        impersonated = Accounts.lookupPrincipalByUserId( request.getImpersonatedUserId( ) );
      } else {
        String accountNumber = Accounts.lookupAccountIdByAlias( request.getAccountAlias( ) );
        impersonated = Accounts.lookupPrincipalByAccountNumberAndUsername( accountNumber, request.getUserName( ) );
      }
      impersonatedAccount = AccountFullName.getInstance( impersonated.getAccountNumber( ) );
    } catch ( AuthException e ) {
      throw new TokensException( Code.ValidationError, e.getMessage( ) );
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
          impersonated.isAccountAdmin() ? (int) TimeUnit.DAYS.toSeconds( 1 ) : 0,
          MoreObjects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) ) );
      reply.setResult( GetImpersonationTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final SecurityTokenValidationException e ) {
      throw new TokensException( Code.ValidationError, e.getMessage( ) );
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
      throw new TokensException( Code.MissingAuthenticationToken, "Missing credential." );
    }
  }

  private static String assumedRoleArn( final BaseRole role,
                                        final String roleSessionName ) throws AuthException {
    return Accounts.getAssumedRoleArn( role, roleSessionName );
  }

  private static BaseRole lookupRole( final String roleArnStringWithAlias,
                                      final String action ) throws TokensException {
    try {
      final Matcher matcher = ROLE_ARN_PATTERN.matcher( roleArnStringWithAlias );
      if ( !matcher.matches( ) ) {
        throw new IllegalArgumentException();
      }
      final String accountNumberOrAlias = matcher.group( ROLE_ARN_PATTERN_ACCOUNT_GROUP );
      final String roleArnString = Accounts.isAccountNumber( accountNumberOrAlias ) ||
          !TokensServiceConfiguration.getRoleArnAliasPattern( ).matcher( accountNumberOrAlias ).matches( ) ?
          roleArnStringWithAlias :
          roleArnStringWithAlias.substring( 0, matcher.start( ROLE_ARN_PATTERN_ACCOUNT_GROUP ) ) +
              Accounts.lookupAccountIdentifiersByAlias( accountNumberOrAlias ).getAccountNumber( ) +
              roleArnStringWithAlias.substring( matcher.end( ROLE_ARN_PATTERN_ACCOUNT_GROUP ) );
      final Ern roleArn = Ern.parse( roleArnString );
      if ( !( roleArn instanceof EuareResourceName ) ||
          !PolicySpec.IAM_RESOURCE_ROLE.equals( ( (EuareResourceName) roleArn ).getType( ) ) ||
          roleArn.getAccount( ) == null ) {
        throw new IllegalArgumentException( );
      }
      final String roleAccountId = roleArn.getAccount( );
      final String roleName = ( (EuareResourceName) roleArn ).getName( );
      return Accounts.lookupRoleByName( roleAccountId, roleName );
    } catch ( Exception e ) {
      throw new TokensException( Code.AccessDenied, "Not authorized to perform sts:" + action );
    }
  }

  private static class RoleResolver implements Function<String,BaseRole> {
    private final BaseRole role;

    private RoleResolver( final BaseRole role ) {
      this.role = role;
    }

    @Override
    public BaseRole apply( @Nullable final String roleFullName ) {
      return role;
    }
  }

  private static OpenIdConnectProvider lookupOpenIdConnectProvider( String accountId, final String url ) throws TokensException {
    try {
      return Accounts.lookupOidcProviderByUrl( accountId, url );
    } catch ( Exception e ) {
      throw new TokensException( Code.InvalidParameterValue, "Invalid openid connect provider: " + url + ", account: " + accountId );
    }
  }
}
