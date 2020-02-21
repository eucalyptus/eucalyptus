/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpHeaderMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


public class CreateHostedZoneResponseType extends Route53Message {


  @Nonnull
  private ChangeInfo changeInfo;

  @Nonnull
  private DelegationSet delegationSet;

  @Nonnull
  private HostedZone hostedZone;

  @Nonnull
  @HttpHeaderMapping(header = "Location")
  @FieldRange(max = 1024)
  private String location;

  private VPC vPC;

  public ChangeInfo getChangeInfo() {
    return changeInfo;
  }

  public void setChangeInfo(final ChangeInfo changeInfo) {
    this.changeInfo = changeInfo;
  }

  public DelegationSet getDelegationSet() {
    return delegationSet;
  }

  public void setDelegationSet(final DelegationSet delegationSet) {
    this.delegationSet = delegationSet;
  }

  public HostedZone getHostedZone() {
    return hostedZone;
  }

  public void setHostedZone(final HostedZone hostedZone) {
    this.hostedZone = hostedZone;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  public VPC getVPC() {
    return vPC;
  }

  public void setVPC(final VPC vPC) {
    this.vPC = vPC;
  }

}
