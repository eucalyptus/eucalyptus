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

package com.eucalyptus.objectstorage.pipeline.binding;

import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpMethod;

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataPutRequestType;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Maps;

public class ObjectStorageFormPOSTBinding extends ObjectStorageRESTBinding {
  private static Logger LOG = Logger.getLogger(ObjectStorageFormPOSTBinding.class);

  private static final Map<String, String> SUPPORTED_OPS = Maps.newHashMap();
  static {
    SUPPORTED_OPS.put(BUCKET + HttpMethod.POST.toString(), "PostObject");
  }

  private static final Map<String, String> UNSUPPORTED_OPS = Maps.newHashMap();
  static {
  }

  @Override
  protected Map<String, String> populateOperationMap() {
    return SUPPORTED_OPS;
  }

  @Override
  protected Map<String, String> populateUnsupportedOperationMap() {
    return UNSUPPORTED_OPS;
  }

  @Override
  public void handleUpstream(final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent) throws Exception {
    LOG.trace(LogUtil.dumpObject(channelEvent));
    UpstreamMessageEvent firstChunkEvent = null;
    DefaultHttpChunk firstChunk = null;
    if (channelEvent instanceof MessageEvent) {
      // Grab the chunk data from the form field if it is found.
      final MessageEvent msgEvent = (MessageEvent) channelEvent;
      try {
        if (msgEvent.getMessage() instanceof MappingHttpRequest) {
          // Get first chunk data here
          MappingHttpRequest request = (MappingHttpRequest) msgEvent.getMessage();
          if (request.getFormFields() != null
              && request.getFormFields().get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString()) != null) {
            firstChunk = new DefaultHttpChunk(
                (ChannelBuffer) request.getFormFields().get(ObjectStorageProperties.FormField.x_ignore_firstdatachunk.toString()));
            if (request.isChunked()) {
              firstChunkEvent = new UpstreamMessageEvent(channelHandlerContext.getChannel(), firstChunk, msgEvent.getRemoteAddress());
            }
          }

        }
        // Do the binding
        this.incomingMessage(channelHandlerContext, msgEvent);

        // Handle the first data chunk properly
        if (firstChunkEvent == null && firstChunk != null && msgEvent.getMessage() instanceof MappingHttpRequest
            && ((MappingHttpRequest) msgEvent.getMessage()).getMessage() instanceof ObjectStorageDataPutRequestType ) {
          ObjectStorageDataPutRequestType dataReq = (ObjectStorageDataPutRequestType) ((MappingHttpRequest) msgEvent.getMessage()).getMessage();
          handleData(channelHandlerContext.getChannel(), dataReq, firstChunk.getContent());
        }
      } catch (Exception e) {
        LOG.error("Error in POST multipart form binding", e);
        Channels.fireExceptionCaught(channelHandlerContext, e);
        return;
      }
    }

    // Send the bound message up
    channelHandlerContext.sendUpstream(channelEvent);

    // Follow immediately with first data chunk
    if (firstChunkEvent != null) {
      LOG.trace("Dispatching follow-up chunk directly after initial request. Size: " + firstChunk.getContent().readableBytes());
      channelHandlerContext.sendUpstream(firstChunkEvent);
    }
  }
}
