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
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.HasTags;
import net.sf.json.JSONException;

/**
 *
 */
public class RequestTag implements AwsKey {
  @Nonnull private final String name;
  @Nonnull private final String tagKey;

  public RequestTag( @Nonnull final String name, @Nonnull final String tagKey ) {
    this.name = Assert.notNull( name, "name" );
    this.tagKey = Assert.notNull( tagKey, "tagKey" );
  }

  @Override
  public String name( ) {
    return name;
  }

  @Override
  public String value( ) throws AuthException {
    return getTagValue( );
  }

  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( name( ) + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  private String getTagValue( ) throws AuthException {
    try {
      final BaseMessage request = Contexts.lookup( ).getRequest( );
      final String value;
      if ( request instanceof HasTags ) {
        value = ( (HasTags) request ).getTagValue(
            PolicyResourceContext.getResourceType( ),
            PolicyResourceContext.getResourceId( ),
            tagKey
        );
      } else {
        throw new AuthException( "Error getting value for request tag condition" );
      }
      return value;
    } catch ( Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for request tag condition", e );
    }
  }
}
