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
package com.eucalyptus.resources.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.compute.common.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.CreateSecurityGroupType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.DeleteSecurityGroupResponseType;
import com.eucalyptus.compute.common.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.DescribeAvailabilityZonesType;
import com.eucalyptus.compute.common.DescribeImagesResponseType;
import com.eucalyptus.compute.common.DescribeImagesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseType;
import com.eucalyptus.compute.common.DescribeKeyPairsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressType;
import com.eucalyptus.compute.common.RunInstancesResponseType;
import com.eucalyptus.compute.common.RunInstancesType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.compute.common.TerminateInstancesItemType;
import com.eucalyptus.compute.common.TerminateInstancesResponseType;
import com.eucalyptus.compute.common.TerminateInstancesType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.Callback.Checked;
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
  private Ec2Client(){ }
  public static Ec2Client getInstance(){
    if(_instance == null)
      _instance = new Ec2Client();
    return _instance;
  }
  
  private class Ec2Context extends AbstractClientContext<ComputeMessage, Compute> {
    private Ec2Context(final String userId) {
      super(userId, Compute.class);
    }
  }
  
  private class ComputeDescribeInstanceTask extends
  EucalyptusClientTask<ComputeMessage, Compute> {
    private final List<String> instanceIds;
    private final AtomicReference<List<RunningInstancesItemType>> result = new AtomicReference<List<RunningInstancesItemType>>();

    private ComputeDescribeInstanceTask(final List<String> instanceId) {
      this.instanceIds = instanceId;
    }

    private DescribeInstancesType describeInstances() {
      final DescribeInstancesType req = new DescribeInstancesType();
      req.setInstancesSet(Lists.newArrayList(this.instanceIds));
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeInstances(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeInstancesResponseType resp = (DescribeInstancesResponseType) response;
      final List<RunningInstancesItemType> resultInstances = Lists
          .newArrayList();
      for (final ReservationInfoType res : resp.getReservationSet()) {
        resultInstances.addAll(res.getInstancesSet());
      }
      this.result.set(resultInstances);
    }

    public List<RunningInstancesItemType> getResult() {
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Callback.Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(terminateInstances(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createSecurityGroup(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(authorize(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final AuthorizeSecurityGroupIngressResponseType resp = (AuthorizeSecurityGroupIngressResponseType) response;
    }
  }

  private class ComputeRevokeIngressRuleTask extends
  EucalyptusClientTask<ComputeMessage, Compute> {
    String groupName = null;
    String protocol = null;
    int portNum = 1;

    ComputeRevokeIngressRuleTask(String groupName, String protocol,
        int portNum) {
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(revoke(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final RevokeSecurityGroupIngressResponseType resp = (RevokeSecurityGroupIngressResponseType) response;
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deleteSecurityGroup(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DeleteSecurityGroupResponseType resp = (DeleteSecurityGroupResponseType) response;
    }
  }

  private class ComputeDescribeSecurityGroupTask extends
  EucalyptusClientTask<ComputeMessage, Compute> {
    private List<String> groups = null;
    private List<SecurityGroupItemType> result = null;

    ComputeDescribeSecurityGroupTask(final List<String> groups) {
      this.groups = groups;
    }

    private DescribeSecurityGroupsType describeSecurityGroups() {
      final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType();
      req.setSecurityGroupSet(Lists.newArrayList(this.groups));
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeSecurityGroups(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
      this.result = resp.getSecurityGroupInfo();
    }

    public List<SecurityGroupItemType> getResult() {
      return this.result;
    }
  }

  private class ComputeLaunchInstanceTask extends
  EucalyptusClientTask<ComputeMessage, Compute> {
    private final String availabilityZone;
    private final String imageId;
    private final String instanceType;
    private String userData;
    private String groupName;
    private int numInstances = 1;
    private final AtomicReference<List<String>> instanceIds = new AtomicReference<List<String>>(
        Collections.<String> emptyList());

    private ComputeLaunchInstanceTask(final String availabilityZone,
        final String imageId, final String instanceType, int numInstances) {
      this.availabilityZone = availabilityZone;
      this.imageId = imageId;
      this.instanceType = instanceType;
      this.numInstances = numInstances;
    }

    private RunInstancesType runInstances() {
      OwnerFullName systemAcct = AccountFullName.getInstance(Principals
          .systemAccount());
      LOG.info("runInstances with zone=" + availabilityZone + ", account="
          + systemAcct);

      final RunInstancesType runInstances = new RunInstancesType();
      runInstances.setImageId(imageId);
      runInstances.setInstanceType(instanceType);
      if (groupName != null) {
        runInstances.setSecurityGroups(Lists.newArrayList(new GroupItemType(
            groupName, null))); // Name or ID can be passed as ID
      }
      if (availabilityZone != null)
        runInstances.setAvailabilityZone(availabilityZone);
      runInstances.setMinCount(Math.max(1, numInstances));
      runInstances.setMaxCount(Math.max(1, numInstances));
      runInstances.setUserData(userData);
      return runInstances;
    }

    @Override
    void dispatchInternal(
        final ClientContext<ComputeMessage, Compute> context,
        final Callback.Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(runInstances(), callback);
    }

    @Override
    void dispatchSuccess(
        final ClientContext<ComputeMessage, Compute> context,
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

    void setSecurityGroup(String groupName) {
      this.groupName = groupName;
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeAvailabilityZones(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
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
        req.setImagesSet(new ArrayList(imageIds));
      }
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeImages(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
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
    private String tagKey = null;
    private String tagValue = null;
    private List<String> resources = null;

    private ComputeDeleteTagsTask(final String tagKey, final String tagValue,
        final List<String> resources) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
      this.resources = resources;
    }

    private DeleteTagsType deleteTags() {
      final DeleteTagsType req = new DeleteTagsType();
      req.setResourcesSet(Lists.newArrayList(this.resources));
      final DeleteResourceTag tag = new DeleteResourceTag();
      tag.setKey(this.tagKey);
      tag.setValue(this.tagValue);
      req.setTagSet(Lists.newArrayList(tag));
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {

      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(deleteTags(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DeleteTagsResponseType resp = (DeleteTagsResponseType) response;
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
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(createTags(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final CreateTagsResponseType resp = (CreateTagsResponseType) response;
    }
  }

  private class ComputeDescribeKeyPairsTask extends
  EucalyptusClientTask<ComputeMessage, Compute> {
    private List<String> keyNames = null;
    private List<DescribeKeyPairsResponseItemType> result = null;

    private ComputeDescribeKeyPairsTask() {
    }

    private ComputeDescribeKeyPairsTask(final String keyName) {
      this.keyNames = Lists.newArrayList(keyName);
    }

    private ComputeDescribeKeyPairsTask(final List<String> keyNames) {
      this.keyNames = keyNames;
    }

    private DescribeKeyPairsType describeKeyPairs() {
      final DescribeKeyPairsType req = new DescribeKeyPairsType();
      if (this.keyNames != null) {
        req.setKeySet(new ArrayList<String>(this.keyNames));
      }
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context
          .getClient();
      client.dispatch(describeKeyPairs(), callback);
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeKeyPairsResponseType resp = (DescribeKeyPairsResponseType) response;
      result = resp.getKeySet();
    }

    List<DescribeKeyPairsResponseItemType> getResult() {
      return result;
    }
  }
  
  private class DescribeTagsTask extends EucalyptusClientTask<ComputeMessage, Compute>{
    private List<String> names = null;
    private List<String> values = null;
    private List<TagInfo> tags = null;
    private DescribeTagsTask(final List<String> names, final List<String> values){
      this.names = names;
      this.values = values;
    }
    
    private DescribeTagsType describeTags(){
      if(names.size() != values.size())
        throw Exceptions.toUndeclared(new Exception("Names and values don't match"));
      
      final DescribeTagsType req = new DescribeTagsType();
      List<Filter> filterSet = Lists.newArrayList();
      for(int i=0; i<names.size(); i++){
        final String name = names.get(i);
        final String value = values.get(i);
        final Filter f = new Filter();
        f.setName(name);
        f.setValueSet(Lists.newArrayList(value));
        filterSet.add(f);
      }
      req.setFilterSet((ArrayList<Filter>)filterSet);
      return req;
    }
    
    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context.getClient();
      client.dispatch(describeTags(), callback);               
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeTagsResponseType resp = 
          (DescribeTagsResponseType) response;
      tags = resp.getTagSet();
    }
    
    public List<TagInfo> getTags(){
      return tags;
    }
  }
  

  private class DescribeVolumesTask extends EucalyptusClientTask<ComputeMessage, Compute>{
    private List<String> volumeIds = null;
    private List<Volume> result = null;
    
    private DescribeVolumesTask(){
      this.volumeIds = null;
    }
    private DescribeVolumesTask(final List<String> volumeIds){
      this.volumeIds = volumeIds;
    }
    
    private DescribeVolumesType describeVolumes(){
      final DescribeVolumesType req = new  DescribeVolumesType();
      if(this.volumeIds != null && this.volumeIds.size() > 0){
        req.setVolumeSet(Lists.newArrayList(this.volumeIds));
      }
      return req;
    }

    @Override
    void dispatchInternal(
        ClientContext<ComputeMessage, Compute> context,
        Checked<ComputeMessage> callback) {
      final DispatchingClient<ComputeMessage, Compute> client = context.getClient();
      client.dispatch(describeVolumes(), callback);          
    }

    @Override
    void dispatchSuccess(
        ClientContext<ComputeMessage, Compute> context,
        ComputeMessage response) {
      final DescribeVolumesResponseType resp = (DescribeVolumesResponseType) response;
      this.result = resp.getVolumeSet();
    }
    
    public List<Volume> getVolumes(){
      return this.result;
    }
  }



  public List<String> launchInstances(final String userId, final String availabilityZone,
      final String imageId, final String instanceType, final int numInstances) {
    return launchInstances(userId, availabilityZone, imageId, instanceType, null, null,
        numInstances);
  }

  public List<String> launchInstances(final String userId, final String availabilityZone,
      final String imageId, final String instanceType, String groupName,
      final String userData, final int numInstances) {
    LOG.info("launching instances at zone=" + availabilityZone + ", imageId="
        + imageId + ", group=" + groupName);
    final ComputeLaunchInstanceTask launchTask = new ComputeLaunchInstanceTask(
        availabilityZone, imageId, instanceType, numInstances);
    if (userData != null)
      launchTask.setUserData(userData);
    if (groupName != null)
      launchTask.setSecurityGroup(groupName);
    final CheckedListenableFuture<Boolean> result = launchTask
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<String> instances = launchTask.getInstanceIds();
        return instances;
      } else
        throw new EucalyptusActivityException("failed to launch the instance");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<String> terminateInstances(final String userId, final List<String> instances) {
    LOG.info(String.format("terminating %d instances", instances.size()));
    if (instances.size() <= 0)
      return instances;

    final ComputeTerminateInstanceTask terminateTask = new ComputeTerminateInstanceTask(
        instances);
    final CheckedListenableFuture<Boolean> result = terminateTask
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<String> terminated = terminateTask.getTerminatedInstances();
        return terminated;
      } else
        throw new EucalyptusActivityException(
            "failed to terminate the instances");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<RunningInstancesItemType> describeInstances(
      final String userId, final List<String> instances) {
    final ComputeDescribeInstanceTask describeTask = 
        new ComputeDescribeInstanceTask(instances);
    final CheckedListenableFuture<Boolean> result = describeTask
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        final List<RunningInstancesItemType> describe = describeTask
            .getResult();
        return describe;
      } else
        throw new EucalyptusActivityException(
            "failed to describe the instances");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<ClusterInfoType> describeAvailabilityZones(final String userId, boolean verbose) {
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
            "failed to describe the availability zones");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }

  }

  public void createSecurityGroup(final String userId, String groupName, String groupDesc) {
    final ComputeCreateGroupTask task = new ComputeCreateGroupTask(
        groupName, groupDesc);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get() && task.getGroupId() != null) {
        return;
      } else
        throw new EucalyptusActivityException("failed to create the group "
            + groupName);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteSecurityGroup(final String userId, String groupName) {
    final ComputeDeleteGroupTask task = new ComputeDeleteGroupTask(
        groupName);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to delete the group "
            + groupName);
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public List<SecurityGroupItemType> describeSecurityGroups(
      final String userId, List<String> groupNames) {
    final ComputeDescribeSecurityGroupTask task = new ComputeDescribeSecurityGroupTask(
        groupNames);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException(
            "failed to describe security groups");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void authorizeSecurityGroup(final String userId, String groupName, String protocol,
      int portNum) {
    final ComputeAuthorizeIngressRuleTask task = new ComputeAuthorizeIngressRuleTask(
        groupName, protocol, portNum);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(String.format(
            "failed to authorize:%s, %s, %d ", groupName, protocol, portNum));
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void revokeSecurityGroup(final String userId, String groupName, String protocol, int portNum) {
    final ComputeRevokeIngressRuleTask task = new ComputeRevokeIngressRuleTask(
        groupName, protocol, portNum);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(String.format(
            "failed to revoke:%s, %s, %d ", groupName, protocol, portNum));
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public List<DescribeKeyPairsResponseItemType> describeKeyPairs(
      final String userId, final List<String> keyNames) {
    final ComputeDescribeKeyPairsTask task = new ComputeDescribeKeyPairsTask(keyNames);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException("failed to describe keypairs");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public List<ImageDetails> describeImages(final String userId, final List<String> imageIds) {
    final ComputeDescribeImagesTask task = new ComputeDescribeImagesTask(imageIds);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return task.getResult();
      } else
        throw new EucalyptusActivityException("failed to describe keypairs");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void createTags(final String userId, final String tagKey, final String tagValue,
      final List<String> resources) {
    final ComputeCreateTagsTask task = new ComputeCreateTagsTask(tagKey, tagValue,
        resources);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to create tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public List<TagInfo> describeTags(final String userId, final List<String> names, final List<String> values){
    final DescribeTagsTask task =
        new DescribeTagsTask(names, values);
    final CheckedListenableFuture<Boolean> result = task.dispatch(new Ec2Context(userId));
    try{
      if(result.get()){
        return task.getTags();
      }else
        throw new EucalyptusActivityException("failed to describe tags");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void deleteTags(final String userId, final String tagKey, final String tagValue,
      final List<String> resources) {
    final ComputeDeleteTagsTask task = new ComputeDeleteTagsTask(tagKey, tagValue,
        resources);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new Ec2Context(userId));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to delete tags");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
    
  public List<Volume> describeVolumes(final String userId, final List<String> volumeIds){
    final DescribeVolumesTask task = new DescribeVolumesTask(volumeIds);
    final CheckedListenableFuture<Boolean> result = task.dispatch(new Ec2Context(userId));
    try{
      if(result.get()){
        return task.getVolumes();
      }else
        throw new EucalyptusActivityException("failed to describe volumes");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }
}
