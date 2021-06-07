/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
      if ( Logs.isExtrrreeeme( ) ) {
        Logs.extreme( ).error( "Request has been set twice.  Old message was: " + oldReq, new IllegalStateException( "Request has been set twice." ) );
      }
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
