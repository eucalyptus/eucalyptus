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
package com.eucalyptus.network;

import static org.hamcrest.Matchers.notNullValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.Parameters;
import com.google.common.base.Strings;

/**
 *
 */
public enum NetworkMode {

  EDGE,

  MANAGED,

  MANAGED_NOVLAN,

  VPCMIDO,

  ;

  @Nonnull
  public String toString( ) {
    return name( ).replace( '_', '-' );
  }

  @Nonnull
  public static NetworkMode fromString( @Nonnull final String value ) {
    return NetworkMode.valueOf( value.replace( '-', '_' ) );
  }

  /**
   * Network mode from the value if present, else the given default
   */
  @Nonnull
  public static NetworkMode fromString( @Nullable final String value,
                                        @Nonnull final NetworkMode defaultMode ) {
    Parameters.checkParam( "defaultMode", defaultMode, notNullValue( ) );
    //noinspection ConstantConditions
    return !Strings.isNullOrEmpty( value ) ?
        fromString( value ) :
        defaultMode;
  }

}
