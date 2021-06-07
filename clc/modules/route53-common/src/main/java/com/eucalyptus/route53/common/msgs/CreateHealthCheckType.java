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
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/healthcheck")
public class CreateHealthCheckType extends Route53Message {

  @Nonnull
  @FieldRange(min = 1, max = 64)
  private String callerReference;

  @Nonnull
  private HealthCheckConfig healthCheckConfig;

  public String getCallerReference() {
    return callerReference;
  }

  public void setCallerReference(final String callerReference) {
    this.callerReference = callerReference;
  }

  public HealthCheckConfig getHealthCheckConfig() {
    return healthCheckConfig;
  }

  public void setHealthCheckConfig(final HealthCheckConfig healthCheckConfig) {
    this.healthCheckConfig = healthCheckConfig;
  }

}
