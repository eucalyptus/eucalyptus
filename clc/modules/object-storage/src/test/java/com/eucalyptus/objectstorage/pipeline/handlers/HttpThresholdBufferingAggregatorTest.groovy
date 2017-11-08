/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.pipeline.handlers

import com.eucalyptus.http.MappingHttpRequest
import groovy.transform.CompileStatic
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFactory
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.UpstreamMessageEvent
import org.jboss.netty.channel.socket.ServerSocketChannel
import org.jboss.netty.channel.socket.ServerSocketChannelConfig
import org.jboss.netty.handler.codec.http.DefaultHttpChunk
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpVersion
import org.junit.Test

/**
 * Created by zhill on 4/4/14.
 */
@CompileStatic
class HttpThresholdBufferingAggregatorTest {
  protected static MappingHttpRequest getInitialRequest(byte[] content, int contentLength, boolean chunked) {
    MappingHttpRequest msg = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "http://objectstorage")
    msg.setHeader(HttpHeaders.Names.CONTENT_LENGTH, contentLength)
    msg.setContent(ChannelBuffers.wrappedBuffer(content))
    msg.setChunked(chunked)
    return msg;
  }

  protected static DefaultHttpChunk getDataChunk(byte[] data) {
    DefaultHttpChunk msg = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(data))
    return msg
  }

  protected static DefaultHttpChunk getLastChunk() {
    //Zero length content to make it the 'last'
    DefaultHttpChunk msg = new DefaultHttpChunk(ChannelBuffers.wrappedBuffer(new byte[0]));
    return msg
  }

  static Channel channel = new ServerSocketChannel() {
    @Override
    ServerSocketChannelConfig getConfig() {
      return null
    }

    @Override
    InetSocketAddress getLocalAddress() {
      return null
    }

    @Override
    InetSocketAddress getRemoteAddress() {
      return null
    }

    @Override
    Integer getId() {
      return null
    }

    @Override
    ChannelFactory getFactory() {
      return null
    }

    @Override
    Channel getParent() {
      return null
    }

    @Override
    ChannelPipeline getPipeline() {
      return null
    }

    @Override
    boolean isOpen() {
      return false
    }

    @Override
    boolean isBound() {
      return false
    }

    @Override
    boolean isConnected() {
      return false
    }

    @Override
    ChannelFuture write(Object o) {
      return null
    }

    @Override
    ChannelFuture write(Object o, SocketAddress socketAddress) {
      return null
    }

    @Override
    ChannelFuture bind(SocketAddress socketAddress) {
      return null
    }

    @Override
    ChannelFuture connect(SocketAddress socketAddress) {
      return null
    }

    @Override
    ChannelFuture disconnect() {
      return null
    }

    @Override
    ChannelFuture unbind() {
      return null
    }

    @Override
    ChannelFuture close() {
      return null
    }

    @Override
    ChannelFuture getCloseFuture() {
      return null
    }

    @Override
    int getInterestOps() {
      return 0
    }

    @Override
    boolean isReadable() {
      return false
    }

    @Override
    boolean isWritable() {
      return false
    }

    @Override
    ChannelFuture setInterestOps(int i) {
      return null
    }

    @Override
    ChannelFuture setReadable(boolean b) {
      return null
    }

    @Override
    Object getAttachment() {
      return null
    }

    @Override
    void setAttachment(Object o) {
    }

    @Override
    int compareTo(Channel channel) {
      return 0
    }
  }

  @Test
  void testAggregationSimple() {
    int size = 10
    HttpThresholdBufferingAggregator aggregator = new HttpThresholdBufferingAggregator(size)
    byte[] content = "".getBytes("UTF-8")
    byte[] chunkContent = "012345678901234567890".getBytes("UTF-8")
    MappingHttpRequest request = getInitialRequest(content, content.length + chunkContent.length, true)
    assert(request.isChunked())
    UpstreamMessageEvent initialEvent = new UpstreamMessageEvent(channel, request , new InetSocketAddress(8773))
    assert(aggregator.offer(initialEvent))
    assert(aggregator.poll() == null)

    UpstreamMessageEvent chunkEvent = new UpstreamMessageEvent(channel, getDataChunk(chunkContent), new InetSocketAddress(8773))
    assert(aggregator.offer(chunkEvent))
    assert(aggregator.poll() != null)
    HttpThresholdBufferingAggregator.AggregatedMessageEvent output = aggregator.poll()
    assert(output.getAggregationCount() == 2)
    assert(output.getCurrentAggregatedSize() == content.length + chunkContent.length)
    ChannelBuffer testbuffer = ChannelBuffers.buffer(content.length + chunkContent.length)
    testbuffer.writeBytes(content)
    testbuffer.writeBytes(chunkContent)
    assert(output.getAggregatedContentBuffer().array() == testbuffer.array())
    assert(output.getMessageEvent() == initialEvent)
  }

  @Test
  void testAggregationNonChunkedAboveThreshold() {
    int size = 10
    HttpThresholdBufferingAggregator aggregator = new HttpThresholdBufferingAggregator(size)
    byte[] content = "blahblahblahblahblahblahblahblahblahblahblah".getBytes("UTF-8")
    MappingHttpRequest request = getInitialRequest(content, content.length, false)
    assert(!request.isChunked())
    UpstreamMessageEvent initialEvent = new UpstreamMessageEvent(channel, request , new InetSocketAddress(8773))
    assert(!aggregator.offer(initialEvent))
    assert(request.getContent().array() == content)
  }

  @Test
  void testAggregationNonChunkedBelowThreshold() {
    int size = 10
    HttpThresholdBufferingAggregator aggregator = new HttpThresholdBufferingAggregator(size)
    byte[] content = "blahblah".getBytes("UTF-8") //something < 10 bytes
    MappingHttpRequest request = getInitialRequest(content, content.length, false)
    assert(!request.isChunked())
    UpstreamMessageEvent initialEvent = new UpstreamMessageEvent(channel, request , new InetSocketAddress(8773))
    assert(!aggregator.offer(initialEvent))
    assert(request.getContent().array() == content)

  }

  @Test
  void testAggregationWithLastChunkBeforeThreshold() {
    int size = 1000
    HttpThresholdBufferingAggregator aggregator = new HttpThresholdBufferingAggregator(size)
    byte[] content = "".getBytes("UTF-8") //no content, to simulate the POST chunking
    byte[] chunkContent = "012345678901234567890".getBytes("UTF-8")
    MappingHttpRequest request = getInitialRequest(content, content.length + chunkContent.length, true)
    assert(request.isChunked())
    UpstreamMessageEvent initialEvent = new UpstreamMessageEvent(channel, request , new InetSocketAddress(8773))
    assert(aggregator.offer(initialEvent))
    assert(aggregator.poll() == null)

    UpstreamMessageEvent chunkEvent = new UpstreamMessageEvent(channel, getDataChunk(chunkContent), new InetSocketAddress(8773))
    assert(aggregator.offer(chunkEvent))
    assert(aggregator.poll() == null)

    DefaultHttpChunk chunk = getLastChunk()
    assert(chunk.isLast())
    chunkEvent = new UpstreamMessageEvent(channel, chunk, new InetSocketAddress(8773))
    assert(aggregator.offer(chunkEvent))
    assert(aggregator.poll() != null)
    HttpThresholdBufferingAggregator.AggregatedMessageEvent output = aggregator.poll()
    assert(output.getAggregationCount() == 3)
    assert(output.getCurrentAggregatedSize() == content.length + chunkContent.length)
    assert(output.getMessageEvent() == initialEvent)
    ChannelBuffer testbuffer = ChannelBuffers.buffer(content.length + chunkContent.length)
    testbuffer.writeBytes(content)
    testbuffer.writeBytes(chunkContent)
    assert(output.getAggregatedContentBuffer().array() == testbuffer.array())


  }
}
