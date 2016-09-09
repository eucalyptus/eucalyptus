/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static com.eucalyptus.auth.AccessKeys.accessKeyIdentifier;
import static com.eucalyptus.auth.login.AccountUsernamePasswordCredentials.AccountUsername;
import static com.eucalyptus.auth.login.HmacCredentials.QueryIdCredential;
import static com.eucalyptus.auth.policy.PolicySpec.IAM_RESOURCE_USER;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_STS;
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.Subject;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.euare.common.oidc.OIDCIssuerIdentifier;
import com.eucalyptus.auth.euare.common.oidc.OIDCUtils;
import com.eucalyptus.auth.euare.policy.OpenIDConnectAudKey;
import com.eucalyptus.auth.euare.policy.OpenIDConnectSubKey;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.auth.tokens.SecurityTokenValidationException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.SslSetup;
import com.eucalyptus.tokens.policy.ExternalIdKey;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

/**
 * Service component for temporary access tokens
 */
@SuppressWarnings( "UnusedDeclaration" )
public class TokensService {
  private static final Logger LOG = Logger.getLogger( TokensService.class );

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
      accessKey = Iterables.find( requestUser.getKeys( ), propertyPredicate( queryId, accessKeyIdentifier( ) ) );
    } catch ( final AuthException | NoSuchElementException e ) {
      throw new TokensException( TokensException.Code.MissingAuthenticationToken, "Invalid credential: " + queryId );
    }

    try {
      final int durationSeconds =
          Objects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) );
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
    if ( queryIdCreds.size( ) == 1 && 
        Iterables.get( queryIdCreds, 0 ).getType( ).isPresent( ) && 
        Iterables.get( queryIdCreds, 0 ).getType( ).get( ) != TemporaryAccessKey.TemporaryKeyType.Access ) {
      throw new TokensException( TokensException.Code.MissingAuthenticationToken, "Temporary credential not permitted." );
    }
    rejectPasswordCredentials( );

    final BaseRole role = lookupRole( request.getRoleArn( ) );

    //TODO Should we fail if a policy is supplied? (since we ignore it)
    try {
      try {
        PolicyEvaluationContext.builder( )
            .attrIfNotNull( ExternalIdKey.CONTEXT_KEY, request.getExternalId( ) )
            .build( ).doWithContext( () ->
            RestrictedTypes.doPrivilegedWithoutOwner(
                Accounts.getRoleFullName( role ),
                new RoleResolver( role ) ) );
      } catch ( final Exception e ) {
        throw new TokensException( TokensException.Code.AccessDenied, e.getMessage( ) );
      }

      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          role,
          Objects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 1 ) ) );
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

  public AssumeRoleWithWebIdentityResponseType assumeRoleWithWebIdentity( final AssumeRoleWithWebIdentityType request ) throws EucalyptusCloudException {
    final AssumeRoleWithWebIdentityResponseType reply = request.getReply( );
    reply.getResponseMetadata().setRequestId( reply.getCorrelationId( ) );

    // Verify that access is not via a role or password credentials.
    //
    // It is not currently safe to allow access via a role (see EUCA-8416).
    // Other forms of temporary credential are not forbidden by the pipeline at
    // the time of authentication.
    final Context ctx = Contexts.lookup( );
    final Subject subject = ctx.getSubject( );
    final Set<Object> queryIdCreds = subject == null ? Collections.emptySet( ) : subject.getPublicCredentials( );
    if ( !queryIdCreds.isEmpty( ) ) {
      throw new TokensException( TokensException.Code.AccessDenied, "Credentials not acceptable for action" );
    }

    final BaseRole role = lookupRole( request.getRoleArn( ) );

    //TODO Should we fail if a policy is supplied? (since we ignore it)
    try {
      final String identityToken = request.getWebIdentityToken();
      // parse JWT
      final String [] jwtParts = identityToken.split("\\.");
      final JSONObject jwtBody = JSONObject.fromObject( new String(
          BaseEncoding.base64Url( ).decode( jwtParts[1] ),
          StandardCharsets.UTF_8 ) );
      final String issuerUrl = jwtBody.getString("iss");
      
      // get account id from role ARN
      final Ern roleArn = Ern.parse( request.getRoleArn() );
      final String roleAccountId = roleArn.getAccount( );

      // fetch oidc provider
      final OIDCIssuerIdentifier issuerIdentifier = OIDCUtils.parseIssuerIdentifier( issuerUrl );
      final OpenIdConnectProvider provider =
          lookupOpenIdConnectProvider( roleAccountId, issuerIdentifier.getHost( ) + issuerIdentifier.getPath( ) );
      final String trustedProviderUrl = OIDCUtils.buildIssuerIdentifier( provider );

      // oidc discovery
      final String configJson = readUrl( trustedProviderUrl + "/.well-known/openid-configuration" ).getLeft();
      final JSONObject config = JSONObject.fromObject(configJson);

      final Pair<String, Certificate []> readResult = readUrl( (String)config.get("jwks_uri") );
      // TODO: improve this test to account for case issues
      final String thumbprint = getServerCertThumbprint( readResult.getRight()[0] );
      if ( !provider.getThumbprints().contains( thumbprint ) ) {
        throw new TokensException( TokensException.Code.ValidationError, "SSL Certificate thumbprint does not match" );
      }
      // verify JWT signature
      final String keysJson = readResult.getLeft();
      if ( isSignatureVerified( jwtParts, keysJson ) ) {
        throw new TokensException( TokensException.Code.ValidationError, "signature not valid" );
      }

      // verify aud = clientId and expiration is valid
      final String aud = jwtBody.getString("aud");
      final String sub = jwtBody.getString("sub");
      if ( !(provider.getClientIds().contains( aud ) ) ) {
        throw new TokensException( TokensException.Code.ValidationError, "clientID does not match" );
      }
      if ( Strings.isNullOrEmpty( sub ) ) {
        throw new TokensException( TokensException.Code.ValidationError, "Invalid subject" );
      }
      final long expiration = jwtBody.getLong("exp") * 1000;
      if ( new Date(expiration).compareTo( new Date() ) < 0 ) {
        throw new TokensException( TokensException.Code.ValidationError, "web token has expired" );
      }

      // verify assume role policy
      try {
        PolicyEvaluationContext.builder( )
            .attr( RestrictedTypes.principalTypeContextKey, Principal.PrincipalType.Federated )
            .attr( RestrictedTypes.principalNameContextKey, Accounts.getOpenIdConnectProviderArn( provider )  )
            .attr( OpenIDConnectAudKey.CONTEXT_KEY, Pair.pair( provider.getUrl( ), aud ) )
            .attr( OpenIDConnectSubKey.CONTEXT_KEY, Pair.pair( provider.getUrl( ), sub ) )
            .build( ).doWithContext( () ->
            RestrictedTypes.doPrivilegedWithoutOwner(
                Accounts.getRoleFullName( role ),
                new RoleResolver( role ) ) );
      } catch ( final Exception e ) {
        throw new TokensException( TokensException.Code.AccessDenied, e.getMessage( ) );
      }

      // issue credentials
      final SecurityToken token = SecurityTokenManager.issueSecurityToken(
          role,
          Objects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 1 ) ) );
      reply.getAssumeRoleWithWebIdentityResult().setCredentials( new CredentialsType(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
      reply.getAssumeRoleWithWebIdentityResult().setAssumedRoleUser( new AssumedRoleUserType(
          role.getRoleId() + ":" + request.getRoleSessionName(),
          assumedRoleArn( role, request.getRoleSessionName() )
      ) );
    } catch ( IllegalArgumentException | CertificateEncodingException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException e ) {
      LOG.error("problem w/ assume role", e);
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    } catch ( InvalidKeyException | SignatureException | IOException | JSONException | SecurityTokenValidationException e ) {
      LOG.error("problem w/ assume role", e);
      throw new TokensException( TokensException.Code.ValidationError, e.getMessage( ) );
    } catch ( final AuthException e ) {
      LOG.error("problem w/ assume role", e);
      throw new EucalyptusCloudException( e.getMessage(), e );
    }

    return reply;
  }

  protected static Pair<String, Certificate []> readUrl(String url) throws IOException {
    final URL location = new URL( url );
    LOG.info("reading from " + url);
    URLConnection conn = location.openConnection();
    SslSetup.configureHttpsUrlConnection( conn );
    final InputStream istr = (InputStream)conn.getContent();
    Certificate [] certs = null;
    if (conn instanceof HttpsURLConnection) {
      certs = ((HttpsURLConnection)conn).getServerCertificates();
    }
    Scanner s = new Scanner(istr).useDelimiter("\\A");
    return Pair.pair(( s.hasNext() ? s.next() : "" ), certs);
  }

  public static String getServerCertThumbprint(Certificate cert) throws CertificateEncodingException {
    return Digest.SHA1.digestHex( cert.getEncoded( ) ).toUpperCase( );
  }

  public static Boolean isSignatureVerified(String [] jwtParts, String jwkText) throws CertificateEncodingException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
    final JSONObject keys = JSONObject.fromObject(jwkText);
    final JSONObject keyData = keys.getJSONArray("keys").getJSONObject(0);
    final String jwks_n = keyData.getString("n");
    final String jwks_e = keyData.getString("e");
    final String jwks_alg = keyData.getString("alg");
    final BigInteger modulus = new BigInteger( 1, BaseEncoding.base64Url().decode(jwks_n) );
    final BigInteger publicExponent = new BigInteger( 1, BaseEncoding.base64Url().decode(jwks_e) );
    final PublicKey key =
        KeyFactory.getInstance( "RSA" ).generatePublic( new RSAPublicKeySpec(modulus, publicExponent) );
    final byte [] sigBytes = BaseEncoding.base64Url( ).decode(jwtParts[2]);
    final byte [] bytesToSign = (jwtParts[0] + "." + jwtParts[1]).getBytes( StandardCharsets.UTF_8 );
    final Signature sig = Signature.getInstance("SHA512withRSA", "BC");
    sig.initVerify(key);
    sig.update(bytesToSign);
    return !sig.verify(sigBytes);
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
          Objects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) ) );
      reply.setResult( GetAccessTokenResultType.forCredentials(
          token.getAccessKeyId(),
          token.getSecretKey(),
          token.getToken(),
          token.getExpires()
      ) );
    } catch ( final EucalyptusCloudException e  ) {
      throw e;
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
          impersonated.isAccountAdmin() ? (int) TimeUnit.DAYS.toSeconds( 1 ) : 0,
          Objects.firstNonNull( request.getDurationSeconds(), (int) TimeUnit.HOURS.toSeconds( 12 ) ) );
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

  private static String assumedRoleArn( final BaseRole role,
                                        final String roleSessionName ) throws AuthException {
    return "arn:aws:sts::"+role.getAccountNumber()+":assumed-role"+Accounts.getRoleFullName( role )+"/"+roleSessionName;
  }

  private static BaseRole lookupRole( final String roleArnString ) throws TokensException {
    try {
      final Ern roleArn = Ern.parse( roleArnString );
      if ( !(roleArn instanceof EuareResourceName) ||
          !PolicySpec.IAM_RESOURCE_ROLE.equals(((EuareResourceName) roleArn).getType( )) ) throw new IllegalArgumentException();
      final String roleAccountId = roleArn.getAccount( );
      final String roleName = ((EuareResourceName) roleArn).getName();

      if ( AccountIdentifiers.SYSTEM_ACCOUNT.equals( roleAccountId ) ) {
        final AccountIdentifiers account = Accounts.lookupAccountIdentifiersByAlias( AccountIdentifiers.SYSTEM_ACCOUNT );
        return Accounts.lookupRoleByName( account.getAccountNumber( ), roleName );
      } else {
        return Accounts.lookupRoleByName( roleAccountId, roleName );
      }
    } catch ( Exception e ) {
      throw new TokensException( TokensException.Code.InvalidParameterValue, "Invalid role: " + roleArnString );
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

  private static EuareOpenIdConnectProvider lookupOpenIdConnectProvider( String accountName, final String url ) throws TokensException {
    try {
      EuareAccount account = com.eucalyptus.auth.euare.Accounts.lookupAccountByName( accountName );
      return account.lookupOpenIdConnectProvider( url );
    } catch ( Exception e ) {
      e.printStackTrace();
      throw new TokensException( TokensException.Code.InvalidParameterValue, "Invalid openid connect provider: " + url + ", account: " + accountName);
    }
  }
}
