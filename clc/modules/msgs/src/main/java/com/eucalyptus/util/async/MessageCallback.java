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
