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
package com.eucalyptus.tokens.ws;

import java.util.Collection;
import java.util.stream.Collectors;
import com.eucalyptus.ws.protocol.RequestLoggingFilter;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
@SuppressWarnings( "unused" )
public class TokensRequestLoggingFilter implements RequestLoggingFilter {
  private static final Iterable<String> ACTION_NVPS = RequestLoggingFilter.actionPairs( "AssumeRoleWithWebIdentity" );
  private static final String WEB_ID_TOKEN_PARAMETER = "WebIdentityToken=";
  private static final String WEB_ID_TOKEN_PARAMETER_REDACTED = WEB_ID_TOKEN_PARAMETER + REDACTED;

  @Override
  public Collection<String> apply( final Collection<String> parametersOrBody ) {
    if ( Iterables.tryFind( ACTION_NVPS, Predicates.in( parametersOrBody ) ).isPresent( ) ) {
      return parametersOrBody.stream( )
          .map( parameterAndValue ->
              parameterAndValue.startsWith( WEB_ID_TOKEN_PARAMETER ) ?
                WEB_ID_TOKEN_PARAMETER_REDACTED :
                parameterAndValue
          )
          .collect( Collectors.toList( ) );
    }
    return parametersOrBody;
  }
}
