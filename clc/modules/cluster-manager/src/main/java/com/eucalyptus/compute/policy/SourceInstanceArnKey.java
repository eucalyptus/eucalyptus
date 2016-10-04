/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
import com.eucalyptus.auth.policy.condition.ArnConditionOp;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes.RoleInstanceProfileSecurityTokenAttributes;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( value = SourceInstanceArnKey.KEY_NAME, evaluationConstraints = Key.EvaluationConstraint.ReceivingHost )
public class SourceInstanceArnKey implements ComputeKey {
  static final String KEY_NAME = "ec2:sourceinstancearn";

  @Override
  public String value( ) throws AuthException {
    return RoleSecurityTokenAttributes.fromContext( RoleInstanceProfileSecurityTokenAttributes.class )
        .transform( RoleInstanceProfileSecurityTokenAttributes::getInstanceArn )
        .orNull( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !ArnConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". ARN conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
    Validation.assertArnValue( value );
  }

  @Override
  public boolean canApply( final String action ) {
    return true;
  }
}
