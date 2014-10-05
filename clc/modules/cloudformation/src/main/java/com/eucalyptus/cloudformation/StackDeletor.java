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
package com.eucalyptus.cloudformation;

import com.eucalyptus.cloudformation.bootstrap.CloudFormationBootstrapper;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflow;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowClient;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowDescriptionTemplate;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
import com.netflix.glisten.WorkflowClientFactory;
import com.netflix.glisten.WorkflowDescriptionTemplate;
import com.netflix.glisten.WorkflowTags;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackDeletor extends Thread {
  private static final Logger LOG = Logger.getLogger(StackDeletor.class);
  private StackEntity stackEntity;
  private String effectiveUserId;


  public StackDeletor(StackEntity stackEntity, String effectiveUserId) {
    this.stackEntity = stackEntity;
    this.effectiveUserId = effectiveUserId;
  }

  @Override
  public void run() {
    try {
      DeleteStackWorkflow deleteStackWorkflow;
      WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(CloudFormationBootstrapper.getSimpleWorkflowClient(), CloudFormationBootstrapper.SWF_DOMAIN, CloudFormationBootstrapper.SWF_TASKLIST);
      WorkflowDescriptionTemplate workflowDescriptionTemplate = new DeleteStackWorkflowDescriptionTemplate();
      InterfaceBasedWorkflowClient<DeleteStackWorkflow> client = workflowClientFactory
        .getNewWorkflowClient(DeleteStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());
      deleteStackWorkflow = new DeleteStackWorkflowClient(client);
      deleteStackWorkflow.deleteStack(stackEntity.getStackId(), stackEntity.getAccountId(), stackEntity.getResourceDependencyManagerJson(), effectiveUserId);
    } catch (Exception ex) {
      LOG.error(ex, ex);
    }
  }
}

