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

import static com.eucalyptus.objectstorage.policy.SignatureVersionKey.getAccessKeyCredential;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.principal.AccessKeyCredential;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.pipeline.handlers.ObjectStorageAuthenticationHandler;
import com.eucalyptus.objectstorage.pipeline.handlers.ObjectStorageFormPOSTAuthenticationHandler;
import com.eucalyptus.util.Exceptions;
import com.google.common.net.HttpHeaders;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( AuthTypeKey.KEY_NAME )
public class AuthTypeKey implements ObjectStorageKey {
  static final String KEY_NAME = "s3:authtype";

  @Override
  public String value( ) throws AuthException {
    return getAuthType( );
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

  private String getAuthType( ) throws AuthException {
    final AccessKeyCredential credential = getAccessKeyCredential( );
    if ( credential != null ) try { // ensure access key credential was used to authenticate
      final Context context = Contexts.lookup( );
      final MappingHttpRequest request = context.getHttpRequest( );
      if ( context.getChannel( ).getPipeline( ).get( ObjectStorageFormPOSTAuthenticationHandler.class ) != null ) {
        return "POST";
      } else if ( context.getChannel( ).getPipeline( ).get( ObjectStorageAuthenticationHandler.class ) != null ) {
        if ( request.containsHeader( HttpHeaders.AUTHORIZATION ) ) {
          return "REST-HEADER";
        } else {
          return "REST-QUERY-STRING";
        }
      }
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for s3 authType condition", e );
    }
    return null;
  }
}
