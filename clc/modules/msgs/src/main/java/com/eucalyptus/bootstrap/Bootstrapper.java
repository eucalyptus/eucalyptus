package com.eucalyptus.bootstrap;

import java.util.List;

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

  public abstract boolean load(Resource current, List<Resource> dependencies) throws Exception;
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