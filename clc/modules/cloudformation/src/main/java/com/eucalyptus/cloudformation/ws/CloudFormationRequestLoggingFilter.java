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
package com.eucalyptus.cloudformation.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.protocol.RequestLoggingFilter;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class CloudFormationRequestLoggingFilter implements RequestLoggingFilter {
  private static final Iterable<String> ACTIONS = ImmutableList.of( "CreateStack", "UpdateStack", "ValidateTemplate" );
  private static final Iterable<String> ACTION_NVPS = FluentIterable
      .from(  Arrays.asList( OperationParameter.values() ) )
      .transform( Functions.toStringFunction( ) )
      .transform( Strings.append( "=" ) )
      .transformAndConcat( Pair.<String,String>explodeRight( ACTIONS ) )
      .transform( Pair.transformer( Strings.join( ) ) );
  private static final Pattern TEMPLATE_BODY_PARAMETER_REGEX = Pattern.compile( "^(TemplateBody)=.*$" );
  private static final Pattern PARAMETERS_PARAMETER_REGEX = Pattern.compile( "^(Parameters\\.member\\.[0-9]+\\.ParameterValue)=.*$" );

  @Override
  public Collection<String> apply( final Collection<String> parametersOrBody ) {
    if ( Iterables.tryFind( ACTION_NVPS, Predicates.in( parametersOrBody ) ).isPresent( ) ) {
      final Iterable<String> templateBodyNVPs =
          Iterables.filter( parametersOrBody, Predicates.contains( TEMPLATE_BODY_PARAMETER_REGEX ) );

      final Iterable<String> parametersNVPs =
          Iterables.filter( parametersOrBody, Predicates.contains( PARAMETERS_PARAMETER_REGEX ) );

      if ( !Iterables.isEmpty( templateBodyNVPs ) || !Iterables.isEmpty( parametersNVPs ) ) {
        final ArrayList<String> parametersCopy = Lists.newArrayList( parametersOrBody );
        redactParameters( parametersCopy, templateBodyNVPs, TEMPLATE_BODY_PARAMETER_REGEX );
        redactParameters( parametersCopy, parametersNVPs, PARAMETERS_PARAMETER_REGEX );
        return parametersCopy;
      }
    }

    return parametersOrBody;
  }

  private void redactParameters( final ArrayList<String> parameters,
                                 final Iterable<String> nvps,
                                 final Pattern pattern ) {
    for ( final String nvp : nvps ) {
      parameters.set(
          parameters.indexOf( nvp ),
          pattern.matcher( nvp ).replaceFirst( "$1="+REDACTED ) );
    }
  }
}
