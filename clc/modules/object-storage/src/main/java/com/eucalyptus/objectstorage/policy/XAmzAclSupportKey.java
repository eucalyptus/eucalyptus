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

import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_CREATEBUCKET;
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_PUTBUCKETACL;
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_PUTOBJECT;
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_PUTOBJECTACL;
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.S3_PUTOBJECTVERSIONACL;
import static com.eucalyptus.objectstorage.policy.S3PolicySpec.VENDOR_S3;
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import java.util.Arrays;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import net.sf.json.JSONException;

/**
 *
 */
public abstract class XAmzAclSupportKey implements ObjectStorageKey {

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_S3, S3_CREATEBUCKET ) )
      .add( qualifiedName( VENDOR_S3, S3_PUTBUCKETACL ) )
      .add( qualifiedName( VENDOR_S3, S3_PUTOBJECT ) )
      .add( qualifiedName( VENDOR_S3, S3_PUTOBJECTACL ) )
      .add( qualifiedName( VENDOR_S3, S3_PUTOBJECTVERSIONACL ) ) // NOTE: supported in policy but not implemented
      .build( );

  private static final Set<String> cannedAcls = ImmutableSet.copyOf( Iterables.transform(
      Arrays.asList( ObjectStorageProperties.CannedACL.values( ) ),
      Functions.toStringFunction( )
  ) );

  private final String keyName;

  public XAmzAclSupportKey( final String keyName ) {
    this.keyName = keyName;
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( keyName + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) {
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

  protected AccessControlList getAccessControlList( ) throws AuthException {
    try {
      final BaseMessage request = Contexts.lookup( ).getRequest( );
      final AccessControlList accessControlList;
      if ( request instanceof CreateBucketType ) {
        accessControlList = ( (CreateBucketType) request ).getAccessControlList( );
      } else if ( request instanceof PutObjectType ) {
        accessControlList = ( (PutObjectType) request ).getAccessControlList( );
      } else if ( request instanceof CopyObjectType ) {
        accessControlList = ( (CopyObjectType) request ).getAccessControlList( );
      } else if ( request instanceof PostObjectType ) {
        accessControlList = ( (PostObjectType) request ).getAccessControlList( );
      } else if ( request instanceof InitiateMultipartUploadType ) {
        accessControlList = ( (InitiateMultipartUploadType) request ).getAccessControlList( );
      } else if ( request instanceof SetObjectAccessControlPolicyType ) {
        final AccessControlPolicy accessControlPolicy =
            ( (SetObjectAccessControlPolicyType) request ).getAccessControlPolicy( );
        accessControlList = accessControlPolicy == null ? null : accessControlPolicy.getAccessControlList( );
      } else if ( request instanceof SetBucketAccessControlPolicyType ) {
        final AccessControlPolicy accessControlPolicy =
            ( (SetBucketAccessControlPolicyType) request ).getAccessControlPolicy( );
        accessControlList = accessControlPolicy == null ? null : accessControlPolicy.getAccessControlList( );
      } else {
        throw new AuthException( "Error getting value for "+keyName+" condition" );
      }
      return accessControlList;
    } catch ( Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for "+keyName+" condition", e );
    }
  }

  protected boolean isCannedAcl( final String value ) {
    return cannedAcls.contains( value );
  }
}
