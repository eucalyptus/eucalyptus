package edu.ucsb.eucalyptus;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.keys.*;
import edu.ucsb.eucalyptus.util.BaseDirectory;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import edu.ucsb.eucalyptus.util.SubDirectory;
import edu.ucsb.eucalyptus.util.UserManagement;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.UrlBase64;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class StartupChecks {

  private static String _KS_FORMAT = "bks";
  private static String _KS_PASS = EucalyptusProperties.NAME;
  private static String _USER_KS = BaseDirectory.CONF.toString() + File.separator + "users." + _KS_FORMAT.toLowerCase();
  private static Logger LOG = Logger.getLogger( StartupChecks.class );
  private static String HEADER_FSTRING = "=================================:: %-20s ::=================================";

  private static void printFail() { LOG.fatal( String.format( HEADER_FSTRING, "STARTUP FAILURE" ) ); }

  private static ConcurrentNavigableMap testMap = null;

  static {
    Security.insertProviderAt( new BouncyCastleProvider(), 1 );
    testMap = new ConcurrentSkipListMap( );
  }

  private static void fail( String... error ) {
    printFail();
    for ( String s : error )
      LOG.fatal( s );
    if ( EntityWrapper.getEntityManagerFactory( EucalyptusProperties.NAME).isOpen() )
      EntityWrapper.getEntityManagerFactory(EucalyptusProperties.NAME).close();
    System.exit( 0xEC2 );
  }

  public static boolean doChecks() {
    StartupChecks.checkDirectories();

    boolean userKs = false;
    boolean serviceKs = false;
    boolean wwwKs = false;
    try {
      userKs = UserKeyStore.getInstance().check();
      serviceKs = ServiceKeyStore.getInstance().check();
      wwwKs = EucaKeyStore.getInstance().check();
    } catch ( GeneralSecurityException e ) {
      LOG.error( e, e );
    }

    boolean hasDb = StartupChecks.checkDatabase();

    if ( !userKs || !serviceKs || !wwwKs ) {
      try {
        StartupChecks.createKeyStores();
        userKs = UserKeyStore.getInstance().check();
        serviceKs = ServiceKeyStore.getInstance().check();
        wwwKs = EucaKeyStore.getInstance().check();
      }
      catch ( Exception e ) {
        LOG.error( e, e );
        StartupChecks.fail( "Error creating keystore instance." );
      }
    }
    if ( !hasDb ) {
      StartupChecks.createDb();
      hasDb = StartupChecks.checkDatabase();
    }

    if ( !hasDb || !userKs || !serviceKs || !wwwKs ) {
      LOG.fatal( String.format( HEADER_FSTRING, "STARTUP FAILURE" ) );
      if ( !hasDb )
        LOG.fatal( "Failed to initialize the database in: " + SubDirectory.DB );
      if ( !userKs )
        LOG.fatal( "Failed to read the user keystore: " + UserKeyStore.getInstance().getFileName() );
      if ( !wwwKs )
        LOG.fatal( "Failed to read the www keystore: " + EucaKeyStore.getInstance().getFileName() );
      if ( !serviceKs )
        LOG.fatal( "Failed to read the services keystore: " + ServiceKeyStore.getInstance().getFileName() );
      StartupChecks.fail( "See errors messages above." );
    }

    EntityWrapper<UserGroupInfo> db3 = new EntityWrapper<UserGroupInfo>();
    try {
      db3.getUnique( new UserGroupInfo("all") );
    } catch ( EucalyptusCloudException e ) {
      db3.add( new UserGroupInfo("all") );
    } finally {
      db3.commit();
    }

    EntityWrapper<ImageInfo> db2 = new EntityWrapper<ImageInfo>();
    List<ImageInfo> imageList = db2.query( new ImageInfo() );
    for ( ImageInfo image : imageList ) {
      if ( image.getImageLocation().split( "/" ).length != 2 ) {
        LOG.info( "Image with invalid location, needs to be reregistered: " + image.getImageId() );
        LOG.info( "Removing entry for: " + image.getImageId() );
        db2.delete( image );
      }
    }
    db2.commit();

    try {
      StartupChecks.importKeys( AbstractKeyStore.getGenericKeystore( _USER_KS, _KS_PASS, _KS_FORMAT ), UserKeyStore.getInstance() );
    } catch ( IOException e ) {
    } catch ( GeneralSecurityException e ) {
      LOG.error( e );
      LOG.debug( e, e );
    }

    return true;
  }

  private static void importKeys( final AbstractKeyStore ks, final AbstractKeyStore newKs ) throws GeneralSecurityException, IOException {
    for ( String alias : ks.getAliases() )
      if ( !newKs.containsEntry( alias ) && !EucalyptusProperties.NAME.equals( alias ) ) {
        LOG.info( String.format( "Importing -> alias=%10s", alias ) );
        newKs.addCertificate( alias, ks.getCertificate( alias ) );
      } else if ( !newKs.containsEntry( "v13-" + alias ) && EucalyptusProperties.NAME.equals( alias ) ) {
        LOG.info( String.format( "Importing -> alias=%10s", alias ) );
        newKs.addKeyPair( "v13-" + EucalyptusProperties.NAME, ks.getCertificate( alias ),
                          ( PrivateKey ) ks.getKey( alias, EucalyptusProperties.NAME ), EucalyptusProperties.NAME );
      }
    newKs.store();
    LOG.info("Backporting keys into database");
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      for( UserInfo user : db.query( new UserInfo() ) ) {
        for( CertificateInfo certInfo : user.getCertificates() ) {
          LOG.info( String.format( "- Trying for user %s with alias %s", user.getUserName(), certInfo.getCertAlias() ) );
          if( newKs.containsEntry( certInfo.getCertAlias() ) ) {
            try {
              X509Certificate cert = newKs.getCertificate( certInfo.getCertAlias() );
              certInfo.setValue( new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) ) );
              db.recast( CertificateInfo.class ).merge( certInfo );
              LOG.info( String.format( "- Backedup for user %s with alias %s", user.getUserName(), certInfo.getCertAlias() ) );
            } catch ( Exception e ) {
              LOG.error( String.format( "- Failed for user %s with alias %s: %s", user.getUserName(), certInfo.getCertAlias(), e ) );
            }
          }
        }
      }
    } catch ( Exception e ) {}
    db.commit();
  }

  private static boolean checkDatabase() { /** initialize the counters **/

    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo adminUser = db.getUnique( new UserInfo( "admin" ) );
      return true;
    }
    catch ( EucalyptusCloudException e ) {
      return false;
    }
    finally {
      db.rollback();
    }

  }

  private static boolean createDb() {
    EntityWrapper<VmType> db2 = new EntityWrapper<VmType>();
    try {
      db2.add( new VmType( "m1.small", 1, 10, 128 ) );
      db2.add( new VmType( "c1.medium", 1, 10, 256 ) );
      db2.add( new VmType( "m1.large", 2, 10, 512 ) );
      db2.add( new VmType( "m1.xlarge", 2, 20, 1024 ) );
      db2.add( new VmType( "c1.xlarge", 4, 20, 2048 ) );
      db2.commit();
    }
    catch ( Exception e ) {
      db2.rollback();
      return false;
    }
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    try {
      UserInfo u = UserManagement.generateAdmin();
      db.add( u );
      UserGroupInfo allGroup = new UserGroupInfo( "all" );
      db.getSession().persist( new Counters() );
      db.commit();
      return true;
    }
    catch ( Exception e ) {
      db.rollback();
      return false;
    }
  }

  private static void createKeyStores() throws IOException, GeneralSecurityException {
    LOG.info( String.format( HEADER_FSTRING, "Create service keystore" ) );
    AbstractKeyStore serviceKeyStore = ServiceKeyStore.getInstance();
    LOG.info( String.format( HEADER_FSTRING, "Create user keystore" ) );
    AbstractKeyStore userKeyStore = UserKeyStore.getInstance();
    LOG.info( String.format( HEADER_FSTRING, "Create www keystore" ) );
    AbstractKeyStore eucaKeyStore = EucaKeyStore.getInstance();

    try {

      LOG.info( String.format( HEADER_FSTRING, "Create system keys" ) );
      KeyTool keyTool = new KeyTool();

      KeyPair sysKp = keyTool.getKeyPair();
      X509Certificate sysX509 = keyTool.getCertificate( sysKp, EucalyptusProperties.getDName( EucalyptusProperties.NAME ) );
      keyTool.writePem( String.format( "%s/cloud-cert.pem", SubDirectory.KEYS.toString() ), sysX509 );
      keyTool.writePem( String.format( "%s/cloud-pk.pem", SubDirectory.KEYS.toString() ), sysKp.getPrivate() );

      KeyPair wwwKp = keyTool.getKeyPair();
      X509Certificate wwwX509 = keyTool.getCertificate( wwwKp, EucalyptusProperties.getDName( EucalyptusProperties.WWW_NAME ) );

      LOG.info( String.format( HEADER_FSTRING, "Store system keys" ) );
      serviceKeyStore.addKeyPair( EucalyptusProperties.NAME, sysX509, sysKp.getPrivate(), EucalyptusProperties.NAME );
      userKeyStore.addKeyPair( EucalyptusProperties.NAME, sysX509, sysKp.getPrivate(), EucalyptusProperties.NAME );
      eucaKeyStore.addKeyPair( EucalyptusProperties.NAME, sysX509, sysKp.getPrivate(), EucalyptusProperties.NAME );
      eucaKeyStore.addKeyPair( EucalyptusProperties.WWW_NAME, wwwX509, wwwKp.getPrivate(), EucalyptusProperties.NAME );
      serviceKeyStore.store();
      userKeyStore.store();
      eucaKeyStore.store();
    }
    catch ( Exception e ) {
      LOG.fatal(e,e);
      serviceKeyStore.remove();
      userKeyStore.remove();
      eucaKeyStore.remove();
      new File( String.format( "%s/cloud-pk.pem", SubDirectory.KEYS.toString() ) ).delete();
      new File( String.format( "%s/cloud-cert.pem", SubDirectory.KEYS.toString() ) ).delete();
      StartupChecks.fail( "Eucalyptus requires the unlimited strength jurisdiction policy files for the JCE.",
                          "Please see the documentation for more information." );
    }
  }

  private static void checkDirectories() {
    for ( BaseDirectory dir : BaseDirectory.values() )
      dir.check();
    SubDirectory.KEYS.create();
  }

}
