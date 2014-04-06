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

import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataGetResponseType;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.ChunkedDataStream;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.server.Statistics;
import com.google.common.base.Strings;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Date;
import java.util.concurrent.Callable;

@ChannelPipelineCoverage("one")
public class ObjectStorageGETOutboundHandler extends ObjectStorageBasicOutboundHandler {
    private static Logger LOG = Logger.getLogger(ObjectStorageGETOutboundHandler.class);

    /*
     * Override the MessageStackHandler implementation of this to ensure we don't send the message down the stack.
     */
    @Override
    public void handleDownstream(final ChannelHandlerContext ctx, final ChannelEvent channelEvent) throws Exception {
        try {
            if (channelEvent instanceof MessageEvent) {
                final MessageEvent msgEvent = (MessageEvent) channelEvent;
                if (msgEvent.getMessage() != null) {
                    Callable<Long> stat = Statistics.startDownstream(ctx.getChannel(), this);
                    boolean isDone = this.handleMessage(ctx, msgEvent);
                    stat.call();

                    if (isDone) {
                        return;
                    }
                }
            }
            ctx.sendDownstream(channelEvent);
        } catch (Exception e) {
            //TODO: zhill - this should be a error message sent downstream
            throw new WebServicesException(e.getMessage(), HttpResponseStatus.BAD_REQUEST);//TODO:GRZE: this is not right; needs to propagate in the right direction wrt server vs. client
        }
    }

    /**
     * Returns true if this handled the message and no further downstream necessary.
     */
    public boolean handleMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        handleBasicOutgoingMessage(ctx, event);
        return handleOutgoingMessage(ctx, event);
    }

    /**
     * Handles marshalling the output
     *
     * @param ctx
     * @param event
     * @throws Exception
     */
    protected boolean handleOutgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        if (event.getMessage() instanceof MappingHttpResponse) {
            MappingHttpResponse httpResponse = (MappingHttpResponse) event.getMessage();
            BaseMessage msg = (BaseMessage) httpResponse.getMessage();

            if (msg instanceof ObjectStorageDataGetResponseType) {
                ObjectStorageDataGetResponseType dataResponse = (ObjectStorageDataGetResponseType) msg;
                writeObjectStorageDataGetResponse(dataResponse, ctx);
                return true;
            }
        }
        return false;
    }

    protected void writeObjectStorageDataGetResponse(final ObjectStorageDataGetResponseType response, final ChannelHandlerContext ctx) {
        DefaultHttpResponse httpResponse = createHttpResponse(response);
        if (!Strings.isNullOrEmpty(response.getCorrelationId())) {
            httpResponse.setHeader(ObjectStorageProperties.AMZ_REQUEST_ID, response.getCorrelationId());
        }

        final Channel channel = ctx.getChannel();
        if (channel.isConnected()) {
            ChannelFuture writeFuture = Channels.future(ctx.getChannel());

            if (response.getDataInputStream() != null) {
                writeFuture.addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) {
                        InputStream input = response.getDataInputStream();
                        final ChunkedDataStream dataStream = new ChunkedDataStream(new PushbackInputStream(input));
                        ChannelFuture bodyWriteFuture = Channels.future(future.getChannel());
                        bodyWriteFuture.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                dataStream.close();
                                Contexts.clear(response.getCorrelationId()); //Do this on channel closure
                                //Close the channel
                                ChannelFutureListener.CLOSE.operationComplete(future);
                            }
                        });
                        Channels.write(ctx, bodyWriteFuture, dataStream);
                    }
                });
            } else {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
            Channels.write(ctx, writeFuture, httpResponse);
        }
    }

    //TODO: zhill - this should all be done in bindings, just need 2-way bindings
    protected DefaultHttpResponse createHttpResponse(ObjectStorageDataGetResponseType reply) {
        DefaultHttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        long contentLength = reply.getSize();
        String contentType = reply.getContentType();
        String etag = reply.getEtag();
        String contentDisposition = reply.getContentDisposition();
        httpResponse.addHeader(HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream");
        if (etag != null) {
            httpResponse.addHeader(HttpHeaders.Names.ETAG, "\"" + etag + "\""); //etag in quotes, per s3-spec.
        }
        httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(reply.getLastModified()));

        if (contentDisposition != null) {
            httpResponse.addHeader("Content-Disposition", contentDisposition);
        }
        httpResponse.addHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
        String versionId = reply.getVersionId();
        if (versionId != null && !ObjectStorageProperties.NULL_VERSION_ID.equals(versionId)) {
            httpResponse.addHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, versionId);
        }
        httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));

        //Add user metadata
        for (MetaDataEntry m : reply.getMetaData()) {
            httpResponse.addHeader(ObjectStorageProperties.AMZ_META_HEADER_PREFIX + m.getName(), m.getValue());
        }

        //write extra headers
        if (reply.getByteRangeEnd() != null) {
            httpResponse.addHeader("Content-Range", reply.getByteRangeStart() + "-" + reply.getByteRangeEnd() + "/" + reply.getSize());
        }
        return httpResponse;
    }
}
