package com.eucalyptus.ws.client;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.MappingHttpRequest;

import edu.ucsb.eucalyptus.msgs.HeartbeatComponentType;
import edu.ucsb.eucalyptus.msgs.HeartbeatType;


public class HeartBeatClient {
  private static Logger LOG = Logger.getLogger( HeartBeatClient.class );
  private InetSocketAddress remoteAddr;
  private ChannelFuture     channelOpenFuture;
  private ChannelFuture     channelWriteFuture;
  private Channel           channel;
  private NioBootstrap      clientBootstrap;
  private String hostName;
  private int port;
  public HeartBeatClient( NioBootstrap clientBootstrap, String hostName, int port ) {
    this.remoteAddr = new InetSocketAddress( hostName, port );
    this.clientBootstrap = clientBootstrap;
    this.hostName = hostName;
    this.port = port;
  }

  
  
  public void send( Collection<ComponentConfiguration> componentConfigurations ) {
    try {
      HeartbeatType hbmsg = new HeartbeatType( );
      for( ComponentConfiguration c : componentConfigurations ) {
        hbmsg.getComponents( ).add( new HeartbeatComponentType( c.getComponent( ).name( ), c.getName( ) ) );
      }
      MappingHttpRequest httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.getHostName( ), this.getPort(), "/services/Heartbeat", hbmsg );
      this.write( httpRequest );
    } catch ( Exception e ) {
      LOG.error( "Error sending configuration to " + LogUtil.dumpObject( componentConfigurations ) );
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
    return this.hostName;
  }



  public int getPort( ) {
    return port;
  }
}
