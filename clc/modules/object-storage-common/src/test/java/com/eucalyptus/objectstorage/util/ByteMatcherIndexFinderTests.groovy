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
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.junit.Test

/**
 * Created by zhill on 4/15/14.
 */
@CompileStatic
class ByteMatcherIndexFinderTests {

    @Test
    void testFindBeginning() {
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer("helloworld123blahblah123".getBytes())
        assert(OSGUtil.findFirstMatchInBuffer(buffer, 0, "123".getBytes()) == 10)
        assert(OSGUtil.findFirstMatchInBuffer(buffer, 11, "123".getBytes()) == 21)
        Random random = new Random(System.currentTimeMillis())
        byte[] randomBytes = new byte[1024]
        random.nextBytes(randomBytes)
        randomBytes[1020] = 0x0D;
        randomBytes[1021] = 0x0A;
        randomBytes[120] = 0x0D;
        randomBytes[121] = 0x0A;
        buffer = ChannelBuffers.wrappedBuffer(randomBytes)
        byte[] crlfBytes = [0x0D , 0x0A]
        assert(OSGUtil.findFirstMatchInBuffer(buffer, 0, crlfBytes) == 120)
    }

    @Test
    void testFindEnding() {
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer("helloworld123blahblah123".getBytes())
        assert(OSGUtil.findLastMatchInBuffer(buffer, 0, "123".getBytes()) == 21)
        assert(OSGUtil.findLastMatchInBuffer(buffer, 11, "123".getBytes()) == 21)

        Random random = new Random(System.currentTimeMillis())
        byte[] randomBytes = new byte[1024]
        random.nextBytes(randomBytes)
        randomBytes[20] = 0x0D;
        randomBytes[21] = 0x0A;
        randomBytes[1020] = 0x0D;
        randomBytes[1021] = 0x0A;
        buffer = ChannelBuffers.wrappedBuffer(randomBytes)
        byte[] crlfBytes = [0x0D , 0x0A]
        assert(OSGUtil.findLastMatchInBuffer(buffer, 0, crlfBytes) == 1020)
    }
}
