/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common;

import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.util.techpreview.TechPreview;

/**
 *
 */
@PublicService
@AwsServiceName("rds")
@PolicyVendor("rds")
@Partition(value = Rds.class, manyToOne = true)
@Description("Amazon RDS API service")
@TechPreview(enableByDefaultProperty = "enable.rds.tech.preview")
public class Rds extends ComponentId {

  private static final long serialVersionUID = 1L;
}
