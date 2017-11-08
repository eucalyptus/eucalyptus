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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.util.Parameters;

/**
 *
 */
public abstract class PolicyVariableSupport implements PolicyVariable {
  
  private final String vendor;
  private final String name;
  private final String qName;
  
  protected PolicyVariableSupport( final String vendor, final String name ) {
    Parameters.checkParam( "vendor", vendor, not( isEmptyOrNullString( ) ) );  
    Parameters.checkParam( "name", name, not( isEmptyOrNullString( ) ) );  
    this.vendor = vendor;
    this.name = name;
    this.qName = PolicySpec.qualifiedName( vendor, name );
  }

  @Override
  public String getVendor( ) {
    return vendor;
  }

  @Override
  public String getName( ) {
    return name;
  }

  public String getQName( ) {
    return qName;
  }
}
