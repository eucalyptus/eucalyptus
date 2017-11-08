/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

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
