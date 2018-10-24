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
package com.eucalyptus.context;

import org.springframework.integration.channel.interceptor.ThreadStatePropagationChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import com.google.common.base.Optional;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@SuppressWarnings( { "Guava", "OptionalUsedAsFieldOrParameterType" } )
public class ContextPropagationChannelInterceptor extends ThreadStatePropagationChannelInterceptor<Optional<Context>> {

  @Override
  protected Optional<Context> obtainPropagatingContext( final Message<?> message,
                                                        final MessageChannel messageChannel ) {
    Optional<Context> context = Optional.fromNullable( Contexts.threadLocal( ) );
    final String correlationId = message.getPayload( ) instanceof BaseMessage ?
        ( (BaseMessage) message.getPayload( ) ).getCorrelationId( ) :
        null;
    if ( !context.isPresent( ) || !context.get( ).getCorrelationId( ).equals( correlationId ) ) {
      if ( Contexts.exists( correlationId ) ) {
        try {
          final Context messageContext = Contexts.lookup( correlationId );
          context = Optional.of( messageContext );
        } catch ( NoSuchContextException ignored ) { }
      }
    }
    return context;
  }

  @Override
  protected void populatePropagatedContext( final Optional<Context> contextOptional,
                                            final Message<?> message,
                                            final MessageChannel messageChannel ) {
    Contexts.threadLocal( contextOptional.orNull( ) );
  }
}
