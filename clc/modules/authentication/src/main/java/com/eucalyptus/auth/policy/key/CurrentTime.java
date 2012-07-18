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
