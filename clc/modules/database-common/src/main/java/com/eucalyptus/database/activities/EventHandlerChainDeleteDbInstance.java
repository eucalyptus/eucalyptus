/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
