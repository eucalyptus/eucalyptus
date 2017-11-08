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

import static com.eucalyptus.auth.policy.PolicySpec.S3_CREATEBUCKET;
import static com.eucalyptus.auth.policy.PolicySpec.S3_PUTBUCKETACL;
import static com.eucalyptus.auth.policy.PolicySpec.S3_PUTOBJECT;
import static com.eucalyptus.auth.policy.PolicySpec.S3_PUTOBJECTACL;
import static com.eucalyptus.auth.policy.PolicySpec.S3_PUTOBJECTVERSIONACL;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_S3;
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
