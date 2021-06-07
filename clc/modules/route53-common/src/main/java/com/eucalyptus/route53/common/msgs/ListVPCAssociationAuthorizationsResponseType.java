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


public class ListVPCAssociationAuthorizationsResponseType extends Route53Message {


  @Nonnull
  @FieldRange(max = 32)
  private String hostedZoneId;

  @FieldRange(max = 256)
  private String nextToken;

  @Nonnull
  @FieldRange(min = 1)
  private VPCs vPCs;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

  public VPCs getVPCs() {
    return vPCs;
  }

  public void setVPCs(final VPCs vPCs) {
    this.vPCs = vPCs;
  }

}
