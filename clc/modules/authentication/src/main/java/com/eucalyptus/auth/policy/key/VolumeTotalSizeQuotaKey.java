package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.CloudMetadata.VolumeMetadata;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.RestrictedTypes;

/**
 * GRZE:NOTE: this class is a {@link Euare} specific type and needs to move as well as not
 * referring to private implementation types. {@link VolumeMetadata} should be considered a public
 * type while {@code Volume} is implementation specific and will change as needed by the
 * implementation.
 */
@PolicyKey( Keys.EC2_QUOTA_VOLUME_TOTAL_SIZE )
public class VolumeTotalSizeQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_VOLUME_TOTAL_SIZE;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_CREATEVOLUME ).equals( action ) &&
         PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_VOLUME ).equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( RestrictedTypes.usageMetricFunction( VolumeMetadata.class ).apply( AccountFullName.getInstance( id ) ) + quantity );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return Long.toString( RestrictedTypes.usageMetricFunction( VolumeMetadata.class ).apply( UserFullName.getInstance( id ) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
