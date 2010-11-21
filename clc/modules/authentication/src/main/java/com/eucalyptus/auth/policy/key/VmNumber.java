package com.eucalyptus.auth.policy.key;

import com.eucalyptus.auth.policy.PolicySpecConstants;
import net.sf.json.JSONException;

public class VmNumber extends QuotaKey {
  
  private static final String KEY = Keys.EC2_VMNUMBER;
  
  private static final String RESOURCE_INSTANCE = PolicySpecConstants.VENDOR_EC2 + ":" + PolicySpecConstants.EC2_RESOURCE_INSTANCE;
  
  @Override
  public String value( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return RESOURCE_INSTANCE.equals( resourceType );
  }
  
}
