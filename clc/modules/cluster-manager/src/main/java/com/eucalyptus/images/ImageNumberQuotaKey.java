package com.eucalyptus.images;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.vm.SystemState;

@PolicyKey( Keys.EC2_QUOTA_IMAGE_NUMBER )
public class ImageNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.EC2_QUOTA_IMAGE_NUMBER; 
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.EC2_REGISTERIMAGE.equals( action ) && PolicySpec.EC2_RESOURCE_IMAGE.equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Integer.toString( ImageUtil.countByAccount( id ) + 1 );
      case GROUP:
        throw new AuthException( "Group level quota not supported" );
      case USER:
        return Integer.toString( ImageUtil.countByUser( id ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
