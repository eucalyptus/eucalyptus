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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;

/**
 *
 */
@SuppressWarnings( "WeakerAccess" )
public class ServiceAdvice extends AbstractRequestHandlerAdvice {

  @Override
  protected final Object doInvoke( final ExecutionCallback executionCallback, final Object o, final Message<?> message ) throws Exception {
    final Object request = message.getPayload( );
    beforeService( request );
    try {
      final Object result = executionCallback.execute( );
      afterService( request, result );
      return result;
    } catch ( Exception exception ) {
      throw serviceError( request, exception );
    }
  }

  /**
   * Callback prior to service invocation.
   *
   * Exceptions thrown from this callback will not hit #serviceError
   */
  protected void beforeService( @Nonnull Object request ) throws Exception { };

  /**
   * Callback post to service invocation.
   *
   * If the service throws an exception this callback is skipped.
   */
  protected void afterService( @Nonnull Object request, @Nullable Object response ) throws Exception { };

  /**
   * Callback on service invocation error.
   *
   * Implementations must throw or return an Exception
   */
  @Nonnull
  protected Exception serviceError(
      @Nonnull Object request,
      @Nonnull Exception exception
  ) throws Exception { return exception; };
}
