package com.eucalyptus.cluster.callback;

import java.net.InetSocketAddress;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.util.ChannelUtil;
import edu.ucsb.eucalyptus.cloud.NodeInfo;
import edu.ucsb.eucalyptus.msgs.GetLogsResponseType;
import edu.ucsb.eucalyptus.msgs.GetLogsType;

public class LogDataCallback extends QueuedEventCallback<GetLogsType, GetLogsResponseType> {
  private static Logger LOG = Logger.getLogger( LogDataCallback.class );
  private final NodeInfo node;
  private final Cluster cluster;
  private boolean self = false;
  public LogDataCallback( Cluster cluster, NodeInfo node ) {
    this.node = node;
    this.cluster = cluster;
    this.self = (null == node);
    if( self ) {
      this.setRequest( new GetLogsType( "self" ) );
    } else {
      this.setRequest( new GetLogsType( node.getServiceTag( ) ) );
    }
  }
    
  @Override
  public void prepare( GetLogsType msg ) throws Exception {}
  
  @Override
  public void fail( Throwable t ) {
    LOG.error( t, t );
  }

  @Override
  public void verify( GetLogsResponseType msg ) throws Exception {
    if( msg == null || msg.getLogs( ) == null ) {
      throw new EucalyptusClusterException( "Failed to get log data from cluster." );
    } else {
      String log = "";
      if( self ) {
        cluster.setLastLog( msg.getLogs( ) );
        try {
          log = new String( Base64.decode( msg.getLogs( ).getCcLog( ) ) ).replaceFirst(".*\b","").substring( 0, 1000 );
        } catch ( Throwable e ) {
          LOG.debug( e, e );
        }
      } else {
        node.setLogs( msg.getLogs( ) );
        try {
          log = new String( Base64.decode( msg.getLogs( ).getNcLog( ) ) ).replaceFirst(".*\b","").substring( 0, 1000 );
        } catch ( Throwable e ) {
          LOG.debug( e, e );
        }        
      }
      LOG.debug( "LOG: " + log );
    }
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
      this.queueResponse( e );
      this.connectFuture.addListener( ChannelFutureListener.CLOSE );
    }
  }

  @Override
  public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws EucalyptusClusterException {
    LOG.debug( "LOG: Got response." );
    super.messageReceived( ctx, e );
  }

  
}
