package com.eucalyptus.auth.policy.key;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;

/**
 * Keys specifying a contract for resource usage or result presentation once an
 * authorization statement approves the request, e.g. instance lifetime, s3 max-key, etc.
 * 
 * @author wenye
 *
 */
public abstract class ContractKey<T> implements Key {

  /**
   * Get a contract object by input values from the policy spec.
   * 
   * @param values The input values.
   * @return A contract object.
   */
  public abstract Contract<T> getContract( String[] values );
  
  /**
   * Check if another contract is better than the current one. The exact "betterness"
   * is defined by specific key type.
   * 
   * @param current The current contract.
   * @param update The new contract.
   * @return If new contract is better than the current one.
   */
  public abstract boolean isBetter( Contract<T> current, Contract<T> update );
  
  @Override
  public final String value( ) throws AuthException {
    throw new RuntimeException( "ContractKey has no value." );
  }
  
}
