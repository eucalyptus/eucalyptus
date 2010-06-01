package com.eucalyptus.cluster.callback;

import java.net.InetSocketAddress;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.util.ChannelUtil;
import edu.ucsb.eucalyptus.msgs.GetLogsResponseType;
import edu.ucsb.eucalyptus.msgs.GetLogsType;

public class LogDataCallback extends QueuedEventCallback<GetLogsType, GetLogsResponseType> {
  private static Logger LOG = Logger.getLogger( LogDataCallback.class );
  private String node;
  
  public LogDataCallback( ) {
    this( "self" );
  }
  
  public LogDataCallback( String node ) {
    this.node = node;
    super.setRequest( new GetLogsType( node ) );
  }
  
  
  
  @Override
  public void prepare( GetLogsType msg ) throws Exception {}
  
  @Override
  public void fail( Throwable t ) {
    LOG.error( t, t );
  }

  @Override
  public void verify( GetLogsResponseType msg ) throws Exception {
  }
  
  public void fire( final String hostname, final int port, final String servicePath ) {
    try {
      NioClientPipeline clientPipeline = new LogClientPipeline( this );
      super.clientBootstrap = ChannelUtil.getClientBootstrap( clientPipeline );
      InetSocketAddress addr = new InetSocketAddress( hostname, port );
      this.connectFuture = this.clientBootstrap.connect( addr );
      String glServicePath = servicePath.replaceAll("EucalyptusCC","EucalyptusGL");
      LOG.debug( "Using GL service path: " + glServicePath );
      HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, addr.getHostName( ), addr.getPort( ), glServicePath, this.getRequest( ) );
      this.prepare( this.getRequest( ) );
      this.connectFuture.addListener( ChannelUtil.WRITE( request ) );
    } catch ( Throwable e ) {
      try {
        this.fail( e );
      } catch ( Exception e1 ) {
        LOG.debug( e1, e1 );
      }
      this.failCallback.failure( this, e );
      super.queueResponse( e );
      this.connectFuture.addListener( ChannelFutureListener.CLOSE );
    }
  }

  
}
