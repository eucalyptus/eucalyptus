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


public class CreateVPCAssociationAuthorizationResponseType extends Route53Message {


  @Nonnull
  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  private VPC vPC;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public VPC getVPC() {
    return vPC;
  }

  public void setVPC(final VPC vPC) {
    this.vPC = vPC;
  }

}
