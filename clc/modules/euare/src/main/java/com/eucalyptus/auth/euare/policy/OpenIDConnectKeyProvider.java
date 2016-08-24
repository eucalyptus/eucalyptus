/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.auth.euare.policy;

import java.util.Map;
import java.util.function.Function;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.KeyProvider;
import com.eucalyptus.util.Strings;
import com.google.common.collect.ImmutableMap;

/**
 *
 */
public class OpenIDConnectKeyProvider implements KeyProvider {

  private static final Map<String,Function<String,Key>> SUFFIX_TO_BUILDER_MAP =
      ImmutableMap.<String,Function<String,Key>>builder( )
          .put( "aud", OpenIDConnectAudKey::new )
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

  private String suffix( String name ) {
    return Strings.substringAfter( ":", name );
  }
}
