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
package com.eucalyptus.auth.euare;

import java.net.ConnectException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeCertificateResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeCertificateResult;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeCertificateType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeOidcProviderResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeOidcProviderResult;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeOidcProviderType;
import com.eucalyptus.auth.euare.common.identity.msgs.OidcProvider;
import com.eucalyptus.auth.euare.common.identity.msgs.ReserveNameType;
import com.eucalyptus.auth.euare.common.identity.msgs.SecurityTokenAttribute;
import com.eucalyptus.auth.euare.common.identity.msgs.SignCertificateResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.SignCertificateResult;
import com.eucalyptus.auth.euare.common.identity.msgs.SignCertificateType;
import com.eucalyptus.auth.euare.common.oidc.OIDCIssuerIdentifier;
import com.eucalyptus.auth.euare.common.oidc.OIDCUtils;
import com.eucalyptus.auth.euare.persist.DatabaseAuthUtils;
import com.eucalyptus.auth.api.PrincipalProvider;
import com.eucalyptus.auth.euare.common.identity.msgs.Account;
import com.eucalyptus.auth.euare.common.identity.msgs.DecodeSecurityTokenResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DecodeSecurityTokenResult;
import com.eucalyptus.auth.euare.common.identity.msgs.DecodeSecurityTokenType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeAccountsResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeAccountsType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeInstanceProfileResult;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeInstanceProfileType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribePrincipalResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribePrincipalType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeRoleResponseType;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeRoleResult;
import com.eucalyptus.auth.euare.common.identity.msgs.DescribeRoleType;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.common.identity.msgs.IdentityMessage;
import com.eucalyptus.auth.euare.common.identity.msgs.Policy;
import com.eucalyptus.auth.euare.common.identity.msgs.Principal;
import com.eucalyptus.auth.euare.common.identity.msgs.SecurityToken;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.AccountIdentifiersImpl;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.SecurityTokenContentImpl;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.EphemeralConfiguration;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class RemotePrincipalProvider implements PrincipalProvider {

  private static int MAX_RETRIES = 4;
  private static int RETRY_SLEEP = 20;
  private static float BACKOFF = 2.5f;

  private final Set<String> endpoints;

  public RemotePrincipalProvider( final Set<String> endpoints ) {
    this.endpoints = endpoints;
  }

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setUserId( userId );
    request.setNonce( nonce );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setRoleId( roleId );
    request.setNonce( nonce );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccessKeyId( keyId );
    request.setNonce( nonce );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setCertificateId( certificateId );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setCanonicalId( canonicalId );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccountId( accountNumber );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String name
  ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccountId( accountNumber );
    request.setUsername( name );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByUserId( final UserPrincipal userPrincipal, final String userId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setUserId( userId );
    request.setNonce( nonce );
    return resultFor( request, userPrincipal );

  }

  @Override
  public UserPrincipal lookupCachedPrincipalByRoleId( final UserPrincipal userPrincipal, final String roleId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setRoleId( roleId );
    request.setNonce( nonce );
    return resultFor( request, userPrincipal );

  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccessKeyId( final UserPrincipal userPrincipal, final String keyId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccessKeyId( keyId );
    request.setNonce( nonce );
    return resultFor( request, userPrincipal );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByCertificateId( final UserPrincipal userPrincipal, final String certificateId ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setCertificateId( certificateId );
    return resultFor( request, userPrincipal );

  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccountNumber( final UserPrincipal userPrincipal, final String accountNumber ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccountId( accountNumber );
    return resultFor( request, userPrincipal );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    if ( Accounts.isSystemAccount( alias ) ) {
      throw new AuthException( AuthException.NO_SUCH_ACCOUNT );
    }
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setAlias( alias );
    return resultFor( request );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setCanonicalId( canonicalId );
    return resultFor( request );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByEmail( final String email ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setEmail( email );
    return resultFor( request );
  }

  @Override
  public List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( final String aliasExpression ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setAliasLike( aliasExpression );
    return resultListFor( request );
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String accountNumber, final String name ) throws AuthException {
    final DescribeInstanceProfileType request = new DescribeInstanceProfileType( );
    request.setAccountId( accountNumber );
    request.setInstanceProfileName( name );
    try {
      final DescribeInstanceProfileResponseType response = send( request );
      final DescribeInstanceProfileResult result = response.getDescribeInstanceProfileResult();
      final EuareResourceName profileErn = (EuareResourceName) Ern.parse( result.getInstanceProfile( ).getInstanceProfileArn( ) );
      final Role role = TypeMappers.transform( result.getRole( ), Role.class );
      return new InstanceProfile( ) {
        @Override public String getAccountNumber( ) { return accountNumber; }
        @Override public String getInstanceProfileId( ) { return result.getInstanceProfile( ).getInstanceProfileId( ); }
        @Override public String getInstanceProfileArn( ) { return result.getInstanceProfile( ).getInstanceProfileArn(); }
        @Nullable
        @Override public Role getRole( ) { return role; }
        @Override public String getName( ) { return profileErn.getName( ); }
        @Override public String getPath( ) { return profileErn.getPath(); }
      };
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public Role lookupRoleByName( final String accountNumber, final String name ) throws AuthException {
    final DescribeRoleType request = new DescribeRoleType( );
    request.setAccountId( accountNumber );
    request.setRoleName( name );
    try {
      final DescribeRoleResponseType response = send( request );
      final DescribeRoleResult result = response.getDescribeRoleResult();
      return TypeMappers.transform( result.getRole( ), Role.class );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public OpenIdConnectProvider lookupOidcProviderByUrl( final String accountNumber, final String url ) throws AuthException {
    final DescribeOidcProviderType request = new DescribeOidcProviderType( );
    request.setAccountId( accountNumber );
    request.setProviderUrl( url );
    try {
      final DescribeOidcProviderResponseType response = send( request );
      final DescribeOidcProviderResult result = response.getDescribeOidcProviderResult( );
      return TypeMappers.transform( result.getOidcProvider( ), OpenIdConnectProvider.class );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public SecurityTokenContent decodeSecurityToken( final String accessKeyIdentifier,
                                                   final String securityToken ) throws AuthException {
    final DecodeSecurityTokenType request = new DecodeSecurityTokenType( );
    request.setAccessKeyId( accessKeyIdentifier );
    request.setSecurityToken( securityToken );
    try {
      final DecodeSecurityTokenResponseType response = send( request );
      final DecodeSecurityTokenResult result = response.getDecodeSecurityTokenResult();
      return TypeMappers.transform( result.getSecurityToken(), SecurityTokenContent.class );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public void reserveGlobalName( final String namespace,
                                 final String name,
                                 final Integer duration,
                                 final String clientToken ) throws AuthException {
    final ReserveNameType request = new ReserveNameType( );
    request.setNamespace( namespace );
    request.setName( name );
    request.setDuration( duration );
    request.setClientToken( clientToken );
    try {
      send( request );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public X509Certificate getCertificateByAccountNumber( final String accountNumber ) throws AuthException {
    try {
      final DescribeCertificateResponseType response = send( new DescribeCertificateType( ) );
      final DescribeCertificateResult result = response.getDescribeCertificateResult( );
      return PEMFiles.getCert( result.getPem( ).getBytes( StandardCharsets.UTF_8 ) );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey,
      final String principal,
      final int expiryInDays
  ) throws AuthException {
    try {
      final SignCertificateType signCertificateType = new SignCertificateType( );
      signCertificateType.setKey( B64.standard.encString( publicKey.getEncoded( ) ) );
      signCertificateType.setPrincipal( principal );
      signCertificateType.setExpirationDays( expiryInDays );
      final SignCertificateResponseType response = send( signCertificateType );
      final SignCertificateResult result = response.getSignCertificateResult( );
      return PEMFiles.getCert( result.getPem().getBytes( StandardCharsets.UTF_8 ) );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private <R extends IdentityMessage> R send( final IdentityMessage request ) throws Exception {
    final URI endpoint = URI.create( endpoints.iterator( ).next( ) );
    final ServiceConfiguration config = new EphemeralConfiguration(
        ComponentIds.lookup( Identity.class ),
        "identity",
        "identity",
        endpoint );
    for ( int n=0; n<=MAX_RETRIES; n++ ) {
      try {
        return AsyncRequests.sendSync( config, request );
      } catch ( Exception e ) {
        final Optional<AsyncExceptions.AsyncWebServiceError> errorOptional = AsyncExceptions.asWebServiceError( e );
        if ( errorOptional.isPresent( ) ) {
          throw e; // rethrow errors from service
        }
        if ( Thread.currentThread( ).isInterrupted( ) || n==MAX_RETRIES ) {
          if ( Exceptions.isCausedBy( e, SSLHandshakeException.class ) ) {
            throw new AuthException( "HTTPS connection failed for region host " + endpoint.getHost( ) );
          }
          if ( Exceptions.isCausedBy( e, SSLException.class ) ) {
            throw new AuthException( "HTTPS error for region host " + endpoint.getHost( ) + ": " + String.valueOf( e.getMessage( ) ) );
          }
          if ( Exceptions.isCausedBy( e, ConnectException.class ) ) {
            throw new AuthException( "Error connecting to region host " + endpoint.getHost( ) );
          }
          throw e;
        } else {
          final long sleep = (long)( RETRY_SLEEP * Math.pow( BACKOFF, n ) );
          Thread.sleep( sleep );
        }
      }
    }
    throw new Exception( "Retry error" ); // not reachable
  }

  private AccountIdentifiers resultFor( final DescribeAccountsType request ) throws AuthException {
    try {
      final DescribeAccountsResponseType response = send( request );
      final List<Account> accounts = response.getDescribeAccountsResult( ).getAccounts( );
      if ( accounts.size( ) != 1 ) {
        throw new AuthException( "Account information not found" );
      }
      return TypeMappers.transform( Iterables.getOnlyElement( accounts ), AccountIdentifiers.class );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private List<AccountIdentifiers> resultListFor( final DescribeAccountsType request ) throws AuthException {
    try {
      final DescribeAccountsResponseType response = send( request );
      final List<Account> accounts = response.getDescribeAccountsResult( ).getAccounts( );
      return Lists.newArrayList( Iterables.transform(
          accounts,
          TypeMappers.lookup( Account.class, AccountIdentifiers.class ) ) );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private UserPrincipal resultFor( final DescribePrincipalType request ) throws AuthException {
    return resultFor( request, null );
  }

  private UserPrincipal resultFor( final DescribePrincipalType request, final UserPrincipal cached ) throws AuthException {
    try {
      if ( cached != null ) {
        request.setPtag( cached.getPTag( ) );
      }
      final DescribePrincipalResponseType response = send( request );
      final Principal principal = response.getDescribePrincipalResult( ).getPrincipal( );
      if ( principal == null ) {
        throw new AuthException( "Invalid identity" );
      }
      if ( principal.getPtag( ) != null && cached != null && principal.getPtag( ).equals( cached.getPTag( ) ) ) {
        return cached;
      }
      final UserPrincipal[] userPrincipalHolder = new UserPrincipal[1];
      final Supplier<UserPrincipal> userPrincipalSupplier = new Supplier<UserPrincipal>() {
        @Override
        public UserPrincipal get() {
          return userPrincipalHolder[0];
        }
      };
      final EuareResourceName ern = (EuareResourceName) EuareResourceName.parse( principal.getArn( ) );
      final ImmutableList<AccessKey> accessKeys = ImmutableList.copyOf(
          Iterables.transform( principal.getAccessKeys( ), accessKeyTransform( userPrincipalSupplier ) ) );
      final ImmutableList<Certificate> certificates = ImmutableList.copyOf(
          Iterables.transform( principal.getCertificates( ), certificateTransform( userPrincipalSupplier ) ) );
      final ImmutableList<PolicyVersion> policies = ImmutableList.copyOf( Iterables.transform(
          principal.getPolicies(),
          TypeMappers.lookup( Policy.class, PolicyVersion.class ) ) );
      return userPrincipalHolder[0] = new UserPrincipal( ) {
        @Nonnull
        @Override
        public String getName( ) {
          return ern.getName();
        }

        @Nonnull
        @Override
        public String getPath() {
          return ern.getPath( );
        }

        @Nonnull
        @Override
        public String getUserId() {
          return principal.getUserId( );
        }

        @Nonnull
        @Override
        public String getAuthenticatedId() {
          return Objects.firstNonNull( principal.getRoleId( ), principal.getUserId( ) );
        }

        @Nonnull
        @Override
        public String getAccountAlias() {
          return principal.getAccountAlias( );
        }

        @Nonnull
        @Override
        public String getAccountNumber() {
          return ern.getAccount( );
        }

        @Nonnull
        @Override
        public String getCanonicalId() {
          return principal.getCanonicalId( );
        }

        @Override
        public boolean isEnabled() {
          return principal.getEnabled( );
        }

        @Override
        public boolean isAccountAdmin() {
          return principal.getRoleId( ) == null && DatabaseAuthUtils.isAccountAdmin( getName( ) );
        }

        @Override
        public boolean isSystemAdmin() {
          return principal.getRoleId( ) == null && Accounts.isSystemAccount( getAccountAlias( ) );
        }

        @Override
        public boolean isSystemUser() {
          return Accounts.isSystemAccount( getAccountAlias( ) );
        }

        @Nullable
        @Override
        public String getPassword() {
          return principal.getPasswordHash( );
        }

        @Nullable
        @Override
        public Long getPasswordExpires() {
          return principal.getPasswordExpiry( );
        }

        @Nonnull
        @Override
        public List<AccessKey> getKeys( ) {
          return accessKeys;
        }

        @Nonnull
        @Override
        public List<Certificate> getCertificates( ) {
          return certificates;
        }

        @Nonnull
        @Override
        public List<PolicyVersion> getPrincipalPolicies( ) {
          return policies;
        }

        @Override
        public String getToken() {
          return principal.getToken( );
        }

        @Nullable
        @Override
        public String getPTag() {
          return principal.getPtag( );
        }
      };
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private static NonNullFunction<com.eucalyptus.auth.euare.common.identity.msgs.AccessKey,AccessKey> accessKeyTransform(
    final Supplier<UserPrincipal> userPrincipalSupplier
  ) {
    return new NonNullFunction<com.eucalyptus.auth.euare.common.identity.msgs.AccessKey,AccessKey>( ) {
      @Nonnull
      @Override
      public AccessKey apply( final com.eucalyptus.auth.euare.common.identity.msgs.AccessKey accessKey ) {
        return new AccessKey( ) {
              @Override public Boolean isActive( ) { return true; }
              @Override public String getAccessKey( ) { return accessKey.getAccessKeyId( ); }
              @Override public String getSecretKey( ) { return accessKey.getSecretAccessKey( ); }
              @Override public Date getCreateDate( ) { return null; }
              @Override public UserPrincipal getPrincipal( )  { return userPrincipalSupplier.get( ); }
            };
      }
    };
  }

  private static NonNullFunction<com.eucalyptus.auth.euare.common.identity.msgs.Certificate,Certificate> certificateTransform(
      final Supplier<UserPrincipal> userPrincipalSupplier
  ) {
    return new NonNullFunction<com.eucalyptus.auth.euare.common.identity.msgs.Certificate,Certificate>( ) {
      @Nonnull
      @Override
      public Certificate apply( final com.eucalyptus.auth.euare.common.identity.msgs.Certificate certificate ) {
        return new Certificate( ) {
          @Override public String getCertificateId( ) { return certificate.getCertificateBody( ); }
          @Override public Boolean isActive( ) { return true; }
          @Override public String getPem( ) { return certificate.getCertificateBody( ); }
          @Override public X509Certificate getX509Certificate( ) { return null; }
          @Override public Date getCreateDate( ) { return null; }
          @Override public UserPrincipal getPrincipal( ) { return userPrincipalSupplier.get( ); }
        };
      }
    };
  }

  @TypeMapper
  public enum AccountToAccountIdentifiersTransform implements Function<Account,AccountIdentifiers> {
    INSTANCE;

    @Nullable
    @Override
    public AccountIdentifiers apply( final Account account ) {
      return new AccountIdentifiersImpl(
        account.getAccountNumber( ),
        account.getAlias( ),
        account.getCanonicalId( )
      );
    }
  }

  @TypeMapper
  public enum RoleToRoleTransform implements Function<com.eucalyptus.auth.euare.common.identity.msgs.Role,Role> {
    INSTANCE;

    @Nullable
    @Override
    public Role apply( final com.eucalyptus.auth.euare.common.identity.msgs.Role role ) {
      final EuareResourceName roleErn = (EuareResourceName) Ern.parse( role.getRoleArn( ) );
      final PolicyVersion rolePolicy = TypeMappers.transform( role.getAssumeRolePolicy( ), PolicyVersion.class );
      return new Role( ) {
        @Override public String getAccountNumber( ) { return roleErn.getAccount( ); }
        @Override public String getRoleId( ) { return role.getRoleId( ); }
        @Override public String getRoleArn( ) { return role.getRoleArn( ); }
        @Override public String getPath( ) { return roleErn.getPath( ); }
        @Override public String getName( ) { return roleErn.getName(); }
        @Override public String getSecret( ) { return role.getSecret(); }
        @Override public PolicyVersion getPolicy( ) { return rolePolicy; }
        @Override public String getDisplayName( ) { return Accounts.getRoleFullName( this ); }
        @Override public OwnerFullName getOwner( ) { return AccountFullName.getInstance( getAccountNumber() ); }
      };
    }
  }

  @TypeMapper
  public enum OidcProviderToOpenIdConnectProviderTransform implements Function<OidcProvider,OpenIdConnectProvider> {
    INSTANCE;

    @Nullable
    @Override
    public OpenIdConnectProvider apply( final OidcProvider provider ) {
      final EuareResourceName providerErn = (EuareResourceName) Ern.parse( provider.getProviderArn( ) );
      final String providerUrl = Strings.trimPrefix( "/", providerErn.getResourceName( ) );
      final OIDCIssuerIdentifier issuerIdentifier =
          OIDCUtils.issuerIdentifierFromProviderUrl( providerUrl, provider.getPort( ) );
      final String providerHost = issuerIdentifier.getHost( );
      final String providerPath = issuerIdentifier.getPath( );
      final List<String> providerClientIds = ImmutableList.copyOf( provider.getClientIds( ) );
      final List<String> providerThumbprints = ImmutableList.copyOf( provider.getThumbprints( ) );
      return new OpenIdConnectProvider( ) {
        @Override public String getAccountNumber( ) { return providerErn.getAccount( ); }
        @Override public String getArn( ) { return provider.getProviderArn( ); }
        @Override public String getUrl( ) { return providerUrl; }
        @Override public String getHost( ) { return providerHost; }
        @Override public Integer getPort( ) { return provider.getPort( ); }
        @Override public String getPath( ) { return providerPath; }
        @Override public List<String> getClientIds( ) { return providerClientIds; }
        @Override public List<String> getThumbprints( ) { return providerThumbprints; }
      };
    }
  }

  @TypeMapper
  public enum SecurityTokenToSecurityTokenContentTransform implements Function<SecurityToken,SecurityTokenContent> {
    INSTANCE;

    @Nullable
    @Override
    public SecurityTokenContent apply( final SecurityToken securityToken ) {
      return new SecurityTokenContentImpl(
          Optional.fromNullable( securityToken.getOriginatingAccessKeyId( ) ),
          Optional.fromNullable( securityToken.getOriginatingUserId( ) ),
          Optional.fromNullable( securityToken.getOriginatingRoleId( ) ),
          securityToken.getNonce( ),
          securityToken.getCreated( ),
          securityToken.getExpires( ),
          securityToken.getAttributes( ) == null ?
              ImmutableMap.of( ) :
              securityToken.getAttributes( )
                  .stream( )
                  .collect( Collectors.toMap( SecurityTokenAttribute::getKey, SecurityTokenAttribute::getValue ) )
      );
    }
  }

  @TypeMapper
  public enum PolicyToPolicyVersionTransform implements Function<Policy,PolicyVersion> {
    INSTANCE;

    @Nullable
    @Override
    public PolicyVersion apply( final Policy policy ) {
      return new PolicyVersion( ) {
        @Override public String getPolicyVersionId( ) { return policy.getVersionId( ); }
        @Override public String getPolicyName( ) { return policy.getName( ); }
        @Override public PolicyScope getPolicyScope( ) { return PolicyScope.valueOf( policy.getScope( ) ); }
        @Override public String getPolicy( ) { return policy.getPolicy( ); }
        @Override public String getPolicyHash( ) { return policy.getHash( ); }
      };
    }
  }
}
