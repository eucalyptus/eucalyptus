/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import com.eucalyptus.component.annotation.ComponentPart;
import java.util.Collection;

/**
 *
 */
@ComponentPart(Compute.class)
public interface ComputeApi {

  // API

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
