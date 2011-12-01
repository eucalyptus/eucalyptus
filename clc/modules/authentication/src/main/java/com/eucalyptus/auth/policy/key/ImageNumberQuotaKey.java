package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.RestrictedTypes;

/**
 * GRZE:NOTE: this class is a {@link Euare} specific type and needs to move as well as not
 * referring to private implementation types. {@link ImageMetadata} should be considered a public
 * type while {@code ImageInfo} is implementation specific and will change as needed by the
 * implementation.
 */
@PolicyKey( Keys.EC2_QUOTA_IMAGE_NUMBER )
public class ImageNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_IMAGE_NUMBER;
  
  @Override
  public void validateValueType( final String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( final String action, final String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_REGISTERIMAGE ).equals( action ) &&
         PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_IMAGE ).equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( final Scope scope, final String id, final String resource, final Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( RestrictedTypes.quantityMetricFunction( ImageMetadata.class ).apply( AccountFullName.getInstance( id ) ) + 1 );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return Long.toString( RestrictedTypes.quantityMetricFunction( ImageMetadata.class ).apply( UserFullName.getInstance( id ) ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
