/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

import java.util.Date;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataGetResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketCorsResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketTaggingResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.ws.handlers.MessageStackHandler;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ObjectStorageOutboundHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger(ObjectStorageOutboundHandler.class);

  @Override
  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent channelEvent) throws Exception {
    if (channelEvent instanceof ExceptionEvent) {
      exceptionCaught(ctx, (ExceptionEvent) channelEvent);
    } else {
      ctx.sendUpstream(channelEvent);
    }
  }

  private void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    // The next stage should be the exception handler that will catch this, marshall as needed, and send it back down.
    ctx.sendUpstream(e);
  }

  @Override
  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent channelEvent) throws Exception {
    if (channelEvent instanceof MessageEvent) {
      final MessageEvent msgEvent = (MessageEvent) channelEvent;
      this.outgoingMessage(ctx, msgEvent);
    }
    ctx.sendDownstream(channelEvent);
  }

  @Override
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    if (event.getMessage() instanceof MappingHttpResponse) {
      MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
      BaseMessage msg = (BaseMessage) httpResponse.getMessage();

      // Need to add the CORS headers before later code nulls out
      // the Message that contains the response fields we need.
      // Watch out for code created in the future that creates a different
      // httpResponse object later in this code (like 
      // ObjectStorageGETOutboundHandler does), or that overrides any of 
      // the fields we set here.
      OSGUtil.addCorsResponseHeaders(httpResponse);

      // Ordering if-else conditions from most to least restrictive i.e. narrow to broad filters
      if (msg instanceof PostObjectResponseType) {
        PostObjectResponseType postObjectResponse = (PostObjectResponseType) msg;
        String redirectUrl = postObjectResponse.getRedirectUrl();
        if (redirectUrl != null) {
          httpResponse.addHeader(HttpHeaders.Names.LOCATION, redirectUrl);
          httpResponse.setStatus(HttpResponseStatus.SEE_OTHER);
          httpResponse.setMessage(null);
        } else {
          Integer successCode = postObjectResponse.getSuccessCode();
          if (successCode != null) {
            if (successCode != 201) {
              httpResponse.setMessage(null);
              httpResponse.setStatus(new HttpResponseStatus(successCode, "OK"));
            } else {
              httpResponse.setStatus(new HttpResponseStatus(successCode, "Created"));
            }
          }
        }
        // have to force a close for browsers
        event.getFuture().addListener(ChannelFutureListener.CLOSE);
      } else if (msg instanceof CopyObjectResponseType) {
        CopyObjectResponseType copyResponse = (CopyObjectResponseType) msg;
        if (copyResponse.getVersionId() != null)
          httpResponse.addHeader("x-amz-version-id", copyResponse.getVersionId());
        if (copyResponse.getCopySourceVersionId() != null)
          httpResponse.addHeader("x-amz-copy-source-version-id", copyResponse.getCopySourceVersionId());
      } else if (msg instanceof CreateBucketResponseType) {
        httpResponse.setStatus(HttpResponseStatus.OK);
        removeResponseBody(msg, httpResponse);
        event.getFuture().addListener(ChannelFutureListener.CLOSE);
      } else if (msg instanceof ObjectStorageDataResponseType) { // Filter for GETs and PUTs related to data
        ObjectStorageDataResponseType response = (ObjectStorageDataResponseType) msg;
        if (response.getEtag() != null) {
          httpResponse.setHeader(HttpHeaders.Names.ETAG, '\"' + response.getEtag() + '\"');
        }
        if (response.getVersionId() != null) {
          httpResponse.setHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, response.getVersionId());
        }
        if (msg instanceof ObjectStorageDataGetResponseType && response.getLastModified() != null) {
          httpResponse.setHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(response.getLastModified()));
        }
        // Remove the content in response for certain operations
        if (msg instanceof PutObjectResponseType || msg instanceof UploadPartResponseType) {
          removeResponseBody(msg, httpResponse);
        }
      } else if (msg instanceof ObjectStorageResponseType) { // Filter for GETs and PUTs *NOT* related to data
        // Remove the content in response for certain operations
        if (msg instanceof SetBucketAccessControlPolicyResponseType || msg instanceof SetBucketLifecycleResponseType
            || msg instanceof SetBucketLoggingStatusResponseType || msg instanceof SetBucketVersioningStatusResponseType
            || msg instanceof SetObjectAccessControlPolicyResponseType || msg instanceof SetBucketTaggingResponseType
            || msg instanceof SetBucketCorsResponseType ) {
          if (msg instanceof SetObjectAccessControlPolicyResponseType && ((SetObjectAccessControlPolicyResponseType) msg).getVersionId() != null) {
            httpResponse.setHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, ((SetObjectAccessControlPolicyResponseType) msg).getVersionId());
          }
          if (((ObjectStorageResponseType)msg).getStatus( ) != null) {
            httpResponse.setStatus(((ObjectStorageResponseType)msg).getStatus( ));
          }
          removeResponseBody(msg, httpResponse);
        }
      }
    }
  }

  private void removeResponseBody(BaseMessage msg, MappingHttpResponse httpResponse) {
    // Populate all the common fields. These fields normally get populated in the outbound stage of the parent handler but get skipped in this case
    // due to an empty message body
    httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(0));
    if (msg.getCorrelationId() != null) {
      httpResponse.setHeader(ObjectStorageProperties.AMZ_REQUEST_ID, msg.getCorrelationId());
    }
    // Null the message body to remove it
    httpResponse.setMessage(null);
  }
}
