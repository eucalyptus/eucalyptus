/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.policy.key;

import static com.eucalyptus.auth.policy.key.Key.EvaluationConstraint.ReceivingHost;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKeyCredential;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.DateConditionOp;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.google.common.collect.Iterables;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( value = Keys.AWS_TOKEN_ISSUETIME, evaluationConstraints = ReceivingHost )
public class TokenIssueTime implements AwsKey {
  static final String KEY = Keys.AWS_TOKEN_ISSUETIME;

  @Override
  public String value( ) throws AuthException {
    try {
      // Token issue time is available via the access key create date for
      // temporary credentials
      final Context context = Contexts.lookup( );
      final Subject subject = context.getSubject( );
      final List<AccessKey> keys = context.getUser( ).getKeys( );
      final Set<AccessKeyCredential> accessKeyCredentials = subject == null ?
          Collections.emptySet( ) :
          subject.getPublicCredentials( AccessKeyCredential.class );
      if ( accessKeyCredentials.size( ) == 1 &&
          Iterables.get( accessKeyCredentials, 0 ).getType( ).isDefined( ) &&
          keys.size( ) == 1 ) {
        return Iso8601DateParser.toString( keys.get( 0 ).getCreateDate( ) );
      }
    } catch ( final IllegalContextAccessException e ) {
      // so null
    }
    return null;
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !DateConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". Date conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
    KeyUtils.validateDateValue( value, KEY );
  }
}
