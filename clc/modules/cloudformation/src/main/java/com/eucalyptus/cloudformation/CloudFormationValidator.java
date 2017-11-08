/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation;

import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.cloudformation.util.CfnIdentityDocumentCredential;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

import static com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec.VENDOR_CLOUDFORMATION;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import javax.annotation.Nonnull;
import javax.security.auth.Subject;

/**
 *
 */
@ComponentNamed
public class CloudFormationValidator extends ServiceAdvice {

  @Override
  protected void beforeService( @Nonnull final Object object ) throws Exception {
    // Authorization check
    if ( object instanceof BaseMessage ) {
      final Context context = Contexts.lookup( );
      final AuthContextSupplier user = context.getAuthContext( );
      final Subject subject = context.getSubject( );
      if ( !Permissions.perhapsAuthorized( VENDOR_CLOUDFORMATION, getIamActionByMessageType( (BaseMessage)object ), user ) &&
          subject.getPublicCredentials( CfnIdentityDocumentCredential.class ).isEmpty( ) ) {
        throw new AccessDeniedException( "You are not authorized to perform this operation." );
      }
    }
  }

}
