/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ElasticGpuSpecificationList extends EucalyptusData {

  private ArrayList<ElasticGpuSpecification> member = new ArrayList<ElasticGpuSpecification>();

  public ArrayList<ElasticGpuSpecification> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ElasticGpuSpecification> member ) {
    this.member = member;
  }
}
