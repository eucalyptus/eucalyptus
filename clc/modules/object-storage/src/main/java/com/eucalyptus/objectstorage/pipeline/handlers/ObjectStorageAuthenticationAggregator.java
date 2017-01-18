/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.OSGChannelWriter;
import com.eucalyptus.objectstorage.OSGMessageResponse;
import com.eucalyptus.objectstorage.exceptions.s3.MaxMessageLengthExceededException;
import com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.StreamingHttpRequest;
import com.google.common.base.Strings;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import static com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.AWS_CONTENT_SHA_HEADER;

/**
 * Aggregates streaming and chunked data for V4 signed request authentication.
 */
public class ObjectStorageAuthenticationAggregator extends HttpChunkAggregator {
  private final int maxContentLength;

  // Http chunking required
  private boolean httpChunked;

  // Streaming request state
  private MappingHttpRequest initialRequest;
  private AwsChunkStream awsChunkStream;

  public ObjectStorageAuthenticationAggregator(int maxContentLength) {
    super(maxContentLength);
    this.maxContentLength = maxContentLength;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    Object msg = event.getMessage();

    if (initialRequest == null && msg instanceof MappingHttpRequest) {
      MappingHttpRequest request = (MappingHttpRequest) msg;
      String authHeader = request.getHeader(SecurityParameter.Authorization.toString());

      // Handle V4 streaming requests
      if (S3V4Authentication.STREAMING_PAYLOAD.equals(request.getHeader(AWS_CONTENT_SHA_HEADER))) {
        initialRequest = request;
        awsChunkStream = new AwsChunkStream();
        ctx.sendUpstream(event);
        return;
      } else {
        // Handle V4 continuable non-streaming requests
        if (!Strings.isNullOrEmpty(authHeader) && authHeader.startsWith(S3V4Authentication.AWS_V4_AUTH_TYPE) && HttpHeaders
            .is100ContinueExpected(request)) {
          httpChunked = true;
          long contentLength = HttpHeaders.getContentLength(request);
          if (contentLength > maxContentLength) {
            Channels.fireExceptionCaught(ctx, new MaxMessageLengthExceededException(null, "Max request content length exceeded."));
            return;
          }

          // Clear expect header and send continue
          HttpHeaders.set100ContinueExpected(request, false);
          OSGChannelWriter.writeResponse(ctx.getChannel(), OSGMessageResponse.Continue);
        }
      }
    } else {
      // Handle V4 streaming chunks
      if (initialRequest != null && msg instanceof HttpChunk) {
        HttpChunk chunk = (HttpChunk) event.getMessage();
        StreamingHttpRequest streamingRequest;
        if (!chunk.isLast()) {
          streamingRequest = awsChunkStream.append(chunk);
          if (streamingRequest != null) {
            streamingRequest.setInitialRequest(initialRequest);
            ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), streamingRequest, event.getRemoteAddress()));
          }
        } else {
          ctx.sendUpstream(event);
        }

        return;
      }
    }

    // Aggregate continuable requests
    if (httpChunked) {
      super.messageReceived(ctx, event);
    } else
      ctx.sendUpstream(event);
  }
}
