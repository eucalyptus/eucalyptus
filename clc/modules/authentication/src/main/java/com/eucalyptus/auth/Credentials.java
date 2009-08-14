package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.apache.ws.security.WSSConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.UrlBase64;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;

import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.google.common.collect.Lists;

public class Credentials {
  private static Logger LOG            = Logger.getLogger( Credentials.class );
  private static String FORMAT         = "pkcs12";
  private static String KEY_STORE_PASS = "eucalyptus";                         //TODO: change the way this is handled
  private static String FILENAME       = "euca.p12";
  public static String  DB_NAME        = "eucalyptus_auth";
  public static User    SYSTEM         = getSystemUser( );
  

  public static void init( ) {
    Security.addProvider( new BouncyCastleProvider( ) );
    org.apache.xml.security.Init.init( );
    WSSConfig.getDefaultWSConfig( ).addJceProvider( "BC", BouncyCastleProvider.class.getCanonicalName( ) );
    WSSConfig.getDefaultWSConfig( ).setTimeStampStrict( true );
    WSSConfig.getDefaultWSConfig( ).setEnableSignatureConfirmation( true );
  }

  public static boolean checkKeystore( ) {
    try {
      return EucaKeyStore.getInstance( ).check( );
    } catch ( GeneralSecurityException e ) {
      LOG.debug(e,e);
      return false;
    }
  }

  public static boolean checkAdmin( ) {
    try {
      getUser( "admin" );
    } catch ( NoSuchUserException e ) {
      try {
        addUser( "admin", Boolean.TRUE );
      } catch ( UserExistsException e1 ) {
        LOG.fatal( e1, e1 );
        return false;
      }
    }
    return true;
  }

  private static User getSystemUser( ) {
    User system = new User( );
    system.setUserName( EucalyptusProperties.NAME );
    system.setIsAdministrator( Boolean.TRUE );
    return system;
  }

  public static User getUser( String userName ) throws NoSuchUserException {
    User user = null;
    EntityWrapper<User> db = getEntityWrapper( );
    User searchUser = new User( userName );
    try {
      user = db.getUnique( searchUser );
    } catch ( EucalyptusCloudException e ) {
      throw new NoSuchUserException( e );
    } finally {
      db.commit( );
    }
    return user;
  }

  public static User addUser( String userName ) throws UserExistsException {
    return addUser( userName, false );
  }

  public static User addUser( String userName, Boolean isAdmin ) throws UserExistsException {
    User newUser = new User( );
    newUser.setUserName( userName );
    String queryId = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, false ).replaceAll( "\\p{Punct}", "" );
    String secretKey = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, true ).replaceAll( "\\p{Punct}", "" );
    newUser.setQueryId( queryId );
    newUser.setSecretKey( secretKey );
    newUser.setIsAdministrator( isAdmin );
    EntityWrapper<User> db = getEntityWrapper( );
    try {
      db.add( newUser );
    } catch ( Exception e ) {
      db.rollback( );
      throw new UserExistsException( e );
    } finally {
      db.commit( );
    }
    return newUser;
  }

  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( Credentials.DB_NAME );
  }

  @Provides(resource=Resource.UserCredentials)
  @Depends(resources={Resource.Database})
  public static class Users extends Bootstrapper {
    public static boolean hasCertificate( final String alias ) {
      X509Cert certInfo = null;
      EntityWrapper<X509Cert> db = getEntityWrapper( );
      try {
        certInfo = db.getUnique( new X509Cert( alias ) );
      } catch ( EucalyptusCloudException e ) {
      } finally {
        db.commit( );
      }
      return certInfo != null;
    }

    public static X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
      EntityWrapper<X509Cert> db = getEntityWrapper( );
      try {
        X509Cert certInfo = db.getUnique( new X509Cert( alias ) );
        byte[] certBytes = UrlBase64.decode( certInfo.getPemCertificate( ).getBytes( ) );
        X509Certificate x509 = Hashes.getPemCert( certBytes );
        return x509;
      } catch ( EucalyptusCloudException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
    }

    public static String getCertificateAlias( final String certPem ) throws GeneralSecurityException {
      String certAlias = null;
      EntityWrapper<X509Cert> db = getEntityWrapper( );
      X509Cert certInfo = new X509Cert( );
      certInfo.setPemCertificate( new String( UrlBase64.encode( certPem.getBytes( ) ) ) );
      try {
        certAlias = db.getUnique( certInfo ).getAlias( );
      } catch ( EucalyptusCloudException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
      return certAlias;
    }

    public static String getQueryId( String userName ) throws GeneralSecurityException {
      String queryId = null;
      EntityWrapper<User> db = getEntityWrapper( );
      User searchUser = new User( userName );
      try {
        User user = db.getUnique( searchUser );
        queryId = user.getQueryId( );
      } catch ( EucalyptusCloudException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
      return queryId;
    }

    public static String getSecretKey( String queryId ) throws GeneralSecurityException {
      String secretKey = null;
      EntityWrapper<User> db = getEntityWrapper( );
      User searchUser = new User( );
      searchUser.setQueryId( queryId );
      try {
        User user = db.getUnique( searchUser );
        secretKey = user.getSecretKey( );
      } catch ( EucalyptusCloudException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
      return secretKey;
    }

    public static String getUserName( String queryId ) throws GeneralSecurityException {
      String userName = null;
      EntityWrapper<User> db = getEntityWrapper( );
      User searchUser = new User( );
      searchUser.setQueryId( queryId );
      try {
        User user = db.getUnique( searchUser );
        userName = user.getUserName( );
      } catch ( EucalyptusCloudException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
      return userName;
    }

    @SuppressWarnings( "unchecked" )
    public static String getUserName( X509Certificate cert ) throws GeneralSecurityException {
      String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) );
      User searchUser = new User( );
      X509Cert searchCert = new X509Cert( );
      searchCert.setPemCertificate( certPem );

      EntityWrapper<User> db = getEntityWrapper( );
      try {
        Session session = db.getSession( );
        Example qbeUser = Example.create( searchUser ).enableLike( MatchMode.EXACT );
        Example qbeCert = Example.create( searchCert ).enableLike( MatchMode.EXACT );
        List<User> users = ( List<User> ) session.createCriteria( User.class ).add( qbeUser ).createCriteria( "certificates" ).add( qbeCert ).list( );
        if ( users.size( ) > 1 ) {
          throw new GeneralSecurityException( "Multiple users with the same certificate." );
        } else if ( users.size( ) < 1 ) { throw new GeneralSecurityException( "No user with the specified certificate." ); }
        return users.get( 0 ).getUserName( );
      } catch ( HibernateException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
    }

    public static String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException {
      return getCertificateAlias( new String( Hashes.getPemBytes( cert ) ) );
    }

    public static void addCertificate( final String userName, final String alias, final X509Certificate cert ) throws GeneralSecurityException {
      String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) );
      EntityWrapper<User> db = getEntityWrapper( );
      User u = null;
      try {
        u = db.getUnique( new User( userName ) );
        X509Cert x509cert = new X509Cert( alias );
        x509cert.setPemCertificate( certPem );
        u.getCertificates( ).add( x509cert );
        db.commit( );
      } catch ( EucalyptusCloudException e ) {
        LOG.error( e, e );
        LOG.error( "username=" + userName + " \nalias=" + alias + " \ncert=" + cert );
        db.rollback( );
        throw new GeneralSecurityException( e );
      }
    }

    public static List<String> getAliases( ) {
      EntityWrapper<X509Cert> db = getEntityWrapper( );
      List<String> certAliases = Lists.newArrayList( );
      try {
        List<X509Cert> certList = db.query( new X509Cert( ) );
        for ( X509Cert cert : certList ) {
          certAliases.add( cert.getAlias( ) );
        }
      } finally {
        db.commit( );
      }
      return certAliases;
    }

    @Override
    public boolean load( ) throws Exception {
      return true;//TODO: check the DB connection here.
    }

    @Override
    public boolean start( ) throws Exception {
      return Credentials.checkAdmin( );
    }
  }

  @Provides(resource=Resource.SystemCredentials)
  public static class System extends Bootstrapper {
    private static System singleton = new System();
    private ConcurrentMap<CertAlias,X509Certificate> certs = new ConcurrentHashMap<CertAlias, X509Certificate>( );
    private ConcurrentMap<CertAlias,KeyPair> keypairs = new ConcurrentHashMap<CertAlias, KeyPair>( );

    private enum CertAlias {
      eucalyptus, walrus, jetty, hsqldb;
      public X509Certificate getCertificate() {
        return System.singleton.certs.get( this );
      }
      public PrivateKey getPrivateKey() {
        return System.singleton.keypairs.get( this ).getPrivate( );
      }
      public KeyPair getKeyPair() {
        return System.singleton.keypairs.get( this );
      }      
      private void init() throws Exception {
        if(EucaKeyStore.getInstance( ).containsEntry( this.name( ) )) {
          try {
            System.singleton.certs.put( this, EucaKeyStore.getInstance( ).getCertificate( this.name( ) ) );
            System.singleton.keypairs.put( this, EucaKeyStore.getInstance( ).getKeyPair( this.name( ),this.name( ) ) );
          } catch ( Exception e ) {
            System.singleton.certs.remove( this );
            System.singleton.keypairs.remove( this );
            LOG.fatal( "Failed to read keys from the keystore.  Please repair the keystore by hand." );
            LOG.fatal( e, e );
          }
        } else {
          System.singleton.createSystemKey( this );
        }
      }
      public boolean check() {
        return (System.singleton.keypairs.containsKey( this ) && System.singleton.certs.containsKey( this ))&&EucaKeyStore.getInstance( ).containsEntry( this.name( ) );
      }
    }
    private System( ) {}
    private void loadSystemKey( String name ) throws Exception {
      CertAlias alias = CertAlias.valueOf( name );
      if( this.certs.containsKey( alias ) ) {
        return;
      } else {
        createSystemKey( alias );
      }
    }
    private void createSystemKey( CertAlias name ) throws Exception {
      KeyTool keyTool = new KeyTool( );
      try {
        KeyPair sysKp = keyTool.getKeyPair( );
        X509Certificate sysX509 = keyTool.getCertificate( sysKp, EucalyptusProperties.getDName( name.name( ) ) );
        System.singleton.certs.put( name, sysX509 );
        System.singleton.keypairs.put( name, sysKp );
        //TODO: might need separate keystore for euca/hsqldb/ssl/jetty/etc.
        EucaKeyStore.getInstance( ).addKeyPair( name.name( ), sysX509, sysKp.getPrivate( ), name.name( ));
        EucaKeyStore.getInstance( ).store( );
      } catch ( Exception e ) {
        System.singleton.certs.remove( name );
        System.singleton.keypairs.remove( name );
        EucaKeyStore.getInstance( ).remove( );
        throw e;
      }
    }
    @Override
    public boolean load( ) throws Exception {
      Credentials.init( );
      for( CertAlias c : CertAlias.values( ) ) {
        try {
          if(!c.check( )) c.init( );
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
  

}
