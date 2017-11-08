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
package com.eucalyptus.cloudwatch.common.internal.domain.metricdata;

public enum Units {
  Seconds,
  Microseconds,
  Milliseconds,
  Bytes,
  Kilobytes,
  Megabytes,
  Gigabytes,
  Terabytes,
  Bits,
  Kilobits,
  Megabits,
  Gigabits,
  Terabits,
  Percent,
  Count,
  BytesPerSecond("Bytes/Second"),
  KilobytesPerSecond("Kilobytes/Second"),
  MegabytesPerSecond("Megabytes/Second"),
  GigabytesPerSecond("Gigabytes/Second"),
  TerabytesPerSecond("Terabytes/Second"),
  BitsPerSecond("Bits/Second"),
  KilobitsPerSecond("Kilobits/Second"),
  MegabitsPerSecond("Megabits/Second"),
  GigabitsPerSecond("Gigabits/Second"),
  TerabitsPerSecond("Terabits/Second"),
  CountPerSecond("Count/Second"),
  None("None");
  private String value;

  Units() {
    this.value = name();
  }

  Units(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Units fromValue(String value) {
    for (Units units: values()) {
      if (units.value.equals(value)) {
        return units;
      }
    }
    throw new IllegalArgumentException("Unknown unit " + value);
  }
}
