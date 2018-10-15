/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ElasticGpuSpecificationResponseList extends EucalyptusData {

  private ArrayList<ElasticGpuSpecificationResponse> member = new ArrayList<ElasticGpuSpecificationResponse>();

  public ArrayList<ElasticGpuSpecificationResponse> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ElasticGpuSpecificationResponse> member ) {
    this.member = member;
  }
}
