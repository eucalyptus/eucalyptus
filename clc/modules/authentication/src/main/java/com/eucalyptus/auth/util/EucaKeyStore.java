package com.eucalyptus.auth.util;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.SubDirectory;

public class EucaKeyStore extends AbstractKeyStore {
  public static String            FORMAT         = "pkcs12";
  private static String           KEY_STORE_PASS = EucalyptusProperties.NAME;
  private static String           FILENAME       = "euca.p12";
  private static Logger           LOG            = Logger.getLogger( EucaKeyStore.class );

  private static AbstractKeyStore singleton      = EucaKeyStore.getInstance( );

  public static AbstractKeyStore getInstance( ) {
    synchronized ( EucaKeyStore.class ) {
      if ( EucaKeyStore.singleton == null ) {
        try {
          singleton = new EucaKeyStore( );
        } catch ( final Exception e ) {
          LOG.error( e, e );
        }
      }
    }
    return EucaKeyStore.singleton;
  }

  public static AbstractKeyStore getCleanInstance( ) throws Exception {
    return new EucaKeyStore( );
  }

  
  private EucaKeyStore( ) throws GeneralSecurityException, IOException {
    super( SubDirectory.KEYS.toString( ) + File.separator + EucaKeyStore.FILENAME, EucaKeyStore.KEY_STORE_PASS, EucaKeyStore.FORMAT );
  }

  @Override
  public boolean check( ) throws GeneralSecurityException {
    return (this.getCertificate( EucalyptusProperties.WWW_NAME )!= null)&&(this.getCertificate( EucalyptusProperties.NAME )!=null);
  }
}
