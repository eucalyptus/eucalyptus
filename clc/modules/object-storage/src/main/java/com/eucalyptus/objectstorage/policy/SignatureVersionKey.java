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

import java.util.Set;
import javax.annotation.Nullable;
import javax.security.auth.Subject;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKeyCredential;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Iterables;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( SignatureVersionKey.KEY_NAME )
public class SignatureVersionKey implements ObjectStorageKey {
  static final String KEY_NAME = "s3:signatureversion";

  @Override
  public String value( ) throws AuthException {
    return getSignatureVersion( );
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

  private String getSignatureVersion( ) throws AuthException {
    final AccessKeyCredential credential = getAccessKeyCredential( );
    if ( credential != null ) {
      switch ( credential.getSignatureVersion( ) ) {
        case v2:
          return "AWS";
        case v4:
          return "AWS4-HMAC-SHA256";
      }
    }
    return null;
  }

  @Nullable
  static AccessKeyCredential getAccessKeyCredential( ) throws AuthException {
    try {
      final Context context = Contexts.lookup( );
      final Subject subject = context.getSubject( );
      if ( subject != null ) {
        final Set<AccessKeyCredential> credentialSet = subject.getPublicCredentials( AccessKeyCredential.class );
        if ( credentialSet.size( ) == 1 ) {
          return Iterables.getOnlyElement( credentialSet );
        }
      }
      return null;
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting s3 signature value", e );
    }
  }
}
