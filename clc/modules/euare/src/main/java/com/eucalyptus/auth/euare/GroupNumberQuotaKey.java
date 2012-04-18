package com.eucalyptus.auth.euare;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;

@PolicyKey( Keys.IAM_QUOTA_GROUP_NUMBER )
public class GroupNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.IAM_QUOTA_GROUP_NUMBER;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_IAM, PolicySpec.IAM_CREATEGROUP ).equals( action ) &&
         PolicySpec.qualifiedName( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_GROUP ).equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( EuareQuotaUtil.countGroupByAccount( id ) + quantity );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return NOT_SUPPORTED;
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
