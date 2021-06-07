/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class EC2SecurityGroupList extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "EC2SecurityGroup")
  private ArrayList<EC2SecurityGroup> member = new ArrayList<>();

  public ArrayList<EC2SecurityGroup> getMember() {
    return member;
  }

  public void setMember(final ArrayList<EC2SecurityGroup> member) {
    this.member = member;
  }
}
