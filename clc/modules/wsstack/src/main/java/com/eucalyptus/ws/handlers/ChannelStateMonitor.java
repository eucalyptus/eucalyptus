package com.eucalyptus.ws.handlers;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.server.NioServer;

import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;

@ChannelPipelineCoverage( "one" )
public class ChannelStateMonitor extends SimpleChannelHandler {
  private static Logger    LOG           = Logger.getLogger( ChannelStateMonitor.class );
  private final AtomicLong readBytes     = new AtomicLong( );
  private final AtomicLong writeBytes    = new AtomicLong( );
  private AtomicLong             openTime      = new AtomicLong( );
  private String           eventUserId   = "unknown";
  private String           correlationId = "unknown";
  
  public void split( ) {
    Long rb = readBytes.getAndSet( 0l );
    Long wb = writeBytes.getAndSet( 0l );
    Long closeTime = System.currentTimeMillis( );
    Long openTime = this.openTime.getAndSet( 0 );
    Long roundTime = ( closeTime - openTime );
    LOG.debug( EventRecord.create( NioServer.class, EventType.SOCKET_CLOSE, closeTime.toString( ), roundTime.toString( ) ) );
    LOG.debug( EventRecord.create( NioServer.class, EventType.SOCKET_BYTES_READ, rb.toString( ), Float.toString( ( wb * 1020.0f ) / ( roundTime * 1024.0f ) ) ) );
    LOG.debug( EventRecord.create( NioServer.class, EventType.SOCKET_BYTES_WRITE, wb.toString( ), Float.toString( ( wb * 1020.0f ) / ( roundTime * 1024.0f ) ) ) );
  }
  
  @Override
  public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    this.split( );
    super.channelClosed( ctx, e );
  }
  
  @Override
  public void channelConnected( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    openTime.getAndSet( System.currentTimeMillis( ) );
    LOG.debug( EventRecord.create( NioServer.class, eventUserId, correlationId, EventType.SOCKET_OPEN, openTime.get( ) ) );
    super.channelConnected( ctx, e );
  }
  
  @Override
  public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    if ( e.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage msg = ( MappingHttpMessage ) e.getMessage( );
      readBytes.addAndGet( msg.getContent( ).readableBytes( ) );
    } else if ( e.getMessage( ) instanceof ChannelBuffer ) {
      ChannelBuffer msg = ( ChannelBuffer ) e.getMessage( );
      readBytes.addAndGet( msg.readableBytes( ) );
    }
    super.messageReceived( ctx, e );
  }
  
  @Override
  public void writeRequested( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    if ( e.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage msg = ( MappingHttpMessage ) e.getMessage( );
      writeBytes.addAndGet( msg.getContent( ).readableBytes( ) );
      if ( msg.getMessage( ) != null && msg.getMessage( ) instanceof EucalyptusMessage ) {
        this.correlationId = ( ( EucalyptusMessage ) msg.getMessage( ) ).getCorrelationId( );
        this.eventUserId = ( ( EucalyptusMessage ) msg.getMessage( ) ).getUserId( );
      }
    } else if ( e.getMessage( ) instanceof ChannelBuffer ) {
      ChannelBuffer msg = ( ChannelBuffer ) e.getMessage( );
      writeBytes.addAndGet( msg.readableBytes( ) );
    }
    super.writeRequested( ctx, e );
  }
  
}
