/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpHeaderMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;


public class CreateHealthCheckResponseType extends Route53Message {


  @Nonnull
  private HealthCheck healthCheck;

  @Nonnull
  @HttpHeaderMapping(header = "Location")
  @FieldRange(max = 1024)
  private String location;

  public HealthCheck getHealthCheck() {
    return healthCheck;
  }

  public void setHealthCheck(final HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

}
