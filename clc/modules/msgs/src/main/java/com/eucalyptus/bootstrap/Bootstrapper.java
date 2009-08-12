package com.eucalyptus.bootstrap;

import java.util.List;

public interface Bootstrapper {
  public static String BASEDIR = "META-INF/";
  public static String PROPERTIES = BASEDIR + "eucalyptus-bootstrap.properties";
  public static String SERVICES_PROPERTY = "euca.services";
  public static String MODEL_PROPERTY = "euca.model";
  public String getVersion();
  public boolean check() throws Exception;
  public boolean destroy() throws Exception;
  public boolean stop() throws Exception;
  public boolean start() throws Exception;
  public boolean load() throws Exception;
}