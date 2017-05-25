/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.cluster.service.ws;

import com.eucalyptus.cluster.proxy.node.ProxyNodeController;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.util.async.AsyncRequestPoolable;
import com.eucalyptus.ws.IoHandlers;
import com.eucalyptus.ws.client.MonitoredSocketChannelInitializer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 *
 */
@ComponentPart( ProxyNodeController.class )
public class NodeClientChannelInitializer extends MonitoredSocketChannelInitializer implements AsyncRequestPoolable {

  @SuppressWarnings( "Guava" )
  private static final Supplier<ChannelHandler> wsSecHandler = Suppliers.memoize( NodeWsSecHandler::new );

  @Override
  protected void initChannel( final SocketChannel socketChannel ) throws Exception {
    super.initChannel( socketChannel );
    final ChannelPipeline pipeline = socketChannel.pipeline( );
    pipeline.addLast( "decoder", IoHandlers.httpResponseDecoder( ) );
    pipeline.addLast( "aggregator", IoHandlers.newHttpChunkAggregator( ) );
    pipeline.addLast( "encoder", IoHandlers.httpRequestEncoder( ) );
    pipeline.addLast( "wrapper", IoHandlers.ioMessageWrappingHandler( ) );
    pipeline.addLast( "serializer", IoHandlers.soapMarshalling( ) );
    pipeline.addLast( "wssec", wsSecHandler.get( ) );
    pipeline.addLast( "addressing", IoHandlers.addressingHandler( "EucalyptusNC#" ) );
    pipeline.addLast( "soap", IoHandlers.soapHandler( ) );
    pipeline.addLast( "binding", IoHandlers.bindingHandler( "eucalyptus_ucsb_edu" ) );
  }
}
