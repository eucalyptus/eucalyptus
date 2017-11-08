/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.util.RestrictedTypes;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( Keys.EC2_QUOTA_SECURITYGROUP_NUMBER )
public class NetworkGroupNumberQuotaKey extends QuotaKey {

  private static final String KEY = Keys.EC2_QUOTA_SECURITYGROUP_NUMBER;

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public boolean canApply( String action ) {
    return PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_CREATESECURITYGROUP ).equals( action );
  }

  @Override
  public String value( PolicyScope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case Account:
        return Long.toString( RestrictedTypes.quantityMetricFunction( CloudMetadata.NetworkGroupMetadata.class ).apply( AccountFullName.getInstance( id ) ) + quantity );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString( RestrictedTypes.quantityMetricFunction( CloudMetadata.NetworkGroupMetadata.class ).apply( UserFullName.getInstance( id ) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }
}
