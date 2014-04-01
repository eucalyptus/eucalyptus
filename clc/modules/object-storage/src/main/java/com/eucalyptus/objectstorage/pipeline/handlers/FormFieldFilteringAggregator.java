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

import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.MalformedPOSTRequestException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.ChannelBufferStreamingInputStream;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;

/**
 * Extension of the ObjectStoragePUTAggregator that scans the input data for form-field delimiter to ensure
 * that only the file/data is included and not trailing form fields
 */
public class FormFieldFilteringAggregator extends ObjectStoragePUTAggregator {
    private static final Logger LOG = Logger.getLogger(FormFieldFilteringAggregator.class);

    //The boundary delimiter to use to filter the chunks.
    private byte[] boundaryBytes;
    private OSGUtil.ByteMatcherBeginningIndexFinder boundaryFinder;

    //Potentially a fully buffered previous chunk because partial boundary match may require next chunk to determine.
    private ChannelBuffer lastBufferedChunk;

    private int trailingBytesFound = 0;

    //Set to true once the end of the file data is reached and no more bytes should be sent through
    private boolean truncateRemaining;

    protected void setupBoundary(String boundaryString) throws Exception {
        Logs.extreme().debug("Initialized boundary for form field filtering: '" + boundaryString + "'");
        this.boundaryBytes = ("\r\n" + boundaryString).getBytes("UTF-8");
        this.truncateRemaining = false;
        boundaryFinder = new OSGUtil.ByteMatcherBeginningIndexFinder(this.boundaryBytes);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        LOG.trace( LogUtil.dumpObject(event));

        if (event.getMessage() instanceof MappingHttpRequest) {
            MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();
            String boundaryString = (String)(httpRequest.getFormFields().get(ObjectStorageProperties.FORM_BOUNDARY_FIELD));
            if(Strings.isNullOrEmpty(boundaryString)) {
                //Error
                LOG.error("No boundary found in the form. Cannot filter content. Failing POST form upload");
                Channels.fireExceptionCaught(ctx, new InternalErrorException("Error processing POST form content"));
                return;
            } else {
                setupBoundary(boundaryString);
            }
        }
        Logs.extreme().debug("Sending event to PUT aggregator");
        super.messageReceived(ctx, event);
    }

    /**
     * Returns a channel buffer that is sanitized for form fields, will remove
     * any subsequent fields found and set isLast = true
     * @param data
     * @return
     */
    protected ChannelBuffer scanForFormBoundary(final ChannelBuffer data) throws Exception {
        if(this.truncateRemaining) {
            this.trailingBytesFound += data.readableBytes();
            Logs.extreme().debug("Truncating remaining chunks. Returning zero-length buffer");
            return ChannelBuffers.wrappedBuffer(new byte[0]);
        }

        ChannelBuffer dataToScan = data;
        if(this.lastBufferedChunk != null) {
            dataToScan = ChannelBuffers.wrappedBuffer(this.lastBufferedChunk, data);
        }

        int lfIndex = dataToScan.indexOf(0, dataToScan.readableBytes(), ChannelBufferIndexFinder.CR);
        int endOffset = lfIndex;

        if(lfIndex >= 0) {
            //Scan the rest starting there
            if(lfIndex + this.boundaryBytes.length > dataToScan.readableBytes()) {
                //Can't be found here not enough space to check, save the slice in the last chunk
                this.lastBufferedChunk = dataToScan.slice(lfIndex, dataToScan.readableBytes() - lfIndex);
                //Return what we know is okay.
                endOffset = lfIndex;
                //return ChannelBuffers.copiedBuffer(dataToScan.slice(0, lfIndex));
            } else {
                //May be able to find the delimiter, look for it
                endOffset = dataToScan.indexOf(lfIndex, dataToScan.readableBytes(), this.boundaryFinder);
                if(endOffset >= lfIndex) {
                    //Boundary found right after the crlf, send everything before the crlf
                    this.lastBufferedChunk = null;
                    this.truncateRemaining = true;
                    this.trailingBytesFound = dataToScan.readableBytes() - endOffset;
                    if(this.trailingBytesFound > this.boundaryBytes.length + 4) { //boundary + --\r\n
                        //If more left in message than
                        this.lastBufferedChunk = null;
                        throw new MalformedPOSTRequestException("Must not have any trailing form parts after file content");
                    }
                }
            }
        } else {
            this.lastBufferedChunk = null;
        }

        if(endOffset == 0) {
            Logs.extreme().debug("Filtered all bytes in buffer. Returning zero-length buffer");
            return ChannelBuffers.wrappedBuffer(new byte[0]);
        } else if(endOffset == -1 || endOffset == dataToScan.readableBytes()) {
            Logs.extreme().debug("Filtered no bytes in buffer. Returning full-length buffer: " + dataToScan.readableBytes());
            return dataToScan;
        } else {
            Logs.extreme().debug("Filtered some bytes. Returning length: "  + endOffset);
            return ChannelBuffers.copiedBuffer(dataToScan.slice(0, endOffset));
        }
    }

    @Override
    protected void appendChunk(ChannelBuffer input, Channel channel) throws Exception {
        ChannelBuffer data = scanForFormBoundary(input);
        Logs.extreme().debug("Filtered POST content. Input buffer length: " + input.readableBytes() + " Filtered buffer length: " + data.readableBytes());
        super.appendChunk(data, channel);
    }
}
