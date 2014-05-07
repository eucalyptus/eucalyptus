/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
 *   Copyright 2012 The Netty Project
 *
 *   The Netty Project licenses this file to you under the Apache License,
 *   version 2.0 (the "License"); you may not use this file except in
 *   compliance with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *   or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers;

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.LifeCycleAwareChannelHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMessage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Very heavily modified version of the HttpChunkAggregator from netty.
 * <p/>
 * Aggregates chunks into a single channelbuffer, but that buffer is expected to be drained
 * at the end of the pipeline by the service operation that is reading the data and pushing it
 * out somewhere.
 */
public class ObjectStoragePUTAggregator extends SimpleChannelUpstreamHandler implements LifeCycleAwareChannelHandler {
    private static final Logger LOG = Logger.getLogger(ObjectStoragePUTAggregator.class);

    //TODO: should be able to remove this since each pipeline instance is for the duration of
    // the request-response cycle. Only ever one channel per instance.

    //Map of correlationId to channel to write to
    protected static final ConcurrentHashMap<Channel, ChannelBufferStreamingInputStream> dataMap = new ConcurrentHashMap<Channel, ChannelBufferStreamingInputStream>();

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent evt) throws Exception {
        //Clear the map for this context
        try {
            Logs.extreme().debug("Removing data map on channel disconnected event for channel: " + ctx.getChannel().getId());
            dataMap.remove(ctx.getChannel());
        } catch (final Throwable f) {
            //Nothing to lookup.
        } finally {
            //Call through
            super.channelDisconnected(ctx, evt);
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent evt) throws Exception {
        //Clear the map for this context
        try {
            Logs.extreme().debug("Removing data map on channel closed event for channel: " + ctx.getChannel().getId());
            dataMap.remove(ctx.getChannel());
        } catch (final Throwable f) {
            //Nothing to lookup.
        } finally {
            super.channelClosed(ctx, evt);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        if (event.getMessage() instanceof MappingHttpRequest) {
            MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();

            if (httpRequest.getMessage() instanceof ObjectStorageDataRequestType) {
                if (httpRequest.isChunked()) {
                    //Chunked request, and beginning, setup map etc.
                    initializeNewPut(ctx, (ObjectStorageDataRequestType) httpRequest.getMessage());
                }
            }
        } else if (event.getMessage() instanceof HttpChunk) {
            //Add the chunk to the current streams channel buffer.
            HttpChunk chunk = (HttpChunk) event.getMessage();
            appendChunk(chunk.getContent(), ctx.getChannel());

            if (chunk.isLast()) {
                //Remove from the map
                Logs.extreme().debug("Removing data map due to last chunk processed event for channel: " + ctx.getChannel().getId());
                dataMap.remove(ctx.getChannel());
            }
        }
        //Always pass it on
        ctx.sendUpstream(event);
    }

    protected void initializeNewPut(ChannelHandlerContext ctx, ObjectStorageDataRequestType request) throws IllegalStateException {
        Logs.extreme().debug("Adding entry to data map in PUT aggregator for channel: " + ctx.getChannel().getId());
        ChannelBufferStreamingInputStream stream = request.getData();
        ChannelBufferStreamingInputStream foundStream = dataMap.putIfAbsent(ctx.getChannel(), stream);
        if (foundStream != null) {
            Logs.extreme().debug("Found existing entry in map for this channel. Streams should never cross. Throwing illegal state for channel: " + ctx.getChannel().getId());
            throw new IllegalStateException("Duplicate messages for same PUT, cannot overwrite data buffer. Channel:" + ctx.getChannel().getId());
        }
    }

    protected void appendChunk(ChannelBuffer input, Channel channel) throws Exception {
        Logs.extreme().debug("Writing content data to stream for channel: " + channel.getId() + " Content length: " + input.readableBytes());
        ChannelBufferStreamingInputStream stream = dataMap.get(channel);
        if (stream == null) {
            throw new IllegalStateException(
                    "received " + HttpChunk.class.getSimpleName() +
                            " without " + HttpMessage.class.getSimpleName());
        }
        //Write the content into the buffer.
        try {
            stream.putChunk(input);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void afterAdd(ChannelHandlerContext arg0) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void afterRemove(ChannelHandlerContext arg0) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeAdd(ChannelHandlerContext arg0) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeRemove(ChannelHandlerContext arg0) throws Exception {
        // TODO Auto-generated method stub

    }
}
