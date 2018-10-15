/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class ElasticGpuSet extends EucalyptusData {

  private ArrayList<ElasticGpus> member = new ArrayList<ElasticGpus>();

  public ArrayList<ElasticGpus> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<ElasticGpus> member ) {
    this.member = member;
  }
}
