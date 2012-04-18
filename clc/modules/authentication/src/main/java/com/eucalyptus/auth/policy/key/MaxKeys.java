package com.eucalyptus.auth.policy.key;

import org.apache.log4j.Logger;
import net.sf.json.JSONException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.NumericLessThanEquals;

@PolicyKey( Keys.S3_MAX_KEYS )
public class MaxKeys extends ContractKey<Long> {

  private static final Logger LOG = Logger.getLogger( MaxKeys.class );
  
  private static final String KEY = Keys.S3_MAX_KEYS;
  
  private static final String ACTION_LISTBUCKET = PolicySpec.VENDOR_S3 + ":" + PolicySpec.S3_LISTBUCKET;
  private static final String ACTION_LISTBUCKETVERSIONS = PolicySpec.VENDOR_S3 + ":" + PolicySpec.S3_LISTBUCKETVERSIONS;

  private static final Long DEFAULT = 9999L;
  
  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( conditionClass != NumericLessThanEquals.class ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". NumericLessThanEquals is required." );
    }
  }

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, Keys.S3_MAX_KEYS );
  }

  @Override
  public Contract<Long> getContract( final String[] values ) {
    return new Contract<Long>( ) {
      @Override
      public Contract.Type getType( ) {
        return Contract.Type.MAXKEYS;
      }
      @Override
      public Long getValue( ) {
        try {
          return Long.valueOf( values[0] );
        } catch ( Exception e ) {
          LOG.debug( e, e );
          return DEFAULT;
        }
      }
    };
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return ( ACTION_LISTBUCKET.equals( action ) || ACTION_LISTBUCKETVERSIONS.equals( action ) );
  }

  @Override
  public boolean isBetter( Contract<Long> current, Contract<Long> update ) {
    return update.getValue( ) > current.getValue( );
  }

}
