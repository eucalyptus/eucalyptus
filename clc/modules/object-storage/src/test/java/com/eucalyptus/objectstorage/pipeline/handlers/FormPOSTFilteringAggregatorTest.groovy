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

package com.eucalyptus.objectstorage.pipeline.handlers

import com.eucalyptus.objectstorage.exceptions.s3.MalformedPOSTRequestException
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception
import com.google.common.primitives.Bytes
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.junit.Test
import static org.junit.Assert.fail

/**
 * Created by zhill on 4/2/14.
 */
class FormPOSTFilteringAggregatorTest {
    static byte[] crlf = ['\r','\n']
    static byte[] boundaryBytes = '--boundary'.getBytes('UTF-8')
    static byte[] boundaryFinalBytes = '--boundary--\r\n'.getBytes('UTF-8')

    @Test
    void testScanForFormBoundarySimple() {
        FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
        int dataSize = 64
        aggregator.setupBoundary(boundaryBytes)
        byte[] initial = POSTRequestGenerator.getRandomData(dataSize)
        //Add some cr lf chars to test handling those mid-stream
        initial[dataSize / 2] = 0x0D; //cr
        initial[dataSize / 2 + 1 ] = 0x0A; //lf
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(initial, crlf, boundaryFinalBytes)
        ChannelBuffer result = aggregator.scanForFormBoundary(buffer)
        byte[] out = new byte[dataSize]
        result.getBytes(0, out)
        assert(out == initial)


        aggregator.setupBoundary(boundaryBytes)
        buffer = ChannelBuffers.copiedBuffer(initial, crlf, boundaryBytes , crlf , POSTRequestGenerator.getRandomData(dataSize), crlf, boundaryFinalBytes)
        try {
            result = aggregator.scanForFormBoundary(buffer)
            out = new byte[result.readableBytes()]
            result.getBytes(0, out)
            assert(out == initial)
            fail('Should have received an exception for trailing content after the boundary')
        } catch(MalformedPOSTRequestException e) {
            println "Correctly caught POST exception due to trailing content after boundary"
        }

    }


    @Test
    void testScanForFormBoundaryChunkedShort() {
        FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
        int dataSize = 128
        aggregator.setupBoundary(boundaryBytes)
        byte[] newlineBoundaryBytes = Bytes.concat(MultipartFormPartParser.PART_LINE_DELIMITER_BYTES, boundaryBytes)

        //First chunk has no boundary data, just regular data
        byte[] data = POSTRequestGenerator.getRandomData(dataSize)
        println 'Testing content: "' + data.length + '"'
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data)
        ChannelBuffer result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == data)

        //2nd chunk has some \r\n as well as the boundary at the end
        data = POSTRequestGenerator.getRandomData(dataSize * 10)
        data[dataSize] = '\r'.getBytes('UTF-8')[0]
        data[dataSize + 1] = '\n'.getBytes('UTF-8')[0]
        println 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        writeBytesToBuffer(buffer, ChannelBuffers.copiedBuffer(newlineBoundaryBytes), data.length - newlineBoundaryBytes.length)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == buffer.copy(0, data.length - newlineBoundaryBytes.length).array())

        //Test with boundary split on chunk
        aggregator.setupBoundary(boundaryBytes)
        data = POSTRequestGenerator.getRandomData(dataSize)
        data[dataSize / 2] = 0x0D;
        data[dataSize / 2 + 1] = 0x0A;

        println 'Testing content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        //only include first boundarySplit bytes of boundary in the buffer... tests the buffering
        int boundarySplit = 8
        writeBytesToBuffer(buffer, ChannelBuffers.copiedBuffer(newlineBoundaryBytes, 0, boundarySplit), data.length - boundarySplit)
        result = aggregator.scanForFormBoundary(buffer)
        int chunkSize = result.readableBytes()
        int firstChunkSize = data.length
        assert(chunkSize + boundarySplit <= firstChunkSize) //To account for any random \r that occur too close to the end and must be buffered
        int bufferedSize = firstChunkSize - chunkSize - boundarySplit
        println 'Buffered ' + bufferedSize + ' bytes'

        assert(result.array() == buffer.copy(0, chunkSize).array())

        //Rest of the boundary
        data = POSTRequestGenerator.getRandomData(newlineBoundaryBytes.length - boundarySplit)
        println 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        writeBytesToBuffer(buffer, ChannelBuffers.copiedBuffer(newlineBoundaryBytes, boundarySplit, newlineBoundaryBytes.length - boundarySplit), 0)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.readableBytes() == bufferedSize) //Account for any random newlines etc that force buffering at the end

    }

    @Test
    void testScanForFormBoundaryChunkedTrailing() {
        FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
        int dataSize = 128
        aggregator.setupBoundary(boundaryBytes)
        byte[] newlineBoundaryBytes = Bytes.concat(MultipartFormPartParser.PART_LINE_DELIMITER_BYTES, boundaryBytes)

        //First chunk has no boundary data, just regular data
        byte[] data = POSTRequestGenerator.getRandomData(dataSize)
        println 'Testing content: "' + data.length + '"'
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data)
        ChannelBuffer result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == data)

        //2nd chunk has some \r\n as well as the boundary at the end
        data = POSTRequestGenerator.getRandomData(dataSize * 10)
        data[dataSize] = '\r'.getBytes('UTF-8')[0]
        data[dataSize + 1] = '\n'.getBytes('UTF-8')[0]
        println 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        writeBytesToBuffer(buffer, ChannelBuffers.copiedBuffer(newlineBoundaryBytes), data.length - newlineBoundaryBytes.length)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == buffer.copy(0, data.length - newlineBoundaryBytes.length).array())

        //Test with trailing after boundary
        aggregator.setupBoundary(boundaryBytes)
        data = POSTRequestGenerator.getRandomData(dataSize)
        data[dataSize / 2] = '\r'.getBytes('UTF-8')[0]
        data[dataSize / 2 + 1] = '\n'.getBytes('UTF-8')[0]

        println 'Testing content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        //only include first boundarySplit bytes of boundary in the buffer... tests the buffering
        int boundarySplit = 8
        writeBytesToBuffer(buffer, ChannelBuffers.copiedBuffer(newlineBoundaryBytes, 0, boundarySplit), data.length - boundarySplit)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == buffer.copy(0, data.length - boundarySplit).array())

        //Next chunk contains the rest of the boundary as well as some trailing data
        data = POSTRequestGenerator.getRandomData(dataSize * 10 + (newlineBoundaryBytes.length - boundarySplit))
        println 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        writeBytesToBuffer(buffer, ChannelBuffers.copiedBuffer(newlineBoundaryBytes, boundarySplit, newlineBoundaryBytes.length - boundarySplit), 0)
        try {
            result = aggregator.scanForFormBoundary(buffer)
            fail('Should have gotten an exception on bad request for trailing POST content')
        } catch(S3Exception e) {
            println 'Correctly caught bad request exception on trailing POST content: ' + e.getMessage() + '; ' + e.getCode()
        }

    }

    //Writes the bytes from src to dest starting at destOffset in dest
    void writeBytesToBuffer(ChannelBuffer dest, ChannelBuffer src, int destOffset) {
        dest.markWriterIndex()
        dest.writerIndex(destOffset)
        dest.writeBytes(src)
        dest.resetWriterIndex()
    }

    @Test
    void testScanForFormBoundaryChunkedLong() {
        FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
        int dataSize = 128
        aggregator.setupBoundary(boundaryBytes)
        byte[] newlineBoundaryBytes = Bytes.concat(MultipartFormPartParser.PART_LINE_DELIMITER_BYTES, boundaryBytes)
        byte[] data = POSTRequestGenerator.getRandomData(dataSize)
        println 'Testing content: "' + data.length + '"'
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data)

        ChannelBuffer result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == data)

        /* Test 3 chunks */
        aggregator.setupBoundary(boundaryBytes)
        data = POSTRequestGenerator.getRandomData(dataSize)
        println 'Testing content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == data)

        data = POSTRequestGenerator.getRandomData(dataSize)
        data[dataSize -1] = '\r'.getBytes('UTF-8')[0]
        println 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.array() == ChannelBuffers.copiedBuffer(data, 0, dataSize -1).array())

        data = '\n--bound'.getBytes('UTF-8')
        println 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.readableBytes() == 0)

        data = 'ary--\r\n'.getBytes('UTF-8')
        print 'adding content: "' + data.length + '"'
        buffer = ChannelBuffers.wrappedBuffer(data)
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.readableBytes() == 0)
    }
}
