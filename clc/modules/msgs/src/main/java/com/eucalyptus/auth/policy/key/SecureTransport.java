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
import org.jboss.netty.handler.ssl.SslHandler;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.Bool;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( value = Keys.AWS_SECURE_TRANSPORT, evaluationConstraints = ReceivingHost )
public class SecureTransport implements AwsKey {
  static final String KEY = Keys.AWS_SECURE_TRANSPORT;

  @Override
  public String value( ) throws AuthException {
    try {
      return String.valueOf( Contexts.lookup( ).getChannel( ).getPipeline( ).get( SslHandler.class ) != null );
    } catch ( final IllegalContextAccessException e ) {
      return String.valueOf( Boolean.FALSE );
    }
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !Bool.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". Boolean conditions are required." );
    }
  }
}

