/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.ws.protocol;

import java.util.Collection;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Strings;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import javaslang.collection.List;

/**
 *
 */
public interface RequestLoggingFilter extends Function<Collection<String>,Collection<String>> {

  String REDACTED = "********";

  Collection<String> apply( Collection<String> parametersOrBody );

  static Iterable<String> actionPairs( final String... actions ) {
    return ImmutableList.copyOf(
        List.of( OperationParameter.values( ) )
            .map( Object::toString )
            .flatMap( FUtils.applyAll( List.of( actions ).map( Strings.prepend( "=" ) ).map( Strings::append ) ) )
    );
  }
}
