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
package com.eucalyptus.cluster.common.broadcast;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.collection.Array;
import io.vavr.control.Option;

/**
 *
 */
public interface BNIHasProperties {

  @JsonProperty( "property" )
  Array<BNIPropertyBase> properties( );

  default Array<BNIProperty> simpleProperties( ) {
    return typedProperties( BNIProperty.class );
  }

  default <T> Option<T> typedProperty( final Class<T> type ) {
    return properties( ).find( type::isInstance ).map( type::cast );
  }

  default <T> Array<T> typedProperties( final Class<T> type ) {
    return properties( ).filter( type::isInstance ).map( type::cast );
  }
}
