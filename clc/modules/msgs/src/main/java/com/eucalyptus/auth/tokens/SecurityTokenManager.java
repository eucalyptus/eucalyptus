/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.tokens;

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
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * Security token manager for temporary credentials.
 */
public class SecurityTokenManager {

  private static final Logger log = Logger.getLogger( SecurityTokenManager.class );
  private static final Supplier<SecureRandom> randomSupplier = Crypto.getSecureRandomSupplier();
  private static final SecurityTokenManager instance = new SecurityTokenManager();

  /**
   * Issue a security token.
   *
   * @param user The user for the token
   * @param accessKey The originating access key for the token
   * @param accessToken The originating user token for the token
   * @param durationSeconds The desired duration for the token
   * @return The newly issued security token
   * @throws AuthException If an error occurs
   */
  @Nonnull
  public static SecurityToken issueSecurityToken( @Nonnull  final User user,
                                                  @Nullable final AccessKey accessKey,
                                                  @Nullable final String accessToken,
                                                            final int durationSeconds ) throws AuthException {
    return instance.doIssueSecurityToken( user, accessKey, accessToken, durationSeconds );
  }

  /**
   * Issue a security token.
   *
   * @param role The role to to assume
   * @param durationSeconds The desired duration for the token
   * @return The newly issued security token
   * @throws AuthException If an error occurs
   */
  @Nonnull
  public static SecurityToken issueSecurityToken( @Nonnull final Role role,
                                                  final int durationSeconds ) throws AuthException {
    return instance.doIssueSecurityToken( role, durationSeconds );
  }

  /**
   * Lookup the access key for a token.
   *
   * @param accessKeyId The identifier for the ephemeral access key
   * @param token The security token for the ephemeral access key
   * @return The access key
   * @throws AuthException If an error occurs
   */
  @Nonnull
  public static AccessKey lookupAccessKey( @Nonnull final String accessKeyId,
                                           @Nonnull final String token ) throws AuthException {
    return instance.doLookupAccessKey( accessKeyId, token );
  }

  /**
   * The accessToken parameter appears redundant but indicates authorization to use the users token.
   */
  @Nonnull
  protected SecurityToken doIssueSecurityToken( @Nonnull  final User user,
                                                @Nullable final AccessKey accessKey,
                                                @Nullable final String accessToken,
                                                          final int durationSeconds ) throws AuthException {
    Preconditions.checkNotNull( user, "User is required" );

    final AccessKey key = accessKey != null || accessToken != null ?
        accessKey :
        Iterables.find(
            Objects.firstNonNull( user.getKeys(), Collections.<AccessKey>emptyList() ),
            AccessKeys.isActive(),
            null );

    if ( key==null && accessToken==null )
      throw new AuthException("Key not found for user");

    final long restrictedDurationMillis =
        restrictDuration( user.isAccountAdmin(), durationSeconds );

    if ( key != null && !key.getUser().getUserId().equals( user.getUserId() ) ) {
      throw new AuthException("Key not valid for user");
    } else if ( key == null &&  accessToken.length() < 30 ) {
      throw new AuthException("Cannot generate token for user");
    }

    final EncryptedSecurityToken encryptedToken = new EncryptedSecurityToken(
        key!=null ? key.getAccessKey() : null,
        user.getUserId(),
        getCurrentTimeMillis(),
        restrictedDurationMillis );
    return  new SecurityToken(
        encryptedToken.getAccessKeyId(),
        encryptedToken.getSecretKey( key == null ? accessToken : key.getSecretKey() ),
        encryptedToken.encrypt( getEncryptionKey( encryptedToken.getAccessKeyId() ) ),
        encryptedToken.getExpires()
        );
  }

  @Nonnull
  protected SecurityToken doIssueSecurityToken( @Nonnull final Role role,
                                                final int durationSeconds ) throws AuthException {
    Preconditions.checkNotNull( role, "Role is required" );

    final long restrictedDurationMillis =
        restrictDuration( false, durationSeconds );

    if ( role.getSecret()==null || role.getSecret().length() < 30 ) {
      throw new AuthException("Cannot generate token for role");
    }

    final EncryptedSecurityToken encryptedToken = new EncryptedSecurityToken(
        role,
        getCurrentTimeMillis(),
        restrictedDurationMillis );
    return  new SecurityToken(
        encryptedToken.getAccessKeyId(),
        encryptedToken.getSecretKey( role.getSecret() ),
        encryptedToken.encrypt( getEncryptionKey( encryptedToken.getAccessKeyId() ) ),
        encryptedToken.getExpires()
    );
  }

  @Nonnull
  protected AccessKey doLookupAccessKey( @Nonnull final String accessKeyId,
                                         @Nonnull final String token ) throws AuthException {
    Preconditions.checkNotNull( accessKeyId, "Access key identifier is required" );
    Preconditions.checkNotNull( token, "Token is required" );

    final EncryptedSecurityToken encryptedToken;
    try {
      encryptedToken = EncryptedSecurityToken.decrypt( accessKeyId, getEncryptionKey( accessKeyId ), token );
    } catch ( GeneralSecurityException e ) {
      log.debug( e, e );
      throw new AuthException("Invalid security token");
    }

    final String originatingAccessKeyId = encryptedToken.getOriginatingAccessKeyId();
    final String userId = encryptedToken.getUserId();
    final boolean active;
    final String secretKey;
    final User user;
    if ( originatingAccessKeyId != null ) {
      final AccessKey key = lookupAccessKeyById( originatingAccessKeyId );
      active = key.isActive();
      secretKey = encryptedToken.getSecretKey( key.getSecretKey() );
      user = key.getUser();
    } else if ( userId != null ) {
      user = lookupUserById( encryptedToken.getUserId() );
      active = user.isEnabled();
      secretKey = encryptedToken.getSecretKey( Objects.firstNonNull( user.getToken(), "") );
    } else  {
      final Role role = lookupRoleById( encryptedToken.getRoleId() );
      user = roleAsUser( role );
      active = true;
      secretKey = encryptedToken.getSecretKey( role.getSecret() );
    }

    return new AccessKey() {
      @Override public Boolean isActive() {
        return active && encryptedToken.isValid();
      }

      @Override public String getAccessKey() {
        return encryptedToken.getAccessKeyId();
      }

      @Override public String getSecretKey() {
        return secretKey;
      }

      @Override public Date getCreateDate() {
        return new Date(encryptedToken.getCreated());
      }

      @Override public User getUser() throws AuthException {
        return user;
      }

      @Override public void setActive(final Boolean active) throws AuthException { }
      @Override public void setCreateDate(final Date createDate) throws AuthException { }
    };
  }

  protected long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected AccessKey lookupAccessKeyById( final String accessKeyId ) throws AuthException {
    return Accounts.lookupAccessKeyById( accessKeyId );
  }

  protected User lookupUserById( final String userId ) throws AuthException {
    return Accounts.lookupUserById( userId );
  }

  protected Role lookupRoleById( final String roleId ) throws AuthException {
    return Accounts.lookupRoleById( roleId );
  }

  protected User roleAsUser( final Role role ) throws AuthException {
    return Accounts.roleAsUser( role );
  }

  protected String getSecurityTokenPassword() {
    return SystemIds.securityTokenPassword();
  }

  private long restrictDuration( final boolean isAdmin,
                                 final int durationSeconds ) {
    return Math.max(
        Math.min(
            durationSeconds == 0 ?
                TimeUnit.HOURS.toMillis( 12 ) :
                TimeUnit.SECONDS.toMillis( durationSeconds ),
            TimeUnit.HOURS.toMillis( isAdmin ? 1 : 36 )
        ),
        TimeUnit.HOURS.toMillis( 1 ) );
  }

  private SecretKey getEncryptionKey( final String salt ) {
    final MessageDigest digest = Digest.SHA256.get();
    digest.update( salt.getBytes( Charsets.UTF_8 ) );
    digest.update( getSecurityTokenPassword().getBytes( Charsets.UTF_8 ) );
    return new SecretKeySpec( digest.digest(), "AES" );
  }

  private static final class EncryptedSecurityToken {
    private static final byte[] TOKEN_PREFIX = new byte[]{ 'e', 'u', 'c', 'a', 0, 1 };

    private String accessKeyId;
    private String originatingId;
    private String nonce;
    private long created;
    private long expires;

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
          durationMillis );
    }

    /**
     * Generate a new token
     */
    private EncryptedSecurityToken( final Role role,
                                    final long created,
                                    final long durationMillis ) {
      this( "$r$" + role.getRoleId(), created, durationMillis );
    }

      /**
      * Generate a new token
      */
    private EncryptedSecurityToken( final String originatingId,
                                    final long created,
                                    final long durationMillis ) {
      this.accessKeyId = Crypto.generateAlphanumericId( 20, "AKI" );
      this.originatingId = originatingId;
      this.nonce = Crypto.generateSessionToken();
      this.created = created;
      this.expires = created + durationMillis;
    }

    /**
     * Reconstruct token
     */
    private EncryptedSecurityToken( final String accessKeyId,
                                    final String originatingId,
                                    final String nonce,
                                    final long created,
                                    final long expires ) {
      this.accessKeyId = accessKeyId;
      this.originatingId = originatingId;
      this.nonce = nonce;
      this.created = created;
      this.expires = expires;
    }

    private String getAccessKeyId() {
      return accessKeyId;
    }

    public String getOriginatingAccessKeyId() {
      return getTrimmedIfPrefixed( "$a$", originatingId );
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

    /**
     * Is the token within its validity period.
     */
    private boolean isValid() {
      final long now = System.currentTimeMillis();
      return now >= created && now < expires;
    }

    private String getSecretKey( final String secret ) {
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
        out.writeInt(2); // format identifier
        out.writeString(originatingId);
        out.writeString(nonce);
        out.writeLong(created);
        out.writeLong(expires);
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
        cipher.init( Cipher.ENCRYPT_MODE, key, new IvParameterSpec( iv ) );
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write( TOKEN_PREFIX );
        out.write( iv );
        out.write( cipher.doFinal(toBytes()) );
        return B64.standard.encString( out.toByteArray() );
      } catch (GeneralSecurityException e) {
        throw Exceptions.toUndeclared( e );
      } catch (IOException e) {
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

        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec( securityTokenBytes, TOKEN_PREFIX.length, 32 ));
        final int offset = TOKEN_PREFIX.length + 32;
        final SecurityTokenInput in = new SecurityTokenInput(
            cipher.doFinal( securityTokenBytes, offset, securityTokenBytes.length-offset ) );
        if ( in.readInt() != 2 ) throw new GeneralSecurityException("Invalid token format");
        final String originatingAccessKeyIdOrUserId = in.readString();
        final String nonce = in.readString();
        final long created = in.readLong();
        final long expires = in.readLong();
        return new EncryptedSecurityToken(
            accessKeyId,
            originatingAccessKeyIdOrUserId,
            nonce,
            created,
            expires );
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
