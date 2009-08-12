package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import com.google.common.collect.Lists;

public abstract class Bootstrapper {
  private static Logger LOG = Logger.getLogger( Bootstrapper.class );
  public static String BASEDIR = "META-INF/";
  public static String PROPERTIES = BASEDIR + "eucalyptus-bootstrap.properties";
  public static String SERVICES_PROPERTY = "euca.services";
  public static String MODEL_PROPERTY = "euca.model";
  public static String VERSION_PROPERTY = "euca.version";
  public String getVersion() {
    return System.getProperty( VERSION_PROPERTY );
  }
  public abstract boolean check() throws Exception;
  public abstract boolean destroy() throws Exception;
  public abstract boolean stop() throws Exception;
  public abstract boolean start() throws Exception;
  public abstract boolean load() throws Exception;
  public static List<ConfigResource> loadConfigResources( JarFile jar ) throws IOException {
    return null;
  }

}