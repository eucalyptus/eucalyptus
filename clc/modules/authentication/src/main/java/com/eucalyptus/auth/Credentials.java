package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.UrlBase64;

import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;

public class Credentials {
  private static Logger LOG            = Logger.getLogger( Credentials.class );
  private static String FORMAT         = "pkcs12";
  private static String KEY_STORE_PASS = "eucalyptus";                         //TODO: change the way this is handled
  private static String FILENAME       = "euca.p12";
  private static String DB_NAME        = "eucalyptus_auth";

  public static void addUser( String userName ) throws UserExistsException {
    addUser( userName, false );
  }

  public static void addUser( String userName, Boolean isAdmin ) throws UserExistsException {
    User newUser = new User( );
    newUser.setUserName( userName );
    String queryId = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, false ).replaceAll( "\\p{Punct}", "" );
    String secretKey = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, true ).replaceAll( "\\p{Punct}", "" );
    newUser.setQueryId( queryId );
    newUser.setSecretKey( secretKey );
    newUser.setIsAdministrator( true );
    EntityWrapper<User> db = new EntityWrapper<User>( DB_NAME );
    db.add( newUser );
    db.commit( );
//    newUser.
  }

  public static class Users {
    public static boolean hasCertificate( final String alias ) {
      X509Cert certInfo = null;
      EntityWrapper<X509Cert> db = new EntityWrapper<X509Cert>( DB_NAME );
      try {
        certInfo = db.getUnique( new X509Cert( alias ) );
      } catch ( EucalyptusCloudException e ) {
      } finally {
        db.commit( );
      }
      return certInfo != null;
    }

    public static X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
      EntityWrapper<X509Cert> db = new EntityWrapper<X509Cert>( DB_NAME );
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
      EntityWrapper<X509Cert> db = new EntityWrapper<X509Cert>( DB_NAME );
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
      EntityWrapper<User> db = new EntityWrapper<User>( DB_NAME );
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
      EntityWrapper<User> db = new EntityWrapper<User>( DB_NAME );
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

    public static String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException {
      return getCertificateAlias( new String( Hashes.getPemBytes( cert ) ) );
    }

    public static void addCertificate( final String userName, final String alias, final X509Certificate cert ) throws GeneralSecurityException {
      X509Cert x509cert = new X509Cert( alias );
      String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) );
      x509cert.setPemCertificate( certPem );
      EntityWrapper<User> db = new EntityWrapper<User>( );
      User u = null;
      try {
        u = db.getUnique( new User(userName) );
        u.getCertificates( ).add( x509cert );
        db.commit( );
      } catch ( EucalyptusCloudException e ) {
        LOG.error("username="+userName+" \nalias="+alias+" \ncert="+cert);
        db.rollback();
      }
    }

    public static List<String> getAliases( ) {
      EntityWrapper<X509Cert> db = new EntityWrapper<X509Cert>( DB_NAME );
      List<String> certAliases = Lists.newArrayList( );
      try {
        List<X509Cert> certList = db.query( new X509Cert( ) );
        for ( X509Cert cert : certList ) {
          certAliases.add( cert.getAlias() );
        }
      } finally {
        db.commit( );
      }
      return certAliases;
    }
  }

}
