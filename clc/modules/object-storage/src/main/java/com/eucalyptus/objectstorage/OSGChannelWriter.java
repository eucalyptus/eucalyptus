/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.context.Context;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;

public class OSGChannelWriter {

  private static Logger LOG = Logger.getLogger(OSGChannelWriter.class);

  public static void writeResponse(final Context ctx, final OSGMessageResponse response) throws InternalErrorException {
    Channel channel = ctx.getChannel();

    if (channel == null || (!channel.isConnected())) {
      throw new InternalErrorException(null, "Response: " + response + " requested, but no channel to write to.");
    }

    final HttpResponseStatus status = response.getHttpResponseStatus();
    if (status != null) {
      HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
      channel.write(httpResponse).addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) {
          // no post processing here, but for debugging
          LOG.debug("Wrote response status: " + status + " for request: " + ctx.getCorrelationId());
        }
      });
    }
    final String responseMessage = response.getHttpResponseBody();
    if (responseMessage != null) {
      channel.write(responseMessage).addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) {
          // no post processing here, but for debugging
          LOG.debug("Wrote response body: " + responseMessage + " for request: " + ctx.getCorrelationId());
        }
      });
    }
  }

  public static void writeResponse(final Channel channel, final OSGMessageResponse response) throws InternalErrorException {
    if (channel == null || (!channel.isConnected())) {
      throw new InternalErrorException(null, "Response: " + response + " requested, but no channel to write to.");
    }

    final HttpResponseStatus status = response.getHttpResponseStatus();
    if (status != null) {
      HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
      channel.write(httpResponse).addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) {
          // no post processing here, but for debugging
          LOG.debug("Wrote response status: " + status);
        }
      });
    }
    final String responseMessage = response.getHttpResponseBody();
    if (responseMessage != null) {
      channel.write(responseMessage).addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) {
          // no post processing here, but for debugging
          LOG.debug("Wrote response body: " + responseMessage);
        }
      });
    }
  }

}
