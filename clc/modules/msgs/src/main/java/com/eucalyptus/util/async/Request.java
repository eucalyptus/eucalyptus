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

import java.util.concurrent.ExecutionException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.util.Callback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface Request<Q extends BaseMessage, R extends BaseMessage> {
  public abstract CheckedListenableFuture<R> dispatch( ServiceConfiguration serviceEndpoint );
  
  public abstract R sendSync( ServiceConfiguration endpoint ) throws ExecutionException, InterruptedException;
  
  public abstract Request<Q, R> then( UnconditionalCallback callback );
  
  public abstract Request<Q, R> then( Callback.Completion callback );
  
  public abstract Request<Q, R> then( Callback.Failure<R> callback );
  
  public abstract Request<Q, R> then( Callback.Success<R> callback );
  
  public abstract Callback.TwiceChecked<Q, R> getCallback( );
  
  public abstract CheckedListenableFuture<R> getResponse( );
  
  public abstract Q getRequest( );
  
  @Deprecated
  public abstract CheckedListenableFuture<R> dispatch( String cluster );
  
}
