package com.eucalyptus.auth;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Bootstrapper;

public class CredentialsBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( CredentialsBootstrapper.class );
  @Override
  public boolean check( ) throws Exception {
    return Credentials.checkKeystore( );
  }

  @Override
  public boolean destroy( ) throws Exception {
    return true;
  }

  @Override
  public boolean load( ) throws Exception {
    Credentials.init( );
    if( !Credentials.checkKeystore( ) ) {
      LOG.info("Looks like this is the first time?");//TODO: need to handle distinction between Cloud and Walrus/EBS here?!
      LOG.info("Generating system keys.");
      Credentials.createSystemKeys( );
    }
    return false;
  }

  @Override
  public boolean start( ) throws Exception {
    //TODO: this depends on the DB
//    Credentials.checkAdmin( );
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

}
