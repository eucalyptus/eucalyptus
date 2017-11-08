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
package com.eucalyptus.util;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.concurrent.ListenableFuture;
import com.google.common.base.Objects;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
public class DispatchingClient<MT extends BaseMessage,CT extends ComponentId> {
  @Nullable
  private final String userId;
  private final AccountFullName accountFullName;
  private final Class<CT> componentIdClass;
  private ServiceConfiguration configuration;

  public DispatchingClient( @Nullable final String userId,
                            @Nonnull  final Class<CT> componentIdClass ) {
    this.userId = userId;
    this.accountFullName = null;
    this.componentIdClass = componentIdClass;
  }

  public DispatchingClient( @Nullable final AccountFullName accountFullName,
                            @Nonnull  final Class<CT> componentIdClass ) {
    this.userId = null;
    this.accountFullName = accountFullName;
    this.componentIdClass = componentIdClass;
  }

  public DispatchingClient( @Nonnull final Class<CT> componentIdClass ) {
    this( (String)null, componentIdClass );
  }

  public void init() throws DispatchingClientException {
    try {
      this.configuration = Topology.lookup( componentIdClass );
    } catch ( final NoSuchElementException e ) {
      throw new DispatchingClientException( e );
    }
  }

  public <REQ extends MT,RES extends MT>
   void dispatch( final REQ request, final Callback.Checked<RES> callback ) {
    dispatch( request, callback, null );
  }

  public <REQ extends MT,RES extends MT>
   void dispatch( final REQ request,
                 final Callback.Checked<RES> callback,
                 @Nullable final Runnable then ) {
    request.setUserId( userId != null ? userId : accountFullName != null ? accountFullName.getAccountNumber( ) : null );
    request.markPrivileged( );
    try {
      final ListenableFuture<RES> future =
          AsyncRequests.dispatch( configuration, request );
      future.addListener( new Runnable() {
        @Override
        public void run() {
          try {
            callback.fire( future.get() );
          } catch ( InterruptedException e ) {
            // future is complete so this can't happen
            callback.fireException( e );
          } catch ( ExecutionException e ) {
            callback.fireException( e.getCause() );
          } finally {
            if ( then != null ) then.run();
          }
        }
      } );
    } catch ( final Exception e ) {
      try {
        callback.fireException( e );
      } finally{
        if ( then != null ) then.run();
      }
    }
  }

  public static final class DispatchingClientException extends Exception {
    private static final long serialVersionUID = 1L;

    public DispatchingClientException( final String message ) {
      super( message );
    }

    public DispatchingClientException( final String message,
                                      final Throwable cause ) {
      super( message, cause );
    }

    public DispatchingClientException( final Throwable cause ) {
      super( cause );
    }
  }
}
