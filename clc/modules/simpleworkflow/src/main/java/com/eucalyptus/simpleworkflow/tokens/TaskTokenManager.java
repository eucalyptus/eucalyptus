/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.tokens;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.annotation.Nonnull;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 *
 */
@ComponentNamed
public class TaskTokenManager {
  private static final Supplier<SecureRandom> randomSupplier = Crypto.getSecureRandomSupplier();
  private static final Supplier<String> securityTokenPasswordSupplier = Suppliers.memoize(
      new Supplier<String>( ) {
        @Override
        public String get() {
          return SystemIds.securityTokenPassword( );
        }
      } );

  @Nonnull
  public String encryptTaskToken( @Nonnull final TaskToken taskToken ) {
    final EncryptedTaskToken encryptedToken = new EncryptedTaskToken( taskToken );
    return encryptedToken.encrypt( getEncryptionKey( taskToken.getAccountNumber() ) );
  }

  @Nonnull
  public TaskToken decryptTaskToken( final String accountNumber,
                                     final String taskToken ) throws TaskTokenException {
    if ( taskToken == null ) throw new TaskTokenException( "Missing task token" );
    try {
      return EncryptedTaskToken.decrypt( getEncryptionKey( accountNumber ), taskToken ).getTaskToken( );
    } catch ( GeneralSecurityException e ) {
      throw new TaskTokenException( "Error decrypting task token", e );
    }
  }

  protected String getTokenPassword() {
    return securityTokenPasswordSupplier.get( );
  }

  private SecretKey getEncryptionKey( final String salt ) {
    final MessageDigest digest = Digest.SHA256.get();
    digest.update( salt.getBytes( Charsets.UTF_8 ) );
    digest.update( getTokenPassword().getBytes( Charsets.UTF_8 ) );
    return new SecretKeySpec( digest.digest(), "AES" );
  }

  private static final class EncryptedTaskToken {
    private static final byte[] TOKEN_PREFIX = new byte[]{ 'e', 'u', 's', 'w', 0, 1 };

    private final TaskToken taskToken;

    private EncryptedTaskToken( final TaskToken taskToken ) {
      this.taskToken = taskToken;
    }

    public TaskToken getTaskToken( ) {
      return taskToken;
    }

    private byte[] toBytes( ) {
      try {
        final TaskTokenOutput out = new TaskTokenOutput();
        out.writeInt( 1 ); // format identifier
        out.writeString( taskToken.getAccountNumber() );
        out.writeString( taskToken.getDomainUuid() );
        out.writeString( taskToken.getRunId() );
        out.writeLong( taskToken.getScheduledEventId() );
        out.writeLong( taskToken.getStartedEventId() );
        out.writeLong(taskToken.getCreated());
        out.writeLong(taskToken.getExpires());
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

    private static EncryptedTaskToken decrypt( final SecretKey key,
                                               final String taskToken ) throws GeneralSecurityException {
      try {
        final Cipher cipher = Ciphers.AES_GCM.get();
        final byte[] taskTokenBytes = B64.standard.dec(taskToken);
        if ( taskTokenBytes.length < 64 + TOKEN_PREFIX.length ||
            !Arrays.equals( TOKEN_PREFIX, Arrays.copyOf( taskTokenBytes, TOKEN_PREFIX.length ) ) ) {
          throw new GeneralSecurityException("Invalid token format");
        }

        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            new IvParameterSpec( taskTokenBytes, TOKEN_PREFIX.length, 32 ),
            randomSupplier.get( )
        );
        final int offset = TOKEN_PREFIX.length + 32;
        final TaskTokenInput in = new TaskTokenInput(
            cipher.doFinal( taskTokenBytes, offset, taskTokenBytes.length-offset ) );
        if ( in.readInt() != 1 ) throw new GeneralSecurityException("Invalid token format");
        final String accountNumber = in.readString();
        final String domainUuid = in.readString();
        final String runId = in.readString();
        final long scheduledEventId = in.readLong();
        final long startedEventId = in.readLong();
        final long created = in.readLong();
        final long expires = in.readLong();
        return new EncryptedTaskToken(
            new TaskToken( accountNumber, domainUuid, runId, scheduledEventId, startedEventId, created, expires ) );
      } catch (IOException e) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  private static final class TaskTokenOutput {
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
      out.finish();
      deflater.end();
      out.close();
      return byteStream.toByteArray();
    }
  }

  private static final class TaskTokenInput {
    private final InputStream in;

    private TaskTokenInput( final byte[] data ) {
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
