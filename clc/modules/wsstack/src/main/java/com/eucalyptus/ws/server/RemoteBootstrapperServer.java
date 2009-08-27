/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.util.NetworkUtil;
@Provides( resource = Resource.RemoteConfiguration ) 
@Depends( remote = Component.eucalyptus )
@ChannelPipelineCoverage("all")
public class RemoteBootstrapperServer extends Bootstrapper implements ChannelDownstreamHandler, ChannelUpstreamHandler, ChannelPipelineFactory {
  private static Logger                 LOG            = Logger.getLogger( RemoteBootstrapperServer.class );
  private int                           port;
  private ServerBootstrap               bootstrap;
  private NioServerSocketChannelFactory socketFactory;
  private int                           BOOTSTRAP_PORT = 19191;//TODO: integrate this with 8773
  private Channel                       channel;
  
  public RemoteBootstrapperServer( ) {
    this.port = BOOTSTRAP_PORT;
    this.socketFactory = new NioServerSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.bootstrap = new ServerBootstrap( this.socketFactory );
    this.bootstrap.setPipelineFactory( this );
  }

  public boolean start( ) {
    return true;
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    this.channel = this.bootstrap.bind( new InetSocketAddress( this.port ) );
    LOG.info( "Waiting for system properties before continuing bootstrap.");
    this.channel.getCloseFuture( ).awaitUninterruptibly( );
    LOG.info( "Channel closed, proceeding with bootstrap.");
    return true;
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
          this.channel.close( );          
        } else {
          ChannelFuture writeFuture = ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.NOT_ACCEPTABLE ) );
          writeFuture.addListener( ChannelFutureListener.CLOSE );          
        }
      }
    }
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = pipeline( );
    // TODO: handle security in here too.
    pipeline.addLast( "decoder", new HttpRequestDecoder( ) );
    pipeline.addLast( "encoder", new HttpResponseEncoder( ) );
    pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler( ) );
    pipeline.addLast( "handler", this );
    return pipeline;
  }

}
