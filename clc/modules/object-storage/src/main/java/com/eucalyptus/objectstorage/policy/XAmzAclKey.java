/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.objectstorage.policy;

import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;

/**
 *
 */
@PolicyKey( XAmzAclKey.KEY_NAME )
public class XAmzAclKey extends XAmzAclSupportKey {
  static final String KEY_NAME = "s3:x-amz-acl";

  public XAmzAclKey( ) {
    super( KEY_NAME );
  }

  @Override
  public String value( ) throws AuthException {
    return getXAmzAcl( );
  }

  private String getXAmzAcl( ) throws AuthException {
    return cannedAcl( getAccessControlList( ) );
  }

  @Nullable
  private String cannedAcl( final AccessControlList acl ) {
    String cannedAcl = null;
    if ( acl != null && acl.getGrants( ) != null && !acl.getGrants( ).isEmpty( ) ) {
      final Grant grant = acl.getGrants( ).get( 0 );
      if ( grant != null &&
          new Grantee(new CanonicalUser("", "")).equals( grant.getGrantee( ) ) &&
          isCannedAcl( grant.getPermission( ) ) ) {
        cannedAcl = grant.getPermission( );
      }
    }
    return cannedAcl;
  }
}
