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

import java.io.IOException;

/**
 * Assertion utilities.
 */
public final class Assert {
  private Assert() {
  }

  /**
   * @throws IllegalArgumentException when {@code expression} is false
   */
  public static void arg(boolean expression, String errorMessageFormat, Object... args) {
    argNot(!expression, errorMessageFormat, args);
  }

  /**
   * @throws IllegalArgumentException when {@code expression} is false
   */
  public static <T> T arg(T argument, boolean expression, String errorMessageFormat, Object... args) {
    return argNot(argument, !expression, errorMessageFormat, args);
  }

  /**
   * @throws IllegalArgumentException when {@code expression} is true
   */
  public static void argNot(boolean expression, String errorMessageFormat, Object... args) {
    if (expression) {
      throw new IllegalArgumentException(String.format(errorMessageFormat, args));
    }
  }

  /**
   * @throws IllegalArgumentException when {@code argument} is true
   */
  public static <T> T argNot(T argument, boolean expression, String errorMessageFormat, Object... args) {
    argNot(expression, errorMessageFormat, args);
    return argument;
  }

  /**
   * @throws IndexOutOfBoundsException when {@code expression} is false
   */
  public static void index(boolean expression, String errorMessageFormat, Object... args) {
    indexNot(!expression, errorMessageFormat, args);
  }

  /**
   * @throws IndexOutOfBoundsException when {@code expression} is false
   */
  public static void indexNot(boolean expression, String errorMessageFormat, Object... args) {
    if (expression) {
      throw new IndexOutOfBoundsException(String.format(errorMessageFormat, args));
    }
  }

  /**
   * @throws IOException if the {@code expression} is false
   */
  public static void io(boolean expression, String errorMessageFormat, Object... args) throws IOException {
    ioNot(!expression, errorMessageFormat, args);
  }

  /**
   * @throws IOException if the {@code expression} is true
   */
  public static void ioNot(boolean expression, String errorMessageFormat, Object... args) throws IOException {
    if (expression) {
      throw new IOException(String.format(errorMessageFormat, args));
    }
  }

  /**
   * @throws NullPointerException when {@code reference} is null
   */
  public static <T> T notNull(T reference, String parameterName) {
    if (reference == null) {
      throw new NullPointerException(parameterName + " cannot be null");
    }
    return reference;
  }

  /**
   * @throws IllegalStateException when {@code expression} is false
   */
  public static void state(boolean expression, String errorMessageFormat, Object... args) {
    stateNot(!expression, errorMessageFormat, args);
  }

  /**
   * @throws IllegalStateException when {@code expression} is true
   */
  public static void stateNot(boolean expression, String errorMessageFormat, Object... args) {
    if (expression) {
      throw new IllegalStateException(String.format(errorMessageFormat, args));
    }
  }
}
