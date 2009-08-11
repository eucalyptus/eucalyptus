package com.eucalyptus;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

public class Bootstrap {
  private static Logger LOG = Logger.getLogger( Bootstrap.class );
  public static void version() {
    LOG.info("Hello there in version.");
  }
  public static boolean check(String cn) {
    LOG.info("Hello there in check: " + cn);
    return true;
  }
  public static boolean destroy() {
    LOG.info("Hello there in destroy.");
    return true;
  }
  public static boolean stop() {
    LOG.info("Hello there in stop.");
    return true;
  }
  public static boolean start() {
    LOG.info("Hello there in start.");
    hello();
    return true;
  }
  public static boolean load(String cn, String ar[]) {
    LOG.info("Hello there in load: " + cn + " array: " + Lists.newArrayList( ar ) );
    hello();
    return true;
  }
  private static native void shutdown(boolean reload);
  private static native void hello();

}
