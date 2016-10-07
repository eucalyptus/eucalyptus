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
