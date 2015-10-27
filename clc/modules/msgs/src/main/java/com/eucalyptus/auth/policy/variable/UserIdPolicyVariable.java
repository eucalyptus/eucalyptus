/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.policy.variable;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;

/**
 *
 */
public class UserIdPolicyVariable extends PolicyVariableSupport {

  protected UserIdPolicyVariable( ) {
    super( "aws", "userid" );
  }

  @Override
  public String evaluate( ) throws AuthException {
    try {
      Context context = Contexts.lookup( );
      if ( Principals.isFakeIdentityUserId( context.getUser( ).getAuthenticatedId( ) ) ) {
        throw new AuthException( "Principal information not available" );  
      }
      return context.getUser( ).getAuthenticatedId( );
    } catch ( Exception e ) {
      throw new AuthException( "Unable to retrieve user identifier for policy variable" );
    }
  }
}
