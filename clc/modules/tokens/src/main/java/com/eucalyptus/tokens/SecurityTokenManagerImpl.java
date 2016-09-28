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

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.euare.UserPrincipalImpl;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.SecurityTokenContentImpl;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.auth.tokens.SecurityTokenValidationException;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * Security token manager for temporary credentials.
 */
@SuppressWarnings( { "Guava", "StaticPseudoFunctionalStyleMethod" } )
public class SecurityTokenManagerImpl implements SecurityTokenManager.SecurityTokenProvider {

  private static final Logger log = Logger.getLogger( SecurityTokenManagerImpl.class );
  private static final Supplier<SecureRandom> randomSupplier = Crypto.getSecureRandomSupplier();
  private static final Supplier<String> securityTokenPasswordSupplier = Suppliers.memoize(
      SystemIds::securityTokenPassword );
  private static final long creationSkewMillis = MoreObjects.firstNonNull(
      Longs.tryParse( System.getProperty( "com.eucalyptus.auth.tokens.creationSkewMillis", "5000" ) ),
      5000L );
  private static final int tokenCacheSize = MoreObjects.firstNonNull(
      Ints.tryParse( System.getProperty( "com.eucalyptus.auth.tokens.cache.maximumSize", "500" ) ),
      500 );
  private static final Cache<Pair<String,String>,SecurityTokenContent> tokenCache =
      CacheBuilder.newBuilder( ).expireAfterAccess( 5, TimeUnit.MINUTES ).maximumSize( tokenCacheSize ).build( );


  /**
   *
   */
  @Nonnull
  public SecurityToken doIssueSecurityToken( @Nonnull  final User user,
                                             @Nullable final AccessKey accessKey,
                                                       final int durationTruncationSeconds,
                                                       final int durationSeconds ) throws AuthException {
    Preconditions.checkNotNull( user, "User is required" );

    final AccessKey key = accessKey != null ?
        accessKey :
        Iterables.find(
            MoreObjects.firstNonNull( user.getKeys(), Collections.emptyList() ),
            AccessKeys.isActive(),
            null );

    if ( key==null )
      throw new AuthException("Key not found for user");

    final long restrictedDurationMillis =
        restrictDuration( 36, durationTruncationSeconds, durationSeconds );

    if ( !key.getPrincipal().getUserId().equals( user.getUserId() ) ) {
      throw new AuthException("Key not valid for user");
    }

    final EncryptedSecurityToken encryptedToken = new EncryptedSecurityToken(
        key.getAccessKey(),
        user.getUserId(),
        getCurrentTimeMillis(),
        restrictedDurationMillis );
    return  new SecurityToken(
        encryptedToken.getAccessKeyId(),
        encryptedToken.getSecretKey( key.getSecretKey() ),
        encryptedToken.encrypt( getEncryptionKey( encryptedToken.getAccessKeyId() ) ),
        encryptedToken.getExpires()
        );
  }

  /**
   *
   */
  @Nonnull
  public SecurityToken doIssueSecurityToken( @Nonnull final User user,
                                                      final int durationTruncationSeconds,
                                                      final int durationSeconds ) throws AuthException {
    Preconditions.checkNotNull( user, "User is required" );

    final String userToken = user.getToken();
    if ( userToken == null || userToken.length() < 30 ) {
      throw new AuthException("Cannot generate token for user");
    }

    final long restrictedDurationMillis =
        restrictDuration( 36, durationTruncationSeconds, durationSeconds );

    final EncryptedSecurityToken encryptedToken = new EncryptedSecurityToken(
        null,
        user.getUserId(),
        getCurrentTimeMillis(),
        restrictedDurationMillis );
    return  new SecurityToken(
        encryptedToken.getAccessKeyId(),
        encryptedToken.getSecretKey( userToken ),
        encryptedToken.encrypt( getEncryptionKey( encryptedToken.getAccessKeyId() ) ),
        encryptedToken.getExpires()
    );
  }

  @Nonnull
  public SecurityToken doIssueSecurityToken( @Nonnull final BaseRole role,
                                             @Nonnull final RoleSecurityTokenAttributes attributes,
                                                      final int durationSeconds ) throws AuthException {
    Preconditions.checkNotNull( role, "Role is required" );

    final long restrictedDurationMillis =
        restrictDuration( 1, 0, durationSeconds );

    if ( role.getSecret()==null || role.getSecret().length() < 30 ) {
      throw new AuthException("Cannot generate token for role");
    }

    final EncryptedSecurityToken encryptedToken = new EncryptedSecurityToken(
        role,
        getCurrentTimeMillis(),
        restrictedDurationMillis,
        attributes.asMap( ) );
    return  new SecurityToken(
        encryptedToken.getAccessKeyId(),
        encryptedToken.getSecretKey( role.getSecret() ),
        encryptedToken.encrypt( getEncryptionKey( encryptedToken.getAccessKeyId() ) ),
        encryptedToken.getExpires()
    );
  }

  @Nonnull
  public TemporaryAccessKey doLookupAccessKey( @Nonnull final String accessKeyId,
                                               @Nonnull final String token ) throws AuthException {
    Preconditions.checkNotNull( accessKeyId, "Access key identifier is required" );
    Preconditions.checkNotNull( token, "Token is required" );

    final SecurityTokenContent securityTokenContent;
    try {
      final Pair<String,String> tokenKey = Pair.pair( accessKeyId, token );
      securityTokenContent = tokenCache.get( tokenKey, () -> doDispatchingDecode( accessKeyId, token ) );
    } catch ( ExecutionException e ) {
      log.debug( e, e );
      throw new InvalidAccessKeyAuthException("Invalid security token");
    }

    final String originatingAccessKeyId = securityTokenContent.getOriginatingAccessKeyId( ).orNull( );
    final String userId = securityTokenContent.getOriginatingUserId().orNull( );
    final UserPrincipal user;
    final TemporaryKeyType type;
    if ( originatingAccessKeyId != null ) {
      user = lookupByAccessKeyId( originatingAccessKeyId, securityTokenContent.getNonce() );
      type = TemporaryKeyType.Session;
    } else if ( userId != null ) {
      user = lookupByUserById( userId, securityTokenContent.getNonce() );
      type = TemporaryKeyType.Access;
    } else  {
      user = lookupByRoleById( securityTokenContent.getOriginatingRoleId( ).get( ), securityTokenContent.getNonce() );
      type = TemporaryKeyType.Role;
    }

    return new TemporaryAccessKey( ) {
      private static final long serialVersionUID = 1L;
      private UserPrincipal principal = new UserPrincipalImpl( user, Collections.<AccessKey>singleton( this ) );

      @Override public Boolean isActive() {
        return user.isEnabled() && EncryptedSecurityToken.isValid( securityTokenContent );
      }

      @Override public String getAccessKey() {
        return accessKeyId;
      }

      @Override public String getSecurityToken() {
        return token;
      }

      @Override public String getSecretKey() {
        return Iterables.getOnlyElement( user.getKeys( ) ).getSecretKey( );
      }

      @Override public TemporaryKeyType getType() {
        return type;
      }

      @Override public Map<String, String> getAttributes() {
        return securityTokenContent.getAttributes( );
      }

      @Override public Date getCreateDate() {
        return new Date(securityTokenContent.getCreated());
      }

      @Override public Date getExpiryDate() {
        return new Date(securityTokenContent.getExpires());
      }

      @Override public UserPrincipal getPrincipal() throws AuthException {
        return principal;
      }
    };
  }

  @Nonnull
  public String doGenerateSecret( @Nonnull final String nonce,
                                  @Nonnull final String secret ) {
    return EncryptedSecurityToken.getSecretKey( nonce, secret );
  }

  protected SecurityTokenContent doDispatchingDecode(
      final String accessKeyId,
      final String token
  ) throws AuthException {
    return Accounts.decodeSecurityToken( accessKeyId, token );
  }

  @Nonnull
  public SecurityTokenContent doDecode(
      final String accessKeyId,
      final String token
  ) throws AuthException {
    final EncryptedSecurityToken encryptedSecurityToken;
    try {
      encryptedSecurityToken = EncryptedSecurityToken.decrypt( accessKeyId, getEncryptionKey( accessKeyId ), token );
    } catch ( GeneralSecurityException e ) {
      throw new AuthException( "Unable to decode token", e );
    }
    return new SecurityTokenContentImpl(
        Optional.fromNullable( encryptedSecurityToken.getOriginatingAccessKeyId() ),
        Optional.fromNullable( encryptedSecurityToken.getUserId() ),
        Optional.fromNullable( encryptedSecurityToken.getRoleId() ),
        encryptedSecurityToken.getNonce( ),
        encryptedSecurityToken.getCreated( ),
        encryptedSecurityToken.getExpires( ),
        encryptedSecurityToken.getAttributes( )
    );
  }

  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected UserPrincipal lookupByUserById( final String userId, final String nonce ) throws AuthException {
    return Accounts.lookupCachedPrincipalByUserId( userId, nonce );
  }

  protected UserPrincipal lookupByRoleById( final String roleId, final String nonce ) throws AuthException {
    return Accounts.lookupCachedPrincipalByRoleId( roleId, nonce );
  }

  protected UserPrincipal lookupByAccessKeyId( final String accessKeyId, final String nonce ) throws AuthException {
    return Accounts.lookupCachedPrincipalByAccessKeyId( accessKeyId, nonce );
  }

  protected String getSecurityTokenPassword() {
    return securityTokenPasswordSupplier.get( );
  }

  private long restrictDuration( final int maximumDurationHours,
                                 final int durationTruncationSeconds,
                                 final int durationSeconds ) throws SecurityTokenValidationException {
    long durationMillis = durationSeconds == 0 ?
        TimeUnit.HOURS.toMillis( 12 ) : // use default
        TimeUnit.SECONDS.toMillis( durationSeconds );

    if ( durationMillis > TimeUnit.HOURS.toMillis( maximumDurationHours ) ) {
      validationFailure( String.format(
          "Invalid duration requested, maximum permitted duration is %s seconds.",
          TimeUnit.HOURS.toSeconds( maximumDurationHours ) ) );
    }

    if ( durationMillis < TimeUnit.MINUTES.toMillis( 15 ) ) {
      validationFailure( "Invalid duration requested, minimum permitted duration is 900 seconds." );
    }

    if ( durationTruncationSeconds > 0 && durationMillis > TimeUnit.SECONDS.toMillis( durationTruncationSeconds ) ) {
      durationMillis = TimeUnit.SECONDS.toMillis( durationTruncationSeconds );
    }

    return durationMillis;
  }

  private void validationFailure( final String message ) throws SecurityTokenValidationException {
    throw new SecurityTokenValidationException( message );
  }

  private SecretKey getEncryptionKey( final String salt ) {
    final MessageDigest digest = Digest.SHA256.get();
    digest.update( salt.getBytes( Charsets.UTF_8 ) );
    digest.update( getSecurityTokenPassword().getBytes( Charsets.UTF_8 ) );
    return new SecretKeySpec( digest.digest(), "AES" );
  }

  /**
   * Immutable token representation
   *
   * Format v3 adds a map for arbitrary attributes.
   */
  private static final class EncryptedSecurityToken {
    private static final byte[] TOKEN_PREFIX = new byte[]{ 'e', 'u', 'c', 'a', 0, 1 };

    private final String accessKeyId;
    private final String originatingId;
    private final String nonce;
    private final long created;
    private final long expires;
    private final ImmutableMap<String,String> attributes;

    /**
     * Generate a new token
     */
    private EncryptedSecurityToken( final String originatingAccessKeyId,
                                    final String userId,
                                    final long created,
                                    final long durationMillis ) {
      this( originatingAccessKeyId != null ?
             "$a$" + originatingAccessKeyId :
             "$u$" + userId,
          created,
          durationMillis,
          null );
    }

    /**
     * Generate a new token
     */
    private EncryptedSecurityToken( final BaseRole role,
                                    final long created,
                                    final long durationMillis,
                                    final Map<String,String> attributes ) {
      this( "$r$" + role.getRoleId( ), created, durationMillis, attributes );
    }

      /**
      * Generate a new token
      */
    private EncryptedSecurityToken( final String originatingId,
                                    final long created,
                                    final long durationMillis,
                                    final Map<String,String> attributes ) {
      this.accessKeyId = Identifiers.generateAccessKeyIdentifier( );
      this.originatingId = originatingId;
      this.nonce = Crypto.generateSessionToken();
      this.created = created;
      this.expires = created + durationMillis;
      this.attributes = attributes == null ? ImmutableMap.of( ) : ImmutableMap.copyOf( attributes );
    }

    /**
     * Reconstruct token
     */
    private EncryptedSecurityToken( final String accessKeyId,
                                    final String originatingId,
                                    final String nonce,
                                    final long created,
                                    final long expires,
                                    final Map<String,String> attributes ) {
      this.accessKeyId = accessKeyId;
      this.originatingId = originatingId;
      this.nonce = nonce;
      this.created = created;
      this.expires = expires;
      this.attributes = ImmutableMap.copyOf( attributes );
    }

    private String getAccessKeyId() {
      return accessKeyId;
    }

    public String getOriginatingAccessKeyId() {
      return getTrimmedIfPrefixed( "$a$", originatingId );
    }

    public String getNonce() {
      return nonce;
    }

    public String getUserId() {
      return getTrimmedIfPrefixed( "$u$", originatingId );
    }

    public String getRoleId() {
      return getTrimmedIfPrefixed( "$r$", originatingId );
    }

    private String getTrimmedIfPrefixed( final String prefix,
                                         final String value ) {
      return value.startsWith( prefix ) ?
          value.substring( prefix.length() ) :
          null;
    }

    public long getCreated() {
      return created;
    }

    private long getExpires() {
      return expires;
    }

    public Map<String, String> getAttributes( ) {
      return attributes;
    }

    /**
     * Is the token within its validity period.
     */
    private static boolean isValid( final SecurityTokenContent token ) {
      final long now = System.currentTimeMillis();
      return ( now + creationSkewMillis ) >= token.getCreated( ) && now < token.getExpires( );
    }

    private String getSecretKey( final String secret ) {
      return getSecretKey( nonce, secret );
    }

    static String getSecretKey( final String nonce, final String secret ) {
      final MessageDigest digest = Digest.SHA256.get();
      digest.update( secret.getBytes( Charsets.UTF_8 ) );

      final StringBuilder keyBuilder = new StringBuilder( 128 );
      while( keyBuilder.length() < 40 ) {
        if ( keyBuilder.length() > 0 ) digest.update(keyBuilder.toString().getBytes(Charsets.UTF_8));
        digest.update( nonce.getBytes(Charsets.UTF_8) );
        keyBuilder.append( B64.standard.encString( digest.digest() ).replaceAll("\\p{Punct}", "") );
      }
      return keyBuilder.substring( 0, 40 );
    }

    private byte[] toBytes() {
      try {
        final SecurityTokenOutput out = new SecurityTokenOutput();
        out.writeInt(3); // format identifier
        out.writeString(originatingId);
        out.writeString(nonce);
        out.writeLong(created);
        out.writeLong(expires);
        out.writeInt(attributes.size());
        for ( final Map.Entry<String,String> entry : attributes.entrySet( ) ) {
          out.writeString( entry.getKey( ) );
          out.writeString( entry.getValue( ) );
        }
        return out.toByteArray( );
      } catch (IOException e) {
        throw Exceptions.toUndeclared( e );
      }
    }

    private String encrypt( final SecretKey key ) {
      try {
        final Cipher cipher = Ciphers.AES_GCM.get();
        final byte[] iv = new byte[32];
        randomSupplier.get().nextBytes(iv);
        cipher.init( Cipher.ENCRYPT_MODE, key, new IvParameterSpec( iv ), randomSupplier.get( ) );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write( TOKEN_PREFIX );
        out.write( iv );
        out.write( cipher.doFinal(toBytes()) );
        return B64.standard.encString( out.toByteArray() );
      } catch ( GeneralSecurityException | IOException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }

    private static EncryptedSecurityToken decrypt( final String accessKeyId,
                                                   final SecretKey key,
                                                   final String securityToken ) throws GeneralSecurityException {
      try {
        final Cipher cipher = Ciphers.AES_GCM.get();
        final byte[] securityTokenBytes = B64.standard.dec(securityToken);
        if ( securityTokenBytes.length < 64 + TOKEN_PREFIX.length ||
            !Arrays.equals( TOKEN_PREFIX, Arrays.copyOf( securityTokenBytes, TOKEN_PREFIX.length ) ) ) {
          throw new GeneralSecurityException("Invalid token format");
        }

        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            new IvParameterSpec( securityTokenBytes, TOKEN_PREFIX.length, 32 ),
            randomSupplier.get( )
        );
        final int offset = TOKEN_PREFIX.length + 32;
        final SecurityTokenInput in = new SecurityTokenInput(
            cipher.doFinal( securityTokenBytes, offset, securityTokenBytes.length-offset ) );
        final int version = in.readInt();
        if ( version != 2 && version != 3 ) throw new GeneralSecurityException("Invalid token format");
        final String originatingAccessKeyIdOrUserId = in.readString();
        final String nonce = in.readString();
        final long created = in.readLong();
        final long expires = in.readLong();
        final Map<String,String> attributes = Maps.newHashMap( );
        if ( version >= 3 ) {
          final int entries = in.readInt( );
          for ( int i=0; i<entries; i++ ) {
            attributes.put( in.readString( ), in.readString( ) );
          }
        }
        return new EncryptedSecurityToken(
            accessKeyId,
            originatingAccessKeyIdOrUserId,
            nonce,
            created,
            expires,
            attributes );
      } catch (IOException e) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  private static final class SecurityTokenOutput {
    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    private final Deflater deflater = new Deflater( Deflater.BEST_COMPRESSION );
    private final DeflaterOutputStream out = new DeflaterOutputStream( byteStream, deflater );

    private void writeString( final String value ) throws IOException {
      final byte[] data = value.getBytes( Charsets.UTF_8 );
      writeInt( data.length );
      out.write( data );
    }

    private void writeInt( final int value ) throws IOException {
      out.write( Ints.toByteArray( value ) );
    }

    private void writeLong( final long value ) throws IOException {
      out.write( Longs.toByteArray( value ) );
    }

    private byte[] toByteArray() throws IOException {
      out.flush();
      out.close();
      return byteStream.toByteArray();
    }
  }

  private static final class SecurityTokenInput {
    private final InputStream in;

    private SecurityTokenInput( final byte[] data ) {
      in = new InflaterInputStream( new ByteArrayInputStream( data ) );
    }

    private String readString() throws IOException {
      final byte[] data = new byte[ readInt() ];
      if ( in.read( data ) != data.length ) throw new IOException();
      return new String( data, Charsets.UTF_8 );
    }

    private int readInt() throws IOException {
      final byte[] data = new byte[4];
      if ( in.read( data ) != 4 ) throw new IOException();
      return Ints.fromByteArray( data );
    }

    private long readLong() throws IOException {
      final byte[] data = new byte[8];
      if ( in.read( data ) != 8 ) throw new IOException();
      return Longs.fromByteArray( data );
    }
  }
}
