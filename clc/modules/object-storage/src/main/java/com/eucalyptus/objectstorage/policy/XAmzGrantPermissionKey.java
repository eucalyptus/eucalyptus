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

import static com.eucalyptus.auth.policy.PolicySpec.*;
import java.util.Set;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.Permission;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *  x-amz-grant-permission (for explicit permissions), where permission can be:
 *
 *  read, write,​ read-acp,​ write​-acp,​ ​full​-control
 */
public abstract class XAmzGrantPermissionKey extends XAmzAclSupportKey {

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( S3PolicySpec.VENDOR_S3, S3PolicySpec.S3_CREATEBUCKET ) )
      .add( qualifiedName( S3PolicySpec.VENDOR_S3, S3PolicySpec.S3_PUTBUCKETACL ) )
      .add( qualifiedName( S3PolicySpec.VENDOR_S3, S3PolicySpec.S3_PUTOBJECT ) )
      .add( qualifiedName( S3PolicySpec.VENDOR_S3, S3PolicySpec.S3_PUTOBJECTACL ) )
      .add( qualifiedName( S3PolicySpec.VENDOR_S3, S3PolicySpec.S3_PUTOBJECTVERSIONACL ) )
      .build( );

  private final String keyName;
  private final Permission permission;

  protected XAmzGrantPermissionKey( final String keyName, final Permission permission ) {
    super( keyName );
    this.keyName = keyName;
    this.permission = permission;
  }

  @Override
  public String value( ) throws AuthException {
    return getGrant( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( keyName + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

  private String getGrant( ) throws AuthException {
    return grant( getAccessControlList( ) );
  }

  @Nullable
  private String grant( final AccessControlList acl ) {
    String grantStr = null;
    if ( acl != null && acl.getGrants( ) != null ) {
      for ( final Grant grant : acl.getGrants( ) ) {
        if ( grant != null &&
            !new Grantee(new CanonicalUser("", "")).equals( grant.getGrantee( ) ) &&
            permission.toString( ).equals( grant.getPermission( ) ) ) {
          if( grant.getGrantee( ).getCanonicalUser( ) != null ) {
            grantStr = "id=" + grant.getGrantee( ).getCanonicalUser( ).getID( );
          } else if ( grant.getGrantee( ).getGroup( ) != null ) {
            grantStr = "uri=" + grant.getGrantee( ).getGroup( ).getUri( );
          } else if ( grant.getGrantee( ).getEmailAddress( ) != null ) {
            grantStr = "emailAddress=" + grant.getGrantee( ).getEmailAddress( );
          }
          break;
        }
      }
    }
    return grantStr;
  }
}
