package com.eucalyptus.cloudwatch.domain.metricdata;

public class MetricUtils {

  public static Double average(Double sum, Double size) {
    if (Math.abs(size) < 0.0001) return 0.0; // TODO: make sure size is really an int
    return sum/size;
  }
  
}
