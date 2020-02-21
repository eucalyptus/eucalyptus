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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HealthCheck extends EucalyptusData {

  @Nonnull
  @FieldRange(min = 1, max = 64)
  private String callerReference;

  private CloudWatchAlarmConfiguration cloudWatchAlarmConfiguration;

  @Nonnull
  private HealthCheckConfig healthCheckConfig;

  @Nonnull
  @FieldRange(min = 1)
  private Long healthCheckVersion;

  @Nonnull
  @FieldRange(max = 64)
  private String id;

  private LinkedService linkedService;

  public String getCallerReference() {
    return callerReference;
  }

  public void setCallerReference(final String callerReference) {
    this.callerReference = callerReference;
  }

  public CloudWatchAlarmConfiguration getCloudWatchAlarmConfiguration() {
    return cloudWatchAlarmConfiguration;
  }

  public void setCloudWatchAlarmConfiguration(final CloudWatchAlarmConfiguration cloudWatchAlarmConfiguration) {
    this.cloudWatchAlarmConfiguration = cloudWatchAlarmConfiguration;
  }

  public HealthCheckConfig getHealthCheckConfig() {
    return healthCheckConfig;
  }

  public void setHealthCheckConfig(final HealthCheckConfig healthCheckConfig) {
    this.healthCheckConfig = healthCheckConfig;
  }

  public Long getHealthCheckVersion() {
    return healthCheckVersion;
  }

  public void setHealthCheckVersion(final Long healthCheckVersion) {
    this.healthCheckVersion = healthCheckVersion;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public LinkedService getLinkedService() {
    return linkedService;
  }

  public void setLinkedService(final LinkedService linkedService) {
    this.linkedService = linkedService;
  }

}
