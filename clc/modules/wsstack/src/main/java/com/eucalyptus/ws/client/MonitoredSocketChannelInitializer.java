/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
