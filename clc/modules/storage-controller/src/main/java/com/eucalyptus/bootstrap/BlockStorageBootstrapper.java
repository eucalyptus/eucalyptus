package com.eucalyptus.bootstrap;

import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsb.eucalyptus.storage.BlockStorageManagerFactory;

@Provides(resource=Resource.PrivilegedContext)
public class BlockStorageBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( BlockStorageBootstrapper.class );
  private static BlockStorageBootstrapper singleton;

  public static Bootstrapper getInstance( ) {
    synchronized ( BlockStorageBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new BlockStorageBootstrapper( );
        LOG.info( "Creating Block Storage Bootstrapper instance." );
      } else {
        LOG.info( "Returning Block Storage Bootstrapper instance." );
      }
    }
    return singleton;
  }

  @Override
  public boolean check( ) throws Exception {
    return true;
  }

  @Override
  public boolean destroy( ) throws Exception {
    return true;
  }

  @Override
  public boolean load(Resource current, List<Resource> dependencies ) throws Exception {
	  BlockStorageManagerFactory.getBlockStorageManager().checkPreconditions();
	  return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

}
