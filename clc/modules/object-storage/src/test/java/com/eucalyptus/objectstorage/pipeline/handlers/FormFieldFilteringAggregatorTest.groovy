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
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.junit.Test

/**
 * Created by zhill on 4/2/14.
 */
class FormFieldFilteringAggregatorTest {

    @Test
    void testScanForFormBoundarySimple() {
        FormFieldFilteringAggregator aggregator = new FormFieldFilteringAggregator();

        aggregator.setupBoundary("--boundary")
        String initial = 'helloworld\r\n--boundary'
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        ChannelBuffer result = aggregator.scanForFormBoundary(buffer)
        assert(result.toString('UTF-8') == 'helloworld')


        aggregator.setupBoundary("--boundary")
        initial = 'helloworld\r\n--boundary\r\ntrailer'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        try {
            result = aggregator.scanForFormBoundary(buffer)
            fail('Should have received an exception for trailing content after the boundary')
        } catch(MalformedPOSTRequestException e) {
            println "Correctly caught POST exception due to trailing content after boundary"
        }

    }

    @Test
    void testScanForFormBoundaryChunked() {
        FormFieldFilteringAggregator aggregator = new FormFieldFilteringAggregator();


        aggregator.setupBoundary("--boundary")
        String initial = 'helloworld\r\n'
        println 'Testing content: "' + initial + '"'
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        ChannelBuffer result = aggregator.scanForFormBoundary(buffer)
        assert(result.toString('UTF-8') == 'helloworld')

        initial = 'helloworld\r\n--boundary'
        println 'adding content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.toString('UTF-8') == '\r\nhelloworld')


        aggregator.setupBoundary("--boundary")
        initial = 'helloworld\r\n--bound'
        println 'Testing content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.toString('UTF-8') == 'helloworld')

        initial = 'ary\r\ntrailingcontenthere'
        println 'adding content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        try {
            result = aggregator.scanForFormBoundary(buffer)
            fail('Should have gotten an exception on bad request for trailing POST content')
        } catch(S3Exception e) {
            println 'Correctly caught bad request exception on trailing POST content: ' + e.getMessage() + '; ' + e.getCode()
        }

        /* Test 3 chunks */
        aggregator.setupBoundary("--boundary")
        initial = 'helloworldblahblahlbah'
        println 'Testing content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.toString('UTF-8') == initial)

        initial = 'moreblahblah\r'
        println 'adding content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.toString('UTF-8') == 'moreblahblah')

        initial = '\n--bound'
        println 'adding content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.readableBytes() == 0)

        initial = 'ary--\r\n'
        print 'adding content: "' + initial + '"'
        buffer = ChannelBuffers.copiedBuffer(initial.getBytes('UTF-8'))
        result = aggregator.scanForFormBoundary(buffer)
        assert(result.readableBytes() == 0)
    }
}
