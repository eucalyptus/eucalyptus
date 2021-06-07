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
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "GET", uri = "/2013-04-01/accountlimit/{Type}")
@HttpNoContent
public class GetAccountLimitType extends Route53Message {

  @Nonnull
  @HttpUriMapping(uri = "Type")
  @FieldRegex(FieldRegexValue.ENUM_ACCOUNTLIMITTYPE)
  private String type;

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
