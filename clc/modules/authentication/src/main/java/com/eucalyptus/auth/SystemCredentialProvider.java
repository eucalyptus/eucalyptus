package com.eucalyptus.auth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.util.EucalyptusProperties;

@Provides( resource = Resource.SystemCredentials )
@Depends(local = Component.eucalyptus)
public class SystemCredentialProvider extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( SystemCredentialProvider.class );
  private static ConcurrentMap<Component, X509Certificate> certs     = new ConcurrentHashMap<Component, X509Certificate>( );
  private static ConcurrentMap<Component, KeyPair>         keypairs  = new ConcurrentHashMap<Component, KeyPair>( );
  private Component name;

  public SystemCredentialProvider( ) {
  }

  private SystemCredentialProvider( Component name ) {
    this.name = name;
  }
  
  public static SystemCredentialProvider getCredentialProvider( Component name ) {
    return new SystemCredentialProvider( name );
  }

  public X509Certificate getCertificate( ) {
    return SystemCredentialProvider.certs.get( this.name );
  }

  public PrivateKey getPrivateKey( ) {
    return SystemCredentialProvider.keypairs.get( this.name ).getPrivate( );
  }

  public KeyPair getKeyPair( ) {
    return SystemCredentialProvider.keypairs.get( this.name );
  }

  private static void init( Component name ) throws Exception {
    new SystemCredentialProvider( name ).init( );
  }

  private void init( ) throws Exception {
    if ( EucaKeyStore.getInstance( ).containsEntry( this.name.name( ) ) ) {
      try {
        SystemCredentialProvider.certs.put( this.name, EucaKeyStore.getInstance( ).getCertificate( this.name.name( ) ) );
        SystemCredentialProvider.keypairs.put( this.name, EucaKeyStore.getInstance( ).getKeyPair( this.name.name( ), this.name.name( ) ) );
      } catch ( Exception e ) {
        SystemCredentialProvider.certs.remove( this );
        SystemCredentialProvider.keypairs.remove( this );
        LOG.fatal( "Failed to read keys from the keystore.  Please repair the keystore by hand." );
        LOG.fatal( e, e );
      }
    } else {
      this.createSystemCredentialProviderKey( this.name );
    }
  }

  private static boolean check( Component name ) {
    return ( SystemCredentialProvider.keypairs.containsKey( name.name( ) ) && SystemCredentialProvider.certs.containsKey( name.name( ) ) ) && EucaKeyStore.getInstance( ).containsEntry( name.name( ) );
  }

  private void loadSystemCredentialProviderKey( String name ) throws Exception {
    Component alias = Component.valueOf( name );
    if ( this.certs.containsKey( alias ) ) {
      return;
    } else {
      createSystemCredentialProviderKey( alias );
    }
  }

  private void createSystemCredentialProviderKey( Component name ) throws Exception {
    KeyTool keyTool = new KeyTool( );
    try {
      KeyPair sysKp = keyTool.getKeyPair( );
      X509Certificate sysX509 = keyTool.getCertificate( sysKp, EucalyptusProperties.getDName( name.name( ) ) );
      SystemCredentialProvider.certs.put( name, sysX509 );
      SystemCredentialProvider.keypairs.put( name, sysKp );
      //TODO: might need separate keystore for euca/hsqldb/ssl/jetty/etc.
      EucaKeyStore.getInstance( ).addKeyPair( name.name( ), sysX509, sysKp.getPrivate( ), name.name( ) );
      EucaKeyStore.getInstance( ).store( );
    } catch ( Exception e ) {
      SystemCredentialProvider.certs.remove( name );
      SystemCredentialProvider.keypairs.remove( name );
      EucaKeyStore.getInstance( ).remove( );
      throw e;
    }
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    try {
      Credentials.init( );
      for ( Component c : Component.values( ) ) {
        try {
          if ( !SystemCredentialProvider.check( c ) ) SystemCredentialProvider.init( c );
        } catch ( Exception e ) {
          LOG.error( e );
          return false;
        }
      }
    } catch ( Exception e ) {
      LOG.error(e,e);
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }
}

