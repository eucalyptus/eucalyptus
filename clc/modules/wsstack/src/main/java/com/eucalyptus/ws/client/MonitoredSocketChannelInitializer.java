/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.ws.client;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.eucalyptus.ws.IoHandlers;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 *
 */
public abstract class MonitoredSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  protected void initChannel( final SocketChannel channel ) throws Exception {
    final ChannelPipeline pipeline = channel.pipeline( );
    addMonitoringHandlers( pipeline, MoreObjects.firstNonNull( StackConfiguration.CLIENT_INTERNAL_TIMEOUT_SECS, 60 ) );
  }

  protected void addMonitoringHandlers( final ChannelPipeline pipeline, final int timeoutSeconds ) {
    final Map<String,ChannelHandler> handlers =
        IoHandlers.channelMonitors( TimeUnit.SECONDS, timeoutSeconds );
    for ( Map.Entry<String, ChannelHandler> e : handlers.entrySet( ) ) {
      pipeline.addLast( e.getKey( ), e.getValue( ) );
    }
  }
}
