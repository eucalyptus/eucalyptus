package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.principal.Authorization;

public interface Key {

  public String value( ) throws AuthException;
  
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException;
  
  public void validateValueType( String value ) throws JSONException;
  
  public boolean canApply( String action, String resourceType );
  
}
