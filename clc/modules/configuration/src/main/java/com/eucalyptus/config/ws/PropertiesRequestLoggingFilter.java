/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
