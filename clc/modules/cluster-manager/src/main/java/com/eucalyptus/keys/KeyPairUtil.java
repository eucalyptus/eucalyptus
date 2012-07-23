/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.keys;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.Lists;

public class KeyPairUtil {
  private static Logger LOG = Logger.getLogger( KeyPairUtil.class );

  public static List<SshKeyPair> getUserKeyPairs( OwnerFullName ownerFullName ) {
    EntityWrapper<SshKeyPair> db = EntityWrapper.get( SshKeyPair.class );;
    List<SshKeyPair> keys = Lists.newArrayList( );
    try {
      keys = db.query( SshKeyPair.named( ownerFullName, null ) );
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
