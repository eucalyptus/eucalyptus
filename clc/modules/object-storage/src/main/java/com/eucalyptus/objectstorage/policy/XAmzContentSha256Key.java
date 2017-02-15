/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.objectstorage.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Strings;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( XAmzContentSha256Key.KEY_NAME )
public class XAmzContentSha256Key implements ObjectStorageKey {
  static final String KEY_NAME = "s3:x-amz-content-sha256";

  @Override
  public String value( ) throws AuthException {
    return getXAmzContentSha256( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public boolean canApply( final String action ) {
    return action != null && action.startsWith( "s3:" );
  }

  private String getXAmzContentSha256( ) throws AuthException {
    try {
      final MappingHttpRequest request = Contexts.lookup( ).getHttpRequest( );
      return Strings.emptyToNull( request.getHeader( S3V4Authentication.AWS_CONTENT_SHA_HEADER ) );
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for s3 x-amz-content-sha256 condition", e );
    }
  }
}

