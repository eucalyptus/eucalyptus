/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
