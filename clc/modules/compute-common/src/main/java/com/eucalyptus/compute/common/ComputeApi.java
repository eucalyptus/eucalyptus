/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

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
}
