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
package com.eucalyptus.autoscaling.activities;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.concurrent.ListenableFuture;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

/**
 * 
 */
class EucalyptusClient {

  private final String userId;
  private ServiceConfiguration configuration;
  
  EucalyptusClient( final String userId ) throws EucalyptusClientException {
    this.userId = userId;
  }

  void init() throws EucalyptusClientException {
    try {
      this.configuration = Topology.lookup( Eucalyptus.class );
    } catch ( final NoSuchElementException e ) {
      throw new EucalyptusClientException( e );
    }
  }

  <REQ extends EucalyptusMessage,RES extends EucalyptusMessage>
  void dispatch( final REQ request, final Callback.Checked<RES> callback ) {
    dispatch( request, callback, null );
  }

    <REQ extends EucalyptusMessage,RES extends EucalyptusMessage> 
  void dispatch( final REQ request, final Callback.Checked<RES> callback, final Runnable then ) {
    request.setEffectiveUserId( userId );
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
  
  static final class EucalyptusClientException extends Exception {
    private static final long serialVersionUID = 1L;

    public EucalyptusClientException( final String message ) {
      super( message );
    }

    public EucalyptusClientException( final String message, 
                                      final Throwable cause ) {
      super( message, cause );
    }

    public EucalyptusClientException( final Throwable cause ) {
      super( cause ); 
    }
  }
}
