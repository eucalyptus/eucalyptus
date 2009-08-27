/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.eucalyptus.ws.util.PipelineRegistry;

public class NioServer {
  private static Logger                 LOG = Logger.getLogger( NioServer.class );
  private int                           port;
  private ServerBootstrap               bootstrap;
  private NioServerSocketChannelFactory socketFactory;
  private Channel                       serverChannel;

  public NioServer( int port ) {
    super( );
    PipelineRegistry.getInstance( ).register( new EucalyptusSoapPipeline( ) );
    PipelineRegistry.getInstance( ).register( new ElasticFoxPipeline( ) );
    PipelineRegistry.getInstance( ).register( new WalrusRESTPipeline( ) );
    PipelineRegistry.getInstance( ).register( new EucalyptusQueryPipeline( ) );
    PipelineRegistry.getInstance( ).register( new WalrusSoapPipeline( ) );
    PipelineRegistry.getInstance( ).register( new HeartbeatPipeline( ) );
    RemoteBootstrapperServer server = RemoteBootstrapperServer.getServer( );
    if ( server != null && port == server.getPort( ) ) {
      LOG.info( "Swapping over to full webservices stack." );
      this.port = server.getPort( );
      this.socketFactory = server.getSocketFactory( );
      this.bootstrap = server.getBootstrap( );
      this.bootstrap.setPipelineFactory( new NioServerPipelineFactory( ) );
    } else {
      this.port = port;
      this.socketFactory = new NioServerSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
      this.bootstrap = new ServerBootstrap( this.socketFactory );
      this.bootstrap.setPipelineFactory( new NioServerPipelineFactory( ) );
    }
  }

  public void start( ) {
    this.serverChannel = this.bootstrap.bind( new InetSocketAddress( this.port ) );
  }

}
