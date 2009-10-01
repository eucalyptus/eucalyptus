package com.eucalyptus.util;

public class LogUtil {
  private static String LONG_BAR = "=============================================================================================================================================================================================================";
  private static String MINI_BAR = "-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";
  public static String FAIL = "FAIL"
  public static String header( String message ) {
    return String.format( "%80.80s\n%s\n%1\$80.80s", LONG_BAR, message );
  }
  public static String subheader( String message ) {
    return String.format( "%80.80s\n%s\n%1\$80.80s", MINI_BAR, message );
  }

  public static String dumpObject( Object o ) {
    return "\n"+o.dump();
  }

  public static String lineObject( Object o ) {
    return String.format("%-200.200s",o.dump().replaceFirst("<\\w*.\\w*@","<"));
  }

}
