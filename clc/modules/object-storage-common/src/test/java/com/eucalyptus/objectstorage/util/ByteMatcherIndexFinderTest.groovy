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

package com.eucalyptus.objectstorage.util

import groovy.transform.CompileStatic
import org.jboss.netty.buffer.ChannelBuffers
import org.junit.Test

/**
 * Created by zhill on 4/3/14.
 */
@CompileStatic
class ByteMatcherIndexFinderTest {
    @Test
    public void testByteMatcher() {
        byte[] boundaryBytes = "\r\n--boundary\r\n".getBytes("UTF-8")
        OSGUtil.ByteMatcherBeginningIndexFinder finder = new OSGUtil.ByteMatcherBeginningIndexFinder(boundaryBytes)

        byte[] content = "blablahblah\r\n--boundary\r\n".getBytes("UTF-8")
        int index = ChannelBuffers.wrappedBuffer(content).bytesBefore(finder)
        assert(index == 11)

        content = "\r\n--boundary\r\nblahblach".getBytes("UTF-8")
        index = ChannelBuffers.wrappedBuffer(content).bytesBefore(finder)
        assert(index == 0)

        content = "blablahblah--boundary\r\n".getBytes("UTF-8")
        index = ChannelBuffers.wrappedBuffer(content).bytesBefore(finder)
        assert(index == -1)

    }
}
