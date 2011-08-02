package com.eucalyptus.keys;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

public class KeyPairUtil {
  private static Logger LOG = Logger.getLogger( KeyPairUtil.class );

  public static List<SshKeyPair> getUserKeyPairs( UserFullName userFullName ) {
    EntityWrapper<SshKeyPair> db = EntityWrapper.get( SshKeyPair.class );;
    List<SshKeyPair> keys = Lists.newArrayList( );
    try {
      keys = db.query( new SshKeyPair( userFullName ) );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return keys;
  }

  public static SshKeyPair deleteUserKeyPair( UserFullName userFullName, String keyName ) throws EucalyptusCloudException {
    EntityWrapper<SshKeyPair> db = EntityWrapper.get( SshKeyPair.class );;
    SshKeyPair key = null;
    try {
      key = db.getUnique( new SshKeyPair( userFullName, keyName ) );
      db.delete( key );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to find key pair: " + keyName, e );
    }
    return key;
  }
  public static PrivateKey createUserKeyPair( UserFullName userName, String keyName ) throws EucalyptusCloudException {
    SshKeyPair newKey = new SshKeyPair( userName, keyName );
    KeyPair newKeys = null;
    try {
      newKeys = Certs.generateKeyPair( );
      String authKeyString = getAuthKeyString( userName, newKeys );
      newKey.setPublicKey( authKeyString );
      newKey.setFingerPrint( Certs.getFingerPrint( newKeys.getPrivate( ) ) );
    } catch ( Exception e ) {
      throw new EucalyptusCloudException( "KeyPair generation error: Key pair creation failed.", e );
    }
    EntityWrapper<SshKeyPair> db = EntityWrapper.get( SshKeyPair.class );;
    try {
      db.add( newKey );
      db.commit( );
    } catch ( Throwable e1 ) {
      db.rollback( );
      throw new EucalyptusCloudException( "KeyPair generation error. Key pair: " + keyName + " already exists." );
    }
    return newKeys.getPrivate( );
  }

  private static String getAuthKeyString( UserFullName userName, KeyPair newKeys ) {
    RSAPublicKey publicKey = ( RSAPublicKey ) newKeys.getPublic( );
    byte[] keyType = "ssh-rsa".getBytes( );
    byte[] expBlob = publicKey.getPublicExponent( ).toByteArray( );
    byte[] modBlob = publicKey.getModulus( ).toByteArray( );
    byte[] authKeyBlob = new byte[3 * 4 + keyType.length + expBlob.length + modBlob.length];

    byte[] lenArray = null;
    lenArray = BigInteger.valueOf( keyType.length ).toByteArray( );
    System.arraycopy( lenArray, 0, authKeyBlob, 4 - lenArray.length, lenArray.length );
    System.arraycopy( keyType, 0, authKeyBlob, 4, keyType.length );

    lenArray = BigInteger.valueOf( expBlob.length ).toByteArray( );
    System.arraycopy( lenArray, 0, authKeyBlob, 4 + keyType.length + 4 - lenArray.length, lenArray.length );
    System.arraycopy( expBlob, 0, authKeyBlob, 4 + ( 4 + keyType.length ), expBlob.length );

    lenArray = BigInteger.valueOf( modBlob.length ).toByteArray( );
    System.arraycopy( lenArray, 0, authKeyBlob, 4 + expBlob.length + 4 + keyType.length + 4 - lenArray.length, lenArray.length );
    System.arraycopy( modBlob, 0, authKeyBlob, 4 + ( 4 + expBlob.length + ( 4 + keyType.length ) ), modBlob.length );

    String authKeyString = String.format( "%s %s %s@eucalyptus", new String( keyType ), new String( Base64.encode( authKeyBlob ) ), userName.toString( ) );
    return authKeyString;
  }

}
