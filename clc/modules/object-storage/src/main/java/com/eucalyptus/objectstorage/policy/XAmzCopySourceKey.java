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
      .add( qualifiedName( S3PolicySpec.VENDOR_S3, S3PolicySpec.S3_PUTOBJECT ) )
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
