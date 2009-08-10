package com.eucalyptus.auth.util;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public abstract class AbstractKeyStore {

  public static AbstractKeyStore getGenericKeystore( final String fileName, final String password, final String format ) throws IOException, GeneralSecurityException {
    return new GenericKeyStore( fileName, password, format );
  }

  private static Logger LOG = Logger.getLogger( AbstractKeyStore.class );


  private KeyStore      keyStore;
  private final String  fileName;
  private final String  password;
  private final String  format;

  public AbstractKeyStore( final String fileName, final String password, final String format ) throws GeneralSecurityException, IOException {
    this.password = password;
    this.fileName = fileName;
    this.format = format;
    this.init( );
  }

  public KeyPair getKeyPair( final String alias, final String password ) throws GeneralSecurityException {
    final Certificate cert = this.keyStore.getCertificate( alias );
    final PrivateKey privateKey = ( PrivateKey ) this.keyStore.getKey( alias, password.toCharArray( ) );
    final KeyPair kp = new KeyPair( cert.getPublicKey( ), privateKey );
    return kp;
  }

  public abstract boolean check( ) throws GeneralSecurityException;

  public boolean containsEntry( final String alias ) {
    try {
      if ( ( X509Certificate ) this.keyStore.getCertificate( alias ) != null ) { return true; }
    } catch ( final KeyStoreException e ) {
    }
    return false;
  }

  public X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
    return ( X509Certificate ) this.keyStore.getCertificate( alias );
  }

  public Key getKey( final String alias, final String password ) throws GeneralSecurityException {
    return this.keyStore.getKey( alias, password.toCharArray( ) );
  }

  public String getCertificateAlias( final String certPem ) throws GeneralSecurityException {
    final X509Certificate cert = AbstractKeyStore.pemToX509( certPem );
    return this.keyStore.getCertificateAlias( cert );
  }

  public static X509Certificate pemToX509( final String certPem ) throws CertificateException, NoSuchProviderException {
    final CertificateFactory certificatefactory = CertificateFactory.getInstance( "X.509", "BC" );
    final X509Certificate cert = ( X509Certificate ) certificatefactory.generateCertificate( new ByteArrayInputStream( certPem.getBytes( ) ) );
    return cert;
  }

  public String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException {
    final String alias = this.keyStore.getCertificateAlias( cert );
    if ( alias == null ) { throw new GeneralSecurityException( "No Such Certificate!" ); }
    return alias;
  }

  public void addCertificate( final String alias, final X509Certificate cert ) throws IOException, GeneralSecurityException {
    AbstractKeyStore.LOG.info( String.format( "Adding certificate %10s %s to %s", alias, cert, this.fileName ) );
    this.keyStore.setCertificateEntry( alias, cert );
    this.store( );
  }

  public void addKeyPair( final String alias, final X509Certificate cert, final PrivateKey privateKey, final String keyPassword ) throws IOException, GeneralSecurityException {
    this.keyStore.setKeyEntry( alias, privateKey, keyPassword.toCharArray( ), new Certificate[] { cert } );
    this.store( );
  }

  public void store( ) throws IOException, GeneralSecurityException {
    AbstractKeyStore.LOG.info( "Writing back keystore: " + this.fileName );
    this.keyStore.store( new FileOutputStream( this.fileName ), this.password.toCharArray( ) );
  }

  private void init( ) throws IOException, GeneralSecurityException {
    this.keyStore = KeyStore.getInstance( this.format, "BC" );
    if ( ( new File( this.fileName ) ).exists( ) ) {
      final FileInputStream fin = new FileInputStream( this.fileName );
      this.keyStore.load( fin, this.password.toCharArray( ) );
      fin.close( );
    } else {
      this.keyStore.load( null, this.password.toCharArray( ) );
    }
  }

  public List<String> getAliases( ) throws KeyStoreException {
    final List<String> aliasList = new ArrayList<String>( );
    final Enumeration<String> aliases = this.keyStore.aliases( );
    while ( aliases.hasMoreElements( ) ) {
      aliasList.add( aliases.nextElement( ) );
    }
    return aliasList;
  }

  public String getFileName( ) {
    return this.fileName;
  }

  public void remove( ) {
    ( new File( this.fileName ) ).delete( );
  }

  static class GenericKeyStore extends AbstractKeyStore {

    private GenericKeyStore( final String fileName, final String password, final String format ) throws IOException, GeneralSecurityException {
      super( fileName, password, format );
    }

    @Override
    public boolean check( ) throws KeyStoreException {
      throw new RuntimeException( "A GenericKeyStore does not have the notion of being checked for correctness." );
    }
  }
}
