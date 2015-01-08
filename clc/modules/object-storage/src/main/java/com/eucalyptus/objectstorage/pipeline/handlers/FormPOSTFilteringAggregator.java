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
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Strings;
import com.google.common.primitives.Bytes;
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
public class FormPOSTFilteringAggregator extends ObjectStoragePUTAggregator {
    private static final Logger LOG = Logger.getLogger(FormPOSTFilteringAggregator.class);

    //The boundary delimiter to use to filter the chunks.
    private byte[] boundaryBytes;
    private OSGUtil.ByteMatcherBeginningIndexFinder boundaryFinder;
    private long bytesSent;

    //Potentially a fully buffered previous chunk because partial boundary match may require next chunk to determine.
    private ChannelBuffer lastBufferedChunk;

    private int trailingBytesFound = 0;

    //Set to true once the end of the file data is reached and no more bytes should be sent through
    private boolean truncateRemaining;

    protected void setupBoundary(byte[] boundary) throws Exception {
        Logs.extreme().debug("Initialized boundary for form field filtering: '" + new String(boundary) + "'");
        //Add the leading \r\n to make matching easy
        this.boundaryBytes = Bytes.concat(MultipartFormPartParser.PART_LINE_DELIMITER_BYTES, boundary);
        this.truncateRemaining = false;
        boundaryFinder = new OSGUtil.ByteMatcherBeginningIndexFinder(this.boundaryBytes);
        this.bytesSent = 0L;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        Logs.extreme().debug( LogUtil.dumpObject(event));
        if (event.getMessage() instanceof MappingHttpRequest) {
            MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();
            byte[] boundary = (byte[])(httpRequest.getFormFields().get(ObjectStorageProperties.FormField.x_ignore_formboundary.toString()));
            if(boundary == null) {
                //Error
                LOG.error("No boundary found in the form. Cannot filter content. Failing POST form upload");
                Channels.fireExceptionCaught(ctx, new InternalErrorException("Error processing POST form content"));
                return;
            } else {
                setupBoundary(boundary);
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
            return ChannelBuffers.wrappedBuffer(new byte[0]);
        }

        ChannelBuffer dataToScan = data;
        if(this.lastBufferedChunk != null) {
            dataToScan = ChannelBuffers.wrappedBuffer(this.lastBufferedChunk, data);
        }

        int lfIndex = 0;
        int endOffset = -1;
        int bufferSize = dataToScan.readableBytes();

        //Scan through looking at all carriage returns to determine if beginning of boundary or end of data
        do {
            lfIndex = dataToScan.indexOf(lfIndex, dataToScan.readableBytes(), ChannelBufferIndexFinder.CR);

            if(lfIndex >= 0) {
                //Scan the rest starting there
                if(lfIndex + this.boundaryBytes.length > bufferSize) {
                    //Can't be found here not enough space to check, save the slice in the last chunk
                    this.lastBufferedChunk = dataToScan.copy(lfIndex, bufferSize - lfIndex);
                    //Return what we know is okay.
                    endOffset = lfIndex;
                } else {
                    //May be able to find the delimiter, look for it in the next N bytes where N=boundary size
                    endOffset = dataToScan.indexOf(lfIndex, lfIndex + this.boundaryBytes.length, this.boundaryFinder);
                    if(endOffset >= lfIndex) {
                        //Boundary found right after the crlf, send everything before the crlf
                        this.lastBufferedChunk = null;
                        this.truncateRemaining = true;
                        this.trailingBytesFound = bufferSize - endOffset;
                        if(this.trailingBytesFound > this.boundaryBytes.length + 4) { //boundary + --\r\n
                            //If more left in message than
                            this.lastBufferedChunk = null;
                            throw new MalformedPOSTRequestException("Must not have any trailing form parts after file content");
                        }
                    } else {
                        //didn't find it. continue
                        lfIndex ++;
                        endOffset = -1;
                    }
                }
            } else {
                this.lastBufferedChunk = null;
                endOffset = bufferSize;
            }
        } while(endOffset < 0);

        if(endOffset == 0) {
            return ChannelBuffers.wrappedBuffer(new byte[0]);
        } else if(endOffset == -1 || endOffset == dataToScan.readableBytes()) {
            return dataToScan;
        } else {
            return ChannelBuffers.copiedBuffer(dataToScan.slice(0, endOffset));
        }
    }

    @Override
    protected void appendChunk(ChannelBuffer input, Channel channel) throws Exception {
        ChannelBuffer data = scanForFormBoundary(input);
        this.bytesSent += data.readableBytes();
        super.appendChunk(data, channel);
    }
}
