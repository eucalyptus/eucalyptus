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
package com.eucalyptus.compute.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.Bool;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( EncryptedKey.KEY_NAME )
public class EncryptedKey extends VolumeComputeKey {
  static final String KEY_NAME = "ec2:encrypted";

  @Override
  public String value( ) throws AuthException {
    final Boolean volumeEncrypted = ComputePolicyContext.getVolumeEncrypted( );
    return volumeEncrypted == null ? null : String.valueOf( volumeEncrypted );

  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !Bool.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". Boolean conditions are required." );
    }
  }
}
