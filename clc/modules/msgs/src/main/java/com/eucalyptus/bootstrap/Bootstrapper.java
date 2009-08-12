package com.eucalyptus.bootstrap;

import java.util.List;

public interface Bootstrapper {
  public static String BASEDIR = "META-INF/";
  public static String PROPERTIES = BASEDIR + "eucalyptus-bootstrap.properties";
  public static String SERVICES_PROPERTY = "euca.services";
  public static String MODEL_PROPERTY = "euca.model";
  public String getVersion();
  public boolean check();
  public boolean destroy();
  public boolean stop();
  public boolean start();
  public boolean load();
}