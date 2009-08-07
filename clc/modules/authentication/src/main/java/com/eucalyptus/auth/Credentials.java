package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.UrlBase64;

import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.entities.CertificateInfo;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;

public class Credentials {
  private static Logger LOG            = Logger.getLogger( Credentials.class );
  private static String FORMAT         = "pkcs12";
  private static String KEY_STORE_PASS = "eucalyptus";                          //TODO: change the way this is handled
  private static String FILENAME       = "euca.p12";

  public static class Users {
    public static boolean hasCertificate( final String alias ) {
      CertificateInfo certInfo = null;
      EntityWrapper<CertificateInfo> db = new EntityWrapper<CertificateInfo>( );
      try {
        certInfo = db.getUnique( new CertificateInfo( alias ) );
      } catch ( EucalyptusCloudException e ) {
      } finally {
        db.commit( );
      }
      return certInfo != null;
    }

    public static X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
      EntityWrapper<CertificateInfo> db = new EntityWrapper<CertificateInfo>( );
      try {
        CertificateInfo certInfo = db.getUnique( new CertificateInfo( alias ) );
        byte[] certBytes = UrlBase64.decode( certInfo.getValue( ).getBytes( ) );
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
      EntityWrapper<CertificateInfo> db = new EntityWrapper<CertificateInfo>( );
      CertificateInfo certInfo = new CertificateInfo( );
      certInfo.setValue( new String( UrlBase64.encode( certPem.getBytes( ) ) ) );
      try {
        certAlias = db.getUnique( certInfo ).getCertAlias( );
      } catch ( EucalyptusCloudException e ) {
        throw new GeneralSecurityException( e );
      } finally {
        db.commit( );
      }
      return certAlias;
    }
    
    public static String getSecretKey( String queryId ) throws GeneralSecurityException {
      String secretKey = null;
      EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>( );
      UserInfo searchUser = new UserInfo();
      searchUser.setQueryId( queryId );
      try {
        UserInfo user = db.getUnique( searchUser );
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

    public static void addCertificate( final String alias, final X509Certificate cert ) throws GeneralSecurityException {
      throw new GeneralSecurityException( "This keystore is read-only." );
    }

    public static List<String> getAliases( ) {
      EntityWrapper<CertificateInfo> db = new EntityWrapper<CertificateInfo>( );
      List<String> certAliases = Lists.newArrayList( );
      try {
        List<CertificateInfo> certList = db.query( new CertificateInfo( ) );
        for ( CertificateInfo cert : certList ) {
          certAliases.add( cert.getCertAlias( ) );
        }
      } finally {
        db.commit( );
      }
      return certAliases;
    }
  }

}
