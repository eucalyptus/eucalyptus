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

import java.util.concurrent.ConcurrentMap;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.util.Strings;
import com.google.common.collect.Maps;

/**
 *
 */
public class PolicyVariables {
  
  private static final ConcurrentMap<String,PolicyVariable> variables = Maps.newConcurrentMap( );

  public static boolean registerPolicyVariable( final PolicyVariable policyVariable ) {
    return variables.putIfAbsent( policyVariable.getQName( ), policyVariable ) == null;
  }
  
  public static PolicyVariable getPolicyVariable( final String qname ) {
    final String cleanQName = Strings.trimSuffix( "}", Strings.trimPrefix( "${", qname ) );
    final PolicyVariable variable = variables.get( cleanQName );
    return variable != null ? 
        variable : 
        cleanQName.length( ) == 1 ?
          new PredefinedPolicyVariable( cleanQName ) :  
          new InvalidPolicyVariable( cleanQName ); 
  }

  public static boolean isValid( final String qname ) {
    return !(getPolicyVariable( qname ) instanceof InvalidPolicyVariable);
  }

  private static final class PredefinedPolicyVariable implements PolicyVariable {
    private final String name;

    PredefinedPolicyVariable( String name ) {
      this.name = name;  
    }
    
    @Override
    public String getVendor() {
      return name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getQName() {
      return name;
    }

    @Override
    public String evaluate( ) {
      return name;
    }
  }

  private static final class InvalidPolicyVariable implements PolicyVariable {
    private final String name;

    InvalidPolicyVariable( String name ) {
      this.name = name;
    }

    @Override
    public String getVendor() {
      return name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getQName() {
      return name;
    }

    @Override
    public String evaluate( ) throws AuthException {
      throw new AuthException( "Invalid policy variable " + name );
    }
  }
}
