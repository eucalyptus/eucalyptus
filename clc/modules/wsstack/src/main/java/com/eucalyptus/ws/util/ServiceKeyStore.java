package com.eucalyptus.ws.util;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;

public class ServiceKeyStore extends AbstractKeyStore {
  private static Logger LOG = Logger.getLogger( ServiceKeyStore.class );

  private static String FORMAT = "pkcs12";
  private static String KEY_STORE_PASS = EucalyptusProperties.NAME;
  private static String FILENAME = "clusters.p12";

  private static AbstractKeyStore singleton = getInstance();
  public static AbstractKeyStore getInstance()
  {
    synchronized( ServiceKeyStore.class ) {
      if( singleton == null )
        try
        {
          singleton = new ServiceKeyStore();
        }
        catch ( Exception e )
        {
          LOG.error( e, e );
        }
    }
    return singleton;
  }


  private ServiceKeyStore() throws GeneralSecurityException, IOException
  {
    super( SubDirectory.KEYS.toString() + File.separator + FILENAME, KEY_STORE_PASS, FORMAT );
  }

  public boolean check() throws GeneralSecurityException
  {
    X509Certificate cert = this.getCertificate( EucalyptusProperties.NAME );
    return cert != null;
  }
}
