package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.RestrictedTypes;

/**
 * GRZE:NOTE: this class is a {@link Euare} specific type and needs to move as well as not
 * referring to private implementation types. {@link VmInstanceMetadata} should be considered a public
 * type while {@code VmInstance} is implementation specific and will change as needed by the
 * implementation.
 */
@PolicyKey( Keys.EC2_QUOTA_VM_INSTANCE_NUMBER )
public class VmInstanceNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_VM_INSTANCE_NUMBER;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RUNINSTANCES ).equals( action ) &&
         PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE ).equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( RestrictedTypes.quantityMetricFunction( VmInstanceMetadata.class ).apply( AccountFullName.getInstance( id ) ) + 1 );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return Long.toString( RestrictedTypes.quantityMetricFunction( VmInstanceMetadata.class ).apply( UserFullName.getInstance( id ) ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
