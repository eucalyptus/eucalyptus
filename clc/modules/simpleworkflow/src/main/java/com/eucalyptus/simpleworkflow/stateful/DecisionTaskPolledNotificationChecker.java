/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.stateful;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.simpleworkflow.WorkflowExecution;

/**
 *
 */
public class DecisionTaskPolledNotificationChecker extends AbstractTaskPolledNotificationChecker {

  public DecisionTaskPolledNotificationChecker( ) {
    super( "decision" );
  }

  @Override
  boolean hasTasks( final String accountNumber,
                    final String domain,
                    final String taskList ) {
    try ( final TransactionResource tx = Entities.transactionFor( WorkflowExecution.class ) ) {
      return Entities.count( WorkflowExecution.exampleWithPendingDecision(
          AccountFullName.getInstance( accountNumber ),
          domain,
          taskList ) ) > 0;
    }
  }
}
