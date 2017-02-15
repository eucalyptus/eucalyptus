/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.auth.principal;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import com.eucalyptus.util.Parameters;
import javaslang.control.Option;

/**
 *
 */
public final class AccessKeyCredential {

  public enum SignatureVersion { v2, v4 }

  private final String accessKeyId;
  private final SignatureVersion signatureVersion;
  private final Long signatureTimestamp;
  private final Option<TemporaryKeyType> type;

  private AccessKeyCredential( @Nonnull final String accessKeyId,
                               @Nonnull final SignatureVersion signatureVersion,
                                        final Long signatureTimestamp,
                               @Nonnull final Option<TemporaryKeyType> type ) {
    this.accessKeyId = Parameters.checkParam( "accessKeyId", accessKeyId, not( isEmptyOrNullString( ) ) );
    this.signatureVersion = Parameters.checkParam( "signatureVersion", signatureVersion, notNullValue( ) );
    this.signatureTimestamp = signatureTimestamp;
    this.type = Parameters.checkParam( "type", type, notNullValue( ) );
  }

  public static AccessKeyCredential of( @Nonnull final String queryId,
                                        @Nonnull final SignatureVersion signatureVersion,
                                                 final Long signatureTimestamp,
                                        @Nonnull final Option<TemporaryKeyType> type ) {
    return new AccessKeyCredential( queryId, signatureVersion, signatureTimestamp, type );
  }

  @Nonnull
  public String getAccessKeyId( ) {
    return accessKeyId;
  }

  @Nonnull
  public SignatureVersion getSignatureVersion( ) {
    return signatureVersion;
  }

  @Nullable
  public Long getSignatureTimestamp( ) {
    return signatureTimestamp;
  }

  @Nonnull
  public Option<TemporaryKeyType> getType( ) {
    return type;
  }

  public String toString( ) {
    return getAccessKeyId( );
  }

  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;

    final AccessKeyCredential that = (AccessKeyCredential) o;

    if ( !accessKeyId.equals( that.accessKeyId ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return accessKeyId.hashCode( );
  }
}
