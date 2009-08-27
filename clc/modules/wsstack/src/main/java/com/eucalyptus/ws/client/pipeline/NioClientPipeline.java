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
package com.eucalyptus.ws.client.pipeline;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;

import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.WsSecHandler;

public class NioClientPipeline implements ChannelPipelineFactory {
  private static Logger            LOG = Logger.getLogger( NioClientPipeline.class );

  private final NioResponseHandler handler;
  private BindingHandler           bindingHandler;
  private final WsSecHandler       wssecHandler;
  
  public NioClientPipeline( final NioResponseHandler handler, final String clientBinding ) {
    this( handler, clientBinding, null );
  }

  public NioClientPipeline( final NioResponseHandler handler, final String clientBinding, final WsSecHandler wssecHandler ) {
    this.handler = handler;
    // TODO: Fix wrapping of the binding
    try {
      Binding binding = BindingManager.getBinding( clientBinding );
      this.bindingHandler = new BindingHandler( binding );
    } catch ( BindingException e ) {
      LOG.error( e, e );
    }
    this.wssecHandler = wssecHandler;
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );

    pipeline.addLast( "decoder", new NioHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "encoder", new NioHttpRequestEncoder( ) );
    pipeline.addLast( "serializer", new SoapMarshallingHandler( ) );
    if ( this.wssecHandler != null ) {
      pipeline.addLast( "wssec", this.wssecHandler );
    }
    pipeline.addLast( "soap", new SoapHandler( ) );
    pipeline.addLast( "binding", bindingHandler );
    pipeline.addLast( "handler", handler );
    return pipeline;
  }
  
  public BindingHandler getBindingHandler( ) {
    return bindingHandler;
  }

  public WsSecHandler getWssecHandler( ) {
    return wssecHandler;
  }

  public NioResponseHandler getHandler( ) {
    return handler;
  }
}
