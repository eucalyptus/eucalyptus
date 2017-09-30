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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.eucalyptus.compute.common.*;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 *
 */
public class Ec2Client {
  private static final Logger LOG = Logger.getLogger(Ec2Client.class);

  private static Ec2Client _instance = null;

  private Ec2Client() {
  }

  public static Ec2Client getInstance() {
    if (_instance == null)
      _instance = new Ec2Client();
    return _instance;
  }

  private class Ec2Context extends
      AbstractClientContext<ComputeMessage, Compute> {
    private Ec2Context(final String userId) {
      super(userId, Compute.class);
    }
  }

  private class ComputeDescribeInstanceTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private final List<String> instanceIds;
    private final AtomicReference<List<ReservationInfoType>> result = new AtomicReference<List<ReservationInfoType>>();

    private ComputeDescribeInstanceTask(final List<String> instanceId) {
      this.instanceIds = instanceId;
    }

    private DescribeInstancesType describeInstances() {
      final DescribeInstancesType req = new DescribeInstancesType();
      if ( this.instanceIds!= null && this.instanceIds.contains("verbose") ) {
        req.setInstancesSet(Lists.newArrayList("verbose"));
      }

      if ( this.instanceIds != null ) {
        req.getFilterSet().add(CloudFilters.filter("instance-id",
                this.instanceIds.stream()
                        .filter( s -> !"verbose".equals(s))
                        .collect(Collectors.toList())
        ));
      }
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeInstances(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeInstancesResponseType resp = (DescribeInstancesResponseType) response;
      this.result.set(resp.getReservationSet());
    }

    public List<RunningInstancesItemType> getInstances() {
      return this.result.get().stream()
              .flatMap( s -> s.getInstancesSet().stream() )
              .collect(Collectors.toList());
    }

    public List<ReservationInfoType> getResult() {
      return this.result.get();
    }
  }

  private class ComputeTerminateInstanceTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private final List<String> instanceIds;
    private final AtomicReference<List<String>> terminatedIds = new AtomicReference<List<String>>();

    private ComputeTerminateInstanceTask(final List<String> instanceId) {
      this.instanceIds = instanceId;
    }

    private TerminateInstancesType terminateInstances() {
      final TerminateInstancesType req = new TerminateInstancesType();
      req.setInstancesSet(Lists.newArrayList(this.instanceIds));
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Callback.Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(terminateInstances(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      TerminateInstancesResponseType resp = (TerminateInstancesResponseType) response;
      this.terminatedIds.set(Lists.transform(resp.getInstancesSet(),
          new Function<TerminateInstancesItemType, String>() {
            @Override
            public String apply(TerminateInstancesItemType item) {
              return item.getInstanceId();
            }
          }));
    }

    List<String> getTerminatedInstances() {
      return this.terminatedIds.get();
    }
  }

  // SPARK: TODO: SYSTEM, STATIC MODE?
  private class ComputeCreateGroupTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String groupName = null;
    private String groupDesc = null;
    private String groupId = null;

    ComputeCreateGroupTask(String groupName, String groupDesc) {
      this.groupName = groupName;
      this.groupDesc = groupDesc;
    }

    private CreateSecurityGroupType createSecurityGroup() {
      final CreateSecurityGroupType req = new CreateSecurityGroupType();
      req.setGroupName(this.groupName);
      req.setGroupDescription(this.groupDesc);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createSecurityGroup(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final CreateSecurityGroupResponseType resp = (CreateSecurityGroupResponseType) response;
      this.groupId = resp.getGroupId();
    }

    public String getGroupId() {
      return this.groupId;
    }
  }

  private class ComputeAuthorizeIngressRuleTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    String groupName = null;
    String protocol = null;
    int portNum = 1;

    ComputeAuthorizeIngressRuleTask(String groupName, String protocol,
        int portNum) {
      this.protocol = protocol;
      this.groupName = groupName;
      this.portNum = portNum;
    }

    private AuthorizeSecurityGroupIngressType authorize() {
      AuthorizeSecurityGroupIngressType req = new AuthorizeSecurityGroupIngressType();
      req.setGroupName(this.groupName);
      IpPermissionType perm = new IpPermissionType();
      perm.setFromPort(this.portNum);
      perm.setToPort(this.portNum);
      perm.setCidrIpRanges(Lists.newArrayList(Arrays.asList("0.0.0.0/0")));
      perm.setIpProtocol(this.protocol); // udp too?
      req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(authorize(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
    }
  }

  private class ComputeRevokeIngressRuleTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    String groupName = null;
    String protocol = null;
    int portNum = 1;

    ComputeRevokeIngressRuleTask(String groupName, String protocol, int portNum) {
      this.groupName = groupName;
      this.protocol = protocol;
      this.portNum = portNum;
    }

    private RevokeSecurityGroupIngressType revoke() {
      RevokeSecurityGroupIngressType req = new RevokeSecurityGroupIngressType();
      req.setGroupName(this.groupName);
      IpPermissionType perm = new IpPermissionType();
      perm.setFromPort(this.portNum);
      perm.setToPort(this.portNum);
      perm.setCidrIpRanges(Lists.newArrayList(Arrays.asList("0.0.0.0/0")));
      perm.setIpProtocol(this.protocol);
      req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(revoke(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
    }
  }

  private class ComputeDeleteGroupTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String groupName = null;

    ComputeDeleteGroupTask(String groupName) {
      this.groupName = groupName;
    }

    private DeleteSecurityGroupType deleteSecurityGroup() {
      final DeleteSecurityGroupType req = new DeleteSecurityGroupType();
      req.setGroupName(this.groupName);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deleteSecurityGroup(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
    }
  }

  private class ComputeDescribeGroupsTask extends EucalyptusClientTask<ComputeMessage, Compute> {
    List<SecurityGroupItemType> groups = null;
    List<String> groupNames = null;
    List<String> groupIds = null;
    ComputeDescribeGroupsTask() { }

    private DescribeSecurityGroupsType describeSecurityGroups() {
      final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType();
      if(groupNames != null && groupNames.size() > 0)
        req.setSecurityGroupSet((ArrayList<String>) groupNames);
      if(groupIds != null && groupIds.size() > 0)
        req.setSecurityGroupIdSet((ArrayList<String>) groupIds);
      return req;
    }

    public void setGroupsNames(final List<String> groupNames) {
      this.groupNames = Lists.newArrayList(groupNames);
    }

    public void setGroupIds(final List<String> groupIds) {
      this.groupIds = Lists.newArrayList(groupIds);
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeSecurityGroups(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
     final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
     groups = resp.getSecurityGroupInfo();
    }

    public List<SecurityGroupItemType> getGroups() {
      return this.groups;
    }
  }

  private class ComputeRunInstanceTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private final String availabilityZone;
    private final String imageId;
    private final String instanceType;
    private String userData;
    private ArrayList<String> groupNames;
    private int numInstances = 1;
    private String subnetId;
    private String privateIp;
    private boolean monitoring;
    private String keyName;

    private final AtomicReference<List<String>> instanceIds = new AtomicReference<List<String>>(
        Collections.<String> emptyList());

    private ComputeRunInstanceTask(final String availabilityZone,
        final String imageId, final String instanceType, int numInstances) {
      this.availabilityZone = availabilityZone;
      this.imageId = imageId;
      this.instanceType = instanceType;
      this.numInstances = numInstances;
    }

    private RunInstancesType runInstances() {
      OwnerFullName systemAcct = AccountFullName.getInstance(Principals
          .systemAccount().getAccountNumber());
      LOG.debug("runInstances with zone=" + availabilityZone + ", account="
          + systemAcct);
      final RunInstancesType req = new RunInstancesType();
      req.setImageId(this.imageId);
      req.setInstanceType(this.instanceType);
      if (keyName != null)
        req.setKeyName(this.keyName);
      req.setMonitoring(this.monitoring);
      if (subnetId != null) {
        final InstanceNetworkInterfaceSetItemRequestType networkInterface = req
            .primaryNetworkInterface(true);
        networkInterface.setSubnetId(this.subnetId);
        networkInterface.setPrivateIpAddress(this.privateIp);
      } else {
        if (groupNames != null && !groupNames.isEmpty())
          req.setGroupSet(this.groupNames);
        req.setAvailabilityZone(this.availabilityZone);
      }
      if (availabilityZone != null)
        req.setAvailabilityZone(availabilityZone);
      req.setMinCount(Math.max(1, numInstances));
      req.setMaxCount(Math.max(1, numInstances));
      if (userData != null)
        req.setUserData(userData);
      return req;
    }

    @Override
    void dispatchInternal(final ClientContext<ComputeMessage, Compute> context,
        final Callback.Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(runInstances(), callback);
    }

    @Override
    void dispatchSuccess(final ClientContext<ComputeMessage, Compute> context,
        final ComputeMessage response) {
      final List<String> instanceIds = Lists.newArrayList();
      RunInstancesResponseType resp = (RunInstancesResponseType) response;
      for (final RunningInstancesItemType item : resp.getRsvInfo()
          .getInstancesSet()) {
        instanceIds.add(item.getInstanceId());
      }

      this.instanceIds.set(ImmutableList.copyOf(instanceIds));
    }

    void setUserData(String userData) {
      this.userData = userData;
    }

    void setSecurityGroups(ArrayList<String> groupNames) {
      this.groupNames = groupNames;
    }

    void setSubnetId(String subnetId) {
      this.subnetId = subnetId;
    }

    void setPrivateIp(String privateIp) {
      this.privateIp = privateIp;
    }

    void setMonitoring(boolean monitoring) {
      this.monitoring = monitoring;
    }

    void setKeyName(String keyName) {
      this.keyName = keyName;
    }

    List<String> getInstanceIds() {
      return instanceIds.get();
    }
  }

  private class ComputeDescribeAvailabilityZonesTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private List<ClusterInfoType> zones = null;
    private boolean verbose = false;

    private ComputeDescribeAvailabilityZonesTask(boolean verbose) {
      this.verbose = verbose;
    }

    private DescribeAvailabilityZonesType describeAvailabilityZones() {
      final DescribeAvailabilityZonesType req = new DescribeAvailabilityZonesType();
      if (this.verbose) {
        req.setAvailabilityZoneSet(Lists.newArrayList("verbose"));
      }
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeAvailabilityZones(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      // TODO Auto-generated method stub
      final DescribeAvailabilityZonesResponseType resp = (DescribeAvailabilityZonesResponseType) response;
      zones = resp.getAvailabilityZoneInfo();
    }

    public List<ClusterInfoType> getAvailabilityZones() {
      return this.zones;
    }
  }

  private class ComputeDescribeImagesTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private List<String> imageIds = null;
    private List<ImageDetails> result = null;

    private ComputeDescribeImagesTask(final List<String> imageIds) {
      this.imageIds = imageIds;
    }

    private DescribeImagesType describeImages() {
      final DescribeImagesType req = new DescribeImagesType();
      if (this.imageIds != null && this.imageIds.size() > 0) {
        req.setFilterSet(Lists.newArrayList(CloudFilters.filter("image-id",
            this.imageIds)));
      }
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeImages(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeImagesResponseType resp = (DescribeImagesResponseType) response;
      result = resp.getImagesSet();
    }

    List<ImageDetails> getResult() {
      return this.result;
    }
  }

  private class ComputeDeleteTagsTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private Map<String, String> tags = null;
    private List<String> resources = null;

    private ComputeDeleteTagsTask(Map<String, String> tags,
        final List<String> resources) {
      this.tags = tags;
      this.resources = resources;
    }

    private DeleteTagsType deleteTags() {
      final DeleteTagsType req = new DeleteTagsType();
      req.setResourcesSet(Lists.newArrayList(this.resources));
      for (Map.Entry<String, String> t : tags.entrySet()) {
        final DeleteResourceTag tag = new DeleteResourceTag();
        tag.setKey(t.getKey());
        if (tag.getValue() != null)
          tag.setValue(t.getValue());
        req.getTagSet().add(tag);
      }
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {

      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deleteTags(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
    }
  }

  private class ComputeCreateTagsTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String tagKey = null;
    private String tagValue = null;
    private List<String> resources = null;

    private ComputeCreateTagsTask(final String tagKey, final String tagValue,
        final List<String> resources) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
      this.resources = resources;
    }

    private CreateTagsType createTags() {
      final CreateTagsType req = new CreateTagsType();
      req.setResourcesSet(Lists.newArrayList(this.resources));
      final ResourceTag tag = new ResourceTag();
      tag.setKey(this.tagKey);
      tag.setValue(this.tagValue);
      req.setTagSet(Lists.newArrayList(tag));
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createTags(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
    }
  }

  private class ComputeDescribeKeyPairsTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private ArrayList<String> keyNames = null;
    private List<DescribeKeyPairsResponseItemType> result = null;

    private ComputeDescribeKeyPairsTask(final ArrayList<String> keyNames) {
      this.keyNames = keyNames;
    }

    private DescribeKeyPairsType describeKeyPairs() {
      final DescribeKeyPairsType req = new DescribeKeyPairsType();
      req.setKeySet(keyNames);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeKeyPairs(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeKeyPairsResponseType resp = (DescribeKeyPairsResponseType) response;
      result = resp.getKeySet();
    }

    List<DescribeKeyPairsResponseItemType> getResult() {
      return result;
    }
  }

  private class DescribeTagsTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private List<String> names = null;
    private List<String> values = null;
    private List<TagInfo> tags = null;

    private DescribeTagsTask(final List<String> names, final List<String> values) {
      this.names = names;
      this.values = values;
    }

    private DescribeTagsType describeTags() {
      if (names.size() != values.size())
        throw Exceptions.toUndeclared(new Exception(
            "Names and values don't match"));

      final DescribeTagsType req = new DescribeTagsType();
      List<Filter> filterSet = Lists.newArrayList();
      for (int i = 0; i < names.size(); i++) {
        final String name = names.get(i);
        final String value = values.get(i);
        final Filter f = new Filter();
        f.setName(name);
        f.setValueSet(Lists.newArrayList(value));
        filterSet.add(f);
      }
      req.setFilterSet((ArrayList<Filter>) filterSet);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeTags(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeTagsResponseType resp = (DescribeTagsResponseType) response;
      tags = resp.getTagSet();
    }

    public List<TagInfo> getTags() {
      return tags;
    }
  }

  private class ComputeCreateSnapshotTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String volumeId = null;
    private String snapshotId = null;

    private ComputeCreateSnapshotTask(final String volumeId) {
      this.volumeId = volumeId;
    }

    private CreateSnapshotType createSnapshot() {
      final CreateSnapshotType req = new CreateSnapshotType();
      req.setVolumeId(this.volumeId);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createSnapshot(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final CreateSnapshotResponseType resp = (CreateSnapshotResponseType) response;
      try {
        this.snapshotId = resp.getSnapshot().getSnapshotId();
      } catch (final Exception ex) {
        ;
      }
    }

    public String getSnapshotId() {
      return this.snapshotId;
    }
  }


  private class ComputeDeleteSnapshotTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String snapshotId = null;

    private ComputeDeleteSnapshotTask( final String snapshotId ) {
      this.snapshotId = snapshotId;
    }

    private DeleteSnapshotType deleteSnapshot() {
     final DeleteSnapshotType req = new  DeleteSnapshotType();
     req.setSnapshotId(this.snapshotId);
     return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deleteSnapshot(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DeleteSnapshotResponseType resp = (DeleteSnapshotResponseType) response;
    }
  }

  private class ComputeRegisterEBSImageTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String snapshotId = null;
    private String description = null;
    private String name = null;
    private String architecture = null;
    private String platform = null;
    private String imageId = null;
    private boolean deleteOnTermination = false;
    private static final String ROOT_DEVICE_NAME = "/dev/sda";

    private ComputeRegisterEBSImageTask(final String snapshotId,
        final String name, final String architecture) {
      this.snapshotId = snapshotId;
      this.architecture = architecture;
      this.name = name;
    }

    private void setDescription(final String description) {
      this.description = description;
    }

    private void setPlatform(final String platform) {
      this.platform = platform;
    }

    private void setDeleteOnTermination(final boolean deleteOnTermination) {
      this.deleteOnTermination = deleteOnTermination;
    }

    private RegisterImageType register() {
      final RegisterImageType req = new RegisterImageType();
      req.setRootDeviceName(ROOT_DEVICE_NAME);
      final BlockDeviceMappingItemType device = new BlockDeviceMappingItemType();
      device.setDeviceName(ROOT_DEVICE_NAME);
      final EbsDeviceMapping ebsMap = new EbsDeviceMapping();
      ebsMap.setSnapshotId(this.snapshotId);
      ebsMap.setDeleteOnTermination(this.deleteOnTermination);
      device.setEbs(ebsMap);
      req.setBlockDeviceMappings(Lists.newArrayList(device));
      req.setArchitecture(this.architecture);
      if (this.description != null)
        req.setDescription(this.description);
      req.setName(this.name);
      if ("windows".equals(this.platform))
        req.setKernelId("windows");
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(register(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final RegisterImageResponseType resp = (RegisterImageResponseType) response;
      this.imageId = resp.getImageId();
    }

    public String getImageId() {
      return this.imageId;
    }
  }

  private class ComputeDeregisterImageTask extends
  EucalyptusClientTask<ComputeMessage, Compute> {
    private String imageId = null;
    private ComputeDeregisterImageTask(final String imageId) {
      this.imageId = imageId;
    }

    private DeregisterImageType deregister() {
      final DeregisterImageType req = new DeregisterImageType();
      req.setImageId(this.imageId);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deregister(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DeregisterImageResponseType resp = (DeregisterImageResponseType) response;
    }
  }

  private class CreateVolumeTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String availabilityZone = null;
    private String snapshotId = null;
    private int size = -1;
    private String volumeId = null;

    private CreateVolumeTask(final String availabilityZone, final int size) {
      this.availabilityZone = availabilityZone;
      this.size = size;
    }

    private CreateVolumeTask(final String availabilityZone, final int size,
        final String snapshotId) {
      this(availabilityZone, size);
      this.snapshotId = snapshotId;
    }

    private CreateVolumeType createVolume() {
      final CreateVolumeType req = new CreateVolumeType();
      req.setAvailabilityZone(this.availabilityZone);
      req.setSize(Integer.toString(this.size));
      if (this.snapshotId != null)
        req.setSnapshotId(snapshotId);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createVolume(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final CreateVolumeResponseType resp = (CreateVolumeResponseType) response;
      final Volume vol = resp.getVolume();
      if (vol != null && !"error".equals(vol.getStatus())) {
        this.volumeId = vol.getVolumeId();
      }
    }

    public String getVolumeId() {
      return this.volumeId;
    }
  }

  private class DeleteVolumeTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private String volumeId = null;

    private DeleteVolumeTask(final String volumeId) {
      this.volumeId = volumeId;
    }

    private DeleteVolumeType deleteVolume() {
      final DeleteVolumeType req = new DeleteVolumeType();
      req.setVolumeId(this.volumeId);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deleteVolume(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
    }
  }

  private class DescribeVolumesTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private boolean verbose;
    private List<String> volumeIds = null;
    private List<Volume> result = null;

    private DescribeVolumesTask(final boolean verbose,
        final List<String> volumeIds) {
      this.verbose = verbose;
      this.volumeIds = volumeIds;
    }

    private DescribeVolumesType describeVolumes() {
      final DescribeVolumesType req = new DescribeVolumesType();
      if (verbose) {
        req.setVolumeSet(Lists.newArrayList("verbose"));
      }
      if (this.volumeIds != null && this.volumeIds.size() > 0) {
        req.getFilterSet().add(CloudFilters.filter("volume-id", volumeIds));
      }
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeVolumes(), callback);
    }

    @Override
    boolean dispatchFailure(
        final ClientContext<ComputeMessage, Compute> context,
        final Throwable throwable) {
      if (AsyncExceptions.isWebServiceErrorCode(throwable,
          "InvalidVolume.NotFound")) {
        this.result = Lists.newArrayList();
        return true;
      } else {
        return super.dispatchFailure(context, throwable);
      }
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeVolumesResponseType resp = (DescribeVolumesResponseType) response;
      this.result = resp.getVolumeSet();
    }

    public List<Volume> getVolumes() {
      return this.result;
    }
  }

  private class DescribeSnapshotsTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private final boolean verbose;
    private List<String> snapshots = null;
    private List<Snapshot> results = null;

    private DescribeSnapshotsTask(final boolean verbose,
        final List<String> subnetIds) {
      this.verbose = verbose;
      this.snapshots = subnetIds;
    }

    private DescribeSnapshotsType describeSnapshots() {
      final DescribeSnapshotsType req = new DescribeSnapshotsType();
      if (verbose) {
        req.setSnapshotSet(Lists.newArrayList("verbose"));
      }
      req.getFilterSet().add(CloudFilters.filter("snapshot-id", this.snapshots));

      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeSnapshots(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeSnapshotsResponseType resp = (DescribeSnapshotsResponseType) response;
      this.results = resp.getSnapshotSet();
    }

    public List<Snapshot> getSnapshots() {
      return this.results;
    }
  }

  private class DescribeSubnetsTask extends
      EucalyptusClientTask<ComputeMessage, Compute> {
    private List<String> subnetIds = null;
    private List<SubnetType> result = null;

    private DescribeSubnetsTask(final List<String> subnetIds) {
      this.subnetIds = subnetIds;
    }

    private DescribeSubnetsType describeSubnets() {
      final DescribeSubnetsType req = new DescribeSubnetsType();
      if (subnetIds != null) {
        final Filter filter = new Filter();
        filter.setName("subnet-id");
        filter.setValueSet(Lists.newArrayList(subnetIds));
        req.getFilterSet().add(filter);
      }
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeSubnets(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeSubnetsResponseType resp = (DescribeSubnetsResponseType) response;
      this.result = resp.getSubnetSet().getItem();
    }

    public List<SubnetType> getSubnets() {
      return this.result;
    }
  }

  private class DescribeInstanceTypesTask extends EucalyptusClientTask<ComputeMessage, Compute> {
    private List<VmTypeDetails> result = null;

    private DescribeInstanceTypesTask() { }

    private DescribeInstanceTypesType describeInstancceTypes() {
      final DescribeInstanceTypesType req = new DescribeInstanceTypesType();
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeInstancceTypes(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeInstanceTypesResponseType resp = (DescribeInstanceTypesResponseType) response;
      this.result = resp.getInstanceTypeDetails();
    }

    public List<VmTypeDetails> getInstanceTypes() {
      return this.result;
    }
  }

  private class ComputeDescribeDefaultVpcsTask extends EucalyptusClientTask<ComputeMessage, Compute> {
    private ArrayList<VpcType> result;

    private DescribeVpcsType describeDefaultVPC() {
      final DescribeVpcsType req = new DescribeVpcsType();
      req.getFilterSet().add(CloudFilters.filter( "isDefault", Collections.singleton(Boolean.TRUE.toString()) ) );
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeDefaultVPC(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeVpcsResponseType resp = (DescribeVpcsResponseType) response;
      result = resp.getVpcSet( ).getItem();
    }

    public List<VpcType> getDefaultVpcs() {
      return this.result;
    }
  }

  private class ComputeCreateDefaultVpcsTask extends EucalyptusClientTask<ComputeMessage, Compute> {
    private VpcType result;
    private String accountNumber;

    public ComputeCreateDefaultVpcsTask(String accountNumber) {
      this.accountNumber = accountNumber;
    }

    private CreateVpcType createDefaultVPC() {
      final CreateVpcType req = new CreateVpcType();
      req.setCidrBlock(accountNumber);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createDefaultVPC(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final CreateVpcResponseType resp = (CreateVpcResponseType) response;
      result = resp.getVpc();
    }

    public VpcType getVpc() { return result; }
  }

  public List<String> runInstances(final String userId, final String imageId,
      final ArrayList<String> groupNames, final String userData,
      final String instanceType, final String availabilityZone,
      final String subnetId, final String privateIp, boolean monitoring,
      final String keyName, final int numInstances)
      throws EucalyptusActivityException {
    LOG.debug("launching instances at zone=" + availabilityZone + ", imageId="
        + imageId);
    final ComputeRunInstanceTask task = new ComputeRunInstanceTask(
        availabilityZone, imageId, instanceType, numInstances);
    task.setUserData(userData);
    task.setSecurityGroups(groupNames);
    task.setSubnetId(subnetId);
    task.setPrivateIp(privateIp);
    task.setMonitoring(monitoring);
    task.setKeyName(keyName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<String> instances = task.getInstanceIds();
        return instances;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to launch the instance");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<String> terminateInstances(final String userId,
      final List<String> instances) throws EucalyptusActivityException {
    LOG.debug(String.format("terminating %d instances", instances.size()));
    if (instances.size() <= 0)
      return instances;

    final ComputeTerminateInstanceTask task = new ComputeTerminateInstanceTask(
        instances);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<String> terminated = task.getTerminatedInstances();
        return terminated;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to terminate the instances");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<RunningInstancesItemType> describeInstances(final String userId,
      final List<String> instances) {
    if (userId == null) // run in verbose mode for system user
      instances.add("verbose");
    final ComputeDescribeInstanceTask task = new ComputeDescribeInstanceTask(
        instances);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<RunningInstancesItemType> describe = task.getInstances();
        return describe;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "Failed to describe the instances");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<ReservationInfoType> describeInstanceReservations(final String userId, final List<String> instances) {
    if (userId == null) // run in verbose mode for system user
      instances.add("verbose");
    final ComputeDescribeInstanceTask task = new ComputeDescribeInstanceTask(
            instances);
    final CheckedListenableFuture<Boolean> result = task
            .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
                task.getErrorMessage() != null ? task.getErrorMessage()
                        : "failed to describe the instances");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<ClusterInfoType> describeAvailabilityZones(final String userId,
      boolean verbose) throws EucalyptusActivityException {
    final ComputeDescribeAvailabilityZonesTask task = new ComputeDescribeAvailabilityZonesTask(
        verbose);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<ClusterInfoType> describe = task.getAvailabilityZones();
        return describe;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe the availability zones");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }

  }

  public void createSecurityGroup(final String userId, String groupName,
      String groupDesc) throws EucalyptusActivityException {
    final ComputeCreateGroupTask task = new ComputeCreateGroupTask(groupName,
        groupDesc);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get() && task.getGroupId() != null) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create the group " + groupName);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteSecurityGroup(final String userId, String groupName)
      throws EucalyptusActivityException {
    final ComputeDeleteGroupTask task = new ComputeDeleteGroupTask(groupName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to delete the group " + groupName);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void authorizeSecurityGroup(final String userId, String groupName,
      String protocol, int portNum) throws EucalyptusActivityException {
    final ComputeAuthorizeIngressRuleTask task = new ComputeAuthorizeIngressRuleTask(
        groupName, protocol, portNum);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : String.format("failed to authorize:%s, %s, %d ", groupName,
                    protocol, portNum));
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void revokeSecurityGroup(final String userId, String groupName,
      String protocol, int portNum) throws EucalyptusActivityException {
    final ComputeRevokeIngressRuleTask task = new ComputeRevokeIngressRuleTask(
        groupName, protocol, portNum);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : String.format("failed to revoke:%s, %s, %d ", groupName,
                    protocol, portNum));
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  /*
   * Throws an error if keypair not found
   */
  public List<DescribeKeyPairsResponseItemType> describeKeyPairs(
      final String userId, final ArrayList<String> keyNames)
      throws EucalyptusActivityException {
    if (userId == null) // run in verbose mode for system user
      keyNames.add("verbose");
    final ComputeDescribeKeyPairsTask task = new ComputeDescribeKeyPairsTask(
        keyNames);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe keypairs");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<ImageDetails> describeImages(final String userId,
      final List<String> imageIds) throws EucalyptusActivityException {
    if (userId == null) // run in verbose mode for system user
      imageIds.add("verbose");
    final ComputeDescribeImagesTask task = new ComputeDescribeImagesTask(
        imageIds);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe images");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void createTags(final String userId, final String tagKey,
      final String tagValue, final List<String> resources)
      throws EucalyptusActivityException {
    final ComputeCreateTagsTask task = new ComputeCreateTagsTask(tagKey,
        tagValue, resources);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<TagInfo> describeTags(final String userId,
      final List<String> names, final List<String> values)
      throws EucalyptusActivityException {
    if (userId == null) // run in verbose mode for system user
      values.add("verbose");
    final DescribeTagsTask task = new DescribeTagsTask(names, values);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getTags();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteTags(final String userId, final List<String> resources,
      ArrayList<String> tags) throws EucalyptusActivityException {
    Map<String, String> tagsMap = new HashMap<String, String>();
    for (String s : tags) {
      tagsMap.put(s, null);
    }
    ;
    deleteTags(userId, resources, tagsMap);
  }

  public void deleteTags(final String userId, final List<String> resources,
      Map<String, String> tags) throws EucalyptusActivityException {
    final ComputeDeleteTagsTask task = new ComputeDeleteTagsTask(tags,
        resources);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to delete tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public String createVolume(final String userId, final String zone,
      final int size, final String snapshotId)
      throws EucalyptusActivityException {
    final CreateVolumeTask task = new CreateVolumeTask(zone, size, snapshotId);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getVolumeId();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create volume");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public String createVolume(final String userId, final String zone,
      final int size) throws EucalyptusActivityException {
    final CreateVolumeTask task = new CreateVolumeTask(zone, size);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getVolumeId();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create volume");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public String createSnapshot(final String userId, final String volumeId)
      throws EucalyptusActivityException {
    final ComputeCreateSnapshotTask task = new ComputeCreateSnapshotTask(
        volumeId);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getSnapshotId();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create snapshot");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteSnapshot(final String userId, final String snapshotId)
      throws EucalyptusActivityException {
    final ComputeDeleteSnapshotTask task = new ComputeDeleteSnapshotTask(snapshotId);

    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (!result.get()) {
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to delete snapshot");
      }
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  public String registerEBSImage(final String userId, final String snapshotId,
      final String imageName, String architecture, final String platform,
      final String description, final boolean deleteOnTermination)
      throws EucalyptusActivityException {
    if (userId == null || userId.length() <= 0)
      throw new IllegalArgumentException("User ID is required");
    if (snapshotId == null || snapshotId.length() <= 0)
      throw new IllegalArgumentException("Snapshot ID is required");
    if (imageName == null || imageName.length() <= 0)
      throw new IllegalArgumentException("Image name is required");
    if (architecture == null)
      architecture = "i386";

    final ComputeRegisterEBSImageTask task = new ComputeRegisterEBSImageTask(
        snapshotId, imageName, architecture);
    if (platform != null)
      task.setPlatform(platform);
    if (description != null)
      task.setDescription(description);
    task.setDeleteOnTermination(deleteOnTermination);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get())
        return task.getImageId();
      else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to register ebs image");
    } catch (final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deregisterImage(final String userId, final String imageId) {
    if (userId == null || userId.length() <= 0)
      throw new IllegalArgumentException("User ID is required");
    if (imageId == null || imageId.length() <= 0)
      throw new IllegalArgumentException("Image ID is required");

    final ComputeDeregisterImageTask task = new ComputeDeregisterImageTask(imageId);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try{
      if (!result.get())
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to deregister image");
      }catch(final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteVolume(final String userId, final String volumeId)
      throws EucalyptusActivityException {
    final DeleteVolumeTask task = new DeleteVolumeTask(volumeId);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to delete volume");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<Volume> describeVolumes(final String userId,
      final List<String> volumeIds) throws EucalyptusActivityException {
    // run in verbose mode for system user
    final DescribeVolumesTask task = new DescribeVolumesTask(userId == null,
        volumeIds);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getVolumes();
      } else {
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe volumes");
      }
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<SubnetType> describeSubnets(final String userId,
      final List<String> subnetIds) throws EucalyptusActivityException {
    if (userId == null) // run in verbose mode for system user
      subnetIds.add("verbose");
    final DescribeSubnetsTask task = new DescribeSubnetsTask(subnetIds);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getSubnets();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe subnets");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<Snapshot> describeSnapshots(final String userId,
      final List<String> snapshotIds) throws EucalyptusActivityException {
    // run in verbose mode for system user
    final DescribeSnapshotsTask task = new DescribeSnapshotsTask(
        userId == null, snapshotIds);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getSnapshots();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe snapshots");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<VmTypeDetails> describeInstanceTypes(final String userId)
      throws EucalyptusActivityException {
    final DescribeInstanceTypesTask task = new DescribeInstanceTypesTask();
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getInstanceTypes();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe instance types");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<SecurityGroupItemType> describeSecurityGroups(final String userId, final List<String> groupNames)
      throws EucalyptusActivityException {
    final ComputeDescribeGroupsTask task = new ComputeDescribeGroupsTask();
    if(groupNames != null)
      task.setGroupsNames(groupNames);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getGroups();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe security groups");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public boolean hasDefaultVPC(final String userId) throws EucalyptusActivityException {
    final ComputeDescribeDefaultVpcsTask task = new ComputeDescribeDefaultVpcsTask();
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        if (!task.getDefaultVpcs().isEmpty() && task.getDefaultVpcs().size() == 1)
          return true;
        else
          return false;
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to describe default VPC");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public VpcType createDefaultVPC(final String accountNumber) throws EucalyptusActivityException {
    final ComputeCreateDefaultVpcsTask task = new ComputeCreateDefaultVpcsTask(accountNumber);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(null));
    try {
      if (result.get()) {
        return task.getVpc();
      } else
        throw new EucalyptusActivityException(
            task.getErrorMessage() != null ? task.getErrorMessage()
                : "failed to create default VPC");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
