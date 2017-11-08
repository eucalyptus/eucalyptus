/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
