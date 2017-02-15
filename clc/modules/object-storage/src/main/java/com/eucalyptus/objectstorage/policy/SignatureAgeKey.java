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
import com.eucalyptus.auth.policy.condition.NumericConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.principal.AccessKeyCredential;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( SignatureAgeKey.KEY_NAME )
public class SignatureAgeKey implements ObjectStorageKey {
  static final String KEY_NAME = "s3:signatureage";

  @Override
  public String value() throws AuthException {
    return getSignatureAge( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !NumericConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". Numeric conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) {
  }

  @Override
  public boolean canApply( final String action ) {
    return action != null && action.startsWith( "s3:" );
  }

  private String getSignatureAge( ) throws AuthException {
    final AccessKeyCredential credential = getAccessKeyCredential( );
    if ( credential != null ) {
      final Long timestamp = credential.getSignatureTimestamp( );
      if ( timestamp != null ) {
        return String.valueOf( Math.max( 0L, System.currentTimeMillis( ) - timestamp ) );
      }
    }
    return null;
  }
}
