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
 *
 *  This file may incorporate work covered under the following copyright and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue;

import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.simplequeue.exceptions.AccessDeniedException;
import com.eucalyptus.simplequeue.exceptions.QueueDoesNotExistException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

import static com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE;
import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import javax.annotation.Nonnull;

/**
 *
 */
@ComponentNamed
public class SimpleQueueValidator extends ServiceAdvice {

  @Override
  protected void beforeService( @Nonnull final Object object ) throws Exception {
    // Authorization check
    if ( object instanceof BaseMessage ) {
      final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );
      if ( !Permissions.perhapsAuthorized( VENDOR_SIMPLEQUEUE, getIamActionByMessageType( (BaseMessage)object ), user ) ) {
        // GetQueueUrl returns no such queue exception
        if ( object instanceof GetQueueUrlType) {
          throw new QueueDoesNotExistException("The specified queue does not exist.");
        } else {
          throw new AccessDeniedException("You are not authorized to perform this operation.");
        }
      }
    }
  }

}
