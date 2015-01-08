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
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VolumeAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VolumeAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AllocateAddressType;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.AttachVolumeResponseType;
import com.eucalyptus.compute.common.AttachVolumeType;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.DetachVolumeResponseType;
import com.eucalyptus.compute.common.DetachVolumeType;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VolumeAttachmentResourceAction extends ResourceAction {

  private AWSEC2VolumeAttachmentProperties properties = new AWSEC2VolumeAttachmentProperties();
  private AWSEC2VolumeAttachmentResourceInfo info = new AWSEC2VolumeAttachmentResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2VolumeAttachmentProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VolumeAttachmentResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    switch (stepNum) {
      case 0:
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AttachVolumeType attachVolumeType = new AttachVolumeType();
        attachVolumeType.setEffectiveUserId(info.getEffectiveUserId());
        DescribeInstancesType describeInstancesType = new DescribeInstancesType();
        describeInstancesType.setInstancesSet(Lists.newArrayList(properties.getInstanceId()));
        describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
        if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
          throw new ValidationErrorException("No such instance " + properties.getInstanceId());
        }
        attachVolumeType.setInstanceId(properties.getInstanceId());
        DescribeVolumesType describeVolumesType = new DescribeVolumesType();
        describeVolumesType.setVolumeSet(Lists.newArrayList(properties.getVolumeId()));
        describeVolumesType.setEffectiveUserId(info.getEffectiveUserId());
        DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
        if (describeVolumesResponseType.getVolumeSet().size()==0) throw new ValidationErrorException("No such volume " + properties.getVolumeId());
        if (!"available".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
          throw new ValidationErrorException("Volume " + properties.getVolumeId() + " not available");
        }
        attachVolumeType.setVolumeId(properties.getVolumeId());
        attachVolumeType.setDevice(properties.getDevice());
        AsyncRequests.<AttachVolumeType, AttachVolumeResponseType> sendSync(configuration, attachVolumeType);
        boolean attached = false;
        for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
          Thread.sleep(5000L);
          DescribeVolumesType describeVolumesType2 = new DescribeVolumesType();
          // TODO: issue below, should not be info.getPhysicalResourceId() but the volume id... DUH!  (then run test again)
          describeVolumesType2.setVolumeSet(Lists.newArrayList(properties.getVolumeId()));
          describeVolumesType2.setEffectiveUserId(info.getEffectiveUserId());
          DescribeVolumesResponseType describeVolumesResponseType2 = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType2);
          if (describeVolumesResponseType2.getVolumeSet().size() == 0) continue;
          if (describeVolumesResponseType2.getVolumeSet().get(0).getAttachmentSet() == null || describeVolumesResponseType2.getVolumeSet().get(0).getAttachmentSet().isEmpty()) continue;
          for (AttachedVolume attachedVolume: describeVolumesResponseType2.getVolumeSet().get(0).getAttachmentSet()) {
            if (attachedVolume.getInstanceId().equals(properties.getInstanceId()) && attachedVolume.getDevice().equals(properties.getDevice()) && attachedVolume.getStatus().equals("attached")) {
              attached = true;
              break;
            }
          }
          if (attached == true) break;
        }
        if (!attached) throw new Exception("Timeout");
        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DetachVolumeType detachVolumeType = new DetachVolumeType();
    detachVolumeType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeInstancesType describeInstancesType = new DescribeInstancesType();
    describeInstancesType.setInstancesSet(Lists.newArrayList(properties.getInstanceId()));
    describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
    if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
      return; // can't be attached to a non-existant instance;
    }
    detachVolumeType.setInstanceId(properties.getInstanceId());
    DescribeVolumesType describeVolumesType = new DescribeVolumesType();
    describeVolumesType.setVolumeSet(Lists.newArrayList(properties.getVolumeId()));
    describeVolumesType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
    if (describeVolumesResponseType.getVolumeSet().size()==0) return;
    detachVolumeType.setVolumeId(properties.getVolumeId());
    detachVolumeType.setDevice(properties.getDevice());
    AsyncRequests.<DetachVolumeType, DetachVolumeResponseType> sendSync(configuration, detachVolumeType);
    boolean detached = false;
    for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
      Thread.sleep(5000L);
      DescribeVolumesType describeVolumesType2 = new DescribeVolumesType();
      describeVolumesType2.setVolumeSet(Lists.newArrayList(properties.getVolumeId()));
      describeVolumesType2.setEffectiveUserId(info.getEffectiveUserId());
      DescribeVolumesResponseType describeVolumesResponseType2 = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType2);
      if (describeVolumesResponseType2.getVolumeSet().size() == 0) return;
      if (describeVolumesResponseType2.getVolumeSet().get(0).getAttachmentSet() == null || describeVolumesResponseType2.getVolumeSet().get(0).getAttachmentSet().isEmpty()) return;
      for (AttachedVolume attachedVolume: describeVolumesResponseType2.getVolumeSet().get(0).getAttachmentSet()) {
        if (attachedVolume.getInstanceId().equals(properties.getInstanceId()) && attachedVolume.getDevice().equals(properties.getDevice()) && attachedVolume.getStatus().equals("detached")) {
          detached = true;
          break;
        }
      }
      if (detached == true) break;
    }
    if (!detached) throw new Exception("Timeout");
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


