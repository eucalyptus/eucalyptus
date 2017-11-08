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
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.ImmutableSet;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( XAmzCopySourceKey.KEY_NAME )
public class XAmzCopySourceKey implements ObjectStorageKey {
  static final String KEY_NAME = "s3:x-amz-copy-source";

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_S3, S3_PUTOBJECT ) )
      .build( );

  @Override
  public String value( ) throws AuthException {
    return getCopySource( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) {
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

  private String getCopySource( ) throws AuthException {
    try {
      final BaseMessage request = Contexts.lookup( ).getRequest( );
      final String copySource;
      if ( request instanceof CopyObjectType ) {
        final CopyObjectType copyObjectType = (CopyObjectType) request;
        String copySourceValue = "/" + copyObjectType.getSourceBucket( ) + "/" + copyObjectType.getSourceObject( );
        if ( copyObjectType.getSourceVersionId( ) != null ) {
          copySourceValue = copySourceValue + "?versionId=" + copyObjectType.getSourceVersionId( );
        }
        copySource = copySourceValue;
      } else {
        throw new AuthException( "Error getting value for s3 metadata directive condition" );
      }
      return copySource;
    } catch ( Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for s3 copy source condition", e );
    }
  }
}
