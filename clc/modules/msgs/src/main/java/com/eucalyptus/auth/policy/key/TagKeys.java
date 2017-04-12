/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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

import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.HasTags;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( value = Keys.AWS_TAG_KEYS )
public class TagKeys implements AwsKey {
  private static final String KEY = Keys.AWS_TAG_KEYS;

  @Override
  public String value( ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public Set<String> values( ) throws AuthException {
    return getTagKeys( );
  }

  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  private Set<String> getTagKeys( ) throws AuthException {
    try {
      final BaseMessage request = Contexts.lookup( ).getRequest( );
      final Set<String> keys;
      if ( request instanceof HasTags ) {
        keys = ( (HasTags) request ).getTagKeys(
            PolicyResourceContext.getResourceType( ),
            PolicyResourceContext.getResourceId( )
        );
      } else {
        throw new AuthException( "Error getting value for request tag keys condition" );
      }
      return keys;
    } catch ( Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for request tag keys condition", e );
    }
  }
}
