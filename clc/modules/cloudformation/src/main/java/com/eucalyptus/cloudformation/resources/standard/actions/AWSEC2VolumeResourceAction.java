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


import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VolumeResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VolumeProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateVolumeResponseType;
import com.eucalyptus.compute.common.CreateVolumeType;
import com.eucalyptus.compute.common.DeleteVolumeResponseType;
import com.eucalyptus.compute.common.DeleteVolumeType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VolumeResourceAction extends ResourceAction {

  private AWSEC2VolumeProperties properties = new AWSEC2VolumeProperties();
  private AWSEC2VolumeResourceInfo info = new AWSEC2VolumeResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2VolumeProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VolumeResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create volume
        CreateVolumeType createVolumeType = new CreateVolumeType();
        createVolumeType.setAvailabilityZone(properties.getAvailabilityZone());
        if (properties.getIops() != null) {
          createVolumeType.setIops(properties.getIops());
        }
        if (properties.getSize() != null) {
          createVolumeType.setSize(properties.getSize());
        }
        if (properties.getSnapshotId() != null) {
          createVolumeType.setSnapshotId(properties.getSnapshotId());
        }
        if (properties.getVolumeType() != null) {
          createVolumeType.setVolumeType(properties.getVolumeType());
        } else {
          createVolumeType.setVolumeType("standard");
        }

        createVolumeType.setEffectiveUserId(info.getEffectiveUserId());
        CreateVolumeResponseType createVolumeResponseType = AsyncRequests.<CreateVolumeType,CreateVolumeResponseType> sendSync(configuration, createVolumeType);
        info.setPhysicalResourceId(createVolumeResponseType.getVolume().getVolumeId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // wait until available
        boolean available = false;
        for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
          Thread.sleep(5000L);
          DescribeVolumesType describeVolumesType = new DescribeVolumesType();
          describeVolumesType.setVolumeSet(Lists.newArrayList(info.getPhysicalResourceId()));
          describeVolumesType.setEffectiveUserId(info.getEffectiveUserId());
          DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
          if (describeVolumesResponseType.getVolumeSet().size()==0) continue;
          if ("available".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
            available = true;
            break;
          }
        }
        if (!available) {
          throw new Exception("Timeout");
        }
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
    // First see if instance exists or has been deleted
    DescribeVolumesType describeVolumesType = new DescribeVolumesType();
    describeVolumesType.setVolumeSet(Lists.newArrayList(info.getPhysicalResourceId()));
    describeVolumesType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
    if (describeVolumesResponseType.getVolumeSet().size() == 0) return; // already deleted
    if ("deleted".equals(
      describeVolumesResponseType.getVolumeSet().get(0).getStatus())) return;

    DeleteVolumeType deleteVolumeType = new DeleteVolumeType();
    deleteVolumeType.setVolumeId(info.getPhysicalResourceId());
    deleteVolumeType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteVolumeType,DeleteVolumeResponseType> sendSync(configuration, deleteVolumeType);
    boolean deleted = false;
    for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
      Thread.sleep(5000L);
      DescribeVolumesType describeVolumesType2 = new DescribeVolumesType();
      describeVolumesType2.setVolumeSet(Lists.newArrayList(info.getPhysicalResourceId()));
      describeVolumesType2.setEffectiveUserId(info.getEffectiveUserId());
      DescribeVolumesResponseType describeVolumesResponseType2 = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType2);
      if (describeVolumesResponseType2.getVolumeSet().size() == 0) {
        deleted = true;
        break;
      }
      if ("deleted".equals(describeVolumesResponseType2.getVolumeSet().get(0).getStatus())) {
        deleted = true;
        break;
      }
    }
    if (!deleted) throw new Exception("Timeout");
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


