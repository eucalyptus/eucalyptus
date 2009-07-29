package com.eucalyptus.ws.handlers;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.WebServicesException;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

@ChannelPipelineCoverage( "one" )
public class NioResponseHandler extends MessageStackHandler {
  private static Logger     LOG      = Logger.getLogger( NioResponseHandler.class );

  private final Lock        canHas   = new ReentrantLock( );
  private final Condition   finished = canHas.newCondition( );
  private EucalyptusMessage response = null;
  private Exception         ex       = null;

  @Override
  public void exceptionCaught( Throwable e ) throws Exception {
    this.canHas.lock( );
    if ( e instanceof Exception ) {
      this.ex = ( Exception ) e;
    } else {
      this.ex = new WebServicesException( e );
    }
    this.finished.signal( );
    this.canHas.unlock( );
  }

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    this.exceptionCaught( e.getCause( ) );
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    MappingHttpMessage httpResponse = ( MappingHttpMessage ) event.getMessage( );
    this.canHas.lock( );
    this.response = (EucalyptusMessage) httpResponse.getMessage( );
    this.finished.signal( );
    this.canHas.unlock( );
  }

  public EucalyptusMessage getResponse( ) throws Exception {
    this.canHas.lock( );
    try {
      if ( this.response == null && this.ex == null ) {
        this.finished.await( );
      }
      if ( ex != null ) { throw this.ex; }
      return this.response;
    } finally {
      this.canHas.unlock( );
    }
  }

}
