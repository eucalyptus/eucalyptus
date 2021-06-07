/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

import java.util.Date;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ObjectStorageHEADOutboundHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(ObjectStorageHEADOutboundHandler.class);

  @Override
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpResponse) {
      MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
      BaseMessage msg = (BaseMessage) httpResponse.getMessage();
      httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));
      httpResponse.setHeader(ObjectStorageProperties.AMZ_REQUEST_ID, msg.getCorrelationId());
      if (msg instanceof ObjectStorageDataResponseType) {
        ObjectStorageDataResponseType headResponse = (ObjectStorageDataResponseType) msg;

        httpResponse.setHeader(HttpHeaders.Names.ETAG, "\"" + headResponse.getEtag() + "\""); // etag in quotes, per s3-spec.

        // Fix for euca-9081
        String contentType = headResponse.getContentType();
        httpResponse.addHeader(HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream");

        String contentLength = String.valueOf(headResponse.getSize());
        if (!Strings.isNullOrEmpty(contentLength)) {
          httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, contentLength);
        }

        if (!Strings.isNullOrEmpty(headResponse.getVersionId())
            && !ObjectStorageProperties.NULL_VERSION_ID.equals(((ObjectStorageDataResponseType) msg).getVersionId())) {
          httpResponse.setHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, ((ObjectStorageDataResponseType) msg).getVersionId());
        }

        // Add user metadata
        for (MetaDataEntry m : headResponse.getMetaData()) {
          httpResponse.addHeader(ObjectStorageProperties.AMZ_META_HEADER_PREFIX + m.getName(), m.getValue());
        }

        Date lastModified = headResponse.getLastModified();
        if (lastModified != null) {
          httpResponse.setHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(lastModified));
        }

        // add copied headers
        OSGUtil.addCopiedHeadersToResponse(httpResponse, headResponse);
      }
      // Need to add the CORS headers before the next line where we null out
      // the Message that contains the response fields we need.
      OSGUtil.addCorsResponseHeaders(httpResponse);
      // Since a HEAD response, never include a body
      httpResponse.setMessage(null);
    }
  }
}
