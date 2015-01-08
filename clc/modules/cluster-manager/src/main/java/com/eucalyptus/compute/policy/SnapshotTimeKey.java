/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.policy;

import java.util.Date;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.DateConditionOp;
import com.eucalyptus.auth.policy.key.Iso8601DateParser;
import com.eucalyptus.auth.policy.key.PolicyKey;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( SnapshotTimeKey.KEY_NAME )
public class SnapshotTimeKey extends SnapshotComputeKey {
  static final String KEY_NAME = "ec2:snapshottime";

  @Override
  public String value( ) throws AuthException {
    final Date time = ComputePolicyContext.getSnapshotTime();
    return time == null ?  null : Iso8601DateParser.toString( time );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !DateConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". Date conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
    Validation.assertDateValue( value );
  }
}
