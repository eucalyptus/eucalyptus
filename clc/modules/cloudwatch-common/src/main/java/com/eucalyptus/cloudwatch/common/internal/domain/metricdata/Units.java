/*************************************************************************
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
