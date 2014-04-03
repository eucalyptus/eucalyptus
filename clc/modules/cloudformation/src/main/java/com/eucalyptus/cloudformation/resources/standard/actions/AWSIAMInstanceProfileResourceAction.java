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


import com.eucalyptus.auth.euare.AddRoleToInstanceProfileResponseType;
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileResponseType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.ListInstanceProfilesType;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMInstanceProfileResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMInstanceProfileProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMInstanceProfileResourceAction extends ResourceAction {

  private AWSIAMInstanceProfileProperties properties = new AWSIAMInstanceProfileProperties();
  private AWSIAMInstanceProfileResourceInfo info = new AWSIAMInstanceProfileResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSIAMInstanceProfileProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMInstanceProfileResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    switch (stepNum) {
      case 0: // create instance profile
        String instanceProfileName = getDefaultPhysicalResourceId();
        CreateInstanceProfileType createInstanceProfileType = new CreateInstanceProfileType();
        createInstanceProfileType.setEffectiveUserId(info.getEffectiveUserId());
        createInstanceProfileType.setPath(properties.getPath());
        createInstanceProfileType.setInstanceProfileName(instanceProfileName);
        CreateInstanceProfileResponseType createInstanceProfileResponseType = AsyncRequests.<CreateInstanceProfileType,CreateInstanceProfileResponseType> sendSync(configuration, createInstanceProfileType);
        String arn = createInstanceProfileResponseType.getCreateInstanceProfileResult().getInstanceProfile().getArn();
        info.setPhysicalResourceId(instanceProfileName);
        info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // add policies
        if (properties.getRoles() != null) {
          for (String roleName: properties.getRoles()) {
            AddRoleToInstanceProfileType addRoleToInstanceProfileType = new AddRoleToInstanceProfileType();
            addRoleToInstanceProfileType.setEffectiveUserId(info.getEffectiveUserId());
            addRoleToInstanceProfileType.setInstanceProfileName(info.getPhysicalResourceId());
            addRoleToInstanceProfileType.setRoleName(roleName);
            AsyncRequests.<AddRoleToInstanceProfileType,AddRoleToInstanceProfileResponseType> sendSync(configuration, addRoleToInstanceProfileType);
          }
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
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    // if no instance profile, bye...
    boolean seenAllInstanceProfiles = false;
    boolean foundInstanceProfile = false;
    String instanceProfileMarker = null;
    while (!seenAllInstanceProfiles && !foundInstanceProfile) {
      ListInstanceProfilesType listInstanceProfilesType = new ListInstanceProfilesType();
      listInstanceProfilesType.setEffectiveUserId(info.getEffectiveUserId());
      if (instanceProfileMarker != null) {
        listInstanceProfilesType.setMarker(instanceProfileMarker);
      }
      ListInstanceProfilesResponseType listInstanceProfilesResponseType = AsyncRequests.<ListInstanceProfilesType,ListInstanceProfilesResponseType> sendSync(configuration, listInstanceProfilesType);
      if (listInstanceProfilesResponseType.getListInstanceProfilesResult().getIsTruncated() == Boolean.TRUE) {
        instanceProfileMarker = listInstanceProfilesResponseType.getListInstanceProfilesResult().getMarker();
      } else {
        seenAllInstanceProfiles = true;
      }
      if (listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles() != null && listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles().getMember() != null) {
        for (InstanceProfileType instanceProfileType: listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles().getMember()) {
          if (instanceProfileType.getInstanceProfileName().equals(info.getPhysicalResourceId())) {
            foundInstanceProfile = true;
            break;
          }
        }
      }
    }
    // we can delete the instance profile without detaching the role

    if (!foundInstanceProfile) return;

    DeleteInstanceProfileType deleteInstanceProfileType = new DeleteInstanceProfileType();
    deleteInstanceProfileType.setInstanceProfileName(info.getPhysicalResourceId());
    deleteInstanceProfileType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteInstanceProfileType,DeleteInstanceProfileResponseType> sendSync(configuration, deleteInstanceProfileType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


