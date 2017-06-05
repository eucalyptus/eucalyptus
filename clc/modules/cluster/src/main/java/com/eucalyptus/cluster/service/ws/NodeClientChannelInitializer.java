/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cluster.service.ws;

import com.eucalyptus.cluster.proxy.node.ProxyNodeController;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.util.async.AsyncRequestPoolable;
import com.eucalyptus.ws.IoHandlers;
import com.eucalyptus.ws.client.MonitoredSocketChannelInitializer;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.primitives.Ints;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 *
 */
@ComponentPart( ProxyNodeController.class )
public class NodeClientChannelInitializer extends MonitoredSocketChannelInitializer implements AsyncRequestPoolable {

  private static final String POOL_SIZE_PROP = "com.eucalyptus.cluster.nodeHttpPoolSize";
  private static final int POOL_SIZE_DEFAULT = 1; // should be enough for anything
  private static int POOL_SIZE =
      MoreObjects.firstNonNull( Ints.tryParse( System.getProperty( POOL_SIZE_PROP, "" ) ), POOL_SIZE_DEFAULT );

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

  @Override
  public int fixedSize( ) {
    return POOL_SIZE;
  }
}
