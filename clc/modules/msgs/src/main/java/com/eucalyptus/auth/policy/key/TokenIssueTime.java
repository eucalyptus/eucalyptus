/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.auth.policy.key;

import static com.eucalyptus.auth.policy.key.Key.EvaluationConstraint.ReceivingHost;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.login.HmacCredentials.QueryIdCredential;
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
      final Set<QueryIdCredential> queryIdCreds = subject == null ?
          Collections.emptySet( ) :
          subject.getPublicCredentials( QueryIdCredential.class );
      if ( queryIdCreds.size( ) == 1 &&
          Iterables.get( queryIdCreds, 0 ).getType( ).isPresent( ) &&
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

  @Override
  public boolean canApply( String action ) {
    return true;
  }
}
