package com.eucalyptus.ws.handlers;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.records.EventType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.records.EventRecord;

@ChannelPipelineCoverage( "one" )
public class ChannelStateMonitor extends SimpleChannelHandler {
  private static Logger    LOG           = Logger.getLogger( ChannelStateMonitor.class );
  private final AtomicLong readBytes     = new AtomicLong( );
  private final AtomicLong writeBytes    = new AtomicLong( );
  private AtomicLong             openTime      = new AtomicLong( );
  private String           eventUserId   = "unknown";
  private String           correlationId = "unknown";
  
  public void split( ChannelHandlerContext ctx ) {
    Long rb = readBytes.getAndSet( 0l );
    Long wb = writeBytes.getAndSet( 0l );
    Long roundTime = ( System.currentTimeMillis( ) - this.openTime.getAndSet( 0 ) );
    LOG.trace( EventRecord.here( ctx.getPipeline( ).getLast( ).getClass( ), EventType.SOCKET_CLOSE, ""+roundTime.toString( ), ""+ctx.getChannel( ).getLocalAddress( ), ""+ctx.getChannel( ).getRemoteAddress( ) ) ); 
    if( rb != null ) {
      LOG.trace( EventRecord.here( ctx.getPipeline( ).getLast( ).getClass( ), EventType.SOCKET_BYTES_READ, ""+rb, Float.toString( ( wb * 1024.0f ) / ( roundTime * 1024.0f ) ) ) );
    }
    if( wb != null ) {
      LOG.trace( EventRecord.here( ctx.getPipeline( ).getLast( ).getClass( ), EventType.SOCKET_BYTES_WRITE, ""+wb, Float.toString( ( wb * 1024.0f ) / ( roundTime * 1024.0f ) ) ) );
    }
  }
  
  @Override
  public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    this.split( ctx );
    super.channelClosed( ctx, e );
  }
  
  @Override
  public void channelConnected( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    openTime.getAndSet( System.currentTimeMillis( ) );
    EventRecord.here( ctx.getPipeline( ).getLast( ).getClass( ), EventType.SOCKET_OPEN, ctx.getChannel( ).getLocalAddress( ).toString( ), ctx.getChannel( ).getRemoteAddress( ).toString( ) ).trace( );
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
      if ( msg.getMessage( ) != null && msg.getMessage( ) instanceof BaseMessage ) {
        this.correlationId = ( ( BaseMessage ) msg.getMessage( ) ).getCorrelationId( );
        this.eventUserId = ( ( BaseMessage ) msg.getMessage( ) ).getUserId( );
      }
    } else if ( e.getMessage( ) instanceof ChannelBuffer ) {
      ChannelBuffer msg = ( ChannelBuffer ) e.getMessage( );
      writeBytes.addAndGet( msg.readableBytes( ) );
    }
    super.writeRequested( ctx, e );
  }
  
}
