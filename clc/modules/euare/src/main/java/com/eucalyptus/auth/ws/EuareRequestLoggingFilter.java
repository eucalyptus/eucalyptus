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
package com.eucalyptus.auth.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class EuareRequestLoggingFilter implements RequestLoggingFilter {
  private static final Iterable<String> CREATE_UPDATE_PROFILE_ACTION_NVPS = Iterables.concat(
      buildActionNVPs( "CreateLoginProfile" ),
      buildActionNVPs( "UpdateLoginProfile" ) );
  private static final Iterable<String> UPLOAD_CERT_ACTION_NVPS = buildActionNVPs( "UploadServerCertificate" );

  private static final String PASSWORD_PARAMETER = "Password=";
  private static final String PASSWORD_PARAMETER_REDACTED = PASSWORD_PARAMETER + REDACTED;

  private static final String PRIVATE_KEY_PARAMETER = "PrivateKey=";
  private static final String PRIVATE_KEY_PARAMETER_REDACTED = PRIVATE_KEY_PARAMETER + REDACTED;

  private static Iterable<String> buildActionNVPs( final String action ) {
    return Iterables.unmodifiableIterable( Iterables.transform(
        Arrays.asList( OperationParameter.values() ),
        Functions.compose( Strings.append( "=" + action ), Functions.toStringFunction() ) ) );
  }

  @Override
  public Collection<String> apply( final Collection<String> parametersOrBody ) {
    if ( isAction( parametersOrBody, CREATE_UPDATE_PROFILE_ACTION_NVPS ) ) {
      return filterParameter( parametersOrBody, PASSWORD_PARAMETER, PASSWORD_PARAMETER_REDACTED );
    } else if ( isAction( parametersOrBody, UPLOAD_CERT_ACTION_NVPS ) ) {
      return filterParameter( parametersOrBody, PRIVATE_KEY_PARAMETER, PRIVATE_KEY_PARAMETER_REDACTED );
    }
    return parametersOrBody;
  }

  private boolean isAction( final Collection<String> parametersOrBody,
                            final Iterable<String> actionNvps ) {
    return Iterables.tryFind( actionNvps, Predicates.in( parametersOrBody ) ).isPresent( );
  }

  private Collection<String> filterParameter( final Collection<String> parametersOrBody,
                                              final String parameter,
                                              final String redacted ) {
    final Optional<String> parameterAndValue =
        Iterables.tryFind( parametersOrBody, Strings.startsWith( parameter ) );
    if ( parameterAndValue.isPresent() ) {
      final ArrayList<String> parametersCopy = Lists.newArrayList( parametersOrBody );
      parametersCopy.set( parametersCopy.indexOf( parameterAndValue.get() ), redacted );
      return parametersCopy;
    }
    return parametersOrBody;
  }
}
