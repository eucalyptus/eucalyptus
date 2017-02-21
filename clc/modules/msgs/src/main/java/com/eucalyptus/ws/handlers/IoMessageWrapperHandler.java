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
package com.eucalyptus.ws.handlers;

import com.eucalyptus.ws.IoMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 *
 */
@ChannelHandler.Sharable
public class IoMessageWrapperHandler extends ChannelDuplexHandler {

  @Override
  public void write( final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
    if ( msg instanceof IoMessage ) {
      super.write( ctx, ((IoMessage)msg).getHttpMessage( ), promise );
    } else {
      super.write( ctx, msg, promise );
    }
  }

  @Override
  public void channelRead( final ChannelHandlerContext ctx, final Object msg ) throws Exception {
    if ( msg instanceof FullHttpRequest || msg instanceof FullHttpResponse ) {
      super.channelRead( ctx, IoMessage.http( (FullHttpMessage) msg ) );
    } else {
      super.channelRead( ctx, msg );
    }
  }
}
