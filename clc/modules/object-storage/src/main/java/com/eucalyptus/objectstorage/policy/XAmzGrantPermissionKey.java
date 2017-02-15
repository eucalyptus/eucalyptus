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
