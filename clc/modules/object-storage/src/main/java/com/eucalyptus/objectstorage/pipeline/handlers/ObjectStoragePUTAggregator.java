/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2012 The Netty Project
 *
 *   The Netty Project licenses this file to you under the Apache License,
 *   version 2.0 (the "License"); you may not use this file except in
 *   compliance with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *   or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMessage;

/**
 * Aggregates chunks into a single channelbuffer, which is expected to be drained at the end of the pipeline by the service
 * operation that is reading the data and pushing it out somewhere.
 */
public class ObjectStoragePUTAggregator extends SimpleChannelUpstreamHandler {
  private ChannelBufferStreamingInputStream inputStream;

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpRequest) {
      MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();

      if (httpRequest.getMessage() instanceof ObjectStorageDataRequestType && httpRequest.isChunked()) {
        ObjectStorageDataRequestType putDataRequest = (ObjectStorageDataRequestType) httpRequest.getMessage();
        inputStream = putDataRequest.getData();
      }
    } else if (event.getMessage() instanceof HttpChunk) {
      HttpChunk chunk = (HttpChunk) event.getMessage();
      appendChunk(chunk.getContent(), ctx.getChannel());
    }

    ctx.sendUpstream(event);
  }

  protected void appendChunk(ChannelBuffer input, Channel channel) throws Exception {
    Assert.state(inputStream != null, "Received an HttpChunk without an HttpMessage");

    try {
      // Write the content into the buffer
      Logs.extreme().debug("Writing content data to stream for channel: " + channel.getId() + " Content length: " + input.readableBytes());
      inputStream.putChunk(input);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
