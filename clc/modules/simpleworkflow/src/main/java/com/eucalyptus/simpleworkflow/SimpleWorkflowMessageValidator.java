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
package com.eucalyptus.simpleworkflow;

import java.util.Map;
import javax.annotation.Nonnull;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage;

/**
 *
 */
@ComponentNamed
public class SimpleWorkflowMessageValidator extends ServiceAdvice {

  @Override
  protected void beforeService( @Nonnull final Object object ) throws SimpleWorkflowException {
    // check system-only mode
    final Context context = Contexts.lookup( );
    if ( SimpleWorkflowProperties.isSystemOnly() &&
            !(Accounts.isSystemAccount( context.getAccountAlias() ) || context.hasAdministrativePrivileges())) {
      throw new SimpleWorkflowUnavailableException( );
    }

    // validate message
    if ( object instanceof SimpleWorkflowMessage ) {
      final SimpleWorkflowMessage simpleWorkflowRequest = (SimpleWorkflowMessage) object;
      final Map<String,String> validationErrorsByField = simpleWorkflowRequest.validate( );
      if ( !validationErrorsByField.isEmpty() ) {
        throw new SimpleWorkflowClientException( "ValidationError", validationErrorsByField.values().iterator().next() );
      }
    }
  }
}