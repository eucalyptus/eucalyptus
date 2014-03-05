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

import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.StackActivityImpl;
import com.netflix.glisten.impl.local.LocalWorkflowOperations;
import org.apache.log4j.Logger;


/**
 * Created by ethomas on 12/19/13.
 */
public class StackCreator extends Thread {
  private static final Logger LOG = Logger.getLogger(StackCreator.class);


  private Template template;
  private String onFailure;

  public StackCreator(Template template, String onFailure) {
    this.template = template;
    this.onFailure = onFailure;
  }
  @Override
  public void run() {
    try {
      CreateStackWorkflowImpl createStackWorkflow = new CreateStackWorkflowImpl();
      createStackWorkflow.setWorkflowOperations(LocalWorkflowOperations.<StackActivity>of(new StackActivityImpl()));
      createStackWorkflow.createStack(JsonHelper.getStringFromJsonNode(template.toJsonNode()),onFailure);
    } catch (Exception ex2) {
      LOG.error(ex2, ex2);
    }
  }
}
