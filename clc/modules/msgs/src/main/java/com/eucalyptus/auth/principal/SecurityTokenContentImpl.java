/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
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
