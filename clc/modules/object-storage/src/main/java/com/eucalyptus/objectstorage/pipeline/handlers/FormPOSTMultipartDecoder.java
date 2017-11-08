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

package com.eucalyptus.objectstorage.pipeline.handlers;

import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.MissingContentLengthException;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.handlers.MessageStackHandler;

/**
 * Populates the form field map in the message based on the content body Subsequent stages/handlers can use the map exclusively
 */
public class FormPOSTMultipartDecoder extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(FormPOSTMultipartDecoder.class);
  private static final int S3_FORM_BUFFER_SIZE = 25 * 1024; // 20KB buffer for S3, give a bit of room extra to be safe here
  private HttpThresholdBufferingAggregator aggregator = new HttpThresholdBufferingAggregator(S3_FORM_BUFFER_SIZE);

  @Override
  public void handleUpstream(final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent) throws Exception {
    LOG.trace(LogUtil.dumpObject(channelEvent));
    try {
      if (channelEvent instanceof MessageEvent) {
        final MessageEvent msgEvent = (MessageEvent) channelEvent;
        this.incomingMessage(channelHandlerContext, msgEvent);
      } else {
        channelHandlerContext.sendUpstream(channelEvent);
      }
    } catch (Exception e) {
      LOG.warn("Caught exception in POST form authentication.", e);
      Channels.fireExceptionCaught(channelHandlerContext, e);
    }
  }

  @Override
  public void incomingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    MappingHttpRequest upstreamRequest = null;
    ChannelBuffer content = null;
    MessageEvent upstreamEvent = event;
    if (aggregator.offer(event)) {
      HttpThresholdBufferingAggregator.AggregatedMessageEvent result = aggregator.poll();
      if (result == null) {
        return;
      } else {
        // process the aggregated result
        aggregator.hardReset(); // ensure no further polls return this result.
        if (result.getMessageEvent() == null || result.getMessageEvent().getMessage() == null) {
          throw new InternalErrorException(null, "Unexpected state in message stack");
        }
        upstreamRequest = (MappingHttpRequest) (result.getMessageEvent().getMessage());
        content = result.getAggregatedContentBuffer();
        upstreamEvent = result.getMessageEvent();
      }
    } else {
      if (event.getMessage() instanceof MappingHttpRequest) {
        upstreamRequest = (MappingHttpRequest) event.getMessage();
        content = upstreamRequest.getContent();
      }
    }

    if (upstreamRequest != null && content != null) {
      this.handleFormMessage(upstreamRequest, content);
    }
    ctx.sendUpstream(upstreamEvent);
  }

  /**
   * Parses and populates the form fields of the httpRequest based on the given content buffer. The contentBuffer may just be the httpRequest's
   * content, but the caller must set it as such
   *
   * @param httpRequest
   * @param contentBuffer
   * @throws Exception
   */
  protected void handleFormMessage(MappingHttpRequest httpRequest, ChannelBuffer contentBuffer) throws Exception {
    Map formFields = httpRequest.getFormFields();
    long contentLength;
    try {
      contentLength = Long.parseLong(httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH));
    } catch (NumberFormatException e) {
      throw new MissingContentLengthException(httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH));
    }

    // Populate the form map with the bucket, content, and field map
    formFields.put(ObjectStorageProperties.FormField.bucket.toString(), getBucketName(httpRequest));

    // add this as it's needed for filtering the body later in the pipeline.
    formFields.putAll(MultipartFormPartParser.parseForm(httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE), contentLength, contentBuffer));
  }

  protected static String getBucketName(MappingHttpRequest httpRequest) {
    // Populate the bucket field from the HOST header or uri path
    String operationPath = httpRequest.getServicePath().replaceFirst(ComponentIds.lookup(ObjectStorage.class).getServicePath().toLowerCase(), "");
    String[] target = OSGUtil.getTarget(operationPath);
    String bucket = null;
    if (target != null && target.length >= 1) {
      bucket = target[0];
    } else {
      // Look in HOST header
      bucket = httpRequest.getHeader(HttpHeaders.Names.HOST).split(".objectstorage", 2)[0];
    }
    return bucket;
  }
}
