package com.eucalyptus.images;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.List;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.ServiceBootstrapper;
import com.eucalyptus.images.util.ImageUtil;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.client.ServiceProxy;
import com.eucalyptus.ws.util.Messaging;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.cloud.entities.CertificateInfo;
import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.cloud.ws.ImageManager;
import edu.ucsb.eucalyptus.msgs.CacheImageType;
import edu.ucsb.eucalyptus.msgs.CheckImageType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.RegisterImageType;
import edu.ucsb.eucalyptus.util.Admin;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import edu.ucsb.eucalyptus.util.XMLParser;

public class WalrusProxy {
    
  public static void checkValid( ImageInfo imgInfo ) {
    String[] parts = imgInfo.getImageLocation().split( "/" );
    CheckImageType check = new CheckImageType();
    check.setUserId( imgInfo.getImageOwnerId( ) );
    check.setBucket( parts[ 0 ] );
    check.setKey( parts[ 1 ] );
    ServiceProxy.lookup( Component.walrus, Component.walrus.name( ) ).dispatch( check );
  }

  public static void triggerCaching( ImageInfo imgInfo ) {
    String[] parts = imgInfo.getImageLocation().split( "/" );
    CacheImageType cache = new CacheImageType();
    cache.setUserId( imgInfo.getImageOwnerId( ) );
    cache.setBucket( parts[ 0 ] );
    cache.setKey( parts[ 1 ] );
    ServiceProxy.lookup( Component.walrus, Component.walrus.name( ) ).dispatch( cache );
  }

  public static void invalidate( ImageInfo imgInfo ) {
    String[] parts = imgInfo.getImageLocation().split( "/" );
    imgInfo.setImageState( "deregistered" );
    try {
      ServiceProxy.lookup( Component.walrus, Component.walrus.name( ) ).dispatch( new FlushCachedImageType( parts[ 0 ], parts[ 1 ] ) );
    } catch ( Exception e ) {}
  }

  public static Document getManifestData( String userId, String bucketName, String objectName ) throws EucalyptusCloudException {
    GetObjectResponseType reply = null;
    try {
      GetObjectType msg = new GetObjectType( bucketName, objectName, true, false, true );
      msg.setUserId( userId );
      
      reply = ( GetObjectResponseType ) ServiceProxy.lookup( Component.walrus, Component.walrus.name( ) ).send( msg );
    }
    catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName );
    }
  
    Document inputSource = null;
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      inputSource = builder.parse( new ByteArrayInputStream( reply.getBase64Data().getBytes() ) );
    }
    catch ( Exception e ) {
      throw new EucalyptusCloudException( "Failed to read manifest file: " + bucketName + "/" + objectName );
    }
    return inputSource;
  }

  public static GetBucketAccessControlPolicyResponseType getBucketAcl( RegisterImageType request, String[] imagePathParts ) throws EucalyptusCloudException {
    GetBucketAccessControlPolicyType getBukkitInfo = Admin.makeMsg( GetBucketAccessControlPolicyType.class, request );
    getBukkitInfo.setBucket( imagePathParts[ 0 ] );
    GetBucketAccessControlPolicyResponseType reply = ( GetBucketAccessControlPolicyResponseType ) Messaging.send( WalrusProperties.WALRUS_REF, getBukkitInfo );
    return reply;
  }

  public static void verifyManifestIntegrity( final ImageInfo imgInfo ) throws EucalyptusCloudException {
    String[] imagePathParts = imgInfo.getImageLocation().split( "/" );
    GetObjectResponseType reply = null;
    GetObjectType msg = new GetObjectType( imagePathParts[ 0 ], imagePathParts[ 1 ], true, false, true );
    msg.setUserId( Component.eucalyptus.name() );
    msg.setEffectiveUserId( Component.eucalyptus.name() );
    try {
      reply = ( GetObjectResponseType ) Messaging.send( WalrusProperties.WALRUS_REF, msg );
    } catch ( EucalyptusCloudException e ) {
      ImageManager.LOG.error( e );
      ImageManager.LOG.debug( e, e );
      throw new EucalyptusCloudException( "Invalid manifest reference: " + imgInfo.getImageLocation() );
    }
  
    if ( reply == null || reply.getBase64Data() == null ) throw new EucalyptusCloudException( "Invalid manifest reference: " + imgInfo.getImageLocation() );
    XMLParser parser = new XMLParser( reply.getBase64Data() );
    String encryptedKey = parser.getValue( "//ec2_encrypted_key" );
    String encryptedIV = parser.getValue( "//ec2_encrypted_iv" );
    String signature = parser.getValue( "//signature" );
    String image = parser.getXML( "image" );
    String machineConfiguration = parser.getXML( "machine_configuration" );
  
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    List<String> aliases = Lists.newArrayList();
    List<UserInfo> users = db.query( new UserInfo() );
    for ( UserInfo user : users )
      for ( CertificateInfo certInfo : user.getCertificates() )
        aliases.add( certInfo.getCertAlias() );
    boolean found = false;
    for ( String alias : aliases )
      found |= ImageUtil.verifyManifestSignature( signature, alias, machineConfiguration + image );
    if ( !found ) throw new EucalyptusCloudException( "Invalid Manifest: Failed to verify signature." );
  
    try {
      PrivateKey pk = SystemCredentialProvider.getCredentialProvider(Component.eucalyptus).getPrivateKey();
      Cipher cipher = Cipher.getInstance( "RSA/ECB/PKCS1Padding" );
      cipher.init( Cipher.DECRYPT_MODE, pk );
      cipher.doFinal( Hashes.hexToBytes( encryptedKey ) );
      cipher.doFinal( Hashes.hexToBytes( encryptedIV ) );
    } catch ( Exception ex ) {
      throw new EucalyptusCloudException( "Invalid Manifest: Failed to recover keys." );
    }
  }

}
