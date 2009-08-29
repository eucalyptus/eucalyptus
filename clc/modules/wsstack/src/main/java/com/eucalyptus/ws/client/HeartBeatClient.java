package com.eucalyptus.ws.client;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.util.HeartBeatUtil;


public class HeartBeatClient {
  private static Logger LOG = Logger.getLogger( HeartBeatClient.class );
  private InetSocketAddress remoteAddr;
  private ChannelFuture     channelOpenFuture;
  private ChannelFuture     channelWriteFuture;
  private Channel           channel;
  private NioBootstrap      clientBootstrap;
  
  public HeartBeatClient( NioBootstrap clientBootstrap, String hostName, int port ) {
    this.remoteAddr = new InetSocketAddress( hostName, port );
    this.clientBootstrap = clientBootstrap;
  }

  
  
  public void send( ComponentConfiguration componentConfiguration ) {
    ChannelBuffer buffer;
    try {
      buffer = HeartBeatUtil.getConfigurationBuffer( componentConfiguration );
      HttpRequest httpRequest = new DefaultHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, "/services/Heartbeat" );
      httpRequest.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpRequest.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
      httpRequest.setContent( buffer );
      this.write( httpRequest );
    } catch ( Exception e ) {
      LOG.error( "Error sending configuration to " + LogUtil.dumpObject( componentConfiguration ) );
    }
  }
  
  public void write( final HttpRequest httpRequest ) throws Exception {
    if ( this.channel == null || !this.channel.isOpen( ) || !this.channel.isConnected( ) ) {
      this.channelOpenFuture = clientBootstrap.connect( this.remoteAddr );
      this.channelOpenFuture.addListener( new DeferedWriter( httpRequest ) ); 
    } else {
      channelWriteFuture = this.channel.write( httpRequest );
      channelWriteFuture.addListener( ChannelFutureListener.CLOSE );
    }
  }
  class DeferedWriter implements ChannelFutureListener {
    private HttpRequest httpRequest;
    public DeferedWriter( HttpRequest httpRequest ) {
      this.httpRequest = httpRequest;
    }
    @Override
    public void operationComplete( ChannelFuture channelFuture ) throws Exception {
      if ( channelFuture.isSuccess( ) ) {
        channel = channelFuture.getChannel( );
        channelWriteFuture = channelFuture.getChannel( ).write( httpRequest );
        channelWriteFuture.addListener( ChannelFutureListener.CLOSE );
        LOG.debug( "Sending configuration info to: " + channel.getRemoteAddress( ) );
        LOG.info( "Greetings to: " + channel.getRemoteAddress( ) );
      } else {
        if( channelFuture != null ) {
          LOG.warn( "Failed to connect to heartbeat service at " + remoteAddr + ": " + channelFuture.getCause( ).getMessage( ) );
        }
        if( channel != null ) {
          channel.close( );            
        }
      }
    }
  }
  public void close( ) {
    if( this.channel != null ) {
      this.channel.close( );      
    }
  }
  public final String getHostName( ) {
    return remoteAddr.getHostName( );
  }
}
