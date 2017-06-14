/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
