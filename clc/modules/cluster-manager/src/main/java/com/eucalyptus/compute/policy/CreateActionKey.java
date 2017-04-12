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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.EC2_CREATETAGS;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_EC2;
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.ImmutableSet;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( CreateActionKey.KEY_NAME )
public class CreateActionKey implements ComputeKey {
  static final String KEY_NAME = "ec2:createaction";

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATETAGS ) )
      .build( );

  @Override
  public String value( ) throws AuthException {
    return getCreateAction( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

  private String getCreateAction( ) throws AuthException {
    try {
      final BaseMessage request = Contexts.lookup( ).getRequest( );
      return request.getClass( ).getSimpleName( ).replaceAll( "Type$", "" );
    } catch ( Exception e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw new AuthException( "Error getting value for create action condition", e );
    }
  }
}
