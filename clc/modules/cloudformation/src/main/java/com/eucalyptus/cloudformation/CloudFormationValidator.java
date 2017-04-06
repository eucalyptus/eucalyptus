/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
