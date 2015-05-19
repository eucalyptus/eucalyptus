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
package com.eucalyptus.auth.policy;

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.auth.policy.PolicyUtils.checkParam;
import java.util.Set;
import com.eucalyptus.auth.principal.Condition;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
public class PolicyCondition implements Condition {

  private final String type;

  private final String key;

  // The values to be compared with the key
  private final Set<String> values;

  public PolicyCondition(
      final String type,
      final String key,
      final Set<String> values
  ) {
    checkParam( "type", type, notNullValue() );
    checkParam( "key", key, notNullValue() );
    checkParam( "values", values, notNullValue() );
    this.type = PolicyUtils.intern( type );
    this.key = PolicyUtils.intern( key );
    this.values = ImmutableSet.copyOf( Iterables.transform( values, PolicyUtils.internString( ) ) );
  }

  public String getType( ) {
    return type;
  }

  public String getKey( ) {
    return key;
  }

  public Set<String> getValues( ) {
    return values;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final PolicyCondition that = (PolicyCondition) o;

    if ( !key.equals( that.key ) ) return false;
    if ( !type.equals( that.type ) ) return false;
    if ( !values.equals( that.values ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + key.hashCode();
    result = 31 * result + values.hashCode();
    return result;
  }
}
