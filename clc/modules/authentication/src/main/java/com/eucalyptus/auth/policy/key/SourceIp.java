package com.eucalyptus.auth.policy.key;

import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.AddressConditionOp;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;

@PolicyKey( Keys.AWS_SOURCEIP )
public class SourceIp implements Key {
  
  private static final String KEY = Keys.AWS_SOURCEIP;
  
  @Override
  public String value( ) throws AuthException {
    try {
      Context context = Contexts.lookup( );
      return context.getRemoteAddress( ).getHostAddress( );
    } catch ( Exception e ) {
      throw new AuthException( "Unable to retrieve current request IP address for authorization", e );
    }
  }
  
  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !AddressConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". Address conditions are required." );
    }
  }
  
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateCidrValue( value, KEY );
  }
  
  @Override
  public boolean canApply( String action, String resourceType ) {
    return true;
  }
  
}
