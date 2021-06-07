/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpNoContent;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/hostedzonelimit/{Id}/{Type}")
@HttpNoContent
public class GetHostedZoneLimitType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  @HttpUriMapping(uri = "Type")
  @FieldRegex(FieldRegexValue.ENUM_HOSTEDZONELIMITTYPE)
  private String type;

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
