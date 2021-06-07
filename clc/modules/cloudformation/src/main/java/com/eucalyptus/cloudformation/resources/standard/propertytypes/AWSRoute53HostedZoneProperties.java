/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class AWSRoute53HostedZoneProperties implements ResourceProperties {

  @Property
  @Required
  private String name;

  @Property
  private Route53HostedZoneConfig hostedZoneConfig;

  @Property(name = "VPCs")
  private ArrayList<Route53Vpc> vpcs = Lists.newArrayList();

  @Property
  private ArrayList<Route53HostedZoneTag> hostedZoneTags = Lists.newArrayList();

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Route53HostedZoneConfig getHostedZoneConfig() {
    return hostedZoneConfig;
  }

  public void setHostedZoneConfig(final Route53HostedZoneConfig hostedZoneConfig) {
    this.hostedZoneConfig = hostedZoneConfig;
  }

  public ArrayList<Route53Vpc> getVpcs() {
    return vpcs;
  }

  public void setVpcs(final ArrayList<Route53Vpc> vpcs) {
    this.vpcs = vpcs;
  }

  public ArrayList<Route53HostedZoneTag> getHostedZoneTags() {
    return hostedZoneTags;
  }

  public void setHostedZoneTags(final ArrayList<Route53HostedZoneTag> hostedZoneTags) {
    this.hostedZoneTags = hostedZoneTags;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("hostedZoneConfig", hostedZoneConfig)
        .add("vpcs", vpcs)
        .add("hostedZoneTags", hostedZoneTags)
        .toString();
  }
}
