/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package edu.ucsb.eucalyptus.msgs;

import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.RoleUser;
import com.eucalyptus.context.Context;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Context for propagation of identity / authorization parameters
 */
public class CallerContext {

  private final String identity; // user, role, etc
  private final boolean privileged;
  private final Map<String,String> evaluatedKeys;

  public CallerContext( final Context context ) throws AuthException {
    identity = context.getUser( ) instanceof RoleUser ?
        ((RoleUser) context.getUser( )).getRoleId( ) :
        context.getUser( ).getUserId( );
    privileged = context.isPrivileged( );
    evaluatedKeys = context.evaluateKeys( );
  }

  public void apply( final BaseMessage message ) {
    message.setUserId( identity );
    if ( privileged ) {
      message.markPrivileged( );
    }
    message.setCallerContext( new BaseCallerContext( Lists.newArrayList(
      Iterables.transform( evaluatedKeys.entrySet( ), MapEntryToEvaluatedIamConditionKey.INSTANCE )
    ) ) );
  }

  private enum MapEntryToEvaluatedIamConditionKey implements Function<Map.Entry<String,String>,EvaluatedIamConditionKey> {
    INSTANCE;

    @Override
    public EvaluatedIamConditionKey apply( final Map.Entry<String, String> entry ) {
      return new EvaluatedIamConditionKey( entry.getKey( ), entry.getValue() );
    }
  }
}
