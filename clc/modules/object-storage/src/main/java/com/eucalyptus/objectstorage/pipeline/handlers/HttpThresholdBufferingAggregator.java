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

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.MissingContentLengthException;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import java.util.ArrayList;

/**
 * Aggregates the HTTP request such that it can move forward in the pipeline.
 * Will aggregate up to the specified buffer size and then make the aggregated event available
 * via 'poll'
 *
 * Expected usage: new instance per channel, to aggregate the data up from and then pass it on.
 * Subsequent chunks are then rejected and no processing is done because they can be passed up directly.
 * Example:
 * offer(mappingHttpRequestEvent) => true
 * poll() => null
 * offer(DefaultHttpChunkEvent) => true
 * poll() => null
 * offer(DefaultHttpChunkEvent) => true
 * poll() => AggregatedMessageEvent
 * offer(DefaultHttpChunkEvent) => false
 * offer(DefaultHttpChunkEvent) => false
 * in this case, poll() will continue to return the aggregated message, but with no new content.
 * use hardReset() to reset and use again if needed
 *
 * In the case where poll() is not called between offer() calls, offer will return false if the threshold is passed,
 * the caller must call poll() to get the current aggregated amount. This keeps memory usage limited.
 *
 *
 * This aggregator operates on a single channel, is not intended for multiple threads, should use multiple instances
 * for multiple channels
 */
public class HttpThresholdBufferingAggregator {
    private static Logger LOG = Logger.getLogger(HttpThresholdBufferingAggregator.class);
    private final static int DEFAULT_BUFFER_SIZE = 128 * 1024; //128k default size

    /**
     * Represents the aggregated event. The initial message event
     * plus the aggregated content buffer.
     */
    public static class AggregatedMessageEvent {
        public AggregatedMessageEvent(MessageEvent initial, ChannelBuffer initialContent) {
            this.messageEvent = initial;
            this.lastReceived = false;
            this.contentBuffers = new ArrayList<ChannelBuffer>(3); //usually no more than a few are needed
            this.contentBuffers.add(initialContent);
        }

        public MessageEvent getMessageEvent() {
            return messageEvent;
        }

        /**
         * May be invoked repeatedly, but will result in distinct copies of the aggregated buffer for each call.
         * Use carefully to avoid memory issues
         * @return
         */
        public ChannelBuffer getAggregatedContentBuffer() {
            return ChannelBuffers.copiedBuffer(this.contentBuffers.toArray(new ChannelBuffer[this.contentBuffers.size()]));
        }

        public void addContentBuffer(ChannelBuffer nextContentBuffer, boolean isLast) {
            this.contentBuffers.add(nextContentBuffer);
            this.lastReceived = isLast;
        }

        public int getAggregationCount() {
            return this.contentBuffers.size();
        }

        public long getCurrentAggregatedSize() {
            long size = 0;
            for(ChannelBuffer b : this.contentBuffers) {
                size += b.readableBytes();
            }
            return size;
        }

        public boolean isLastReceived() {
            return this.lastReceived;
        }

        private MessageEvent messageEvent;
        private ArrayList<ChannelBuffer> contentBuffers;
        private boolean lastReceived;
    }


    private int maxBufferingSize; //The size after which the message will be passed along
    private volatile AggregatedMessageEvent currentEvent;

    public HttpThresholdBufferingAggregator() {
        this.maxBufferingSize = DEFAULT_BUFFER_SIZE;
    }

    public HttpThresholdBufferingAggregator(int bufferSize) {
        this.maxBufferingSize = bufferSize;
    }

    /**
     * Returns the event to send upstream if one is ready, otherwise returns null
     * @return
     */
    public AggregatedMessageEvent poll() {
        if(this.currentEvent != null && (this.currentEvent.isLastReceived() || this.currentEvent.getCurrentAggregatedSize() >= this.maxBufferingSize)) {
            LOG.trace("Poll returning event: " + this.currentEvent.toString() + " data: " + this.currentEvent.getAggregatedContentBuffer().toString());
            return this.currentEvent;
        } else {
            LOG.trace("Poll returning null");
            return null;
        }
    }

    /**
     * Flushes and resets this instance. Will discard all pending data
     */
    public void hardReset() {
        LOG.trace("HardReset invoked");
        this.currentEvent = null;
    }

    /**
     * Handles the filtering, returns true if accepted, false if not accepted.
     * Reasons for not accepting:
     * 1. Message itself is larger than threshold
     * 2. Message is a chunk, but no preceding request has been processed
     * 3. The message was aggregated and returned causing reset, similar to #2. Subsequent chunks will be rejected
     * 4. There is a pending aggregation, but a MappingHttpRequest event is offered
     *
     * @param event
     * @return
     */
    public boolean offer(final MessageEvent event) throws Exception {
        if(event.getMessage() instanceof MappingHttpRequest && this.currentEvent == null) {
            return offerRequest(event);

        } else if(event.getMessage() instanceof DefaultHttpChunk && this.currentEvent != null) {
            return offerChunk((DefaultHttpChunk) event.getMessage());
        } else {
            //Reject the data
            return false;
        }
    }

    protected boolean offerRequest(MessageEvent event) throws Exception {
        if( !(event.getMessage() instanceof MappingHttpRequest) || this.currentEvent != null) {
            return false; //reject, wrong type or pending already
        }

        MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();
        if(Strings.isNullOrEmpty(httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH))) {
            throw new MissingContentLengthException();
        }

        try {
            long contentLength = Long.parseLong(httpRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH));
        } catch(NumberFormatException e) {
            LOG.error("Client specified content-length in invalid format, not a integer",e);
            throw new MissingContentLengthException("Content-Length not valid integer");
        }

        //First chunk, or full message
        if(this.currentEvent == null && httpRequest.isChunked() && httpRequest.getContent().readableBytes() < this.maxBufferingSize) {
            //Long message, less than threshold, start aggregating.
            this.currentEvent = new AggregatedMessageEvent(event, httpRequest.getContent());
            return true;
        } else {
            //Message is big enough or is not chunked so no follow-ups expected
            return false;
        }
    }

    protected boolean offerChunk(final DefaultHttpChunk httpChunk ) throws Exception {
        if(this.currentEvent == null || this.currentEvent.getCurrentAggregatedSize() > this.maxBufferingSize) {
            //reject because either not ready, or already exceeded the threshold and poll() not yet called
            return false;
        }

        //Buffer content
        this.currentEvent.addContentBuffer(httpChunk.getContent(), httpChunk.isLast());
        return true;
    }
}
