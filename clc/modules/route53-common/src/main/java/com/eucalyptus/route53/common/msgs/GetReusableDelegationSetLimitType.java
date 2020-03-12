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


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/reusabledelegationsetlimit/{Id}/{Type}")
@HttpNoContent
public class GetReusableDelegationSetLimitType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Id")
  @FieldRange(max = 32)
  private String delegationSetId;

  @Nonnull
  @HttpUriMapping(uri = "Type")
  @FieldRegex(FieldRegexValue.ENUM_REUSABLEDELEGATIONSETLIMITTYPE)
  private String type;

  public String getDelegationSetId() {
    return delegationSetId;
  }

  public void setDelegationSetId(final String delegationSetId) {
    this.delegationSetId = delegationSetId;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
