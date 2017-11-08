/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.resources.client;

import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Optional;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author Sang-Min Park
 *
 */
public abstract class EucalyptusClientTask<TM extends BaseMessage, TC extends ComponentId> {
  private static final Logger LOG = Logger.getLogger(EucalyptusClientTask.class);
  private String errorCode;
  private String errorMessage;

  protected EucalyptusClientTask( ) {

  }

  final CheckedListenableFuture<Boolean> dispatch(
      final ClientContext<TM, TC> context) {
    try {
      final CheckedListenableFuture<Boolean> future = Futures
          .newGenericeFuture();
      dispatchInternal(context, new Callback.Checked<TM>() {
        @Override
        public void fireException(final Throwable throwable) {
          boolean result = false;
          try {
            result = dispatchFailure(context, throwable);
          } finally {
            future.set( result );
          }
        }

        @Override
        public void fire(final TM response) {
          try {
            dispatchSuccess(context, response);
          } finally {
            future.set(true);
          }
        }
      });
      return future;
    } catch (Exception e) {
      NoSuchElementException nsee = Exceptions.findCause( e, NoSuchElementException.class );
      if ( nsee != null ) {
        LOG.warn( nsee.getMessage( ) );
      } else {
        LOG.error( "Got error", e );
      }
    }
    return Futures.predestinedFuture(false);
  }

  abstract void dispatchInternal(ClientContext<TM, TC> context,
      Callback.Checked<TM> callback);

  /**
   * @return True if the failure was handled and should be treated as success.
   */
  boolean dispatchFailure(ClientContext<TM, TC> context, Throwable throwable) {
    final Optional<AsyncExceptions.AsyncWebServiceError> serviceErrorOption =
        AsyncExceptions.asWebServiceError( throwable );
    if ( serviceErrorOption.isPresent( ) ) {
      errorCode = serviceErrorOption.get( ).getCode();
      errorMessage = serviceErrorOption.get( ).getMessage();
      return false;
    }

    final NoSuchElementException ex2 = Exceptions.findCause( throwable, NoSuchElementException.class );
    if ( ex2 != null ) {
      errorMessage = ex2.getMessage();
      return false;
    }

    LOG.error("Eucalyptus client error", throwable);
    return false;
  }

  @Nullable
  String getErrorCode() {
    return errorCode;
  }

  @Nullable
  String getErrorMessage() {
    return errorMessage;
  }

  abstract void dispatchSuccess(ClientContext<TM, TC> context, TM response);
}
