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
package com.eucalyptus.resources.client;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author Sang-Min Park
 *
 */
public abstract class EucalyptusClientTask<TM extends BaseMessage, TC extends ComponentId> {
  private static final Logger LOG = Logger.getLogger(EucalyptusClientTask.class);
  private volatile boolean dispatched = false;

  protected EucalyptusClientTask() {}

  final CheckedListenableFuture<Boolean> dispatch(
      final ClientContext<TM, TC> context) {
    try {
      final CheckedListenableFuture<Boolean> future = Futures
          .newGenericeFuture();
      dispatchInternal(context, new Callback.Checked<TM>() {
        @Override
        public void fireException(final Throwable throwable) {
          try {
            dispatchFailure(context, throwable);
          } finally {
            future.set(false);
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
      dispatched = true;
      return future;
    } catch (Exception e) {
      LOG.error(e, e);
    }
    return Futures.predestinedFuture(false);
  }

  abstract void dispatchInternal(ClientContext<TM, TC> context,
      Callback.Checked<TM> callback);

  void dispatchFailure(ClientContext<TM, TC> context, Throwable throwable) {
    LOG.error("Eucalyptus client error", throwable);
  }

  abstract void dispatchSuccess(ClientContext<TM, TC> context, TM response);
}
