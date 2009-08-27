package com.eucalyptus.ws.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.stages.UnrollableStage;

@ChannelPipelineCoverage("one")
public class HeartbeatHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler, UnrollableStage {
  private static Logger LOG = Logger.getLogger( HeartbeatHandler.class );
  private Channel channel;
  private static boolean initialized = false;
  
  
  public HeartbeatHandler( ) {
    super( );
    initialized = true;
  }

  public HeartbeatHandler( Channel channel ) {
    super( );
    this.channel = channel;
  }

  @Override
  public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    ctx.sendDownstream( e );
  }

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      Object message = ( ( MessageEvent ) e ).getMessage( );
      if ( message instanceof HttpRequest ) {
        HttpRequest request = ( ( HttpRequest ) message );
        if( !initialized ) {
          initialize( ctx, request );
        } else {
          MappingHttpResponse response = new MappingHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK );
          String resp = "";
          for( Component c : Component.values( ) ) {
            resp += String.format( "name=%-20.20s enabled=%-10.10s local=%-10.10s\n", c.name( ), c.isEnabled( ), c.isLocal( ) );
          }
          ChannelBuffer buf = ChannelBuffers.copiedBuffer( resp.getBytes( ) );
          response.setContent( buf );
          response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buf.readableBytes( ) ) );
          response.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
          ChannelFuture writeFuture = ctx.getChannel( ).write( response );
          writeFuture.addListener( ChannelFutureListener.CLOSE );          
        }
      }
    }
  }

  private void initialize( ChannelHandlerContext ctx, HttpRequest request ) throws IOException, SocketException {
    ByteArrayInputStream bis = new ByteArrayInputStream( request.getContent( ).toByteBuffer( ).array( ) );
    Properties props = new Properties( );
    props.load( bis );
    boolean foundDb = false;
    List<String> localAddrs = NetworkUtil.getAllAddresses( );
    for ( Entry<Object, Object> entry : props.entrySet( ) ) {
      String key = (String)entry.getKey();
      String value = (String)entry.getValue();
      if( key.startsWith("euca.db.host") ) {
        try {
          if( NetworkUtil.testReachability( value ) && !localAddrs.contains( value )) {
            LOG.info( "Found candidate db host address: " + value );
            String oldValue = System.setProperty( "euca.db.host", value );
            LOG.info( "Setting property: euca.db.host=" + value + " [oldvalue="+oldValue+"]" );              
            Component.db.setHostAddress( value );
            //TODO: test we can connect here.
            foundDb = true;
          }
        } catch ( Exception e1 ) {
          LOG.warn( "Ignoring proposed database address: " + value );
        }
      } else {
        String oldValue = System.setProperty( ( String ) entry.getKey( ), ( String ) entry.getValue( ) );
        LOG.info( "Setting property: " + entry.getKey( ) + "=" + entry.getValue( ) + " [oldvalue="+oldValue+"]" );
      }
    }
    if( foundDb ) {
      ChannelFuture writeFuture = ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK ) );
      writeFuture.addListener( ChannelFutureListener.CLOSE );
      if( this.channel != null ) {
        this.channel.close();
      }
    } else {
      ChannelFuture writeFuture = ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.NOT_ACCEPTABLE ) );
      writeFuture.addListener( ChannelFutureListener.CLOSE );          
    }
  }

  @Override
  public String getStageName( ) {
    return "heartbeat";
  }

  @Override
  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "heartbeat", new HeartbeatHandler( ) );
  }

}
