/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.concurrent.ListenableFuture;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * A synchronous version of {@link DispatchingClient}
 */
public class SynchronousClient<MT extends BaseMessage,CT extends ComponentId> {
  @Nullable
  protected final String userId;
  private final Class<CT> componentIdClass;
  protected ServiceConfiguration configuration;

  public SynchronousClient( @Nullable final String userId,
                            @Nonnull  final Class<CT> componentIdClass ) {
    this.userId = userId;
    this.componentIdClass = componentIdClass;
  }

  public SynchronousClient( @Nonnull final Class<CT> componentIdClass ) {
    this( null, componentIdClass );
  }

  public void init() throws SynchronousClientException {
    try {
      final ComponentId componentId = ComponentIds.lookup( componentIdClass );
      if ( componentId.isAlwaysLocal() ||
           ( BootstrapArgs.isCloudController() && componentId.isCloudLocal() && !componentId.isRegisterable() ) ) {
        this.configuration = ServiceConfigurations.createEphemeral( componentId );
      } else {
        this.configuration = Topology.lookup( componentIdClass );
      }
    } catch ( final NoSuchElementException e ) {
      throw new SynchronousClientException( e );
    }
  }
  
  //This is the core difference from Dispatching.
  public <REQ extends MT,RES extends MT> RES sendSync( final REQ request) throws Exception {
    request.setEffectiveUserId( userId );
    return AsyncRequests.sendSync( configuration, request );
  }

  public static final class SynchronousClientException extends Exception {
    private static final long serialVersionUID = 1L;

    public SynchronousClientException( final String message ) {
      super( message );
    }

    public SynchronousClientException( final String message,
                                      final Throwable cause ) {
      super( message, cause );
    }

    public SynchronousClientException( final Throwable cause ) {
      super( cause );
    }
  }
}
