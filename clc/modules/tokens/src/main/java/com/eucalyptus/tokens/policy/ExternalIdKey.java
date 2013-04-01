/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tokens.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.PolicyKey;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( ExternalIdKey.KEY_NAME )
public class ExternalIdKey implements Key {
  static final String KEY_NAME = "sts:externalid";

  @Override
  public String value() throws AuthException {
    return ExternalIdContext.getExternalId();
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
  }

  @Override
  public boolean canApply( final String action, final String resourceType ) {
    return PolicySpec.qualifiedName(
        PolicySpec.VENDOR_STS,
        PolicySpec.STS_ASSUME_ROLE ).equals( action );
  }
}
