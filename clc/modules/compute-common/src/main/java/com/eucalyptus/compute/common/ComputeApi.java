/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import com.eucalyptus.component.annotation.ComponentPart;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
@ComponentPart(Compute.class)
public interface ComputeApi {

  // API

  DescribeInstancesResponseType describeInstances( DescribeInstancesType request );

  AllocateAddressResponseType allocateAddress( AllocateAddressType request );

  AssociateAddressResponseType associateAddress( AssociateAddressType request );

  DescribeAddressesResponseType describeAddresses( DescribeAddressesType request );

  ReleaseAddressResponseType releaseAddress( ReleaseAddressType request );

  CreateNetworkInterfaceResponseType createNetworkInterface( CreateNetworkInterfaceType request );

  AttachNetworkInterfaceResponseType attachNetworkInterface( AttachNetworkInterfaceType request );

  DetachNetworkInterfaceResponseType detachNetworkInterface( DetachNetworkInterfaceType request );

  DeleteNetworkInterfaceResponseType deleteNetworkInterface( DeleteNetworkInterfaceType request );

  DescribeNetworkInterfacesResponseType describeNetworkInterfaces( DescribeNetworkInterfacesType request );

  ModifyNetworkInterfaceAttributeResponseType modifyNetworkInterfaceAttribute( ModifyNetworkInterfaceAttributeType request );

  CreateTagsResponseType createTags( CreateTagsType request );

  DeleteTagsResponseType deleteTags( DeleteTagsType request );

  DescribeTagsResponseType describeTags( DescribeTagsType request );

  ModifyInstanceTypeAttributeResponseType modifyInstanceType( ModifyInstanceTypeAttributeType request );

  CreateLaunchTemplateResponseType createLaunchTemplate( CreateLaunchTemplateType request );

  DeleteLaunchTemplateResponseType deleteLaunchTemplate( DeleteLaunchTemplateType request );

  DescribeSecurityGroupsResponseType describeSecurityGroups( DescribeSecurityGroupsType request );

  DescribeSubnetsResponseType describeSubnets( DescribeSubnetsType request );

  DescribeVpcsResponseType describeVpcs( DescribeVpcsType request );

  // Helpers

  default DescribeInstancesResponseType describeInstances() {
    return describeInstances(new DescribeInstancesType());
  }

  default DescribeInstancesResponseType describeInstances(final Filter... filters) {
    final DescribeInstancesType request = new DescribeInstancesType();
    for (final Filter filter : filters) {
      request.getFilterSet().add(filter);
    }
    return describeInstances(request);
  }

  default DescribeAddressesResponseType describeAddresses() {
    return describeAddresses(new DescribeAddressesType());
  }

  default DescribeAddressesResponseType describeAddresses(final Filter... filters) {
    final DescribeAddressesType request = new DescribeAddressesType();
    for (final Filter filter : filters) {
      request.getFilterSet().add(filter);
    }
    return describeAddresses(request);
  }

  default AllocateAddressResponseType allocateAddress(final String domain) {
    return allocateAddress(domain, Collections.emptyMap());
  }

  default AllocateAddressResponseType allocateAddress(final String domain, final Map<String,String> tags) {
    final AllocateAddressType request = new AllocateAddressType();
    request.setDomain(domain);
    if (tags != null && !tags.isEmpty()) {
      final ResourceTagSpecification specification = new ResourceTagSpecification();
      specification.setResourceType("elastic-ip");
      for (final Map.Entry<String, String> entry : tags.entrySet()) {
        specification.getTagSet().add(new ResourceTag(entry.getKey(), entry.getValue()));
      }
      request.getTagSpecification().add(specification);
    }
    return allocateAddress(request);
  }

  default ReleaseAddressResponseType releaseAddressByAllocationId(final String allocationId){
    final ReleaseAddressType releaseAddress = new ReleaseAddressType();
    releaseAddress.setAllocationId(allocationId);
    return releaseAddress(releaseAddress);
  }

  default DescribeNetworkInterfacesResponseType describeNetworkInterfaces() {
    return describeNetworkInterfaces(new DescribeNetworkInterfacesType());
  }

  default AttachNetworkInterfaceResponseType attachNetworkInterface(
      final String instanceId,
      final int deviceIndex,
      final String networkInterfaceId
  ) {
    final AttachNetworkInterfaceType request = new AttachNetworkInterfaceType();
    request.setInstanceId(instanceId);
    request.setDeviceIndex(deviceIndex);
    request.setNetworkInterfaceId(networkInterfaceId);
    return attachNetworkInterface(request);
  }

  default DescribeSecurityGroupsResponseType describeSecurityGroups() {
    return describeSecurityGroups(new DescribeSecurityGroupsType());
  }

  default DescribeSecurityGroupsResponseType describeSecurityGroups(final String groupId) {
    return describeSecurityGroups( filter("group-id", groupId) );
  }

  default DescribeSecurityGroupsResponseType describeSecurityGroups(final Collection<String> groupIds) {
    return describeSecurityGroups( filter("group-id", groupIds) );
  }

  default DescribeSecurityGroupsResponseType describeSecurityGroups(final Filter... filters) {
    final DescribeSecurityGroupsType request = new DescribeSecurityGroupsType();
    for (final Filter filter : filters) {
      request.getFilterSet().add(filter);
    }
    return describeSecurityGroups(request);
  }

  default DescribeSubnetsResponseType describeSubnets() {
    return describeSubnets(new DescribeSubnetsType());
  }

  default DescribeSubnetsResponseType describeSubnets(final String subnetId) {
    return describeSubnets( filter("subnet-id", subnetId) );
  }

  default DescribeSubnetsResponseType describeSubnets(final Collection<String> subnetIds) {
    return describeSubnets( filter("subnet-id", subnetIds) );
  }

  default DescribeSubnetsResponseType describeSubnets(final Filter... filters) {
    final DescribeSubnetsType request = new DescribeSubnetsType();
    for (final Filter filter : filters) {
      request.getFilterSet().add(filter);
    }
    return describeSubnets(request);
  }

  default DescribeVpcsResponseType describeVpcs() {
    return describeVpcs(new DescribeVpcsType());
  }

  default DescribeVpcsResponseType describeVpcs(final String vpcId) {
    return describeVpcs( filter("vpc-id", vpcId) );
  }

  default DescribeVpcsResponseType describeVpcs(final Collection<String> vpcIds) {
    return describeVpcs( filter("vpc-id", vpcIds) );
  }

  default DescribeVpcsResponseType describeVpcs(final Filter... filters) {
    final DescribeVpcsType request = new DescribeVpcsType();
    for (final Filter filter : filters) {
      request.getFilterSet().add(filter);
    }
    return describeVpcs(request);
  }

  static Filter filter(final String name, final String value) {
    final Filter filter = new Filter();
    filter.setName(name);
    filter.getValueSet().add(value);
    return filter;
  }

  static Filter filter(final String name, final Collection<String> values) {
    final Filter filter = new Filter();
    filter.setName(name);
    filter.getValueSet().addAll(values);
    return filter;
  }
}
