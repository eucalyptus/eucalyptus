/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;


public class UpdateHostedZoneCommentResponseType extends Route53Message {


  @Nonnull
  private HostedZone hostedZone;

  public HostedZone getHostedZone() {
    return hostedZone;
  }

  public void setHostedZone(final HostedZone hostedZone) {
    this.hostedZone = hostedZone;
  }

}
