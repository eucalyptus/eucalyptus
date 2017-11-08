/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare.policy;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Key.EvaluationConstraint;
import com.eucalyptus.auth.policy.key.KeyProvider;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes.RoleWithWebIdSecurityTokenAttributes;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 *
 */
@SuppressWarnings( "Guava" )
public class OpenIDConnectKeyProvider implements KeyProvider {

  private static final Map<String,Function<String,Key>> SUFFIX_TO_BUILDER_MAP =
      ImmutableMap.<String,Function<String,Key>>builder( )
          .put( "aud", OpenIDConnectAudKey::new )
          .put( "sub", OpenIDConnectSubKey::new )
          .build( );

  @Override
  public String getName( ) {
    return "OpenIDConnect";
  }

  @Override
  public boolean provides( final String name ) {
    return SUFFIX_TO_BUILDER_MAP.containsKey( suffix( name ) );
  }

  @Override
  public Key getKey( final String name ) {
    return SUFFIX_TO_BUILDER_MAP.get( suffix( name ) ).apply( name );
  }

  @Override
  public Map<String, Key> getKeyInstances( final EvaluationConstraint constraint ) {
    final Map<String,Key> keyInstances = Maps.newHashMap( );
    if ( constraint == EvaluationConstraint.ReceivingHost ) {
      final Optional<RoleWithWebIdSecurityTokenAttributes> attributes =
          OpenIDConnectProviderKeySupport.getRoleAttributes( );
      if ( attributes.isPresent( ) ) {
        final String providerUrl = attributes.get( ).getProviderUrl( );
        keyInstances.putAll( SUFFIX_TO_BUILDER_MAP.entrySet( ).stream( )
            .map( entry -> entry.getValue( ).apply( providerUrl + ":" + entry.getKey( ) ) )
            .collect( Collectors.toMap( Key::name, key -> key ) ) );
      }
    }
    return keyInstances;
  }

  private String suffix( String name ) {
    final int index = name == null ? -1 : name.lastIndexOf( ":" );
    if ( index > 0 && index < name.length( ) ) {
      return name.substring( index + 1 );
    } else {
      return "";
    }
  }
}
