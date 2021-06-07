/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class TrafficPolicyInstances extends EucalyptusData {

  @HttpEmbedded(multiple = true)
  @HttpParameterMapping(parameter = "TrafficPolicyInstance")
  private ArrayList<TrafficPolicyInstance> member = new ArrayList<>();

  public ArrayList<TrafficPolicyInstance> getMember() {
    return member;
  }

  public void setMember(final ArrayList<TrafficPolicyInstance> member) {
    this.member = member;
  }
}
