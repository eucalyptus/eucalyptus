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
package com.eucalyptus.auth.euare.policy;

import static com.eucalyptus.auth.euare.common.policy.IamPolicySpec.*;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.principal.PolicyScope;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( IAM_QUOTA_POLICY_NUMBER )
public class PolicyNumberQuotaKey extends EuareQuotaKey {

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, IAM_QUOTA_POLICY_NUMBER );
  }

  @Override
  public boolean canApply( String action ) {
    return PolicySpec.qualifiedName( VENDOR_IAM, IAM_CREATEPOLICY ).equals( action );
  }

  @Override
  public String value(
      final PolicyScope scope,
      final String id,
      final String resource,
      final Long quantity
  ) throws AuthException {
    switch ( scope ) {
      case Account:
        return Long.toString( EuareQuotaUtil.countPoliciesByAccount( id ) + quantity );
    }
    return unsupportedValue( scope );
  }
}
