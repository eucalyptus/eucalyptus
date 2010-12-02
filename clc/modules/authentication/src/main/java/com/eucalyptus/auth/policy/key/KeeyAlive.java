package com.eucalyptus.auth.policy.key;

import java.util.List;
import net.sf.json.JSONException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.NumericEquals;
import com.eucalyptus.auth.policy.condition.NumericGreaterThan;

@PolicyKey( Keys.EC2_KEEPALIVE )
public class KeeyAlive extends ContractKey {
  
  private static final String KEY = Keys.EC2_KEEPALIVE;
  
  private static final String ACTION_RUNINSTANCES = PolicySpec.VENDOR_EC2 + ":" + PolicySpec.EC2_RUNINSTANCES;
  
  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( NumericEquals.class != conditionClass ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". NumericEquals is required." );
    }
  }

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public Contract getContract( String[] values ) {
    return new SingleValueContract( this.getClass( ).getName( ), values[0] );
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return ACTION_RUNINSTANCES.equals( action );
  }

  @Override
  public boolean isBetter( Contract current, Contract update ) {
    return ( new NumericGreaterThan( ) ).check( update.getValue( ), current.getValue( ) );
  }
  
}
