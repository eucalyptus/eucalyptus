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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PrincipalProvider;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.AccountIdentifiersImpl;
import com.eucalyptus.auth.principal.BaseInstanceProfile;
import com.eucalyptus.auth.principal.BaseOpenIdConnectProvider;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.HasRole;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * <h2>Eucalyptus/AWS IDs & Access Keys:</h2>
 * <p>
 * <strong>NOTE:IMPORTANT: It SHOULD NOT be the @Id of the underlying entity as this value is not
 * guaranteed to be fixed in the future (e.g., disrupted by upgrade, version changes,
 * etc.).</strong>
 * </p>
 * <ol>
 * <li>- AWS Account Number: Public ID for an ACCOUNT.</li>
 * <ul>
 * <li>- "globally" unique 12-digit number associated with the Eucalyptus account.</li>
 * <li>- is a shared value; other users may need it or discover it during normal operation of the
 * system</li>
 * <li>- _MUST_ be a 12-digit number. User commands require this value as input in certain cases and
 * enforce the length of the ID.</li>
 * </ul>
 * </li>
 * <li>AWS Access Key: Identifier value corresponding to the AWS Secret Access Key used to sign
 * requests.</li>
 * <ul>
 * <li>- "globally" unique 20 alpha-numeric characters
 * <li>
 * <li>- is a shared value; other users may need it or discover it during normal operation of the
 * system
 * <li>
 * <li>- _MUST_ be 20-alphanum characters; per the specification (e.g.,
 * s3.amazonaws.com/awsdocs/ImportExport/latest/AWSImportExport-dg.pdf). User commands require this
 * value as input in certain cases and enforce the length of the ID.
 * <li>
 * </ul>
 * </ol>
 */
public class Accounts {
  private static final Logger LOG = Logger.getLogger( Accounts.class );

  private static Supplier<PrincipalProvider> identities = serviceLoaderSupplier( PrincipalProvider.class );

  protected static <T> Supplier<T> serviceLoaderSupplier( final Class<T> serviceClass ) {
    return Suppliers.memoize( new Supplier<T>() {
      @Override
      public T get( ) {
        return ServiceLoader.load( serviceClass ).iterator( ).next( );
      }
    } );
  }

  public static void setIdentityProvider( PrincipalProvider provider ) {
    synchronized ( Accounts.class ) {
      LOG.info( "Setting the identity provider to: " + provider.getClass( ) );
      identities = Suppliers.ofInstance( provider );
    }
  }

  protected static PrincipalProvider getIdentityProvider( ) {
    return identities.get();
  }

  /**
   * Get the euare service certificate for a region, locate by account number.
   *
   * @param accountNumber The account number used to identify the region
   * @return The euare certificate for the accounts region
   * @throws AuthException On error
   */
  public static X509Certificate getEuareCertificate( final String accountNumber ) throws AuthException {
    return getIdentityProvider( ).getCertificateByAccountNumber( accountNumber );
  }

  /**
   * Create a certificate signed by the euare certificate for a region, locate by account number.
   *
   * @param accountNumber The account number used to identify the region
   * @param publicKey The public key for the certificate
   * @param principal The principal for the certificate subject
   * @param expiryInDays The certificate expiry
   * @return The ne certificate
   * @throws AuthException On error
   */
  public static X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey,
      final String principal,
      final int expiryInDays
  ) throws AuthException {
    return getIdentityProvider().signCertificate( accountNumber, publicKey, principal, expiryInDays );
  }

  public static String lookupAccountIdByAlias( String alias ) throws AuthException {
    if ( isAccountNumber( alias ) ) {
      return alias;
    } else {
      return getIdentityProvider( ).lookupAccountIdentifiersByAlias( alias ).getAccountNumber( );
    }
  }

  public static String lookupAccountIdByCanonicalId( String canonicalId ) throws AuthException {
    throwIfFakeIdentity( canonicalId );
    return getIdentityProvider( ).lookupAccountIdentifiersByCanonicalId( canonicalId ).getAccountNumber();
  }

  public static String lookupCanonicalIdByAccountId( String accountId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId ).getCanonicalId();
  }

  public static String lookupCanonicalIdByEmail( String email ) throws AuthException {
    return getIdentityProvider( ).lookupAccountIdentifiersByEmail( email ).getCanonicalId();
  }

  public static String lookupAccountAliasById( String accountId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId ).getAccountAlias();
  }

  public static AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    return Accounts.getIdentityProvider( ).lookupAccountIdentifiersByAlias( alias );
  }

  public static AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    throwIfFakeIdentity( canonicalId );
    return Accounts.getIdentityProvider( ).lookupAccountIdentifiersByCanonicalId( canonicalId );
  }

  public static AccountIdentifiers lookupAccountIdentifiersById( final String accountId ) throws AuthException {
    final UserPrincipal user = Accounts.getIdentityProvider( ).lookupPrincipalByAccountNumber( accountId );
    return new AccountIdentifiersImpl(
        user.getAccountNumber( ),
        user.getAccountAlias(),
        user.getCanonicalId( )
    );
  }

  public static boolean isAdministrativeAccount( String accountName ) {
    return
        AccountIdentifiers.SYSTEM_ACCOUNT.equals( accountName );
  }

  public static boolean isSystemAccount( String accountName ) {
    return
        AccountIdentifiers.SYSTEM_ACCOUNT.equals( accountName ) ||
        Objects.toString( accountName, "" ).startsWith( AccountIdentifiers.SYSTEM_ACCOUNT_PREFIX );
  }

  @Nonnull
  public static List<String> listAccountNumbersForName( final String accountAliasExpression ) throws AuthException {
    return Lists.newArrayList( Iterables.transform(
        listAccountIdentifiersForName( accountAliasExpression ),
        AccountIdentifiers.Properties.accountNumber() ) );
  }

  @Nonnull
  public static List<AccountIdentifiers> listAccountIdentifiersForName( final String accountAliasExpression ) throws AuthException {
    return getIdentityProvider( ).listAccountIdentifiersByAliasMatch( accountAliasExpression );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByAccountNumber( String accountNumber ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumber( accountNumber );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByAccountNumberAndUsername( String accountNumber, String username ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccountNumberAndUsername( accountNumber, username );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByCanonicalId( String canonicalId ) throws AuthException {
    throwIfFakeIdentity( canonicalId );
    return getIdentityProvider( ).lookupPrincipalByCanonicalId( canonicalId );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByAccessKeyId( String accessKeyId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByAccessKeyId( accessKeyId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByUserId( String userId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByUserId( userId, null );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByUserId( String userId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByUserId( userId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByRoleId( String roleId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByRoleId( roleId, null );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByRoleId( String roleId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByRoleId( roleId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupPrincipalByCertificateId( String certificateId ) throws AuthException {
    return getIdentityProvider( ).lookupPrincipalByCertificateId( certificateId );
  }

  @Nonnull
  public static UserPrincipal lookupCachedPrincipalByAccountNumber( String accountNumber ) throws AuthException {
    return getIdentityProvider( ).lookupCachedPrincipalByAccountNumber( null, accountNumber );
  }

  @Nonnull
  public static UserPrincipal lookupCachedPrincipalByAccessKeyId( String accessKeyId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupCachedPrincipalByAccessKeyId( null, accessKeyId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupCachedPrincipalByUserId( String userId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupCachedPrincipalByUserId( null, userId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupCachedPrincipalByRoleId( String roleId, String nonce ) throws AuthException {
    return getIdentityProvider( ).lookupCachedPrincipalByRoleId( null, roleId, nonce );
  }

  @Nonnull
  public static UserPrincipal lookupCachedPrincipalByCertificateId( String certificateId ) throws AuthException {
    return getIdentityProvider( ).lookupCachedPrincipalByCertificateId( null, certificateId );
  }

  @Nonnull
  public static InstanceProfile lookupInstanceProfileByName( String accountNumber, String name ) throws AuthException {
    return getIdentityProvider( ).lookupInstanceProfileByName( accountNumber, name );
  }

  @Nonnull
  public static Role lookupRoleByName( String accountNumber, String name ) throws AuthException {
    return getIdentityProvider( ).lookupRoleByName( accountNumber, name );
  }

  @Nonnull
  public static OpenIdConnectProvider lookupOidcProviderByUrl( String accountNumber, String url ) throws AuthException {
    return getIdentityProvider( ).lookupOidcProviderByUrl( accountNumber, url );
  }

  @Nonnull
  public static SecurityTokenContent decodeSecurityToken( String accessKeyIdentifier, String securityToken ) throws AuthException {
    return getIdentityProvider().decodeSecurityToken( accessKeyIdentifier, securityToken );
  }

  /**
   * Lookup a system account by alias
   *
   * @param alias The alias for the account
   * @return The principal representing the accounts admin user
   * @throws AuthException If the alias does not represent a system account or on other error
   */
  public static UserPrincipal lookupSystemAccountByAlias( final String alias ) throws AuthException {
    if ( !isSystemAccount( alias ) ) {
      throw new AuthException( "Not a system account: " + alias );
    }
    final String accountNumber = lookupAccountIdentifiersByAlias( alias ).getAccountNumber( );
    return lookupPrincipalByAccountNumber( accountNumber );
  }

  /**
   * Lookup the admin use for the eucalyptus account.
   *
   * @return The principal representing the eucalyptus admin user
   * @throws AuthException
   */
  public static UserPrincipal lookupSystemAdmin( ) throws AuthException {
    return lookupSystemAccountByAlias( AccountIdentifiers.SYSTEM_ACCOUNT );
  }

  public static String getNameFromFullName( final String fullName ) {
    String name = fullName;
    if ( name != null ) {
      int pathEndIndex = name.lastIndexOf( '/' );
      if ( pathEndIndex > -1 && pathEndIndex < name.length( )) {
        name = name.substring( pathEndIndex + 1, name.length( ) );
      }
    }
    return name;
  }

  public static String getAccountFullName( AccountIdentifiers account ) {
    return "/" + account.getAccountAlias();
  }

  public static String getUserFullName( User user ) {
    if ( user.getPath( ).endsWith( "/" ) ) {
      return user.getPath( ) + user.getName( );
    } else {
      return user.getPath( ) + "/" + user.getName( );
    }
  }

  public static String getRoleFullName( BaseRole role ) {
    if ( role.getPath( ).endsWith( "/" ) ) {
      return role.getPath( ) + role.getName( );
    } else {
      return role.getPath( ) + "/" + role.getName( );
    }
  }

  public static String getInstanceProfileFullName( BaseInstanceProfile instanceProfile ) {
    if ( instanceProfile.getPath( ).endsWith( "/" ) ) {
      return instanceProfile.getPath( ) + instanceProfile.getName( );
    } else {
      return instanceProfile.getPath( ) + "/" + instanceProfile.getName( );
    }
  }

  public static String getAuthenticatedFullName( final UserPrincipal user ) {
    if ( isRoleIdentifier( user.getAuthenticatedId( ) ) ) {
      return getRoleFullName( ((HasRole)user).getRole( ) );
    } else {
      return getUserFullName( user );
    }
  }

  @Nonnull
  public static String getAccountArn( @Nonnull final String accountId ) throws AuthException {
    if ( !accountId.matches( "[0-9]{12}" ) ) {
      throw new AuthException( "Invalid account identifier '" + accountId +"'" );
    }
    return "arn:aws:iam::"+accountId+":root";
  }

  public static String getUserArn( final User user ) throws AuthException {
    return buildArn( user.getAccountNumber( ), PolicySpec.IAM_RESOURCE_USER, user.getPath(), user.getName() );
  }

  public static String getUserArn( final UserPrincipal user ) {
    return buildArn( user.getAccountNumber( ), PolicySpec.IAM_RESOURCE_USER, user.getPath(), user.getName() );
  }

  public static String getRoleArn( final BaseRole role ) throws AuthException {
    return buildArn( role.getAccountNumber( ), PolicySpec.IAM_RESOURCE_ROLE, role.getPath(), role.getName() );
  }

  public static String getRoleArn( final Role role ) {
    return buildArn( role.getAccountNumber( ), PolicySpec.IAM_RESOURCE_ROLE, role.getPath(), role.getName() );
  }

  public static String getAssumedRoleArn( final BaseRole role,
                                          final String roleSessionName ) throws AuthException {
    return "arn:aws:sts::"+role.getAccountNumber()+":assumed-role"+Accounts.getRoleFullName( role )+"/"+roleSessionName;
  }

  public static String getAuthenticatedArn( final UserPrincipal user ) {
    if ( isRoleIdentifier( user.getAuthenticatedId( ) ) ) {
      return getRoleArn( ((HasRole)user).getRole( ) );
    } else {
      return getUserArn( user );
    }
  }

  public static String getInstanceProfileArn( final BaseInstanceProfile instanceProfile ) throws AuthException {
    return buildArn( instanceProfile.getAccountNumber( ), PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE, instanceProfile.getPath( ), instanceProfile.getName( ) );
  }

  public static String getOpenIdConnectProviderArn( final BaseOpenIdConnectProvider provider ) throws AuthException {
    return buildArn(
        provider.getAccountNumber( ),
        PolicySpec.IAM_RESOURCE_OPENID_CONNECT_PROVIDER,
        "/",
        provider.getUrl( ) );
  }

  protected static String buildArn( final String accountNumber,
                                    final String type,
                                    final String path,
                                    final String name ) {
    return new EuareResourceName( accountNumber, type, path, name ).toString( );
  }

  /**
   * Check the prefix of the given identifier to check for a role.
   *
   * This method does not check the full identifier, so can be used to check
   * for assumed role identities where the role identifier is combined with a
   * session name suffix.
   *
   * @param identifier The identifier to check
   * @return True if the identifier is for a role
   */
  public static boolean isRoleIdentifier( @Nonnull  final String identifier ) {
    return identifier.startsWith( "ARO" );
  }

  public static boolean isAccountNumber( final String identifier ) {
    return identifier.matches( "[0-9]{12}" );
  }

  public static Function<User,String> toUserId() {
    return UserStringProperties.USER_ID;
  }

  /**
   * Get the base identifier, removing any text after ':'.
   */
  public static String getIdentifier( @Nullable final String identifier ) {
    String cleanedId = identifier;
    int suffixIndex;
    if ( identifier != null && ( suffixIndex = identifier.indexOf( ':' ) ) > 0 ) {
      cleanedId = identifier.substring( 0, suffixIndex );
    }
    return cleanedId;
  }

  /**
   * Get the value after the base identifier, the text after ':'.
   */
  @Nullable
  public static String getIdentifierSuffix( @Nullable final String identifier ) {
    String idSuffix = null;
    int suffixIndex;
    if ( identifier != null && ( suffixIndex = identifier.indexOf( ':' ) ) > 0 ) {
      idSuffix = identifier.substring( suffixIndex + 1 );
    }
    return idSuffix;
  }

  private static void throwIfFakeIdentity( final String id ) throws AuthException {
    if ( Principals.isFakeIdentify( id ) ) {
      throw new AuthException( "Invalid identity: " + id );
    }
  }
  
  private enum UserStringProperties implements Function<User,String> {
    ACCOUNT_NUMBER {
      @Override
      public String apply( final User user ) {
        try {
          return user.getAccountNumber( );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    },
    USER_ID {
      @Override
      public String apply( final User user ) {
        return user.getUserId( );
      }
    }
  }

}
