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


public class GetAccountLimitResponseType extends Route53Message {


  @Nonnull
  @FieldRange()
  private Long count;

  @Nonnull
  private AccountLimit limit;

  public Long getCount() {
    return count;
  }

  public void setCount(final Long count) {
    this.count = count;
  }

  public AccountLimit getLimit() {
    return limit;
  }

  public void setLimit(final AccountLimit limit) {
    this.limit = limit;
  }

}
