/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * Remote callback implementation that delegates all methods.
 */
public class DelegatingRemoteCallback<Q extends BaseMessage, R extends BaseMessage> implements RemoteCallback<Q,R>{

  private final RemoteCallback<Q,R> delegate;

  public DelegatingRemoteCallback( final RemoteCallback<Q,R> delegate ) {
    this.delegate = delegate;
  }

  @Override
  public Q getRequest() {
    return delegate.getRequest();
  }

  @Override
  public void initialize( final Q request ) throws Exception {
    delegate.initialize( request );
  }

  @Override
  public void fire( final R msg ) {
    delegate.fire( msg );
  }

  @Override
  public void fireException( final Throwable t ) {
    delegate.fireException( t );
  }

  public static <Q extends BaseMessage, R extends BaseMessage> RemoteCallback<Q,R> suppressException(
      final RemoteCallback<Q,R> delegate
      ) {
    return new DelegatingRemoteCallback<Q,R>( delegate ){
      @Override
      public void fireException( final Throwable t ) {
        // ignored
      }
    };
  }
}
