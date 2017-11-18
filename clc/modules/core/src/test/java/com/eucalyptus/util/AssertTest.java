/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.util;

import com.eucalyptus.testing.Asserts;
import junit.framework.TestCase;

import java.io.IOException;

public class AssertTest extends TestCase {
  public void testArg() {
    Assert.arg(true, "test");
    Assert.arg("test", true, "test");
    Asserts.assertThrows(() -> Assert.arg(false, "test"), IllegalArgumentException.class);
  }

  public void testArgNot() {
    Assert.argNot(false, "test");
    Assert.argNot("test", false, "test");
    Asserts.assertThrows(() -> Assert.argNot(true, "test"), IllegalArgumentException.class);
  }

  public void testIndex() {
    Assert.index(true, "test");
    Asserts.assertThrows(() -> Assert.index(false, "test"), IndexOutOfBoundsException.class);
  }

  public void testIndexNot() {
    Assert.indexNot(false, "test");
    Asserts.assertThrows(() -> Assert.indexNot(true, "test"), IndexOutOfBoundsException.class);
  }

  public void testIo() throws Throwable {
    Assert.io(true, "test");
    Asserts.assertThrows(() -> Assert.io(false, "test"), IOException.class);
  }

  public void testIoNot() throws Throwable {
    Assert.ioNot(false, "test");
    Asserts.assertThrows(() -> Assert.ioNot(true, "test"), IOException.class);
  }

  public void testState() throws Throwable {
    Assert.state(true, "test");
    Asserts.assertThrows(() -> Assert.state(false, "test"), IllegalStateException.class);
  }

  public void testStateNot() throws Throwable {
    Assert.stateNot(false, "test");
    Asserts.assertThrows(() -> Assert.stateNot(true, "test"), IllegalStateException.class);
  }

  public void testNotNull() {
    Assert.notNull(1, "test");
    Asserts.assertThrows(() -> Assert.notNull(null, "test"), NullPointerException.class);
  }
}
