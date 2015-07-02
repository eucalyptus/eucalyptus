/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.resources.client;

import java.util.ArrayList;

import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.CreateStackType;
import com.eucalyptus.cloudformation.DeleteStackType;
import com.eucalyptus.cloudformation.DescribeStackResourcesResponseType;
import com.eucalyptus.cloudformation.DescribeStackResourcesType;
import com.eucalyptus.cloudformation.DescribeStacksResponseType;
import com.eucalyptus.cloudformation.DescribeStacksType;
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.Parameters;
import com.eucalyptus.cloudformation.ResourceList;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.cloudformation.StackResource;
import com.eucalyptus.cloudformation.StackResources;
import com.eucalyptus.cloudformation.Stacks;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.cloudformation.CloudFormationMessage;

public class CloudFormationClient {

  private static CloudFormationClient _instance = null;
  private CloudFormationClient(){ }
  public static CloudFormationClient getInstance(){
    if(_instance == null)
      _instance = new CloudFormationClient();
    return _instance;
  }
  
  private class CloudFormationContext extends AbstractClientContext<CloudFormationMessage, CloudFormation> {
    private CloudFormationContext(final String userId){
      super(userId, CloudFormation.class);
    }
  }
  
  private abstract class CloudFormationStackTask<T extends CloudFormationMessage> extends
    EucalyptusClientTask<CloudFormationMessage, CloudFormation> {
  
    abstract T getRequest();
  
    @Override
    void dispatchInternal(ClientContext<CloudFormationMessage, CloudFormation> context,
        Checked<CloudFormationMessage> callback) {
    
      final DispatchingClient<CloudFormationMessage, CloudFormation> client = context.getClient();
      client.dispatch(getRequest(), callback);
    }
    
    @Override
    void dispatchSuccess(ClientContext<CloudFormationMessage, CloudFormation> context,
        CloudFormationMessage response) {
    }
  }
  
  private class CloudFormationCreateStackTask extends CloudFormationStackTask<CreateStackType> {
    private String templateBody = null;
    private String name = null;
    private ArrayList<Parameter> parameters = null;

    CloudFormationCreateStackTask(final String name, final String templateBody,
        ArrayList<Parameter> parameters) {
      this.name = name;
      this.templateBody = templateBody;
      this.parameters = parameters;
    }

    @Override
    CreateStackType getRequest() {
      final CreateStackType req = new CreateStackType();
      req.setStackName(name);
      req.setTemplateBody(templateBody);
      if (parameters != null) {
        Parameters params = new Parameters();
        params.getMember().addAll(parameters);
        req.setParameters(params);
      }
      ResourceList rl = new ResourceList();
      rl.getMember().add("CAPABILITY_IAM");
      req.setCapabilities(rl);
      return req;
    }
  }

  private class CloudFormationDeleteStackTask extends CloudFormationStackTask<DeleteStackType> {
    private String name = null;
    
    CloudFormationDeleteStackTask(final String name) {
      this.name = name;
    }
    
    DeleteStackType getRequest() {
      final DeleteStackType req = new DeleteStackType();
      req.setStackName(name);
      return req;
    }
  }
  
  private class CloudFormationDescribeStackTask extends CloudFormationStackTask<DescribeStacksType> {
    private String name = null;
    private DescribeStacksResponseType result = null;
    
    CloudFormationDescribeStackTask(final String name) {
      this.name = name;
    }
    
    DescribeStacksType getRequest() {
      final DescribeStacksType req = new DescribeStacksType();
      req.setStackName(name);
      return req;
    }
    
    @Override
    void dispatchSuccess(ClientContext<CloudFormationMessage, CloudFormation> context,
        CloudFormationMessage response) {
      result = (DescribeStacksResponseType) response;
    }
    
    Stack getResult() {
      Stacks stacks = result.getDescribeStacksResult() != null ? 
          result.getDescribeStacksResult().getStacks() : null;
      if (stacks == null || stacks.getMember().isEmpty())
        return null;
      else
        return stacks.getMember().get(0); 
    }
  }

  private class CloudFormationDescribeStackResourcesTask extends
    CloudFormationStackTask<DescribeStackResourcesType> {
    private String name = null;
    private String logicalId = null;
    private DescribeStackResourcesResponseType result = null;

    CloudFormationDescribeStackResourcesTask(final String name, final String logicalId) {
      this.name = name;
      this.logicalId = logicalId;
    }

    DescribeStackResourcesType getRequest() {
      final DescribeStackResourcesType req = new DescribeStackResourcesType();
      req.setStackName(name);
      if ( logicalId != null )
        req.setLogicalResourceId(logicalId);
      return req;
    }

    @Override
    void dispatchSuccess(ClientContext<CloudFormationMessage, CloudFormation> context,
        CloudFormationMessage response) {
      result = (DescribeStackResourcesResponseType) response;
    }

    ArrayList<StackResource> getResult() {
      StackResources res = result.getDescribeStackResourcesResult() != null ?
          result.getDescribeStackResourcesResult().getStackResources() : null;
      if ( res == null || res.getMember().isEmpty() )
        return null;
      else
        return res.getMember();
    }
  }
  /*
   * Return stack's description or null if stack can't be found
   */
  public Stack describeStack(final String userId, String name) {
    final CloudFormationDescribeStackTask task = new CloudFormationDescribeStackTask(
        name);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new CloudFormationContext(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
            : "failed to describe stack " + name);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  /*
   * Return stack resources or one resource by its logical.
   * Return null if stack or resource can't be found
   */
  public ArrayList<StackResource> describeStackResources(final String userId, String name,
      String resourceLogicalId) {
    final CloudFormationDescribeStackResourcesTask task =
        new CloudFormationDescribeStackResourcesTask(
        name, resourceLogicalId);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new CloudFormationContext(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe stack " + name + " resources");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteStack(final String userId, String name) {
    final CloudFormationDeleteStackTask task = new CloudFormationDeleteStackTask(
        name);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new CloudFormationContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to remove stack " + name);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public void createStack(final String userId, String name, String templateBody,
      ArrayList<Parameter> parameters) {
    final CloudFormationCreateStackTask task = new CloudFormationCreateStackTask(
        name, templateBody, parameters);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new CloudFormationContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create stack " + name);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
