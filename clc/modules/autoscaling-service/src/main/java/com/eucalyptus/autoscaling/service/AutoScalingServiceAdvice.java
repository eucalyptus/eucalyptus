/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
