package com.eucalyptus.auth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.bootstrap.Component.Name;
import com.eucalyptus.util.EucalyptusProperties;

@Provides( resource = Resource.SystemCredentials )
public class SystemCredentialProvider extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( SystemCredentialProvider.class );
  private static ConcurrentMap<Component.Name, X509Certificate> certs     = new ConcurrentHashMap<Component.Name, X509Certificate>( );
  private static ConcurrentMap<Component.Name, KeyPair>         keypairs  = new ConcurrentHashMap<Component.Name, KeyPair>( );
  private Component.Name name;

  public SystemCredentialProvider( ) {
  }

  private SystemCredentialProvider( Name name ) {
    this.name = name;
  }

  public X509Certificate getCertificate( ) {
    return SystemCredentialProvider.certs.get( this );
  }

  public PrivateKey getPrivateKey( ) {
    return SystemCredentialProvider.keypairs.get( this ).getPrivate( );
  }

  public KeyPair getKeyPair( ) {
    return SystemCredentialProvider.keypairs.get( this );
  }

  private static void init( Component.Name name ) throws Exception {
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

  private static boolean check( Component.Name name ) {
    return ( SystemCredentialProvider.keypairs.containsKey( name.name( ) ) && SystemCredentialProvider.certs.containsKey( name.name( ) ) ) && EucaKeyStore.getInstance( ).containsEntry( name.name( ) );
  }

  private void loadSystemCredentialProviderKey( String name ) throws Exception {
    Component.Name alias = Component.Name.valueOf( name );
    if ( this.certs.containsKey( alias ) ) {
      return;
    } else {
      createSystemCredentialProviderKey( alias );
    }
  }

  private void createSystemCredentialProviderKey( Component.Name name ) throws Exception {
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
  public boolean load( Resource current, List<Resource> dependencies ) throws Exception {
    Credentials.init( );
    for ( Component.Name c : Component.Name.values( ) ) {
      try {
        if ( !SystemCredentialProvider.check( c ) ) SystemCredentialProvider.init( c );
      } catch ( Exception e ) {
        LOG.error( e );
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }
}

