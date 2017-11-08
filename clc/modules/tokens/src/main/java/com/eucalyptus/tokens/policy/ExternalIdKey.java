/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.tokens.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.PolicyEvaluationWriteContextKey;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.util.TypedKey;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( ExternalIdKey.KEY_NAME )
public class ExternalIdKey implements Key {
  static final String KEY_NAME = "sts:externalid";

  private static final TypedKey<String> EXTERNAL_ID_KEY = TypedKey.create( "ExternalID" );
  public static final PolicyEvaluationWriteContextKey<String> CONTEXT_KEY =
      PolicyEvaluationWriteContextKey.create( EXTERNAL_ID_KEY );

  @Override
  public String value() throws AuthException {
    return PolicyEvaluationContext.get( ).getAttribute( EXTERNAL_ID_KEY );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
  }

  @Override
  public boolean canApply( final String action ) {
    return PolicySpec.qualifiedName(
        PolicySpec.VENDOR_STS,
        PolicySpec.STS_ASSUMEROLE ).equals( action );
  }
}
