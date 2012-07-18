/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.auth.policy.key;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;

/**
 * Keys specifying a contract for resource usage or result presentation once an
 * authorization statement approves the request, e.g. instance lifetime, s3 max-key, etc.
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
