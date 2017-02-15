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
