package com.eucalyptus.cloudwatch.common.internal.metricdata;

/**
* Created by ethomas on 6/16/15.
*/
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
