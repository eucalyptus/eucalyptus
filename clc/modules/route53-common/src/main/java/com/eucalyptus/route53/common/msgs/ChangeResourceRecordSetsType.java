/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/hostedzone/{Id}/rrset/")
public class ChangeResourceRecordSetsType extends Route53Message {

  @Nonnull
  private ChangeBatch changeBatch;

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(max = 32)
  private String hostedZoneId;

  public ChangeBatch getChangeBatch() {
    return changeBatch;
  }

  public void setChangeBatch(final ChangeBatch changeBatch) {
    this.changeBatch = changeBatch;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

}
