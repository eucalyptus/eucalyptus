/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadata.AddressMetadata;
import com.eucalyptus.cloud.CloudMetadata.VolumeMetadata;
import com.eucalyptus.util.RestrictedTypes;

@PolicyKey( Keys.EC2_QUOTA_ADDRESS_NUMBER )
public class AddressNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_ADDRESS_NUMBER;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_ALLOCATEADDRESS ).equals( action ) &&
        PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_ADDRESS ).equals( resourceType ) ) {
     return true;
   }
   return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( RestrictedTypes.quantityMetricFunction( AddressMetadata.class ).apply( AccountFullName.getInstance( id ) ) + quantity );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return Long.toString( RestrictedTypes.quantityMetricFunction( AddressMetadata.class ).apply( UserFullName.getInstance( id ) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
