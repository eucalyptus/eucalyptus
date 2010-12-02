package com.eucalyptus.auth.policy.key;

import java.util.Date;
import net.sf.json.JSONException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.DateConditionOp;

@PolicyKey( Keys.AWS_CURRENTTIME )
public class CurrentTime implements Key {
  
  private static final String KEY = Keys.AWS_CURRENTTIME;

  @Override
  public String value( ) throws AuthException {
    return Iso8601DateParser.toString( new Date( ) );
  }

  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !DateConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". Date conditions are required." );
    }
  }

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateDateValue( value, KEY );
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return true;
  }
  
}
