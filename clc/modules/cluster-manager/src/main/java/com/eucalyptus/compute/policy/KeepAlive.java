/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.compute.policy;

import java.util.Date;
import org.apache.log4j.Logger;
import net.sf.json.JSONException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.NumericEquals;
import com.eucalyptus.auth.policy.key.ContractKey;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;

@PolicyKey( Keys.EC2_KEEPALIVE )
public class KeepAlive extends ContractKey<Date> {
  
  private static final Logger LOG = Logger.getLogger( KeepAlive.class );
  
  private static final String KEY = Keys.EC2_KEEPALIVE;
  
  private static final String ACTION_RUNINSTANCES = PolicySpec.VENDOR_EC2 + ":" + PolicySpec.EC2_RUNINSTANCES;
  
  private static final long YEAR = 1000L * 60L * 60L * 24L * 365L; // one year by default
  private static final long MINUTE = 1000L * 60L; // minute
  
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
  public boolean canApply( String action ) {
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
