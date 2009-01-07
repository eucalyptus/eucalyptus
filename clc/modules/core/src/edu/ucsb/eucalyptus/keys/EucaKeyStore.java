package edu.ucsb.eucalyptus.keys;

import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

public class EucaKeyStore extends AbstractKeyStore {
  public static String FORMAT = "pkcs12";
  private static String KEY_STORE_PASS = EucalyptusProperties.NAME;
  private static String FILENAME = "euca.p12";
  private static Logger LOG = Logger.getLogger( EucaKeyStore.class );

  private static AbstractKeyStore singleton = getInstance();

  public static AbstractKeyStore getInstance()
  {
    synchronized( EucaKeyStore.class ) {
      if( singleton == null )
        try
        {
          singleton = new EucaKeyStore();
        }
        catch ( Exception e )
        {
          LOG.error( e, e );
        }
    }
    return singleton;
  }


  private EucaKeyStore( ) throws GeneralSecurityException, IOException
  {
    super( SubDirectory.KEYS.toString() + File.separator + FILENAME, KEY_STORE_PASS, FORMAT );
  }

  public boolean check() throws GeneralSecurityException
  {
    X509Certificate cert = this.getCertificate( EucalyptusProperties.WWW_NAME );
    return cert != null;
  }
}
