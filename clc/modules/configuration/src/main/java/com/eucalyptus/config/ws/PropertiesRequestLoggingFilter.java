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
package com.eucalyptus.config.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.protocol.RequestLoggingFilter;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
*
*/
@SuppressWarnings( "UnusedDeclaration" )
public final class PropertiesRequestLoggingFilter implements RequestLoggingFilter {
  private static final Iterable<String> ACTION_NVPS = Iterables.unmodifiableIterable( Iterables.transform(
      Arrays.asList( OperationParameter.values( ) ),
      Functions.compose( Strings.append( "=ModifyPropertyValue" ), Functions.toStringFunction( ) ) ) );
  private static final String NAME_PARAMETER = "Name=";
  private static final String VALUE_PARAMETER = "Value=";
  private static final String VALUE_PARAMETER_REDACTED = VALUE_PARAMETER + REDACTED;

  @Override
  public Collection<String> apply( final Collection<String> parametersOrBody ) {
    if ( Iterables.tryFind( ACTION_NVPS, Predicates.in( parametersOrBody ) ).isPresent( ) ) {
      final Optional<String> nameParameterAndValue =
          Iterables.tryFind( parametersOrBody, Strings.startsWith( NAME_PARAMETER ) );
      final Optional<String> valueParameterAndValue =
          Iterables.tryFind( parametersOrBody, Strings.startsWith( VALUE_PARAMETER ) );

      if ( nameParameterAndValue.isPresent() && valueParameterAndValue.isPresent() ) try {
        final ConfigurableProperty entry =
            PropertyDirectory.getPropertyEntry( Strings.trimPrefix( NAME_PARAMETER, nameParameterAndValue.get() ) );
        if ( ConfigurableFieldType.KEYVALUEHIDDEN == entry.getWidgetType() ) {
          final ArrayList<String> parametersCopy = Lists.newArrayList( parametersOrBody );
          parametersCopy.set( parametersCopy.indexOf( valueParameterAndValue.get() ), VALUE_PARAMETER_REDACTED );
          return parametersCopy;
        }
      } catch ( IllegalAccessException e ) {
        // property not found
      }
    }

    return parametersOrBody;
  }
}
