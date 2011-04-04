package com.eucalyptus.auth.euare;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;

@PolicyKey( Keys.IAM_QUOTA_USER_NUMBER )
public class UserNumberQuotaKey extends QuotaKey {
  
  private static final String KEY = Keys.IAM_QUOTA_USER_NUMBER;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    if ( PolicySpec.IAM_CREATEUSER.equals( action ) && PolicySpec.IAM_RESOURCE_USER.equals( resourceType ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( EuareQuotaUtil.countUserByAccount( id ) + quantity );
      case GROUP:
        throw new AuthException( "Group level quota not supported" );
      case USER:
        throw new AuthException( "User level quota not supported" );
    }
    throw new AuthException( "Invalid scope" );
  }
  
}
