/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;


public class GetHealthCheckCountResponseType extends Route53Message {


  @Nonnull
  private Long healthCheckCount;

  public Long getHealthCheckCount() {
    return healthCheckCount;
  }

  public void setHealthCheckCount(final Long healthCheckCount) {
    this.healthCheckCount = healthCheckCount;
  }

}
