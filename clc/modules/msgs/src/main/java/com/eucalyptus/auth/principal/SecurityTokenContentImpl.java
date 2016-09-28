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
package com.eucalyptus.auth.principal;

import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 *
 */
@SuppressWarnings( { "OptionalUsedAsFieldOrParameterType", "Guava" } )
public class SecurityTokenContentImpl implements SecurityTokenContent {
  private final Optional<String> originatingAccessKeyId;
  private final Optional<String> originatingUserId;
  private final Optional<String> originatingRoleId;
  private final String nonce;
  private final long created;
  private final long expires;
  private final Map<String,String> attributes;

  public SecurityTokenContentImpl(
      final Optional<String> originatingAccessKeyId,
      final Optional<String> originatingUserId,
      final Optional<String> originatingRoleId,
      final String nonce,
      final long created,
      final long expires,
      final Map<String,String> attributes
  ) {
    Parameters.checkParamNotNull( "originatingAccessKeyId", originatingAccessKeyId );
    Parameters.checkParamNotNull( "originatingUserId", originatingUserId );
    Parameters.checkParamNotNull( "originatingRoleId", originatingRoleId );
    Parameters.checkParamNotNullOrEmpty( "nonce", nonce );
    Parameters.checkParamNotNull( "attributes", attributes );
    if ( Iterables.size( Optional.presentInstances( Arrays.asList( originatingAccessKeyId, originatingUserId, originatingRoleId ) ) ) != 1 ) {
      throw new IllegalArgumentException( "One originating identifier expected" );
    }
    this.originatingAccessKeyId = originatingAccessKeyId;
    this.originatingUserId = originatingUserId;
    this.originatingRoleId = originatingRoleId;
    this.nonce = nonce;
    this.created = created;
    this.expires = expires;
    this.attributes = ImmutableMap.copyOf( attributes );
  }

  @Nonnull
  @Override
  public Optional<String> getOriginatingAccessKeyId( ) {
    return originatingAccessKeyId;
  }

  @Nonnull
  @Override
  public Optional<String> getOriginatingUserId( ) {
    return originatingUserId;
  }

  @Nonnull
  @Override
  public Optional<String> getOriginatingRoleId( ) {
    return originatingRoleId;
  }

  @Nonnull
  @Override
  public String getNonce( ) {
    return nonce;
  }

  @Override
  public long getCreated( ) {
    return created;
  }

  @Override
  public long getExpires( ) {
    return expires;
  }

  @Nonnull
  @Override
  public Map<String, String> getAttributes( ) {
    return attributes;
  }
}
