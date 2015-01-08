/*
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 */

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.MissingContentLengthException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import java.util.Map;

/**
 * Populates the form field map in the message based on the content body
 * Subsequent stages/handlers can use the map exclusively
 */
@ChannelPipelineCoverage("one")
public class FormPOSTMultipartDecoder extends MessageStackHandler {
    private static Logger LOG = Logger.getLogger(FormPOSTMultipartDecoder.class);
    private static final int S3_FORM_BUFFER_SIZE = 25 * 1024; //20KB buffer for S3, give a bit of room extra to be safe here
    private HttpThresholdBufferingAggregator aggregator = new HttpThresholdBufferingAggregator(S3_FORM_BUFFER_SIZE);

    @Override
    public void handleUpstream(final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent) throws Exception {
        LOG.trace( LogUtil.dumpObject(channelEvent));
        try {
            if (channelEvent instanceof MessageEvent) {
                final MessageEvent msgEvent = (MessageEvent) channelEvent;
                this.incomingMessage(channelHandlerContext, msgEvent);
            } else {
                channelHandlerContext.sendUpstream(channelEvent);
            }
        } catch(Exception e) {
            LOG.warn("Caught exception in POST form authentication.", e);
            Channels.fireExceptionCaught(channelHandlerContext, e);
        }
    }

    @Override
    public void incomingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        MappingHttpRequest upstreamRequest = null;
        ChannelBuffer content = null;
        MessageEvent upstreamEvent = event;
        if(aggregator.offer(event)) {
            HttpThresholdBufferingAggregator.AggregatedMessageEvent result = aggregator.poll();
            if(result == null) {
                return;
            } else {
                //process the aggregated result
                aggregator.hardReset(); //ensure no further polls return this result.
                if(result.getMessageEvent() == null || result.getMessageEvent().getMessage() == null) {
                    throw new InternalErrorException("Unexpected state in message stack");
                }
                upstreamRequest = (MappingHttpRequest)(result.getMessageEvent().getMessage());
                content = result.getAggregatedContentBuffer();
                upstreamEvent = result.getMessageEvent();
            }
        } else {
            if ( event.getMessage() instanceof MappingHttpRequest ) {
                upstreamRequest = (MappingHttpRequest)event.getMessage();
                content = upstreamRequest.getContent();
            }
        }

        if(upstreamRequest != null && content != null) {
            this.handleFormMessage(upstreamRequest, content);
        }
        ctx.sendUpstream(upstreamEvent);
    }

    /**
     * Parses and populates the form fields of the httpRequest based on the given content buffer. The contentBuffer
     * may just be the httpRequest's content, but the caller must set it as such
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
        } catch(NumberFormatException e) {
            throw new MissingContentLengthException(httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH));
        }

        //Populate the form map with the bucket, content, and field map
        formFields.put(ObjectStorageProperties.FormField.bucket.toString(), getBucketName(httpRequest));

        //add this as it's needed for filtering the body later in the pipeline.
        formFields.putAll(MultipartFormPartParser.parseForm(httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE),
                contentLength,
                contentBuffer));
    }

    protected static String getBucketName(MappingHttpRequest httpRequest) {
        //Populate the bucket field from the HOST header or uri path
        String operationPath = httpRequest.getServicePath().replaceFirst(ComponentIds.lookup(ObjectStorage.class).getServicePath().toLowerCase(), "");
        String[] target = OSGUtil.getTarget(operationPath);
        String bucket = target[0];
        if("/".equals(bucket)) {
            //Look in HOST header
            bucket = httpRequest.getHeader(HttpHeaders.Names.HOST).split(".objectstorage",2)[0];
        }
        return bucket;
    }
}
