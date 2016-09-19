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
package com.eucalyptus.auth.euare;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.PolicyScope;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( IamPolicySpec.IAM_QUOTA_SERVER_CERTIFICATE_NUMBER )
public class ServerCertificateNumberQuotaKey extends QuotaKey {

  private static final String KEY = IamPolicySpec.IAM_QUOTA_SERVER_CERTIFICATE_NUMBER;

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public boolean canApply( String action ) {
    return PolicySpec.qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_UPLOADSERVERCERTIFICATE ).equals( action );
  }

  @Override
  public String value( PolicyScope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case Account:
        return Long.toString( EuareQuotaUtil.countServerCertificatesByAccount( id ) + quantity );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return NOT_SUPPORTED;
    }
    throw new AuthException( "Invalid scope" );
  }
}
