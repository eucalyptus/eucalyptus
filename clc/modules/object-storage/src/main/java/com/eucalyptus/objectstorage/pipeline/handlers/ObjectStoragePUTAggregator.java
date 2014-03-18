package com.eucalyptus.objectstorage.pipeline.handlers;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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

    //Map of correlationId to channel to write to
    private static final ConcurrentHashMap<Channel, ChannelBufferStreamingInputStream> dataMap = new ConcurrentHashMap<Channel, ChannelBufferStreamingInputStream>();

	/* Implementation from SimpleChannelUpstreamHandler. Override not needed, can override the specific methods for channel events
    @Override
    public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    	if (e instanceof MessageEvent) {
    		messageReceived(ctx, (MessageEvent) e);
    	} else if (e instanceof WriteCompletionEvent) {
    		WriteCompletionEvent evt = (WriteCompletionEvent) e;
    		writeComplete(ctx, evt);
    	} else if (e instanceof ChildChannelStateEvent) {
    		ChildChannelStateEvent evt = (ChildChannelStateEvent) e;
    		if (evt.getChildChannel().isOpen()) {
    			childChannelOpen(ctx, evt);
    		} else {
    			childChannelClosed(ctx, evt);
    		}
    	} else if (e instanceof ChannelStateEvent) {
    		ChannelStateEvent evt = (ChannelStateEvent) e;
    		switch (evt.getState()) {
    		case OPEN:
    			if (Boolean.TRUE.equals(evt.getValue())) {
    				channelOpen(ctx, evt);
    			} else {
    				channelClosed(ctx, evt);
    			}
    			break;
    		case BOUND:
    			if (evt.getValue() != null) {
    				channelBound(ctx, evt);
    			} else {
    				channelUnbound(ctx, evt);
    			}
    			break;
    		case CONNECTED:
    			if (evt.getValue() != null) {
    				channelConnected(ctx, evt);
    			} else {
    				channelDisconnected(ctx, evt);
    			}
    			break;
    		case INTEREST_OPS:
    			channelInterestChanged(ctx, evt);
    			break;
    		default:
    			ctx.sendUpstream(e);
    		}
    	} else if (e instanceof ExceptionEvent) {
    		exceptionCaught(ctx, (ExceptionEvent) e);
    	} else {
    		ctx.sendUpstream(e);
    	}
    }
	 */

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

    protected void appendChunk(ChannelBuffer input, Channel channel) throws IllegalStateException {
        Logs.extreme().debug("Writing content data to stream for channel: " + channel.getId());
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
