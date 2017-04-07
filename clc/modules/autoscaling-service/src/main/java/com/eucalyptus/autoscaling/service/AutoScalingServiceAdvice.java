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
package com.eucalyptus.autoscaling.service;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.policy.AutoScalingPolicySpec;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;

/**
 *
 */
@ComponentNamed
public class AutoScalingServiceAdvice extends ServiceAdvice {

  @Override
  protected void beforeService( @Nonnull final Object requestObject ) throws Exception {
    if ( requestObject instanceof AutoScalingMessage ) {
      final AutoScalingMessage request = (AutoScalingMessage) requestObject;
      final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );

      // Authorization check
      if ( !Permissions.perhapsAuthorized( AutoScalingPolicySpec.VENDOR_AUTOSCALING, getIamActionByMessageType( request ), user ) ) {
        throw new AutoScalingAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
      }

      // Validation
      final Map<String,String> validationErrorsByField = request.validate();
      if ( !validationErrorsByField.isEmpty() ) {
        throw new AutoScalingClientException( "ValidationError", validationErrorsByField.values().iterator().next() );
      }
    } else {
      throw new AutoScalingAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }
  }
}
