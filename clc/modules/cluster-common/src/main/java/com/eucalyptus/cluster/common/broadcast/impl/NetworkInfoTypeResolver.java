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
package com.eucalyptus.cluster.common.broadcast.impl;

import com.fasterxml.jackson.databind.AbstractTypeResolver;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;

/**
 *
 */
public class NetworkInfoTypeResolver extends AbstractTypeResolver {

  @Override
  public JavaType findTypeMapping( final DeserializationConfig config, final JavaType type ) {
    final Class<?> src = type.getRawClass( );
    if ( !src.isInterface( ) || src.getSimpleName( ).startsWith( "Immutable" ) ) {
      return null;
    }
    final Class<?> dst;
    try {
      dst = Class.forName( src.getPackage( ).getName( ) + ".Immutable" + src.getSimpleName( ) );
    } catch ( ClassNotFoundException ignore ) {
      return null;
    }
    return config.getTypeFactory( ).constructSpecializedType( type, dst );
  }
}
