/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.resources.client;

import java.util.ArrayList;

import com.eucalyptus.cloudformation.common.CloudFormation;
import com.eucalyptus.cloudformation.common.msgs.CreateStackType;
import com.eucalyptus.cloudformation.common.msgs.DeleteStackType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStackResourcesResponseType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStackResourcesType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksResponseType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksType;
import com.eucalyptus.cloudformation.common.msgs.Parameter;
import com.eucalyptus.cloudformation.common.msgs.Parameters;
import com.eucalyptus.cloudformation.common.msgs.ResourceList;
import com.eucalyptus.cloudformation.common.msgs.Stack;
import com.eucalyptus.cloudformation.common.msgs.StackResource;
import com.eucalyptus.cloudformation.common.msgs.StackResources;
import com.eucalyptus.cloudformation.common.msgs.Stacks;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.cloudformation.common.msgs.CloudFormationMessage;

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
