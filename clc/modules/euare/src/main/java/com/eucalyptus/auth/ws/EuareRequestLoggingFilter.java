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
