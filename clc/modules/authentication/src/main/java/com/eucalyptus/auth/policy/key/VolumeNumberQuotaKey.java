package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.EuareService;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.cloud.CloudMetadata.VolumeMetadata;
import com.eucalyptus.util.Types;

/**
 * GRZE:NOTE: this class is a {@link EuareService} specific type and needs to move as well as not
 * referring to private implementation types. {@link VolumeMetadata} should be considered a public
 * type while {@link Volume} is implementation specific and will change as needed by the
 * implementation.
 */
@PolicyKey( Keys.EC2_QUOTA_VOLUME_NUMBER )
public class VolumeNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_VOLUME_NUMBER;
  
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
        return Long.toString( Types.quantityMetricFunction( VolumeMetadata.class ).apply( AccountFullName.getInstance( id ) ) + quantity );
      case GROUP:
        throw new AuthException( "Group level quota not supported" );
      case USER:
        return Long.toString( Types.quantityMetricFunction( VolumeMetadata.class ).apply( UserFullName.getInstance( id ) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
