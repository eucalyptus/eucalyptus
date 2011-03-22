package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;

public class TestQuota extends QuotaKey {
  
  @Override
  public void validateValueType( String value ) throws JSONException {
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    return true;
  }
  
  @Override
  public String value( Scope scope, String id, String resource, Long quantity ) {
    return "11";
  }
  
}
