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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2EIPAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2EIPAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.AllocateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AllocateAddressType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateAddressType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressResponseType;
import edu.ucsb.eucalyptus.msgs.ReleaseAddressType;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2EIPAssociationResourceAction extends ResourceAction {

  private AWSEC2EIPAssociationProperties properties = new AWSEC2EIPAssociationProperties();
  private AWSEC2EIPAssociationResourceInfo info = new AWSEC2EIPAssociationResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2EIPAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2EIPAssociationResourceInfo) resourceInfo;
  }

  @Override
  public void create() throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);
    AllocateAddressType allocateAddressType = new AllocateAddressType();
    allocateAddressType.setEffectiveUserId(info.getEffectiveUserId());
    AllocateAddressResponseType allocateAddressResponseType = AsyncRequests.<AllocateAddressType, AllocateAddressResponseType> sendSync(configuration, allocateAddressType);
    AssociateAddressType associateAddressType = new AssociateAddressType();
    if (properties.getInstanceId() != null) {
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(properties.getInstanceId()));
      describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
        throw new ValidationErrorException("No such instance " + properties.getInstanceId());
      }
      associateAddressType.setInstanceId(properties.getInstanceId());
    }
    if (properties.getEip() != null) {
      DescribeAddressesType describeAddressesType = new DescribeAddressesType();
      describeAddressesType.setPublicIpsSet(Lists.newArrayList(properties.getEip()));
      describeAddressesType.setEffectiveUserId(info.getEffectiveUserId());
      DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
      if (describeAddressesResponseType.getAddressesSet() == null || describeAddressesResponseType.getAddressesSet().isEmpty()) {
        throw new ValidationErrorException("No such EIP " + properties.getEip());
      }
      associateAddressType.setPublicIp(properties.getEip());
    }
    AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);
    info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getLogicalResourceId())));
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);
    if (properties.getEip() != null) {
      DescribeAddressesType describeAddressesType = new DescribeAddressesType();
      describeAddressesType.setPublicIpsSet(Lists.newArrayList(properties.getEip()));
      describeAddressesType.setEffectiveUserId(info.getEffectiveUserId());
      DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
      if (describeAddressesResponseType.getAddressesSet() != null && !describeAddressesResponseType.getAddressesSet().isEmpty()) {
        DisassociateAddressType disassociateAddressType = new DisassociateAddressType();
        disassociateAddressType.setPublicIp(properties.getEip());
        disassociateAddressType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<DisassociateAddressType, DisassociateAddressResponseType> sendSync(configuration, disassociateAddressType);
      }
    }
  }

  @Override
  public void rollback() throws Exception {
    delete();
  }

}


