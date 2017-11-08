/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.XAmzContentSHA256MismatchException;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.StreamingHttpRequest;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import static com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.AWS_CONTENT_SHA_HEADER;
import static com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.AWS_V4_AUTH_TYPE;
import static com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.STREAMING_PAYLOAD;
import static com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.UNSIGNED_PAYLOAD;

import java.security.MessageDigest;

/**
 * Aggregates streaming and chunked data for V4 signed request authentication.
 */
public class ObjectStorageAuthenticationAggregator implements ChannelUpstreamHandler {
  // Content validation
  private MessageDigest contentDigest;
  private byte[] expectedDigest;

  // Streaming request state
  private MappingHttpRequest initialRequest;
  private AwsChunkStream awsChunkStream;

  @Override
  public void handleUpstream(final ChannelHandlerContext ctx, final ChannelEvent channelEvent) throws Exception {
    if (channelEvent instanceof MessageEvent) {
      handleUpstreamMessage(ctx, (MessageEvent) channelEvent);
    } else {
      ctx.sendUpstream(channelEvent);
    }
  }

  private void handleUpstreamMessage(final ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    final Object msg = event.getMessage();
    boolean lastPart = false;

    if (initialRequest == null && msg instanceof MappingHttpRequest) {
      final MappingHttpRequest request = (MappingHttpRequest) msg;
      final String authHeader = request.getHeader(SecurityParameter.Authorization.toString());
      final String shaHeader = request.getHeader(AWS_CONTENT_SHA_HEADER);

      if (STREAMING_PAYLOAD.equals(shaHeader)) {
        // Handle V4 streaming requests
        initialRequest = request;
        awsChunkStream = new AwsChunkStream();

        // Handle initial request body, if any
        StreamingHttpRequest streamingRequest = awsChunkStream.append(initialRequest);
        if (streamingRequest != null)
          event = new UpstreamMessageEvent(ctx.getChannel(), streamingRequest, event.getRemoteAddress());
        ctx.sendUpstream(event);
        return;
      } else if (!Strings.isNullOrEmpty(authHeader) && authHeader.startsWith(AWS_V4_AUTH_TYPE) && !UNSIGNED_PAYLOAD.equals(shaHeader)) {
        // Handle content validation for V4 non-streaming requests
        expectedDigest = BaseEncoding.base16().lowerCase().decode(shaHeader);
        contentDigest = Digest.SHA256.get();
        contentDigest.update(request.getContent().toByteBuffer());
        lastPart = !request.isChunked();
      }
    } else if (msg instanceof HttpChunk) {
      final HttpChunk chunk = (HttpChunk) msg;

      // Handle V4 streaming chunks
      if (initialRequest != null) {
        if (!chunk.isLast()) {
          StreamingHttpRequest streamingRequest = awsChunkStream.append(chunk);
          if (streamingRequest != null) {
            streamingRequest.setInitialRequest(initialRequest);
            ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), streamingRequest, event.getRemoteAddress()));
          }
        } else {
          ctx.sendUpstream(event);
        }

        return;
      } else if (expectedDigest != null) {
        contentDigest.update(chunk.getContent().toByteBuffer());
        lastPart = chunk.isLast();
      }
    }

    if (lastPart && !MessageDigest.isEqual(expectedDigest, contentDigest.digest())) {
      Channels.fireExceptionCaught(ctx, new XAmzContentSHA256MismatchException());
      return;
    }

    ctx.sendUpstream(event);
  }
}
