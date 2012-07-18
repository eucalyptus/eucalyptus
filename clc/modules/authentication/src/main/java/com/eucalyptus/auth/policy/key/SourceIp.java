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
