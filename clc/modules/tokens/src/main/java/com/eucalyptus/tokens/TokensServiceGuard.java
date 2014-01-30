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
package com.eucalyptus.tokens;

import static com.eucalyptus.tokens.TokensServiceConfiguration.getDisabledActions;
import static com.eucalyptus.tokens.TokensServiceConfiguration.getEnabledActions;
import com.eucalyptus.util.RestrictedTypes;

/**
 * Guard component invoked before service actions.
 */
public class TokensServiceGuard {

  public TokenMessage actionCheck( final Object object ) throws TokensException {
    // Check type
    if ( !(object instanceof TokenMessage ) ) {
      throw new TokensException( TokensException.Code.InvalidAction, "Invalid action" );
    }

    // Check action enabled
    final TokenMessage message = TokenMessage.class.cast( object );
    final String action = RestrictedTypes.getIamActionByMessageType( message ).toLowerCase( );
    if ( ( !getEnabledActions().isEmpty( ) && !getEnabledActions().contains( action ) ) ||
        getDisabledActions().contains( action ) ) {
      throw new TokensException( TokensException.Code.ServiceUnavailable, "Service unavailable" );
    }

    return message;
  }
}
