package com.eucalyptus.keys;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
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
    } catch ( Exception e ) {
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
    } catch ( Exception e ) {
      db.rollback( );
      throw new EucalyptusCloudException( "Failed to find key pair: " + keyName, e );
    }
    return key;
  }

}
