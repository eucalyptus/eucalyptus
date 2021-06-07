/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataPutRequestType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;

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

      if (httpRequest.getMessage() instanceof ObjectStorageDataPutRequestType && httpRequest.isChunked()) {
        ObjectStorageDataPutRequestType putDataRequest = (ObjectStorageDataPutRequestType) httpRequest.getMessage();
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
