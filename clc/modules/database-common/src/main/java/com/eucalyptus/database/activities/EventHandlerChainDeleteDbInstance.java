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
package com.eucalyptus.database.activities;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.component.Topology;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
import com.eucalyptus.resources.client.CloudFormationClient;
import com.eucalyptus.resources.client.EuareClient;
/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainDeleteDbInstance extends EventHandlerChain<DeleteDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainDeleteDbInstance.class );

  @Override
  public EventHandlerChain<DeleteDBInstanceEvent> build() {
    this.append(new RollbackRun(this));
    return this;
  }
  
  public static class RollbackRun extends AbstractEventHandler<DeleteDBInstanceEvent> {
    protected RollbackRun(EventHandlerChain<DeleteDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(DeleteDBInstanceEvent evt) throws EventHandlerException {
      // check that CF is ENABLED
      if (!Topology.isEnabled(CloudFormation.class))
        throw new EventHandlerException("CloudFormation is not enabled");
      final String accountId = EventHandlerChainCreateDbInstance.getAccountByUser(evt.getUserId());
      final String stackName = EventHandlerChainCreateDbInstance.getStackName(accountId);

      try {
        LOG.info("Removing " + stackName  + " stack");
        CloudFormationClient.getInstance().deleteStack(evt.getUserId(), stackName);
        EuareClient.getInstance().deleteServerCertificate(evt.getUserId(),
            EventHandlerChainCreateDbInstance.getCertificateName(accountId));
      } catch (final Exception ex) {
        throw new EventHandlerException(ex.getMessage());
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      ;
    }
  }
}
