package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;

@Provides(resource=Resource.Nothing)
@Depends(resources={Resource.Nothing})
public abstract class Bootstrapper {
 
  private static Logger LOG = Logger.getLogger( Bootstrapper.class );
  public static String SERVICES_PROPERTY = "euca.services";
  public static String MODEL_PROPERTY = "euca.model";
  public static String VERSION_PROPERTY = "euca.version";

  public String getVersion() {
    return System.getProperty( VERSION_PROPERTY );
  }

  public abstract boolean load() throws Exception;
  public abstract boolean start() throws Exception;
  public abstract boolean check() throws Exception;
  public abstract boolean stop() throws Exception;
  public abstract boolean destroy() throws Exception;

  
}