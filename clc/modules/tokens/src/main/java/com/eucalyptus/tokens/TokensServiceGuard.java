/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens;

import static com.eucalyptus.tokens.TokensServiceConfiguration.getDisabledActions;
import static com.eucalyptus.tokens.TokensServiceConfiguration.getEnabledActions;
import javax.annotation.Nonnull;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.tokens.common.msgs.TokenMessage;
import com.eucalyptus.util.RestrictedTypes;

/**
 * Guard component invoked before service actions.
 */
@ComponentNamed
public class TokensServiceGuard extends ServiceAdvice {

  public void beforeService( @Nonnull final Object object ) throws TokensException {
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
  }
}
