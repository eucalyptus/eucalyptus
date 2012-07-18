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

import java.util.concurrent.atomic.AtomicReference;
import java.lang.IllegalStateException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.records.Logs;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class MessageCallback<Q extends BaseMessage, R extends BaseMessage> implements RemoteCallback<Q, R> {
  private Logger                   LOG     = Logger.getLogger( this.getClass( ) );
  private final AtomicReference<Q> request = new AtomicReference<Q>( null );
  
  protected MessageCallback( ) {
    super( );
  }
  
  protected MessageCallback( Q request ) {
    super( );
    if ( request.getUserId( ) == null ) {
      request.setUser( Principals.systemUser( ) );
    }
    this.request.set( request );
  }
  
  /**
   * @see com.eucalyptus.util.async.RemoteCallback#getRequest()
   * @return
   */
  @Override
  public Q getRequest( ) {
    return this.request.get( );
  }
  
  /**
   * Optional method for setting the request after using the no-arg constructor. Useful in cases
   * where additional work needs to be done before calling super()
   * in inheriting classes.
   * 
   * @param request
   */
  protected void setRequest( Q request ) {
    Q oldReq = null;
    if ( ( oldReq = this.request.getAndSet( request ) ) != null ) {
      Logs.extreme( ).error( "Request has been set twice.  Old message was: " + oldReq, new IllegalStateException( "Request has been set twice." ) );
    }
  }
  
  /**
   * @see com.eucalyptus.util.async.RemoteCallback#initialize(Q)
   * @param request
   * @throws Exception
   */
  @Override
  public void initialize( Q request ) throws Exception {
    Logs.extreme( ).trace( this.getClass( ) + ":"
                           + this.request.get( ).getClass( ).getSimpleName( )
                           + " should implement: initialize( ) to check any preconditions!" );
  }
  
  /**
   * @see com.eucalyptus.util.async.RemoteCallback#fire(R)
   * @param msg
   */
  @Override
  public abstract void fire( R msg );
  
  /**
   * @see com.eucalyptus.util.async.RemoteCallback#fireException(java.lang.Throwable)
   * @param t
   */
  @Override
  public void fireException( Throwable t ) {
    Logs.extreme( ).error( this.getClass( ) + ":"
                           + this.request.get( ).getClass( ).getSimpleName( )
                           + " should implement: fireException( Throwable t ) to handle errors!" );
    Logs.exhaust( ).error( t, t );
  }
  
}
