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

import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

@ChannelPipelineCoverage("one")
public class ObjectStorageOutboundHandler extends MessageStackHandler {
    @Override
    public void handleUpstream(ChannelHandlerContext ctx,
                               ChannelEvent channelEvent) throws Exception {
        if (channelEvent instanceof ExceptionEvent) {
            exceptionCaught(ctx, (ExceptionEvent) channelEvent);
        } else {
            ctx.sendUpstream(channelEvent);
        }
    }

    private void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        //The next stage should be the exception handler that will catch this, marshall as needed, and send it back down.
        ctx.sendUpstream(e);
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx,
                                 ChannelEvent channelEvent) throws Exception {
        if (channelEvent instanceof MessageEvent) {
            final MessageEvent msgEvent = (MessageEvent) channelEvent;
            this.outgoingMessage(ctx, msgEvent);
        }
        ctx.sendDownstream(channelEvent);
    }

    private static Logger LOG = Logger.getLogger(ObjectStorageOutboundHandler.class);

    @Override
    public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        if (event.getMessage() instanceof MappingHttpResponse) {
            MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
            BaseMessage msg = (BaseMessage) httpResponse.getMessage();

            if (msg instanceof PutObjectResponseType) {
                PutObjectResponseType putObjectResponse = (PutObjectResponseType) msg;
                httpResponse.setHeader(HttpHeaders.Names.ETAG, '\"' + putObjectResponse.getEtag() + '\"');
                if (putObjectResponse.getLastModified() != null) {
                    httpResponse.setHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(putObjectResponse.getLastModified()));
                }
                if (putObjectResponse.getVersionId() != null) {
                    httpResponse.setHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, putObjectResponse.getVersionId());
                }
            } else if (msg instanceof ObjectStorageDataResponseType) {
                ObjectStorageDataResponseType response = (ObjectStorageDataResponseType) msg;
                if (response.getEtag() != null) {
                    httpResponse.addHeader(HttpHeaders.Names.ETAG, '\"' + response.getEtag() + '\"');
                }
                if (response.getLastModified() != null) {
                    httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(response.getLastModified()));
                }
                if (response.getVersionId() != null) {
                    httpResponse.addHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, response.getVersionId());
                }
            } else if (msg instanceof PostObjectResponseType) {
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
                //have to force a close for browsers
                event.getFuture().addListener(ChannelFutureListener.CLOSE);
            } else if (msg instanceof CopyObjectResponseType) {
                CopyObjectResponseType copyResponse = (CopyObjectResponseType) msg;
                if (copyResponse.getVersionId() != null)
                    httpResponse.addHeader("x-amz-version-id", copyResponse.getVersionId());
                if (copyResponse.getCopySourceVersionId() != null)
                    httpResponse.addHeader("x-amz-copy-source-version-id", copyResponse.getCopySourceVersionId());
            } else if (msg instanceof CreateBucketResponseType) {
                httpResponse.setStatus(HttpResponseStatus.OK);
                httpResponse.setMessage(null);
                event.getFuture().addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
