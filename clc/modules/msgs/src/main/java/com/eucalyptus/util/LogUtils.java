package com.eucalyptus.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import net.sf.json.JSONObject;

public class LogUtils {
  private static String LONG_BAR = "=============================================================================================================================================================================================================";
  public static String header( String message ) {
    return String.format( "\n%80.80s\n%s\n%1$80.80s", LONG_BAR, message );
  }

  public static String dumpObject( Object o ) {
    return JSONObject.fromObject( o ).toString( );
  }
}
