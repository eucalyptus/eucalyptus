package com.eucalyptus.auth.policy.key;

import com.eucalyptus.auth.policy.PolicySpec;
import net.sf.json.JSONException;

@PolicyKey( Keys.EC2_VMNUMBER )
public class VmNumber extends QuotaKey {
  
  private static final String KEY = Keys.EC2_VMNUMBER;
  
  private static final String RESOURCE_INSTANCE = PolicySpec.VENDOR_EC2 + ":" + PolicySpec.EC2_RESOURCE_INSTANCE;
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return RESOURCE_INSTANCE.equals( resourceType );
  }

  @Override
  public String value( Scope scope, String id, String resource, Integer quantity ) {
    // TODO Auto-generated method stub
    return null;
  }

}
