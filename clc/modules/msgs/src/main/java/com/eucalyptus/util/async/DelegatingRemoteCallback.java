/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
