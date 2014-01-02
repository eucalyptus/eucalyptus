/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
