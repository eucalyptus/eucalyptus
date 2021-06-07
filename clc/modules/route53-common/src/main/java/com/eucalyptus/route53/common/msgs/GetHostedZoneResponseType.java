/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


public class GetHostedZoneResponseType extends Route53Message {


  private DelegationSet delegationSet;

  @Nonnull
  private HostedZone hostedZone;

  @FieldRange(min = 1)
  private VPCs vPCs;

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

  public VPCs getVPCs() {
    return vPCs;
  }

  public void setVPCs(final VPCs vPCs) {
    this.vPCs = vPCs;
  }

}
