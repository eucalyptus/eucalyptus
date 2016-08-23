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

package com.eucalyptus.auth;

import com.eucalyptus.util.TypedKey;

/**
 *
 */
public final class PolicyEvaluationWriteContextKey<T> {

  private final TypedKey<T> key;

  private PolicyEvaluationWriteContextKey( final TypedKey<T> key ) {
    this.key = key;
  }

  public static <T> PolicyEvaluationWriteContextKey<T> create( final TypedKey<T> key ) {
    return new PolicyEvaluationWriteContextKey<>( key );
  }

  protected TypedKey<T> getKey( ) {
    return key;
  }
}
