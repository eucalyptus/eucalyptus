/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeTagsResult extends EucalyptusData {

  private TagDescriptions tagDescriptions;

  public TagDescriptions getTagDescriptions() {
    return tagDescriptions;
  }

  public void setTagDescriptions(final TagDescriptions tagDescriptions) {
    this.tagDescriptions = tagDescriptions;
  }

}
