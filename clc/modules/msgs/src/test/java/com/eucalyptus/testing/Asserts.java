package com.eucalyptus.testing;

import javaslang.control.Try.CheckedRunnable;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.function.Consumer;

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
