package com.eucalyptus.auth.policy.key;

import java.util.Date;
import org.apache.log4j.Logger;
import net.sf.json.JSONException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.NumericEquals;

@PolicyKey( Keys.EC2_KEEPALIVE )
public class KeepAlive extends ContractKey<Date> {
  
  private static final Logger LOG = Logger.getLogger( KeepAlive.class );
  
  private static final String KEY = Keys.EC2_KEEPALIVE;
  
  private static final String ACTION_RUNINSTANCES = PolicySpec.VENDOR_EC2 + ":" + PolicySpec.EC2_RUNINSTANCES;
  
  private static final long YEAR = 1000 * 60 * 60 * 24 * 365; // one year by default
  private static final long MINUTE = 1000 * 60; // minute
  
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
  public Contract<Date> getContract( final String[] values ) {
    return new Contract<Date>( ) {
      @Override
      public Contract.Type getType( ) {
        return Contract.Type.EXPIRATION;
      }
      @Override
      public Date getValue( ) {
        return getExpiration( values[0] );
      }
    };
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return ACTION_RUNINSTANCES.equals( action );
  }

  @Override
  public boolean isBetter( Contract<Date> current, Contract<Date> update ) {
    return update.getValue( ).after( current.getValue( ) );
  }
  
  /**
   * @param keepAlive In minute.
   * @return
   */
  private static Date getExpiration( String keepAlive ) {
    try {
      return new Date( System.currentTimeMillis( ) + Long.valueOf( keepAlive ) * MINUTE );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      return new Date( System.currentTimeMillis( ) + YEAR );
    }
  }
  
}
