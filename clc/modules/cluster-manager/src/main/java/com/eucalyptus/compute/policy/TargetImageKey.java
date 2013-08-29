/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.*;
import java.util.NoSuchElementException;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ArnConditionOp;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.images.Images;
import com.eucalyptus.records.Logs;
import com.google.common.collect.ImmutableSet;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( TargetImageKey.KEY_NAME )
public class TargetImageKey implements ComputeKey {
  static final String KEY_NAME = "ec2:targetimage"; //TODO:STEVE: or request(ed)image?
  private static final Set<String> actions = ImmutableSet.<String>builder()
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .build( );

  @Override
  public String value( ) throws AuthException {
    try {
      String imageId = null;
      final BaseMessage request = Contexts.lookup().getRequest( );
      if ( request instanceof RunInstancesType ) {
        imageId = ((RunInstancesType) request).getImageId();
      }

      if ( imageId != null ) {
        String accountNumber = "";
        try {
          accountNumber = Images.lookupImage( imageId ).getOwnerAccountNumber( );
        } catch ( NoSuchElementException e ) {
          Logs.exhaust().debug( "Image not found when evaluating target image condition key: " + imageId );
        }
        return String.format( "arn:aws:ec2:eucalyptus:%s:image/%s", accountNumber, imageId );
      }
    } catch ( IllegalContextAccessException e ) {
      Logs.exhaust().debug( "Contextual request not found when evaluating target image condition key." );
    }
    return null;
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !ArnConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". ARN conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
    Validation.assertArnValue( value );
  }

  @Override
  public boolean canApply( final String action,
                           final String resourceType ) {
    return actions.contains( action );
  }
}
