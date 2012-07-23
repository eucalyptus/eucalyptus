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
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.NumericLessThanEquals;
import com.eucalyptus.auth.principal.Account;

public abstract class QuotaKey implements Key {

  public static final String NOT_SUPPORTED = "Not supported";
  
  public static enum Scope {
    ACCOUNT,
    GROUP,
    USER,
  }
  
  public abstract String value( Scope scope, String id, String resource, Long quantity ) throws AuthException;
  
  public static final Long MB = 1024 * 1024L;
  
  @Override
  public final String value( ) throws AuthException {
    throw new RuntimeException( "QuotaKey should not call the default value interface." );
  }
  
  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( conditionClass != NumericLessThanEquals.class ) {
      throw new JSONException( "A quota key is not allowed in condition " + conditionClass.getName( ) + ". NumericLessThanEquals is required." );
    }
  }
  
  public static Long toMb( Long sizeInBytes ) {
    return sizeInBytes / MB;
  }
}
