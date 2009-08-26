package com.eucalyptus.ws.client;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;

@Provides(resource=Resource.RemoteConfiguration)
@Depends(resources=Resource.Database,local=Component.eucalyptus)
public class RemoteBootstrapperClient extends Bootstrapper implements Runnable, ChannelPipelineFactory {
  private static Logger     LOG = Logger.getLogger( RemoteBootstrapperClient.class );

  private NioBootstrap      clientBootstrap;
  private ChannelFactory    channelFactory;
  private NioClientPipeline clientPipeline;

  public RemoteBootstrapperClient( ) {
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.clientBootstrap = new NioBootstrap( channelFactory );
    this.clientBootstrap.setPipelineFactory( this );
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );
    pipeline.addLast( "decoder", new HttpResponseDecoder( ) );
    pipeline.addLast( "encoder", new HttpRequestEncoder( ) );
    pipeline.addLast( "heartbeat", new HeartbeatHandler( ) );
    return pipeline;
  }
  
  @ChannelPipelineCoverage("one")
  class HeartbeatHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {

    @Override
    public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if( e instanceof ExceptionEvent ) {
        LOG.info( ((ExceptionEvent)e ).getCause( ) );
        ctx.getChannel( ).close( );
      } else {
        LOG.info( "Sending downstream: " + e );
        ctx.sendUpstream( e );
      }
    }

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      if( e instanceof ExceptionEvent ) {
        LOG.info( ((ExceptionEvent)e ).getCause( ) );
        ctx.getChannel( ).close( );
      } else {
        LOG.info( "Sending upstream: " + e );
        ctx.sendUpstream( e );
      }
    }
    
  }

  class HeartbeatClient {
    private String            hostname;
    private int               port;
    private String            servicePath;
    private InetSocketAddress remoteAddr;
    private ChannelFuture     channelOpenFuture;
    private ChannelFuture     channelWriteFuture;
    private Channel           channel;

    public HeartbeatClient( String hostname, int port, String servicePath ) {
      this.remoteAddr = new InetSocketAddress( hostname, port );
      this.servicePath = servicePath;
    }
    public void write( final HttpRequest httpRequest ) throws Exception {
      if ( this.channel == null || !this.channel.isOpen( ) || !this.channel.isConnected( ) ) {
        this.channelOpenFuture = RemoteBootstrapperClient.this.clientBootstrap.connect( this.remoteAddr );
        this.channelOpenFuture.addListener( new ChannelFutureListener( ) {          
          @Override
          public void operationComplete( ChannelFuture channelFuture ) throws Exception {
            if ( channelFuture.isSuccess( ) ) {
              channel = channelFuture.getChannel( );
              channelWriteFuture = channelFuture.getChannel( ).write( httpRequest );
            } else {
              if( channelFuture != null ) {
                LOG.error( channelFuture.getCause( ), channelFuture.getCause( ) );
              }
              channel.close( );
            }
          }
        } );
      }
    }
  }


  @Override
  public boolean load( Resource current ) throws Exception {
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    (new Thread( new RemoteBootstrapperClient())).start();
    return true;
  }

  @Override
  public void run( ) {
    while(true){
      try {
        List<WalrusConfiguration> walrusConfigs = Configuration.getWalrusConfigurations( );
        for( WalrusConfiguration w : walrusConfigs ) {
          LOG.info( "Sending configuration info to walrus at: " + w.getHostName( ) );
          HeartbeatClient hb = new HeartbeatClient( w.getHostName( ), 19191, "/services/Heartbeat" );
          String properties = "euca.db.host=192.168.7.7\n" + 
          "euca.db.password=\n" + 
          "euca.db.port=9001\n";
          ChannelBuffer buffer = ChannelBuffers.copiedBuffer( properties.getBytes( ) );
          HttpRequest httpRequest = new DefaultHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, "/services/Heartbeat" );
          httpRequest.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
//          httpRequest.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
          httpRequest.setContent( buffer );
          try {
            hb.write( httpRequest );
          } catch ( Exception e ) {
            LOG.error( e );
          }
        }
      } catch ( EucalyptusCloudException e1 ) {
        LOG.error( e1,e1 );
      }
      try {
        Thread.sleep(5000);
      } catch ( InterruptedException e ) {}
    }
  }

}
