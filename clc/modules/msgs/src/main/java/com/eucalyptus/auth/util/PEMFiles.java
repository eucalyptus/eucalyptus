package com.eucalyptus.auth.util;

import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMWriter;

public class PEMFiles {
  private static Logger LOG = Logger.getLogger( PEMFiles.class );
  public static void writePem( final String fileName, final Object securityToken ) {
    PEMWriter privOut = null;
    try {
      privOut = new PEMWriter( new FileWriter( fileName ) );
      privOut.writeObject( securityToken );
      privOut.close( );
    } catch ( final IOException e ) {
      LOG.error( e, e );
    }
  }

}
