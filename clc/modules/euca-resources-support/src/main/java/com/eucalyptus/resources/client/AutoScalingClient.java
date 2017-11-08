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
package com.eucalyptus.resources.client;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationNames;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups;
import com.eucalyptus.autoscaling.common.msgs.SetDesiredCapacityResponseType;
import com.eucalyptus.autoscaling.common.msgs.SetDesiredCapacityType;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class AutoScalingClient {
  private static final Logger LOG = Logger.getLogger(AutoScalingClient.class);
  private static AutoScalingClient _instance = null;
  private AutoScalingClient(){ }
  public static AutoScalingClient getInstance(){
    if(_instance == null)
      _instance = new AutoScalingClient();
    return _instance;
  }
  
  private class AutoScalingContext extends AbstractClientContext<AutoScalingMessage, AutoScaling> {
    private AutoScalingContext(String userId){
      super(userId, AutoScaling.class);
    }
  }

  private class AutoScalingSetDesiredCapacityTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String groupName = null;
    private Integer capacity = null;

    private AutoScalingSetDesiredCapacityTask(final String groupName,
        final Integer capacity) {
      this.groupName = groupName;
      this.capacity = capacity;
    }

    private SetDesiredCapacityType setDesiredCapacity() {
      final SetDesiredCapacityType req = new SetDesiredCapacityType();
      req.setAutoScalingGroupName(groupName);
      req.setDesiredCapacity(this.capacity);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(setDesiredCapacity(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final SetDesiredCapacityResponseType resp = (SetDesiredCapacityResponseType) response;
    }
  }

  private class AutoScalingUpdateGroupTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String groupName = null;
    private List<String> availabilityZones = null;
    private Integer capacity = null;
    private String launchConfigName = null;

    private AutoScalingUpdateGroupTask(final String groupName,
        final List<String> zones, final Integer capacity,
        final String launchConfig) {
      this.groupName = groupName;
      this.availabilityZones = zones;
      this.capacity = capacity;
      this.launchConfigName = launchConfig;
    }

    private UpdateAutoScalingGroupType updateAutoScalingGroup() {
      final UpdateAutoScalingGroupType req = new UpdateAutoScalingGroupType();
      req.setAutoScalingGroupName(this.groupName);

      if (this.availabilityZones != null && this.availabilityZones.size() > 0) {
        AvailabilityZones zones = new AvailabilityZones();
        zones.setMember(Lists.<String> newArrayList());
        zones.getMember().addAll(this.availabilityZones);
        req.setAvailabilityZones(zones);
      }
      if (this.capacity != null) {
        req.setDesiredCapacity(this.capacity);
        req.setMaxSize(this.capacity);
        req.setMinSize(this.capacity);
      }
      if (this.launchConfigName != null) {
        req.setLaunchConfigurationName(this.launchConfigName);
      }
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(updateAutoScalingGroup(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final UpdateAutoScalingGroupResponseType resp = (UpdateAutoScalingGroupResponseType) response;
    }

  }

  private class AutoScalingDescribeGroupsTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private List<String> groupNames = null;
    private DescribeAutoScalingGroupsResponseType response = null;

    private AutoScalingDescribeGroupsTask(final List<String> groupNames) {
      this.groupNames = groupNames;
    }

    private DescribeAutoScalingGroupsType describeAutoScalingGroup() {
      final DescribeAutoScalingGroupsType req = new DescribeAutoScalingGroupsType();
      final AutoScalingGroupNames names = new AutoScalingGroupNames();
      names.setMember(Lists.<String> newArrayList());
      names.getMember().addAll(this.groupNames);
      req.setAutoScalingGroupNames(names);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(describeAutoScalingGroup(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      this.response = (DescribeAutoScalingGroupsResponseType) response;
    }

    public DescribeAutoScalingGroupsResponseType getResponse() {
      return this.response;
    }
  }

  private class AutoScalingCreateGroupTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String groupName = null;
    private List<String> availabilityZones = null;
    private int capacity = 1;
    private String launchConfigName = null;
    private String tagKey = null;
    private String tagValue = null;

    private AutoScalingCreateGroupTask(final String groupName,
        final List<String> zones, final int capacity,
        final String launchConfig, final String tagKey, final String tagValue) {
      this.groupName = groupName;
      this.availabilityZones = zones;
      this.capacity = capacity;
      this.launchConfigName = launchConfig;
      this.tagKey = tagKey;
      this.tagValue = tagValue;
    }

    private CreateAutoScalingGroupType createAutoScalingGroup() {
      final CreateAutoScalingGroupType req = new CreateAutoScalingGroupType();
      req.setAutoScalingGroupName(this.groupName);
      AvailabilityZones zones = new AvailabilityZones();
      zones.setMember(Lists.<String> newArrayList());
      zones.getMember().addAll(this.availabilityZones);
      req.setAvailabilityZones(zones);
      req.setDesiredCapacity(this.capacity);
      req.setMaxSize(this.capacity);
      req.setMinSize(this.capacity);
      req.setHealthCheckType("EC2");
      req.setLaunchConfigurationName(this.launchConfigName);
      
      if(tagKey != null && tagValue != null) {
        final Tags tags = new Tags();
        final TagType tag = new TagType();
        tag.setKey(this.tagKey);
        tag.setValue(this.tagValue);
        tag.setPropagateAtLaunch(true);
        tag.setResourceType("auto-scaling-group");
        tag.setResourceId(this.groupName);
        tags.setMember(Lists.newArrayList(tag));
        req.setTags(tags);
      }
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(createAutoScalingGroup(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      CreateAutoScalingGroupResponseType resp = (CreateAutoScalingGroupResponseType) response;
    }
  }

  private class AutoScalingDeleteGroupTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String groupName = null;
    private boolean terminateInstances = false;

    private AutoScalingDeleteGroupTask(final String groupName,
        final boolean terminateInstances) {
      this.groupName = groupName;
      this.terminateInstances = terminateInstances;
    }

    private DeleteAutoScalingGroupType deleteAutoScalingGroup() {
      final DeleteAutoScalingGroupType req = new DeleteAutoScalingGroupType();
      req.setAutoScalingGroupName(this.groupName);
      req.setForceDelete(this.terminateInstances);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(deleteAutoScalingGroup(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final DeleteAutoScalingGroupResponseType resp = (DeleteAutoScalingGroupResponseType) response;
    }
  }

  private class AutoScalingDeleteLaunchConfigTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String launchConfigName = null;

    private AutoScalingDeleteLaunchConfigTask(final String launchConfigName) {
      this.launchConfigName = launchConfigName;
    }

    private DeleteLaunchConfigurationType deleteLaunchConfiguration() {
      final DeleteLaunchConfigurationType req = new DeleteLaunchConfigurationType();
      req.setLaunchConfigurationName(this.launchConfigName);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(deleteLaunchConfiguration(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final DeleteLaunchConfigurationResponseType resp = (DeleteLaunchConfigurationResponseType) response;
    }
  }

  private class AutoScalingDescribeLaunchConfigsTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String launchConfigName = null;
    private LaunchConfigurationType result = null;

    private AutoScalingDescribeLaunchConfigsTask(final String launchConfigName) {
      this.launchConfigName = launchConfigName;
    }

    private DescribeLaunchConfigurationsType describeLaunchConfigurations() {
      final DescribeLaunchConfigurationsType req = new DescribeLaunchConfigurationsType();
      final LaunchConfigurationNames names = new LaunchConfigurationNames();
      names.setMember(Lists.newArrayList(this.launchConfigName));
      req.setLaunchConfigurationNames(names);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(describeLaunchConfigurations(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final DescribeLaunchConfigurationsResponseType resp = (DescribeLaunchConfigurationsResponseType) response;
      try {
        this.result = resp.getDescribeLaunchConfigurationsResult()
            .getLaunchConfigurations().getMember().get(0);
      } catch (final Exception ex) {
        LOG.error("Launch configuration is not found from the response");
      }
    }

    private LaunchConfigurationType getResult() {
      return this.result;
    }
  }

  private class AutoScalingCreateLaunchConfigTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String imageId = null;
    private String instanceType = null;
    private String instanceProfileName = null;
    private String launchConfigName = null;
    private String securityGroup = null;
    private String keyName = null;
    private String userData = null;

    private AutoScalingCreateLaunchConfigTask(final String imageId,
        final String instanceType, String instanceProfileName,
        final String launchConfigName, final String sgroupName,
        final String keyName, final String userData) {
      this.imageId = imageId;
      this.instanceType = instanceType;
      this.instanceProfileName = instanceProfileName;
      this.launchConfigName = launchConfigName;
      this.securityGroup = sgroupName;
      this.keyName = keyName;
      this.userData = userData;
    }

    private CreateLaunchConfigurationType createLaunchConfiguration() {
      final CreateLaunchConfigurationType req = new CreateLaunchConfigurationType();
      req.setImageId(this.imageId);
      req.setInstanceType(this.instanceType);
      if (this.instanceProfileName != null)
        req.setIamInstanceProfile(this.instanceProfileName);
      if (this.keyName != null)
        req.setKeyName(this.keyName);

      req.setLaunchConfigurationName(this.launchConfigName);
      SecurityGroups groups = new SecurityGroups();
      groups.setMember(Lists.<String> newArrayList());
      groups.getMember().add(this.securityGroup);
      req.setSecurityGroups(groups);
      req.setUserData(userData);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(createLaunchConfiguration(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final CreateLaunchConfigurationResponseType resp = (CreateLaunchConfigurationResponseType) response;
    }
  }

  private class AutoscalingDeleteTagsTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String tagKey = null;
    private String tagValue = null;
    private String asgName = null;

    private AutoscalingDeleteTagsTask(final String tagKey,
        final String tagValue, final String asgName) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
      this.asgName = asgName;
    }

    private com.eucalyptus.autoscaling.common.msgs.DeleteTagsType deleteTags() {
      final com.eucalyptus.autoscaling.common.msgs.DeleteTagsType req = new com.eucalyptus.autoscaling.common.msgs.DeleteTagsType();
      final Tags tags = new Tags();
      final TagType tag = new TagType();
      tag.setKey(this.tagKey);
      tag.setValue(this.tagValue);
      tag.setPropagateAtLaunch(true);
      tag.setResourceType("auto-scaling-group");
      tag.setResourceId(this.asgName);
      tags.setMember(Lists.newArrayList(tag));
      req.setTags(tags);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(deleteTags(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final com.eucalyptus.autoscaling.common.msgs.DeleteTagsResponseType resp = (com.eucalyptus.autoscaling.common.msgs.DeleteTagsResponseType) response;
    }
  }

  private class AutoscalingCreateOrUpdateTagsTask extends
  EucalyptusClientTask<AutoScalingMessage, AutoScaling> {
    private String tagKey = null;
    private String tagValue = null;
    private String asgName = null;

    private AutoscalingCreateOrUpdateTagsTask(final String tagKey,
        final String tagValue, final String asgName) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
      this.asgName = asgName;
    }

    private CreateOrUpdateTagsType createOrUpdateTags() {
      final CreateOrUpdateTagsType req = new CreateOrUpdateTagsType();
      final Tags tags = new Tags();
      final TagType tag = new TagType();
      tag.setKey(this.tagKey);
      tag.setValue(this.tagValue);
      tag.setPropagateAtLaunch(true);
      tag.setResourceType("auto-scaling-group");
      tag.setResourceId(this.asgName);
      tags.setMember(Lists.newArrayList(tag));
      req.setTags(tags);
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) {
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context
          .getClient();
      client.dispatch(createOrUpdateTags(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      final CreateOrUpdateTagsResponseType resp = (CreateOrUpdateTagsResponseType) response;
    }
  }
  
  private class AutoScalingDescribeTagsTask extends EucalyptusClientTask<AutoScalingMessage, AutoScaling>{
    private List<com.eucalyptus.autoscaling.common.msgs.TagDescription> result = null;
    private com.eucalyptus.autoscaling.common.msgs.DescribeTagsType describeTags(){
      final com.eucalyptus.autoscaling.common.msgs.DescribeTagsType req = new
          com.eucalyptus.autoscaling.common.msgs.DescribeTagsType();
      return req;
    }
    
    @Override
    void dispatchInternal(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) { 
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
      client.dispatch(describeTags(), callback);      
    }

    @Override
    void dispatchSuccess(
        ClientContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      com.eucalyptus.autoscaling.common.msgs.DescribeTagsResponseType resp =
          (com.eucalyptus.autoscaling.common.msgs.DescribeTagsResponseType) response;
      if(resp.getDescribeTagsResult()!=null && resp.getDescribeTagsResult().getTags()!=null)
        this.result = resp.getDescribeTagsResult().getTags().getMember();    }
    
    public List<com.eucalyptus.autoscaling.common.msgs.TagDescription> getTags(){
      return this.result;
    }
  }


  public void createLaunchConfiguration(final String userId, final String imageId,
      final String instanceType, final String instanceProfileName,
      final String launchConfigName, final String securityGroup,
      final String keyName, final String userData) {
    final AutoScalingCreateLaunchConfigTask task = new AutoScalingCreateLaunchConfigTask(
        imageId, instanceType, instanceProfileName, launchConfigName,
        securityGroup, keyName, userData);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to create launch configuration");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void createAutoScalingGroup(final String userId, final String groupName,
      final List<String> availabilityZones, final int capacity,
      final String launchConfigName, final String tagKey, final String tagValue) {
    final AutoScalingCreateGroupTask task = new AutoScalingCreateGroupTask(
        groupName, availabilityZones, capacity, launchConfigName, tagKey,
        tagValue);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to create autoscaling group");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public LaunchConfigurationType describeLaunchConfiguration(final String userId, final String launchConfigName) {
    final AutoScalingDescribeLaunchConfigsTask task = new AutoScalingDescribeLaunchConfigsTask(
        launchConfigName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get() && task.getResult() != null) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
            "failed to describe launch configuration");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteLaunchConfiguration(final String userId, final String launchConfigName) {
    final AutoScalingDeleteLaunchConfigTask task = new AutoScalingDeleteLaunchConfigTask(
        launchConfigName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to delete launch configuration");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteAutoScalingGroup(final String userId, final String groupName, final boolean terminateInstances) {
    final AutoScalingDeleteGroupTask task = new AutoScalingDeleteGroupTask(
        groupName, terminateInstances);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to delete autoscaling group");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(final String userId, final List<String> groupNames) {
    final AutoScalingDescribeGroupsTask task = new AutoScalingDescribeGroupsTask(
        groupNames);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return task.getResponse();
      } else
        throw new EucalyptusActivityException(
            "failed to describe autoscaling groups");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void updateAutoScalingGroup(final String userId, final String groupName,
      final List<String> zones, final int capacity) {
    updateAutoScalingGroup(userId, groupName, zones, capacity, null);
  }

  public void updateAutoScalingGroup(final String userId, final String groupName,
      final List<String> zones, final int capacity,
      final String launchConfigName) {
    final AutoScalingUpdateGroupTask task = new AutoScalingUpdateGroupTask(
        groupName, zones, capacity, launchConfigName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to update autoscaling group");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void setAutoScalingDesiredCapacity(final String userId, final String groupName,
      final int capacity) {
    final AutoScalingSetDesiredCapacityTask task = new AutoScalingSetDesiredCapacityTask(
        groupName, capacity);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to set autoscaling group capacity");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void createOrUpdateAutoscalingTags(final String userId, final String tagKey,
      final String tagValue, final String asgName) {
    final AutoscalingCreateOrUpdateTagsTask task = new AutoscalingCreateOrUpdateTagsTask(
        tagKey, tagValue, asgName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to create/update autoscaling tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  

  public List<com.eucalyptus.autoscaling.common.msgs.TagDescription> describeAutoScalingTags(final String userId) {
    final AutoScalingDescribeTagsTask task =
        new AutoScalingDescribeTagsTask();
    final CheckedListenableFuture<Boolean> result = 
        task.dispatch(new AutoScalingContext(userId));
    try{
      if(result.get()){
        return task.getTags();
      }else
        throw new EucalyptusActivityException("failed to describe tags");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteAutoscalingTags(final String userId, final String tagKey, final String tagValue,
      final String asgName) {
    final AutoscalingDeleteTagsTask task = new AutoscalingDeleteTagsTask(
        tagKey, tagValue, asgName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new AutoScalingContext(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to delete autoscaling tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
