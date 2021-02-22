/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.Collection;
import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart(Compute.class)
public interface ComputeApi {

  CreateTagsResponseType createTags( CreateTagsType request );

  DeleteTagsResponseType deleteTags( DeleteTagsType request );

  DescribeTagsResponseType describeTags( DescribeTagsType request );

  ModifyInstanceTypeAttributeResponseType modifyInstanceType( ModifyInstanceTypeAttributeType request );

  CreateLaunchTemplateResponseType createLaunchTemplate( CreateLaunchTemplateType request );

  DeleteLaunchTemplateResponseType deleteLaunchTemplate( DeleteLaunchTemplateType request );

  DescribeSubnetsResponseType describeSubnets( DescribeSubnetsType request );

  default DescribeSubnetsResponseType describeSubnets( final Collection<String> subnetIds ) {
    final DescribeSubnetsType describeSubnetsType = new DescribeSubnetsType();
    final SubnetIdSetType subnetIdSetType = new SubnetIdSetType();
    for ( final String subnetId : subnetIds ) {
      final SubnetIdSetItemType item = new SubnetIdSetItemType();
      item.setSubnetId( subnetId );
      subnetIdSetType.getItem().add( item );
    }
    describeSubnetsType.setSubnetSet(subnetIdSetType);
    return describeSubnets( describeSubnetsType );
  }
}
