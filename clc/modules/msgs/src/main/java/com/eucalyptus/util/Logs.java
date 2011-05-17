package com.eucalyptus.util;

import org.apache.log4j.Logger;


public class Logs {
  public static boolean DEBUG = false;
  public static boolean TRACE = false;
  public static boolean EXTREME = false;
  public static Logger exhaust( ) {
    return Logger.getLogger( "EXHAUST" );
  }
}
