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
package com.eucalyptus.cloudformation.ws;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

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
