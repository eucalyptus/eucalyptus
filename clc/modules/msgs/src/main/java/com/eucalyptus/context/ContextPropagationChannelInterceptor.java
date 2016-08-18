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
