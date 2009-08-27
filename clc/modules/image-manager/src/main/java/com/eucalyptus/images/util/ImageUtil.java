package com.eucalyptus.images.util;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.UserCredentialProvider;
import com.eucalyptus.util.EntityWrapper;

import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.ws.ImageManager;

public class ImageUtil {
  private static Logger LOG = Logger.getLogger( ImageUtil.class );
  public static String generateImageId( final String imagePrefix, final String imageLocation ) {
    Adler32 hash = new Adler32();
    String key = imageLocation + System.currentTimeMillis();
    hash.update( key.getBytes() );
    String imageId = String.format( "%s-%08X", imagePrefix, hash.getValue() );
    return imageId;
  }

  public static String newImageId( final String imagePrefix, final String imageLocation ) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
    ImageInfo query = new ImageInfo();
    query.setImageId( generateImageId( imagePrefix, imageLocation ) );
    LOG.info( "Trying to lookup using created AMI id=" + query.getImageId() );
    for ( ; db.query( query ).size() != 0; query.setImageId( generateImageId( imagePrefix, imageLocation ) ) ) ;
    db.commit();
    LOG.info( "Assigning imageId=" + query.getImageId() );
    return query.getImageId();
  }

  public static boolean verifyManifestSignature( final String signature, final String alias, String pad ) {
    boolean ret = false;
    try {
      Signature sigVerifier = Signature.getInstance( "SHA1withRSA" );
      X509Certificate cert = UserCredentialProvider.getCertificate( alias );
      PublicKey publicKey = cert.getPublicKey();
      sigVerifier.initVerify( publicKey );
      sigVerifier.update( pad.getBytes() );
      sigVerifier.verify( Hashes.hexToBytes( signature ) );
      ret = true;
    } catch ( Exception ex ) {
      ImageManager.LOG.warn( ex.getMessage() );
    }
    return ret;
  }

}
