/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
