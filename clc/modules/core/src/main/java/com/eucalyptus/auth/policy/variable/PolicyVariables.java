/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
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
