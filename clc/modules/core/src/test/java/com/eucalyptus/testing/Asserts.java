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

package com.eucalyptus.testing;


import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.function.Consumer;
import io.vavr.CheckedRunnable;

/**
 * Test assertion utilities.
 */
public final class Asserts {
  private Asserts() {
  }

  /**
   * Asserts that the {@code actual} is an instance of the {@throwableHierarchy} of causes.
   */
  @SafeVarargs
  public static void assertMatches(Throwable actual, Class<? extends Throwable>... throwableHierarchy) {
    Throwable current = actual;
    for (Class<? extends Throwable> expected : throwableHierarchy) {
      if (!expected.equals(current.getClass())) {
        fail(String.format("Bad exception type. Expected %s but was %s", Arrays.toString(throwableHierarchy), actual));
      }
      current = current.getCause();
    }
  }

  /**
   * Asserts that the {@code runnable} throws an instance of the {@throwableHierarchy} of causes.
   */
  @SafeVarargs
  public static void assertThrows(CheckedRunnable runnable, Class<? extends Throwable>... throwableHierarchy) {
    assertThrows(runnable, t -> {
    }, throwableHierarchy);
  }

  @SafeVarargs
  private static void assertThrows(CheckedRunnable runnable, Consumer<Throwable> exceptionConsumer,
                                   Class<? extends Throwable>... throwableHierarchy) {
    boolean fail = false;
    try {
      runnable.run();
      fail = true;
    } catch (Throwable t) {
      assertMatches(t, throwableHierarchy);
      exceptionConsumer.accept(t);
    }

    if (fail) {
      fail("No exception was thrown");
    }
  }
}
