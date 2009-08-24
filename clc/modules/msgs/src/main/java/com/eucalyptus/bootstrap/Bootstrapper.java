package com.eucalyptus.bootstrap;


import org.apache.log4j.Logger;

@Provides
@Depends
public abstract class Bootstrapper {
 
  private static Logger LOG = Logger.getLogger( Bootstrapper.class );
  public static String SERVICES_PROPERTY = "euca.services";
  public static String MODEL_PROPERTY = "euca.model";
  public static String VERSION_PROPERTY = "euca.version";

  public String getVersion() {
    return System.getProperty( VERSION_PROPERTY );
  }
  //TODO: does this make sense anymore? -------------------------\/
  public abstract boolean load(Resource current) throws Exception;
  public abstract boolean start() throws Exception;
  public boolean check() throws Exception {
    return true;
  }
  public boolean stop() throws Exception {
    return true;
  }
  public boolean destroy() throws Exception {
    return true;
  }

  
}